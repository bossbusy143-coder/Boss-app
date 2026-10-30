package com.example.data.verification

data class VerificationState(
    val isVerified: Boolean = false,
    val token: String? = null,
    val verifiedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val sessionId: String? = null,
    val isClockTampered: Boolean = false
) {
    val isValid: Boolean
        get() = isVerified && !isClockTampered && expiresAt > System.currentTimeMillis()

    val remainingMinutes: Long
        get() = if (isValid) {
            val diff = expiresAt - System.currentTimeMillis()
            (diff / (60 * 1000L)).coerceIn(0L, 24 * 60L)
        } else 0L

    val remainingHours: Int
        get() = (remainingMinutes / 60).toInt()

    val formattedRemainingTime: String
        get() {
            if (!isValid) return "Expired"
            val hours = remainingHours
            val mins = (remainingMinutes % 60).toInt()
            return when {
                hours > 0 -> "${hours}h ${mins}m"
                else -> "${mins}m"
            }
        }
}
