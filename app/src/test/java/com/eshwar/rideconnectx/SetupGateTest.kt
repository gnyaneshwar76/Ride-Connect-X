package com.eshwar.rideconnectx

import com.eshwar.rideconnectx.presentation.navigation.Routes
import com.eshwar.rideconnectx.presentation.navigation.SetupGate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unfinished setup is resumed on launch, never skipped (N3). */
class SetupGateTest {

    @Test
    fun `signed in with setup unfinished resumes setup`() {
        assertEquals(Routes.PERMISSIONS, SetupGate.launchRoute(signedIn = true, profileDone = false))
    }

    @Test
    fun `finished setup opens the dashboard`() {
        assertEquals(Routes.DASHBOARD, SetupGate.launchRoute(signedIn = true, profileDone = true))
    }

    @Test
    fun `signed out opens the intro`() {
        assertEquals(Routes.INTRO, SetupGate.launchRoute(signedIn = false, profileDone = true))
    }

    @Test
    fun `an unanswered guest-merge question comes first`() {
        assertEquals(Routes.PROFILE_FOUND, SetupGate.launchRoute(signedIn = true, profileDone = true, guestMergePending = true))
        assertEquals(Routes.PROFILE_FOUND, SetupGate.afterPermissions(profileDone = false, guestMergePending = true))
        assertEquals(Routes.VEHICLE, SetupGate.afterPermissions(profileDone = false, guestMergePending = false))
        assertEquals(Routes.DASHBOARD, SetupGate.afterPermissions(profileDone = true, guestMergePending = false))
    }
}
