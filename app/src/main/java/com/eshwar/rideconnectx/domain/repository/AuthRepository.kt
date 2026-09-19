package com.eshwar.rideconnectx.domain.repository

import android.app.Activity
import com.eshwar.rideconnectx.domain.model.AuthResult
import com.eshwar.rideconnectx.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /**
     * Single source of truth for who is signed in. Starts as
     * [AuthState.Loading] while Firebase restores the persisted session.
     */
    val authState: Flow<AuthState>

    val isGoogleSignInAvailable: Boolean

    suspend fun signInWithGoogle(activity: Activity): AuthResult

    suspend fun signUpWithEmail(email: String, password: String, name: String): AuthResult

    suspend fun signInWithEmail(email: String, password: String): AuthResult

    suspend fun sendPasswordReset(email: String): AuthResult

    /** Local-only session; never reaches Firebase or Firestore. */
    suspend fun continueAsGuest(name: String): AuthResult

    /**
     * Ends the session.
     *
     * [keepLocalProfile] is the "move this profile to another account" flow —
     * the only case where the rider name, nickname, city, vehicle, colour and
     * photo are meant to survive and be written into the next account. The
     * default clears them, so sign-out is an account boundary and a new call
     * site cannot reopen the leak by forgetting a flag.
     */
    suspend fun signOut(keepLocalProfile: Boolean = false)

    suspend fun deleteAccount(): AuthResult
}
