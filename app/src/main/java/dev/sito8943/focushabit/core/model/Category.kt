package dev.sito8943.focushabit.core.model

/** A user-defined group of habits (e.g. "Health"). Local only. */
data class Category(
    val id: Long,
    val name: String,
    /** `#rrggbb`, one of [HabitColors.palette] when created here. */
    val color: String,
    /** Font Awesome 7 Free solid icon name, or null to show the initial. */
    val icon: String? = null,
    /** [CategoryPrefab.key] when added from a suggestion and not renamed: its name follows the language. */
    val prefabKey: String? = null,
) {
    /** [name], or the suggestion's name in the current language while it keeps [prefabKey]. */
    fun displayName(spanish: Boolean): String =
        CATEGORY_PREFABS.firstOrNull { it.key == prefabKey }?.name(spanish) ?: name
}

/** Ready-made categories the user can add with one tap. */
data class CategoryPrefab(
    val key: String,
    val nameEn: String,
    val nameEs: String,
    val color: String,
    val icon: String,
) {
    fun name(spanish: Boolean): String = if (spanish) nameEs else nameEn

    /** True when [name] is this suggestion's name in either language (any case, trimmed). */
    fun matches(name: String): Boolean = name.trim().let { it.equals(nameEn, true) || it.equals(nameEs, true) }
}

/** The suggestion [name] belongs to, if [key] is set and the name still matches it. */
fun keepPrefabKey(key: String?, name: String): String? =
    key?.takeIf { k -> CATEGORY_PREFABS.any { it.key == k && it.matches(name) } }

val CATEGORY_PREFABS: List<CategoryPrefab> = listOf(
    CategoryPrefab("health", "Health", "Salud", "#e74c3c", "heart-pulse"),
    CategoryPrefab("fitness", "Fitness", "Deporte", "#f39c12", "dumbbell"),
    CategoryPrefab("mind", "Mind", "Mente", "#9b59b6", "brain"),
    CategoryPrefab("learning", "Learning", "Aprendizaje", "#3498db", "book"),
    CategoryPrefab("work", "Work", "Trabajo", "#1abc9c", "briefcase"),
    CategoryPrefab("home", "Home", "Hogar", "#2ecc71", "house"),
    CategoryPrefab("finance", "Finance", "Finanzas", "#2ecc71", "piggy-bank"),
    CategoryPrefab("social", "Social", "Social", "#3498db", "users"),
)
