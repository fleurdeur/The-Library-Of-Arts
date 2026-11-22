package org.libraryofarts.core.common

import android.graphics.Bitmap

/**
 * A 64-bit difference hash for the on-device "Scan a Painting" identifier.
 * Hashes are shipped inside an offline pack, so scanning never needs a network
 * call and never leaves the device.
 */
object PerceptualHash {
    private const val WIDTH = 9
    private const val HEIGHT = 8

    fun dHash(bitmap: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, true)
        val gray = IntArray(WIDTH * HEIGHT)
        var i = 0
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val c = small.getPixel(x, y)
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                gray[i++] = (r * 299 + g * 587 + b * 114) / 1000
            }
        }
        var hash = 0L
        var bit = 0
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH - 1) {
                val left = gray[y * WIDTH + x]
                val right = gray[y * WIDTH + x + 1]
                if (left > right) hash = hash or (1L shl bit)
                bit++
            }
        }
        if (small !== bitmap) small.recycle()
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    fun toHex(hash: Long): String = "%016x".format(hash)

    fun fromHex(hex: String): Long = java.lang.Long.parseUnsignedLong(hex, 16)
}
