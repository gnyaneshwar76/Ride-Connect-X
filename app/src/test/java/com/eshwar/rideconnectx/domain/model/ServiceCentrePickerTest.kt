package com.eshwar.rideconnectx.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Service-centre picker: labels, fallback area and ordering (N14). */
class ServiceCentrePickerTest {

    @Test
    fun `nearby centres read as Name - Area, two-wheelers only`() {
        val labels = ServiceCentrePicker.nearbyLabels(
            listOf(
                OsmPlace("Suzuki Service", suburb = "Dammaiguda", shop = "motorcycle"),
                OsmPlace("Sai Motors", city = "Hyderabad", shop = "motorcycle_repair"),
                OsmPlace("Bike Point", shop = "motorcycle"),
                OsmPlace("Speed Suzuki", shop = "motorcycle", derivedArea = "Nagaram"),
                OsmPlace("", shop = "motorcycle"), // unnamed - useless in a picker
                OsmPlace("Suzuki Service", suburb = "Dammaiguda", shop = "motorcycle"), // node + building
                // Cars, not scooters (rider, 28 Sep).
                OsmPlace("Maruti Suzuki Arena", brand = "Maruti Suzuki", shop = "car"),
                OsmPlace("NEXA Kapra", brand = "Suzuki"),
                OsmPlace("Suzuki Workshop", shop = "car_repair"),
                OsmPlace("Suzuki Arena ECIL", brand = "Suzuki"),
            ),
            riderArea = "Kapra",
        )
        assertEquals(
            listOf(
                "Suzuki Service – Dammaiguda", "Sai Motors – Hyderabad",
                "Bike Point – Kapra", "Speed Suzuki – Nagaram",
            ),
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
