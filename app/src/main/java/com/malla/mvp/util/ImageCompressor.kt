package com.malla.mvp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {
    fun compress(context: Context, uri: Uri, maxSizeKB: Int = 512): Uri {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        val file = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        var quality = 90
        do {
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out) }
            quality -= 10
        } while (file.length() > maxSizeKB * 1024 && quality > 10)
        return Uri.fromFile(file)
    }
}
