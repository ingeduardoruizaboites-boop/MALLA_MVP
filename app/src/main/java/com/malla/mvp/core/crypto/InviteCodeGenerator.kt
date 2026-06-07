package com.malla.mvp.core.crypto

import kotlin.random.Random

object InviteCodeGenerator {

    private const val CODE_LENGTH = 8
    private const val VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 horas

    private val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // sin confundibles

    data class InviteCode(
        val code: String,
        val expiresAt: Long
    )

    // Genera un código aleatorio de 8 caracteres
    fun generate(): InviteCode {
        val code = (1..CODE_LENGTH).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return InviteCode(
            code = code,
            expiresAt = System.currentTimeMillis() + VALIDITY_MS
        )
    }

    // Valida que un código tenga el formato correcto y no haya expirado
    fun isValid(code: InviteCode?): Boolean {
        if (code == null) return false
        if (System.currentTimeMillis() > code.expiresAt) return false
        return code.code.length == CODE_LENGTH && code.code.all { it in chars }
    }
}
