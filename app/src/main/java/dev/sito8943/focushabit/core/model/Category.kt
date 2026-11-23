package dev.sito8943.focushabit.core.model

/** A user-defined group of habits (e.g. "Health"). Local only. */
data class Category(
    val id: Long,
    val name: String,
    /** `#rrggbb`, one of [HabitColors.palette] when created here. */
    val color: String,
    /** Font Awesome 7 Free solid icon name, or null to show the initial. */
    val icon: String? = null,
)

/** Ready-made categories the user can add with one tap. */
data class CategoryPrefab(
    val key: String,
    val nameEn: String,
    val nameEs: String,
    val color: String,
    val icon: String,
) {
    fun name(spanish: Boolean): String = if (spanish) nameEs else nameEn
}

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
