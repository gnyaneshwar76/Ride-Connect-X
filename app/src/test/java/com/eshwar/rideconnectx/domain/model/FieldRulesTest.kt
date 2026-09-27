package com.eshwar.rideconnectx.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text field's limits (N16). */
class FieldRulesTest {

    @Test
    fun `names take letters, spaces and dot apostrophe hyphen, 2 to 30`() {
        assertNull(GuestNameRules.validate("Gnyaneshwar .P"))
        assertNull(GuestNameRules.validate("D'Souza-Rao"))
        assertNotNull(GuestNameRules.validate("R"))
        assertNotNull(GuestNameRules.validate("Rocky123"))
        assertNotNull(GuestNameRules.validate("a".repeat(31)))
    }

    @Test
    fun `a nickname is one word of 2 to 12 letters`() {
        assertNull(FieldRules.nicknameError("Eshwar"))
        assertNotNull(FieldRules.nicknameError("E"))
        assertNotNull(FieldRules.nicknameError("Rocky Bhai"))
        assertNotNull(FieldRules.nicknameError("Abcdefghijklm"))
    }

    @Test
    fun `centre, task and contact names need letters and a sensible length`() {
        assertTrue(FieldRules.isValidCentre(""))
        assertFalse(FieldRules.isValidCentre("AB"))
        assertFalse(FieldRules.isValidCentre("2000"))
        assertFalse(FieldRules.isValidCentre("S".repeat(61)))
        assertTrue(FieldRules.isValidTaskName("Oil"))
        assertFalse(FieldRules.isValidTaskName("O"))
        assertTrue(FieldRules.isValidContactName("Amma"))
        assertFalse(FieldRules.isValidContactName("A"))
    }

    @Test
    fun `phones are digits with an optional leading plus, 10 to 15 digits`() {
        assertEquals("+919876543210", FieldRules.phoneInput("+91 98765-43210"))
        assertEquals("9876543210", FieldRules.phoneInput("98765+43210"))
        assertTrue(FieldRules.isValidPhone("+919876543210"))
        assertTrue(FieldRules.isValidPhone("9876543210"))
        assertFalse(FieldRules.isValidPhone("98765432"))
        assertFalse(FieldRules.isValidPhone("98765 43210"))
    }

    @Test
    fun `passwords stay 8 to 64`() {
        assertTrue(PasswordRules.isValid("abcdefg1"))
        assertFalse(PasswordRules.isValid("a1".repeat(33)))
    }
}
