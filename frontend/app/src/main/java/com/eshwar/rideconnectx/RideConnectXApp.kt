package com.eshwar.rideconnectx

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.eshwar.rideconnectx.appcheck.appCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RideConnectXApp : Application() {

    /**
     * Turns on Firebase App Check before anything can talk to Firestore.
     *
     * `google-services.json` ships inside every APK and is not a secret, so
     * without this, anyone who unzips the APK can call the project's Firestore
     * from a script. The security rules still stop them reading another rider's
     * document — but nothing stops them burning the project's quota. App Check
     * is the control that rejects traffic which did not come from a genuine,
     * Play-installed copy of this app.
     *
     * Debug builds use the debug provider instead: Play Integrity cannot attest
     * a locally installed build, so without this branch every emulator and
     * sideloaded test build would be refused. The debug token it prints in
     * logcat has to be registered once in the Firebase console.
     *
     * **Enforcement is a console setting, not a code one.** Until App Check is
     * switched to "enforced" for Firestore in the Firebase console, this only
     * reports; it does not yet block anything.
     */
    override fun onCreate() {
        super.onCreate()
        runCatching {
            FirebaseApp.initializeApp(this)
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(appCheckProviderFactory())
        }.onFailure {
            // A failure here must never stop the app starting. The rider's
            // dashboard and the BLE link do not depend on Firebase at all.
            Log.w("RCX-App", "App Check setup failed; continuing without it", it)
        }
    }
}
