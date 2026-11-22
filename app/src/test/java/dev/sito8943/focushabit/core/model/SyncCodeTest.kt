package dev.sito8943.focushabit.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCodeTest {

    @Test
    fun `generated codes are valid and avoid ambiguous characters`() {
        repeat(500) {
            val code = SyncCode.generate()
            assertTrue(code, SyncCode.isValid(code))
            assertTrue(code, code.none { it in "IO01" })
        }
    }

    @Test
    fun `normalize uppercases, strips and truncates like the web`() {
        assertEquals("AB12", SyncCode.normalize(" ab-12x "))
        assertEquals("", SyncCode.normalize("--"))
    }

    @Test
    fun `validation accepts any letter or digit`() {
        assertTrue(SyncCode.isValid("IO01"))
        assertFalse(SyncCode.isValid("AB1"))
        assertFalse(SyncCode.isValid("ab12"))
    }
}
