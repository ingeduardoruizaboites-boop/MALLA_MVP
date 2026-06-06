package com.malla.mvp.identity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec

object IdentityManager {
    private const val TAG = "IdentityManager"
    private const val KEY_ALIAS = "malla_identity"
    private const val USER_NAME_KEY = "user_name"
    private const val USER_STATUS_KEY = "user_status"
    private const val AVATAR_FILE = "avatar.jpg"
    private const val BANNER_FILE = "banner.jpg"

    val deviceId: String = java.util.UUID.randomUUID().toString().take(8)

    @Volatile private var cachedPublicKeyBase64: String? = null

    fun init(context: Context) {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!ks.containsAlias(KEY_ALIAS)) {
                generateKeyPair()
                Log.d(TAG, "[IDENTITY] Nuevo keypair ECDH generado en Keystore")
            } else {
                Log.d(TAG, "[IDENTITY] Keypair existente cargado del Keystore")
            }
            getPublicKeyBase64()
        } catch (e: Exception) {
            Log.e(TAG, "[IDENTITY:ERR] Error inicializando identidad: ${e.message}", e)
        }
    }

    private fun generateKeyPair() {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        kpg.initialize(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false)
                .build()
        )
        kpg.generateKeyPair()
    }

    fun getPublicKeyBase64(): String? {
        cachedPublicKeyBase64?.let { return it }
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
                ?: throw IllegalStateException("Keypair no encontrado")
            val base64 = Base64.encodeToString(entry.certificate.publicKey.encoded, Base64.NO_WRAP)
            cachedPublicKeyBase64 = base64
            base64
        } catch (e: Exception) {
            Log.e(TAG, "[IDENTITY:ERR] No se pudo obtener clave pública: ${e.message}", e)
            null
        }
    }

    fun getIdentityId(): String {
        val pubKey = getPublicKeyBase64()
        return if (pubKey != null) "MALLA-${pubKey.take(12)}" else "MALLA-$deviceId"
    }

    fun getPrivateKey(): PrivateKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Keypair no encontrado en Keystore")
        return entry.privateKey
    }

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
