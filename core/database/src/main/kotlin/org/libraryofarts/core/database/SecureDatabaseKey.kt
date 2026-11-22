package org.libraryofarts.core.database

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.libraryofarts.core.common.Crypto
import java.security.KeyStore

/**
 * The database passphrase is random, generated on first launch and wrapped by
 * an AES-256-GCM key held in the Android Keystore. It is never logged, backed
 * up in plaintext or sent anywhere.
 */
class SecureDatabaseKey(private val context: Context) {
    private val prefs by lazy {
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
    }

    fun passphrase(): String {
        prefs.getString(KEY_DB_PASSPHRASE, null)?.let { return it }
        val generated = Crypto.randomPassphrase(32)
        prefs.edit().putString(KEY_DB_PASSPHRASE, generated).apply()
        return generated
    }

    fun clearAllOnReset() {
        prefs.edit().clear().apply()
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry("_androidx_security_master_key_")
        }
    }

    private companion object {
        const val KEY_DB_PASSPHRASE = "db_passphrase_v1"
    }
}
