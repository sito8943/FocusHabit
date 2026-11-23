package dev.sito8943.focushabit.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A habit as stored on this phone. Room is the local source of truth; the
 * server row is addressed only through [remoteId] (the local [id] never
 * matches the backend one).
 *
 * Deletion is logical ([deletedAt]) while the row exists on the server: the
 * soft delete still has to be pushed. A row that never reached the server is
 * deleted for real.
 */
@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["remote_id"], unique = true),
        Index(value = ["code"]),
    ],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "remote_id")
    val remoteId: Long? = null,
    /** Recovery code the habit belongs to. */
    val code: String,
    val name: String,
    val color: String,
    /** ISO-8601 instant. Local time until the server returns its own. Orders the list. */
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: String? = null,
    // Local only for now: the server (and the web) do not have these yet.
    /** Font Awesome icon name. */
    val icon: String? = null,
    /** `build` or `quit`. */
    @ColumnInfo(defaultValue = "build")
    val kind: String = "build",
    /** `daily`, `weekdays`, `interval`, `week` or `month` (see `Frequency.storage`). */
    @ColumnInfo(defaultValue = "daily")
    val frequency: String = "daily",
    @ColumnInfo(name = "frequency_value", defaultValue = "1")
    val frequencyValue: Int = 1,
    val goal: Int? = null,
    /** `YYYY-MM-DD`, local day the habit started counting. */
    @ColumnInfo(name = "start_date", defaultValue = "")
    val startDate: String = "",
    /** `HH:mm`, optional time of day. */
    val time: String? = null,
    /** Optional [CategoryEntity] id; cleared by hand when the category is deleted. */
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
)

/** A user-defined group of habits. Local only: the server has no categories. */
@Entity(tableName = "categories", indices = [Index(value = ["code"])])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Recovery code it belongs to, like the habits. */
    val code: String,
    val name: String,
    val color: String,
    val icon: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    /** Suggestion it came from while not renamed; the UI shows its translated name. */
    @ColumnInfo(name = "prefab_key")
    val prefabKey: String? = null,
)

/**
 * One day marked as done. [logDate] is `YYYY-MM-DD`, the same plain date
 * string the server stores.
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["remote_id"], unique = true),
        Index(value = ["habit_id", "log_date"]),
    ],
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "remote_id")
    val remoteId: Long? = null,
    @ColumnInfo(name = "habit_id")
    val habitId: Long,
    @ColumnInfo(name = "log_date")
    val logDate: String,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: String? = null,
)
