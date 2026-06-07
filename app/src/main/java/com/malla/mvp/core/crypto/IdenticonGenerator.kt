package com.malla.mvp.core.crypto

import java.security.MessageDigest

object IdenticonGenerator {
    fun generate(pubKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pubKeyBytes)

        val r = hash[0].toInt() and 0xFF
        val g = hash[1].toInt() and 0xFF
        val b = hash[2].toInt() and 0xFF
        val mainColor = "#%02x%02x%02x".format(r, g, b)

        val grid = Array(5) { row ->
            BooleanArray(5) { col ->
                val mirrorCol = if (col > 2) 4 - col else col
                val byteIdx = (row * 3 + mirrorCol) % hash.size
                (hash[byteIdx].toInt() and 0x01) == 1
            }
        }

        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60">""")
        sb.append("""<rect width="60" height="60" fill="#0a0e14" rx="8"/>""")

        grid.forEachIndexed { row, cols ->
            cols.forEachIndexed { col, filled ->
                if (filled) {
                    val x = col * 10 + 5
                    val y = row * 10 + 5
                    sb.append("""<rect x="$x" y="$y" width="8" height="8" fill="$mainColor" rx="1"/>""")
                }
            }
        }
        sb.append("</svg>")
        return sb.toString()
    }
}
