package com.example.data.verification

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TokenSigner {
    // Internal signature key component mixed with app identity for HMAC integrity
    private const val SIGNING_SALT = "GyanSarthi_AroLinks_SecureGate_2026_HMAC_v1"

    fun sign(payload: String): String {
        return try {
            val keyBytes = SIGNING_SALT.toByteArray(StandardCharsets.UTF_8)
            val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKey)
            val hmacBytes = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
            Base64.encodeToString(hmacBytes, Base64.NO_WRAP or Base64.URL_SAFE).trim().replace("=", "")
        } catch (e: Exception) {
            ""
        }
    }

    fun verify(payload: String, signature: String): Boolean {
        if (signature.isBlank()) return false
        val expected = sign(payload)
        if (expected.isBlank()) return false
        val normalizedSig = signature.trim().replace(" ", "+").replace("=", "")
        val normalizedExpected = expected.trim().replace("=", "")
        return normalizedExpected == normalizedSig
    }

    fun buildPayload(sessionId: String, expiresAt: Long): String {
        return "session=$sessionId&expires=$expiresAt"
    }

    fun buildPayload(sessionId: String, verifiedAt: Long, expiresAt: Long): String {
        return "session=$sessionId&verifiedAt=$verifiedAt&expires=$expiresAt"
    }

    fun generateSignedToken(sessionId: String, expiresAt: Long): String {
        val payload = buildPayload(sessionId, expiresAt)
        val sig = sign(payload)
        val payloadBase64 = Base64.encodeToString(payload.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
        return "$payloadBase64.$sig"
    }

    fun generateSignedToken(sessionId: String, verifiedAt: Long, expiresAt: Long): String {
        val payload = buildPayload(sessionId, verifiedAt, expiresAt)
        val sig = sign(payload)
        val payloadBase64 = Base64.encodeToString(payload.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
        return "$payloadBase64.$sig"
    }

    data class ValidationResult(
        val isValid: Boolean,
        val expiresAt: Long = 0L,
        val verifiedAt: Long = 0L,
        val sessionId: String = ""
    ) {
        val isValidSignature: Boolean
            get() = isValid
    }

    fun parseAndValidateSignedToken(token: String): ValidationResult {
        val parts = token.split(".")
        if (parts.size != 2) return ValidationResult(false)
        val payloadBase64 = parts[0]
        val signature = parts[1]
        val payload = try {
            String(Base64.decode(payloadBase64, Base64.NO_WRAP or Base64.URL_SAFE), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            return ValidationResult(false)
        }
        if (!verify(payload, signature)) return ValidationResult(false)

        val params = payload.split("&").associate {
            val key = it.substringBefore("=")
            val value = it.substringAfter("=")
            key to value
        }

        val sessionId = params["session"] ?: ""
        val expiresAt = params["expires"]?.toLongOrNull() ?: 0L
        val verifiedAt = params["verifiedAt"]?.toLongOrNull() ?: (if (expiresAt > 0L) expiresAt - 24 * 60 * 60 * 1000L else 0L)

        val isNotExpired = expiresAt > System.currentTimeMillis()
        return ValidationResult(
            isValid = isNotExpired,
            expiresAt = expiresAt,
            verifiedAt = verifiedAt,
            sessionId = sessionId
        )
    }
}
