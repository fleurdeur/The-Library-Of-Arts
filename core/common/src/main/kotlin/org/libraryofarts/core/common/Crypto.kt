package org.libraryofarts.core.common

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Local-only cryptography helpers:
 *  - random database passphrases (wrapped by the Android Keystore elsewhere),
 *  - AES-256-GCM backup files with a passphrase-derived key,
 *  - SHA-256 and ECDSA P-256 pack verification.
 */
object Crypto {
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12

    fun randomPassphrase(bytes: Int = 32): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return Base64.getEncoder().encodeToString(data)
    }

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    fun encryptBackup(plain: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain)
        return byteArrayOf(MAGIC) + salt + iv + encrypted
    }

    fun decryptBackup(payload: ByteArray, passphrase: CharArray): ByteArray {
        require(payload.size > 1 + SALT_BYTES + IV_BYTES && payload[0] == MAGIC) {
            "Not a Library of Arts backup"
        }
        val salt = payload.copyOfRange(1, 1 + SALT_BYTES)
        val iv = payload.copyOfRange(1 + SALT_BYTES, 1 + SALT_BYTES + IV_BYTES)
        val encrypted = payload.copyOfRange(1 + SALT_BYTES + IV_BYTES, payload.size)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    fun verifyEcdsaSha256(payload: ByteArray, signatureBase64: String, publicKeyBase64: String): Boolean {
        return try {
            val keyFactory = java.security.KeyFactory.getInstance("EC")
            val keySpec = java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64))
            val publicKey = keyFactory.generatePublic(keySpec)
            val signature = java.security.Signature.getInstance("SHA256withECDSA")
            signature.initVerify(publicKey)
            signature.update(payload)
            signature.verify(Base64.getDecoder().decode(signatureBase64))
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private const val MAGIC: Byte = 0x4C
}
