package com.eshwar.rideconnectx.domain.model

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: UserSession) : AuthState
}

sealed interface AuthError {
    data object NoInternet : AuthError
    data object Cancelled : AuthError
    data object NoCredentialAvailable : AuthError
    data object WebClientIdMissing : AuthError
    data class InvalidCredentials(val detail: String) : AuthError
    data class AccountExists(val detail: String) : AuthError
    data class WeakPassword(val detail: String) : AuthError
    data class TokenFailure(val detail: String) : AuthError
    data class CloudSyncFailure(val detail: String) : AuthError
    data class Unknown(val detail: String) : AuthError

    val message: String
        get() = when (this) {
            NoInternet -> "No internet connection. Check your network and try again."
            Cancelled -> "Sign-in cancelled."
            NoCredentialAvailable -> "No Google account found on this device. Add one in Settings, then try again."
            WebClientIdMissing ->
                "Google Sign-In isn't configured. Re-download google-services.json after enabling Google in Firebase Console."
            is InvalidCredentials -> detail
            is AccountExists -> detail
            is WeakPassword -> detail
            is TokenFailure -> "Couldn't verify your Google account. $detail"
            is CloudSyncFailure -> "Signed in, but your data couldn't sync. $detail"
            is Unknown -> detail
        }
}

sealed interface AuthResult {
    data class Success(val user: UserSession) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}
