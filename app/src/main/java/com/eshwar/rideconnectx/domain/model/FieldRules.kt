package com.eshwar.rideconnectx.domain.model

/**
 * Length and character limits for every free-text field (N16), in one place.
 * Each field caps its input at the MAX (so nothing oversized is ever typed or
 * pasted) and checks again on save, so a value that gets past the input — a
 * contact picked from the address book, a restored profile — is still held to
 * the same rule. Pure, so it is testable without a device.
 *
 * The rider's name uses [GuestNameRules], the password [PasswordRules].
 */
object FieldRules {
    const val NICKNAME_MIN = 2
    const val NICKNAME_MAX = 12
    const val CITY_MAX = 40
    const val CENTRE_MIN = 3
    const val CENTRE_MAX = 60
    const val NOTES_MAX = 200
    const val TASK_MIN = 2
    const val TASK_MAX = 30
    const val CONTACT_NAME_MIN = 2
    const val CONTACT_NAME_MAX = 30
    const val PHONE_MIN_DIGITS = 10
    const val PHONE_MAX_DIGITS = 15
    /** Digits plus a leading '+'. */
    const val PHONE_MAX_CHARS = PHONE_MAX_DIGITS + 1

    /** One word the dashboard can fit: letters only, 2–12. */
    fun nicknameError(raw: String): String? {
        val v = raw.trim()
        return when {
            v.isEmpty() -> "Add a nickname"
            !v.all(Char::isLetter) -> "Letters only, one word"
            v.length < NICKNAME_MIN -> "At least $NICKNAME_MIN letters"
            v.length > NICKNAME_MAX -> "Keep it to $NICKNAME_MAX letters"
            else -> null
        }
    }

    /** Optional (blank saves as "Not recorded"); otherwise 3–60 with a letter. */
    fun isValidCentre(raw: String): Boolean {
        val v = raw.trim()
        return v.isEmpty() || (v.length in CENTRE_MIN..CENTRE_MAX && v.any(Char::isLetter))
    }

    fun isValidTaskName(raw: String): Boolean {
        val v = raw.trim()
        return v.length in TASK_MIN..TASK_MAX && v.any(Char::isLetter)
    }

    fun isValidContactName(raw: String): Boolean {
        val v = raw.trim()
        return v.length in CONTACT_NAME_MIN..CONTACT_NAME_MAX && v.any(Char::isLetter)
    }

    /** What a phone field accepts as typed: digits, and '+' only at the start. */
    fun phoneInput(raw: String): String {
        val plus = raw.trimStart().startsWith("+")
        return ((if (plus) "+" else "") + raw.filter(Char::isDigit)).take(PHONE_MAX_CHARS)
    }

    /** Digits only, with an optional leading '+', 10–15 digits. */
    fun isValidPhone(raw: String): Boolean {
        val v = raw.trim().removePrefix("+")
        return v.all(Char::isDigit) && v.length in PHONE_MIN_DIGITS..PHONE_MAX_DIGITS
    }
}
