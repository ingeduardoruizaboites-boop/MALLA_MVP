package com.malla.mvp.identity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object IdentityManager {
    val deviceId: String = java.util.UUID.randomUUID().toString().take(8)
    private const val USER_NAME_KEY = "user_name"
    private const val USER_STATUS_KEY = "user_status"
    private const val AVATAR_FILE = "avatar.jpg"
    private const val BANNER_FILE = "banner.jpg"

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences("identity", Context.MODE_PRIVATE)
        return prefs.getString(USER_NAME_KEY, "Usuario Malla") ?: "Usuario Malla"
    }

    fun setUserName(context: Context, name: String) {
        context.getSharedPreferences("identity", Context.MODE_PRIVATE).edit().putString(USER_NAME_KEY, name).apply()
    }

    fun getUserStatus(context: Context): String {
        val prefs = context.getSharedPreferences("identity", Context.MODE_PRIVATE)
        return prefs.getString(USER_STATUS_KEY, "Conectado") ?: "Conectado"
    }

    fun setUserStatus(context: Context, status: String) {
        context.getSharedPreferences("identity", Context.MODE_PRIVATE).edit().putString(USER_STATUS_KEY, status).apply()
    }

    fun saveAvatar(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val file = File(context.filesDir, AVATAR_FILE)
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun loadAvatar(context: Context): Bitmap? {
        val file = File(context.filesDir, AVATAR_FILE)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun saveBanner(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val file = File(context.filesDir, BANNER_FILE)
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun loadBanner(context: Context): Bitmap? {
        val file = File(context.filesDir, BANNER_FILE)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
}
