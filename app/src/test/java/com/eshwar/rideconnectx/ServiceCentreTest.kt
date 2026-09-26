package com.eshwar.rideconnectx

import com.eshwar.rideconnectx.presentation.viewmodel.isValidCentre
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A service centre is a name, not an odometer reading (N10). */
class ServiceCentreTest {

    @Test
    fun `a number typed into the centre field is refused`() {
        assertFalse(isValidCentre("2000"))
        assertFalse(isValidCentre(" 12,345 "))
    }

    @Test
    fun `names are accepted, with or without numbers in them`() {
        assertTrue(isValidCentre("Sai Suzuki"))
        assertTrue(isValidCentre("Suzuki Service 24x7"))
    }

    @Test
    fun `blank is allowed and saved as not recorded`() {
        assertTrue(isValidCentre(""))
    }
}
