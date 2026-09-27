package com.eshwar.rideconnectx.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** City picker: prefix matches first, and typed names map to the list's spelling (N15). */
class IndianCitiesTest {
    @Test
    fun `prefix matches come before inner matches`() {
        assertEquals("Hyderabad", IndianCities.search("hyd").first())
        assertEquals(listOf("Jabalpur", "Jaipur", "Jalandhar"), IndianCities.search("ja").take(3))
        // Nothing starts with "nagar"; the ones containing it still come up.
        assertEquals("Ahmednagar", IndianCities.search("nagar").first())
    }

    @Test
    fun `a typed city resolves to the list's spelling`() {
        assertEquals("Hyderabad", IndianCities.find(" hyderabad "))
        assertNull(IndianCities.find("Atlantis"))
    }
}
