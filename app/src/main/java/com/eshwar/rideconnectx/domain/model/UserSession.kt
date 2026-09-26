package com.eshwar.rideconnectx.domain.model

/** How the current user signed in. */
enum class LoginMethod { NONE, GUEST, GOOGLE, EMAIL, PHONE }

/**
 * The signed-in user. [LoginMethod.NONE] means nobody is signed in yet.
 *
 * Guest sessions carry only a display name — no email, no account, no network.
 */
data class UserSession(
    val name: String = "",
    val email: String = "",
    val method: LoginMethod = LoginMethod.NONE,
    /** Firebase UID — empty for guests, who never touch the cloud. */
    val uid: String = "",
    val phone: String = "",
    val photoUrl: String = "",
) {
    val isSignedIn: Boolean get() = method != LoginMethod.NONE
    val isGuest: Boolean get() = method == LoginMethod.GUEST

    /** Only cloud-backed accounts sync to Firestore. */
    val syncsToCloud: Boolean get() = uid.isNotEmpty() && !isGuest
}

/** Outcome of a sign-in attempt. */
sealed interface SignInResult {
    data class Success(val session: UserSession) : SignInResult

    /**
     * Google Sign-In has no OAuth client ID yet, so it cannot run.
     * Populate `google_oauth_client_id` in strings.xml to enable it.
     */
    data object GoogleNotConfigured : SignInResult

    data object Cancelled : SignInResult
    data class Failed(val message: String) : SignInResult
}

/**
 * Validation for the guest display name, per the spec:
 * required, 2–30 characters, no emoji, not blank after trimming.
 */
object GuestNameRules {
    const val MIN = 2
    const val MAX = 30

    /** Returns an error message, or null when [raw] is acceptable. */
    fun validate(raw: String): String? {
        val name = raw.trim()
        return when {
            name.isEmpty() -> "Please enter your name"
            name.length < MIN -> "At least $MIN characters"
            name.length > MAX -> "At most $MAX characters"
            name.any { it.isSurrogate() } -> "Emoji aren't supported"
            else -> null
        }
    }

    fun isValid(raw: String) = validate(raw) == null
}

/**
 * Password rule for new email accounts: at least 8 characters, with a letter
 * and a number. Firebase alone only asks for 6.
 *
 * Sign-in never applies it — accounts made before the rule keep working.
 */
object PasswordRules {
    const val MIN = 8

    fun isValid(password: String) =
        password.length >= MIN && password.any { it.isLetter() } && password.any { it.isDigit() }
}
