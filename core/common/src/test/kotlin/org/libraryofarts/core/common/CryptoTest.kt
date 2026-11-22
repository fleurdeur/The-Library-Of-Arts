package org.libraryofarts.core.common

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoTest {
    @Test
    fun `backup round trip`() {
        val plain = "A private journal entry about a Vermeer.".toByteArray()
        val encrypted = Crypto.encryptBackup(plain, "correct horse battery".toCharArray())
        assertNotEquals(plain.toList(), encrypted.toList())
        val decrypted = Crypto.decryptBackup(encrypted, "correct horse battery".toCharArray())
        assertArrayEquals(plain, decrypted)
    }

    @Test(expected = Exception::class)
    fun `wrong passphrase fails`() {
        val encrypted = Crypto.encryptBackup("secret".toByteArray(), "passphrase-one".toCharArray())
        Crypto.decryptBackup(encrypted, "passphrase-two".toCharArray())
    }

    @Test
    fun `sha256 is stable`() {
        val digest = Crypto.sha256Hex("art".toByteArray())
        assertTrue(digest.length == 64 && digest.all { it in "0123456789abcdef" })
        assertTrue(Crypto.randomPassphrase().length > 20)
    }
}
