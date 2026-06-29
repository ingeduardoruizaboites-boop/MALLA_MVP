package com.malla.mvp.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileInputStream

object ImageSaver {
    fun saveToGallery(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return
            val inputStream = FileInputStream(file)
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MALLA")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { context.contentResolver.openOutputStream(it)?.use { outputStream -> inputStream.copyTo(outputStream) } }
            Toast.makeText(context, "Imagen guardada en galería", Toast.LENGTH_SHORT).show()
            inputStream.close()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
        }
    }
}
