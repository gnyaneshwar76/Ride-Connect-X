package com.eshwar.rideconnectx.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RideAccumulatorTest {

    @Test
    fun `sums trip increases, survives a trip reset and a bad frame`() {
        val a = RideAccumulator()
        a.onReading(10.0f, 2400, 0L)        // connect, standing
        a.onReading(10.0f, 2400, 60_000L)   // still standing
        a.onReading(10.5f, 2400, 120_000L)  // +0.5 km
        a.onReading(0.0f, 2401, 150_000L)   // trip reset -> odometer +1 km
        a.onReading(0.3f, 2401, 180_000L)   // +0.3 km
        a.onReading(9999f, 2401, 190_000L)  // garbage frame, ignored
        val ride = a.finish("guest")!!
        assertEquals(1800, ride.distanceMeters)
        assertEquals(60_000L, ride.startedAt)  // last still reading before moving
        assertEquals(180_000L, ride.endedAt)
        assertEquals(54, ride.avgSpeedKmh)     // 1.8 km in 2 min
    }

    @Test
    fun `too short to count is dropped`() {
        val a = RideAccumulator()
        a.onReading(5.0f, 100, 0L)
        a.onReading(5.1f, 100, 30_000L)
        assertNull(a.finish("guest"))
    }
}
