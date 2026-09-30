package com.eshwar.rideconnectx.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixes use `lat` as metres along a straight road, so distances are easy to read. */
class GpsCountdownTest {
    private fun counter() = GpsCountdown { a, b -> kotlin.math.abs(b.lat - a.lat) }
    private fun fix(m: Double, t: Long, speed: Float = 8f, acc: Float = 5f) =
        GpsCountdown.Fix(lat = m, lon = 0.0, accuracyM = acc, speedMps = speed, atMs = t)

    @Test
    fun `counts down between Maps updates and never goes back up for the same turn`() {
        val c = counter()
        c.onFix(fix(0.0, 0))
        c.onMaps("left", 100, 0)
        c.onFix(fix(8.0, 1000))
        assertEquals(92, c.shown(1000))
        assertEquals(88, c.shown(1500))          // predicted from speed between fixes
        c.onMaps("left", 90, 1600)               // Maps' stale repeat: keep counting
        assertTrue(c.shown(1600) <= 88)
        c.onMaps("left", 70, 1700)               // Maps lower: take it
        assertEquals(70, c.shown(1700))
        c.onMaps("right", 400, 1800)             // next turn: start from Maps
        assertEquals(400, c.shown(1800))
    }

    @Test
    fun `standing still or a bad fix counts nothing, and it stops at zero`() {
        val c = counter()
        c.onFix(fix(0.0, 0, speed = 0f))
        c.onMaps("left", 20, 0)
        c.onFix(fix(6.0, 1000, speed = 0.3f))    // jitter at a red light
        c.onFix(fix(30.0, 2000, acc = 80f))      // poor fix
        assertEquals(20, c.shown(2000))
        c.onFix(fix(30.0, 3000))
        c.onFix(fix(60.0, 4000))
        assertEquals(0, c.shown(4000))
    }

    @Test
    fun `Maps far above the count means we over-counted, so Maps wins`() {
        val c = counter()
        c.onFix(fix(0.0, 0))
        c.onMaps("left", 300, 0)
        c.onFix(fix(100.0, 1000))
        assertEquals(200, c.shown(1000))
        c.onMaps("left", 280, 1100)
        assertEquals(280, c.shown(1100))
    }
}
