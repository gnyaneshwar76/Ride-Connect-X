package com.eshwar.rideconnectx.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Service-centre picker: labels, fallback area and ordering (N14). */
class ServiceCentrePickerTest {

    @Test
    fun `nearby centres read as Area - Name, falling back to the rider's area`() {
        val labels = ServiceCentrePicker.nearbyLabels(
            listOf(
                OsmPlace("Suzuki Service", suburb = "Dammaiguda"),
                OsmPlace("Sai Motors", city = "Hyderabad"),
                OsmPlace("Bike Point"),
                OsmPlace(""), // unnamed - useless in a picker
                OsmPlace("Suzuki Service", suburb = "Dammaiguda"), // node + building
            ),
            riderArea = "Kapra",
        )
        assertEquals(
            listOf("Dammaiguda – Suzuki Service", "Hyderabad – Sai Motors", "Kapra – Bike Point"),
            labels,
        )
    }

    @Test
    fun `past centres come first, once, filtered by what is typed`() {
        val past = listOf("Sai Suzuki", "dammaiguda – suzuki service")
        val nearby = listOf("Dammaiguda – Suzuki Service", "Kapra – Bike Point")
        assertEquals(
            listOf("Sai Suzuki", "dammaiguda – suzuki service", "Kapra – Bike Point"),
            ServiceCentrePicker.options(past, nearby, typed = ""),
        )
        assertEquals(listOf("Kapra – Bike Point"), ServiceCentrePicker.options(past, nearby, typed = "kapra"))
    }

    @Test
    fun `the query asks around the rider with a US-formatted point`() {
        val q = ServiceCentrePicker.overpassQuery(17.49, 78.57)
        assertTrue(q.contains("(around:10000,17.49000,78.57000)"))
        assertTrue(q.startsWith("[out:json]"))
    }
}
