package com.eshwar.rideconnectx.presentation.navigation

/**
 * Where the app opens to. Pure, so the rule is testable without a device.
 *
 * Setup is resumed, never skipped: a signed-in rider whose profile is not
 * finished goes back into setup, not to the dashboard. Splash used to send
 * anyone signed in straight to the dashboard, so Back out of Create Profile
 * and reopening landed a half-made account there (rider, 26 Sep).
 */
object SetupGate {
    fun launchRoute(signedIn: Boolean, profileDone: Boolean): String = when {
        !signedIn -> Routes.INTRO
        profileDone -> Routes.DASHBOARD
        // Permissions forwards straight to Create Profile when nothing is missing.
        else -> Routes.PERMISSIONS
    }
}
