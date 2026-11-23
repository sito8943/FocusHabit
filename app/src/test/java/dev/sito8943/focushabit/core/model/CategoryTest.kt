package dev.sito8943.focushabit.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryTest {

    @Test
    fun `a suggestion shows its name in the current language`() {
        val category = Category(1, "Salud", "#000000", prefabKey = "health")
        assertEquals("Health", category.displayName(spanish = false))
        assertEquals("Salud", category.displayName(spanish = true))
    }

    @Test
    fun `a plain or unknown category shows its stored name`() {
        assertEquals("Gym", Category(1, "Gym", "#000000").displayName(spanish = true))
        assertEquals("Gym", Category(1, "Gym", "#000000", prefabKey = "gone").displayName(spanish = false))
    }

    @Test
    fun `the key stays while the name matches the suggestion in either language`() {
        assertEquals("health", keepPrefabKey("health", " health "))
        assertEquals("health", keepPrefabKey("health", "SALUD"))
    }

    @Test
    fun `a rename drops the key`() {
        assertNull(keepPrefabKey("health", "Salud mental"))
        assertNull(keepPrefabKey(null, "Salud"))
    }
}
