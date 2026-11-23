package dev.sito8943.focushabit.feature.categories

import dev.sito8943.focushabit.core.model.CATEGORY_PREFABS
import dev.sito8943.focushabit.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CategoriesUiStateTest {

    private fun state(vararg names: String) =
        CategoriesUiState(names.mapIndexed { i, n -> CategoryItem(Category(i + 1L, n, "#000000"), 0) }, loading = false)

    @Test
    fun `suggestions hide prefabs already added, in either language and any case`() {
        val keys = state(" salud ", "Work").suggestions(spanish = true).map { it.key }
        assertFalse("health" in keys)
        assertFalse("work" in keys)
        assertEquals(CATEGORY_PREFABS.size - 2, keys.size)
    }
}
