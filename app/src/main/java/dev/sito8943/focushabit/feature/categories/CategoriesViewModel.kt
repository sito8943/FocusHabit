package dev.sito8943.focushabit.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sito8943.focushabit.core.data.HabitRepository
import dev.sito8943.focushabit.core.model.CATEGORY_PREFABS
import dev.sito8943.focushabit.core.model.Category
import dev.sito8943.focushabit.core.model.CategoryPrefab
import dev.sito8943.focushabit.core.settings.SyncCodeStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryItem(val category: Category, val habitCount: Int)

data class CategoriesUiState(
    val items: List<CategoryItem> = emptyList(),
    val loading: Boolean = true,
) {
    /** Prefabs not added yet, by key or by name in either language. */
    fun suggestions(spanish: Boolean): List<CategoryPrefab> {
        val keys = items.mapNotNullTo(HashSet()) { it.category.prefabKey }
        val names = items.map { it.category.name }
        return CATEGORY_PREFABS.filter { prefab -> prefab.key !in keys && names.none(prefab::matches) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel(
    private val repository: HabitRepository,
    private val codeStore: SyncCodeStore,
) : ViewModel() {

    val state: StateFlow<CategoriesUiState> = codeStore.code
        .flatMapLatest { code ->
            combine(repository.observeCategories(code), repository.observeHabits(code)) { categories, habits ->
                val counts = habits.groupingBy { it.categoryId }.eachCount()
                CategoriesUiState(categories.map { CategoryItem(it, counts[it.id] ?: 0) }, loading = false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    /** Called with the id of each category created here (not edits). */
    var onCreated: ((Long) -> Unit)? = null

    /** Creates it when [Category.id] is 0, otherwise edits it. */
    fun save(category: Category) {
        if (category.name.isBlank()) return
        // Taken now: leaving the screen right after saving still selects it.
        val notify = onCreated.takeIf { category.id == 0L }
        viewModelScope.launch {
            val id = repository.saveCategory(codeStore.code.value, category.copy(name = category.name.trim()))
            notify?.invoke(id)
        }
    }

    fun addPrefab(prefab: CategoryPrefab, spanish: Boolean) =
        save(Category(id = 0, name = prefab.name(spanish), color = prefab.color, icon = prefab.icon, prefabKey = prefab.key))

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }
}
