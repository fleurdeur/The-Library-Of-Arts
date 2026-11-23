package org.libraryofarts.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.libraryofarts.core.common.Crypto

/**
 * The database passphrase is random, generated on first launch, and wrapped by
 * an AES-256-GCM key held in the Android Keystore. It is never logged, backed
 * up in plaintext or sent anywhere.
 *
 * Two wrappers are supported. Existing installs keep their
 * EncryptedSharedPreferences key. New installs use a tiny direct Keystore
 * wrapper, which avoids a known class of startup failures in the deprecated
 * security-crypto library. If the Keystore itself is unavailable (some work
 * profiles and broken vendor keystores), the passphrase is kept in private,
 * no-backup app storage so the encrypted database still opens instead of the
 * app crashing on launch.
 */
class SecureDatabaseKey(private val context: Context) {
    @Volatile private var cached: String? = null

    private val legacyPrefs by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setUserAuthenticationRequired(false)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "loa_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull()
    }

    private val fallbackPrefs by lazy {
        context.getSharedPreferences("loa_secure_key_fallback", Context.MODE_PRIVATE)
    }

    @Synchronized
    fun passphrase(): String {
        cached?.let { return it }

        runCatching { legacyPrefs?.getString(KEY_DB_PASSPHRASE, null) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { existing ->
                cached = existing
                return existing
            }

        runCatching { readWrapped() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { existing ->
                cached = existing
                return existing
            }

        // Keystore wrapping can fail on some vendor devices. The private
        // fallback file is part of the key chain and must be read before a new
        // passphrase is generated, otherwise the next launch cannot open the
        // database that was just created.
        runCatching { fallbackPrefs.getString(KEY_FALLBACK, null) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { existing ->
                cached = existing
                return existing
            }

        // A database exists but none of its keys can be recovered. Keeping it
        // would make every launch fail with "The library could not be opened".
        // Rebuild a clean library instead; saved state cannot be recovered
        // once the Android Keystore has discarded the wrapping key.
        if (context.getDatabasePath(DB_NAME).exists()) {
            runCatching { context.deleteDatabase(DB_NAME) }
            runCatching { java.io.File(context.getDatabasePath(DB_NAME).path + "-wal").delete() }
            runCatching { java.io.File(context.getDatabasePath(DB_NAME).path + "-shm").delete() }
        }

        val generated = Crypto.randomPassphrase(32)
        val wrapped = runCatching { writeWrapped(generated) }.isSuccess
        if (!wrapped) {
            // Last-resort private storage: the database remains encrypted with
            // the random passphrase, but the wrapping key is the app sandbox.
            fallbackPrefs.edit().putString(KEY_FALLBACK, generated).apply()
        }
        cached = generated
        return generated
    }

    fun clearAllOnReset() {
        cached = null
        runCatching { fallbackPrefs.edit().clear().apply() }
        runCatching { legacyPrefs?.edit()?.clear()?.apply() }
        runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply() }
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                .deleteEntry(KEY_ALIAS)
                .also { }
        }
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                .deleteEntry("_androidx_security_master_key_")
        }
    }

    private fun readWrapped(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val ciphertext = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val key = existingKey() ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(128, Base64.getDecoder().decode(iv)),
        )
        val plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext))
        return String(plain, Charsets.UTF_8)
    }

    private fun writeWrapped(passphrase: String) {
        val key = existingKey() ?: generateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_IV, Base64.getEncoder().encodeToString(cipher.iv))
            .putString(KEY_CIPHERTEXT, Base64.getEncoder().encodeToString(ciphertext))
            .apply()
    }

    private fun existingKey(): SecretKey? = runCatching {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        store.getKey(KEY_ALIAS, null) as? SecretKey
    }.getOrNull()

    private fun generateKey(): SecretKey? = runCatching {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        generator.generateKey()
    }.getOrNull()

    companion object {
        const val DB_NAME = "library_of_arts.db"
        const val KEY_DB_PASSPHRASE = "db_passphrase_v1"
        const val PREFS_NAME = "loa_database_wrap"
        const val KEY_IV = "iv_v1"
        const val KEY_CIPHERTEXT = "ciphertext_v1"
        const val KEY_FALLBACK = "db_passphrase_fallback_v1"
        const val KEY_ALIAS = "loa_database_wrap_key_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
