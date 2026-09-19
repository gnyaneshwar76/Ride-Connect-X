package com.eshwar.rideconnectx.data.remote

import com.eshwar.rideconnectx.domain.model.UserSession
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud copy of everything that should follow a rider between devices.
 *
 * Schema:
 *   users/{uid}
 *     profile      { name, email, photoUrl, loginMethod, createdAt, lastLoginAt }
 *     settings     { theme, units, voiceGuidance, autoConnect }
 *     scooter      { vehicleId, colorId, lastDeviceAddress, lastConnectedAt }
 *     preferences  { mapType, avoidTolls, avoidHighways, notifications }
 *   users/{uid}/favorites/{placeId}
 *   users/{uid}/rides/{rideId}
 *
 * Profile fields live in maps on the user document rather than sub-documents so
 * a single read restores the whole account. Favorites and rides grow unbounded,
 * so they are subcollections.
 */
@Singleton
class FirestoreUserDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val USERS = "users"
        const val FAVORITES = "favorites"
        const val RIDES = "rides"

        const val PROFILE = "profile"
        const val SETTINGS = "settings"
        const val SCOOTER = "scooter"
        const val PREFERENCES = "preferences"
    }

    private fun userDoc(uid: String) = db.collection(USERS).document(uid)

    suspend fun exists(uid: String): Boolean =
        runCatching { userDoc(uid).get().await().exists() }.getOrDefault(false)

    /** Called on first sign-in. Seeds defaults without overwriting anything later. */
    suspend fun createUser(session: UserSession): Result<Unit> = runCatching {
        userDoc(session.uid).set(
            mapOf(
                PROFILE to mapOf(
                    "name" to session.name,
                    "email" to session.email,
                    "photoUrl" to session.photoUrl,
                    "loginMethod" to session.method.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                ),
                SETTINGS to mapOf(
                    "theme" to "system",
                    "units" to "metric",
                    "voiceGuidance" to true,
                    "autoConnect" to true,
                ),
                SCOOTER to emptyMap<String, Any>(),
                PREFERENCES to mapOf(
                    "mapType" to "normal",
                    "avoidTolls" to false,
                    "avoidHighways" to false,
                    "notifications" to true,
                ),
            )
        ).await()
    }

    /** Called on every subsequent sign-in. Merges, so nothing is lost. */
    suspend fun touchLogin(session: UserSession): Result<Unit> = runCatching {
        userDoc(session.uid).set(
            mapOf(
                PROFILE to mapOf(
                    "name" to session.name,
                    "email" to session.email,
                    "photoUrl" to session.photoUrl,
                    "loginMethod" to session.method.name,
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                )
            ),
            SetOptions.merge(),
        ).await()
    }

    /** One read that restores the whole account after a fresh install. */
    suspend fun fetchUser(uid: String): Result<Map<String, Any>?> = runCatching {
        userDoc(uid).get().await().data
    }

    /** Merges individual profile fields, e.g. the rider name and city. */
    suspend fun updateProfileFields(uid: String, fields: Map<String, Any?>): Result<Unit> =
        runCatching {
            userDoc(uid).set(mapOf(PROFILE to fields), SetOptions.merge()).await()
        }

    suspend fun updateSettings(uid: String, settings: Map<String, Any>): Result<Unit> =
        runCatching {
            userDoc(uid).set(mapOf(SETTINGS to settings), SetOptions.merge()).await()
        }

    suspend fun updateScooter(uid: String, scooter: Map<String, Any?>): Result<Unit> =
        runCatching {
            userDoc(uid).set(mapOf(SCOOTER to scooter), SetOptions.merge()).await()
        }

    suspend fun updatePreferences(uid: String, preferences: Map<String, Any>): Result<Unit> =
        runCatching {
            userDoc(uid).set(mapOf(PREFERENCES to preferences), SetOptions.merge()).await()
        }

    suspend fun addFavorite(uid: String, placeId: String, place: Map<String, Any>): Result<Unit> =
        runCatching {
            userDoc(uid).collection(FAVORITES).document(placeId).set(place).await()
        }

    suspend fun removeFavorite(uid: String, placeId: String): Result<Unit> = runCatching {
        userDoc(uid).collection(FAVORITES).document(placeId).delete().await()
    }

    suspend fun fetchFavorites(uid: String): Result<List<Map<String, Any>>> = runCatching {
        userDoc(uid).collection(FAVORITES).get().await().documents.mapNotNull { it.data }
    }

    suspend fun addRide(uid: String, ride: Map<String, Any>): Result<Unit> = runCatching {
        userDoc(uid).collection(RIDES).add(ride).await()
    }

    suspend fun fetchRides(uid: String, limit: Long = 200): Result<List<Map<String, Any>>> =
        runCatching {
            userDoc(uid).collection(RIDES)
                .orderBy("startedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get().await().documents.mapNotNull { it.data }
        }

    suspend fun deleteUser(uid: String): Result<Unit> = runCatching {
        userDoc(uid).delete().await()
    }
}
