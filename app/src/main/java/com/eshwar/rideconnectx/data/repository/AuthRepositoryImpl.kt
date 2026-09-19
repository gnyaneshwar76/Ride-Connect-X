package com.eshwar.rideconnectx.data.repository

import android.app.Activity
import android.content.Context
import com.eshwar.rideconnectx.data.local.UserPreferencesStore
import com.eshwar.rideconnectx.data.remote.FirebaseAuthDataSource
import com.eshwar.rideconnectx.data.remote.FirestoreUserDataSource
import com.eshwar.rideconnectx.domain.model.AuthError
import com.eshwar.rideconnectx.domain.model.AuthResult
import com.eshwar.rideconnectx.domain.model.AuthState
import com.eshwar.rideconnectx.domain.model.GuestNameRules
import com.eshwar.rideconnectx.domain.model.LoginMethod
import com.eshwar.rideconnectx.domain.model.UserSession
import com.eshwar.rideconnectx.core.di.ApplicationScope
import com.eshwar.rideconnectx.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remote: FirebaseAuthDataSource,
    private val firestore: FirestoreUserDataSource,
    private val prefs: UserPreferencesStore,
    private val photoStore: com.eshwar.rideconnectx.core.util.ProfilePhotoStore,
    @ApplicationScope private val appScope: CoroutineScope,
) : AuthRepository {

    private companion object { const val TAG = "RCX-Auth" }

    /**
     * Firebase drives the state for cloud accounts; DataStore covers guests,
     * who have no Firebase user. Combining the two means one stream answers
     * "is anyone signed in", online or off.
     */
    override val authState: Flow<AuthState> =
        combine(
            remote.authStateChanges.onStart { emit(remote.currentUser) },
            prefs.session,
        ) { firebaseUser, localSession ->
            when {
                firebaseUser != null -> AuthState.Authenticated(
                    localSession.takeIf { it.uid == firebaseUser.uid }
                        ?: with(remote) { firebaseUser.toSession(localSession.method.orGoogle()) }
                )
                localSession.isGuest -> AuthState.Authenticated(localSession)
                else -> AuthState.Unauthenticated
            }
        }.onStart { emit(AuthState.Loading) }

    override val isGoogleSignInAvailable: Boolean get() = remote.isGoogleSignInAvailable

    /**
     * Google sign-in, with [persist] on [appScope] for the reason below.
     *
     * The Activity itself has to be handed to the caller's thread, so only the
     * persistence is moved. Google used to get away with running persist on the
     * caller's scope because it was two quick writes; once [persist] also read
     * the profile back down, the extra round trip gave the navigation time to
     * clear the AuthViewModel mid-write and the Firestore write died with
     * `JobCancellationException`.
     */
    override suspend fun signInWithGoogle(activity: Activity): AuthResult {
        val result = remote.signInWithGoogle(activity)
        return appScope.async { result.persist() }.await()
    }

    /**
     * Runs on [appScope] rather than the caller's scope.
     *
     * `createUserWithEmailAndPassword` signs the user in the moment it returns,
     * which flips [authState] and navigates away from Sign In — clearing the
     * AuthViewModel and cancelling its viewModelScope. The email path then has
     * one more suspension point than Google does (`updateProfile`), so it was
     * reliably being cancelled there, before [persist] ever ran and before
     * Firestore was ever told about the account. Hence: Google users in the
     * database, email users not.
     */
    override suspend fun signUpWithEmail(email: String, password: String, name: String): AuthResult =
        appScope.async { remote.signUpWithEmail(email, password, name).persist() }.await()

    override suspend fun signInWithEmail(email: String, password: String): AuthResult =
        appScope.async { remote.signInWithEmail(email, password).persist() }.await()

    override suspend fun sendPasswordReset(email: String): AuthResult =
        remote.sendPasswordReset(email)

    override suspend fun continueAsGuest(name: String): AuthResult {
        val clean = name.trim()
        GuestNameRules.validate(clean)?.let {
            return AuthResult.Failure(AuthError.InvalidCredentials(it))
        }
        val session = UserSession(name = clean, method = LoginMethod.GUEST)
        prefs.saveSession(session)
        // A guest types their name on the screen before Create Profile, and
        // Create Profile then asked for it again with an empty field — because
        // the name only went into the session, not into the rider name the rest
        // of the app reads. It is the same name; store it once.
        prefs.saveRiderName(clean)
        return AuthResult.Success(session)
    }

    override suspend fun signOut(keepLocalProfile: Boolean) {
        remote.signOut(context)
        prefs.clearSession()
        // The boundary lives here, not in a ViewModel: `AuthViewModel.signOut`
        // reaches this same method, and a profile left behind is merge-written
        // into the *next* uid's document by `carryLocalProfileToCloud`.
        if (!keepLocalProfile) {
            prefs.clearProfile()
            photoStore.clear()
        }
    }

    override suspend fun deleteAccount(): AuthResult {
        val uid = prefs.session.first().uid
        if (uid.isNotEmpty()) firestore.deleteUser(uid)
        val result = remote.deleteAccount()
        prefs.clearSession()
        // Deleting the account is a stronger statement than signing out, so the
        // local profile and avatar go too. Leaving them meant the next account
        // signed in on this phone inherited the deleted rider's name, city,
        // vehicle and face.
        prefs.clearProfile()
        photoStore.clear()
        return result
    }

    /**
     * Mirrors a successful sign-in into DataStore for offline reads, then
     * creates or refreshes the Firestore document.
     *
     * A Firestore failure does not fail the sign-in — the user is authenticated
     * either way — but it is surfaced so the UI can warn that sync is behind.
     */
    private suspend fun AuthResult.persist(): AuthResult {
        if (this !is AuthResult.Success) return this

        prefs.saveSession(user)

        // Recorded here rather than in the ViewModel so it shares this scope and
        // cannot be lost to the same cancellation that used to drop the
        // Firestore write. The Sign In screen states that continuing is consent.
        prefs.acceptTerms()
        prefs.acceptPrivacy()

        val isReturning = firestore.exists(user.uid)
        Log.d(TAG, "persist uid=${user.uid} method=${user.method} returning=$isReturning")

        val synced =
            if (isReturning) firestore.touchLogin(user) else firestore.createUser(user)

        // The whole point of signing in with an account: a returning rider gets
        // their profile back instead of being asked to build it again.
        //
        // And the mirror of it: a rider arriving at a *new* account with a
        // profile already on the device — a guest signing up, or someone moving
        // to a different email — takes that profile with them. Without this the
        // profile stayed local, the new account stayed empty, and the work was
        // lost the first time they reinstalled.
        if (isReturning) restoreFromCloud(user.uid) else carryLocalProfileToCloud(user.uid)

        return synced.fold(
            onSuccess = {
                Log.d(TAG, "Firestore users/${user.uid} written")
                this
            },
            onFailure = {
                Log.e(TAG, "Firestore write failed for users/${user.uid}", it)
                AuthResult.Failure(
                    AuthError.CloudSyncFailure(it.message ?: "Try again when you're online.")
                )
            },
        )
    }

    /**
     * Pulls a returning rider's account back down onto this device.
     *
     * Without this, reinstalling and signing in with the same Google account
     * still landed on Create Profile every time — the cloud copy existed and was
     * being written, but nothing ever read it back, so the phone started blank.
     *
     * Restores the rider's name and city, the chosen vehicle and colourway, and
     * the settings and preferences maps. Profile setup is then marked done, so
     * the rider goes straight to the dashboard.
     *
     * Deliberately forgiving: a missing or partial document leaves the rider on
     * Create Profile rather than dropping them onto a dashboard with no vehicle.
     */
    private suspend fun restoreFromCloud(uid: String) {
        val data = firestore.fetchUser(uid).getOrElse {
            Log.e(TAG, "Cloud restore failed for users/$uid", it)
            return
        }

        val cloud = com.eshwar.rideconnectx.domain.model.CloudProfile.from(data)

        cloud.riderName?.let { prefs.saveRiderName(it) }
        cloud.nickname?.let { prefs.saveRiderNickname(it) }
        cloud.location?.let { prefs.saveRiderLocation(it) }
        cloud.vehicleId?.let { prefs.saveVehicle(it) }
        cloud.colorId?.let { prefs.saveColor(it) }
        // The picture is part of the profile, so it comes back with it. Skipped
        // silently when the device already has one — see `restoreFromCloud`.
        cloud.photoBase64?.let { photoStore.restoreFromCloud(it) }
        if (cloud.isComplete) prefs.setProfileCompleted(true)

        Log.d(
            TAG,
            "Cloud restore users/$uid name=${cloud.riderName != null} " +
                "vehicle=${cloud.vehicleId != null} colour=${cloud.colorId != null} " +
                "photo=${cloud.photoBase64 != null} profileComplete=${cloud.isComplete}",
        )
    }

    /**
     * Takes the profile already on this device up to a freshly created account.
     *
     * This is what makes "guest, then sign up" and "move to another email" keep
     * the rider's work. `clearSession()` deliberately leaves the rider name,
     * nickname, city, vehicle, paint and photo alone — only the account
     * identity goes — so at this point everything is still here and just needs
     * an owner.
     *
     * Writes nothing when there is nothing to write, so a genuinely new rider
     * does not get an empty document full of blank fields.
     */
    private suspend fun carryLocalProfileToCloud(uid: String) {
        val riderName = prefs.riderName.first()
        val nickname = prefs.riderNickname.first()
        val location = prefs.riderLocation.first()
        val vehicleId = prefs.selectedVehicleId.first().orEmpty()
        val colorId = prefs.selectedColorId.first().orEmpty()

        val profile = buildMap<String, Any> {
            if (riderName.isNotBlank()) put("riderName", riderName)
            if (nickname.isNotBlank()) put("nickname", nickname)
            if (location.isNotBlank()) put("location", location)
            photoStore.encodeForCloud()?.let { put("photoBase64", it) }
        }

        if (profile.isEmpty() && vehicleId.isBlank()) {
            Log.d(TAG, "New account, nothing local to carry over")
            return
        }

        if (profile.isNotEmpty()) firestore.updateProfileFields(uid, profile)
        if (vehicleId.isNotBlank()) {
            firestore.updateScooter(
                uid,
                mapOf("vehicleId" to vehicleId, "colorId" to colorId),
            )
        }

        Log.d(
            TAG,
            "Carried local profile to users/$uid " +
                "name=${riderName.isNotBlank()} vehicle=${vehicleId.isNotBlank()}",
        )
    }

    private fun LoginMethod.orGoogle() =
        if (this == LoginMethod.NONE || this == LoginMethod.GUEST) LoginMethod.GOOGLE else this
}
