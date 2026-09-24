package com.eshwar.rideconnectx.data.repository

import android.app.Activity
import android.content.Context
import com.eshwar.rideconnectx.data.local.ServicePreferencesStore
import com.eshwar.rideconnectx.data.local.SessionDataStore
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeoutOrNull
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
    private val sessionStore: SessionDataStore,
    private val servicePrefs: ServicePreferencesStore,
    private val database: com.eshwar.rideconnectx.data.local.db.RideDatabase,
    @ApplicationScope private val appScope: CoroutineScope,
) : AuthRepository {

    private companion object {
        const val TAG = "RCX-Auth"
        const val LAST_READINGS = "lastReadings"
        const val SAVE_TIMEOUT_MS = 5_000L

        /** Every Room table carrying an ownerId column (DB v8). */
        val OWNED_TABLES = listOf(
            "emergency_contacts", "notifications", "rides", "service_records",
            "service_tasks", "fuel_samples", "refuels",
        )
    }

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
        val session = prefs.session.first()
        // The last readings go up to the account that saw them, then off the
        // phone. Capped at a few seconds so offline never blocks sign-out; if
        // the cloud does not confirm, they are held for this account only and
        // uploaded at its next sign-in. Guests have no cloud; theirs are erased.
        if (!session.isGuest && session.uid.isNotEmpty()) {
            sessionStore.cachedTelemetry.first()?.let { c ->
                val saved = withTimeoutOrNull(SAVE_TIMEOUT_MS) {
                    uploadReadings(session.uid, c)
                } ?: false
                if (!saved) {
                    Log.w(TAG, "Last readings not confirmed by cloud - held for next sign-in")
                    sessionStore.savePendingReadings(session.uid, c)
                }
            }
        }
        clearReadings()

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

    /**
     * Order matters, and each step stops the rest on failure:
     *  1. Confirm identity. Firebase refuses to delete a stale sign-in, and the
     *     old code deleted the cloud data first - so that refusal left an
     *     account with its data gone but the account itself still there.
     *  2. Cloud data (needs the account to still exist - rules check the uid).
     *  3. The account.
     *  4. This account's data on the phone - other accounts' rows are kept.
     * Runs on [appScope]: the screen navigates away as the account vanishes.
     */
    override suspend fun deleteAccount(activity: Activity, password: String?): AuthResult =
        appScope.async { deleteAccountSteps(activity, password) }.await()

    private suspend fun deleteAccountSteps(activity: Activity, password: String?): AuthResult {
        val session = prefs.session.first()
        if (session.isGuest || session.uid.isEmpty()) {
            return AuthResult.Failure(AuthError.Unknown("Guests have no account to delete."))
        }
        val uid = session.uid

        val proof = if (session.method == LoginMethod.EMAIL) {
            remote.reauthenticateWithPassword(password.orEmpty())
        } else {
            remote.reauthenticateWithGoogle(activity)
        }
        if (proof is AuthResult.Failure) return proof

        firestore.deleteUser(uid).onFailure {
            Log.e(TAG, "Cloud delete failed", it)
            return AuthResult.Failure(AuthError.Unknown("Couldn't delete your cloud data. Check your internet and try again."))
        }
        val result = remote.deleteAccount()
        if (result is AuthResult.Failure) return result

        database.openHelper.writableDatabase.apply {
            beginTransaction()
            try {
                OWNED_TABLES.forEach { execSQL("DELETE FROM $it WHERE ownerId = ?", arrayOf(uid)) }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
        sessionStore.clearPendingReadings()
        remote.signOut(context)
        prefs.clearSession()
        clearReadings()
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

        // Unknown is not "new": treating a failed read as a first sign-in
        // overwrote a returning rider's cloud profile with defaults.
        val isReturning = firestore.exists(user.uid)
            ?: return AuthResult.Failure(
                AuthError.CloudSyncFailure("Couldn't reach your account. Try again when you're online.")
            )
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
        flushPendingReadings(user.uid)

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
        restoreLastReadings(data)

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

    /**
     * True only once the cloud confirms. One document, merge-written: the cloud
     * saves it whole or not at all, and a repeat overwrites rather than
     * duplicates - so retrying is always safe.
     */
    private suspend fun uploadReadings(uid: String, c: SessionDataStore.CachedTelemetry): Boolean =
        firestore.updateScooter(
            uid,
            mapOf(
                LAST_READINGS to mapOf(
                    "odometerKm" to c.odometerKm,
                    "tripAKm" to c.tripAKm.toDouble(),
                    "tripBKm" to c.tripBKm.toDouble(),
                    "fuelSegments" to c.fuelSegments,
                    "capturedAt" to c.capturedAt,
                )
            ),
        ).isSuccess

    /**
     * Readings held from an offline sign-out of [uid]. They are newer than the
     * cloud copy, so they go on screen first; the held copy is deleted only
     * after the cloud confirms the upload. Signed in, Firestore itself waits
     * for the network, so this simply completes when the internet is back.
     */
    private fun flushPendingReadings(uid: String) = appScope.launch {
        val (owner, c) = sessionStore.pendingReadings.first() ?: return@launch
        if (owner != uid) return@launch // someone else's - stays hidden
        sessionStore.cacheTelemetry(c.odometerKm, c.tripAKm, c.tripBKm, c.fuelSegments, c.capturedAt)
        if (uploadReadings(uid, c)) sessionStore.clearPendingReadings()
    }

    /** Readings and the service screen's odometer floor both belong to the account. */
    private suspend fun clearReadings() {
        sessionStore.clearTelemetry()
        servicePrefs.clearOdometer()
    }

    /** The account's own last readings, saved by [signOut]. Absent = dashboard shows "—". */
    private suspend fun restoreLastReadings(data: Map<String, Any>?) {
        val r = (data?.get("scooter") as? Map<*, *>)?.get(LAST_READINGS) as? Map<*, *> ?: return
        val odo = (r["odometerKm"] as? Number)?.toInt() ?: return
        if (!ServicePreferencesStore.isPlausibleOdometer(0, odo)) return
        sessionStore.cacheTelemetry(
            odometerKm = odo,
            tripAKm = (r["tripAKm"] as? Number)?.toFloat() ?: 0f,
            tripBKm = (r["tripBKm"] as? Number)?.toFloat() ?: 0f,
            fuelSegments = ((r["fuelSegments"] as? Number)?.toInt() ?: 0).coerceIn(0, 5),
            capturedAt = (r["capturedAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    private fun LoginMethod.orGoogle() =
        if (this == LoginMethod.NONE || this == LoginMethod.GUEST) LoginMethod.GOOGLE else this
}
