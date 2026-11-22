package dev.sito8943.focushabit.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Everything the server does not have yet: new rows and soft deletes of rows
 * it does have. Logs of a deleted habit do not count: deleting the habit on
 * the server takes them with it.
 */
private const val PENDING_COUNT = """
    SELECT
      (SELECT COUNT(*) FROM habits WHERE remote_id IS NULL AND deleted_at IS NULL)
    + (SELECT COUNT(*) FROM habits WHERE remote_id IS NOT NULL AND deleted_at IS NOT NULL)
    + (SELECT COUNT(*) FROM habit_logs l JOIN habits h ON h.id = l.habit_id
         WHERE h.deleted_at IS NULL
           AND ((l.remote_id IS NULL AND l.deleted_at IS NULL)
             OR (l.remote_id IS NOT NULL AND l.deleted_at IS NOT NULL)))
"""

@Dao
interface HabitDao {

    @Query(
        "SELECT * FROM habits WHERE code = :code AND deleted_at IS NULL ORDER BY created_at, id",
    )
    fun observeActive(code: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun get(id: Long): HabitEntity?

    /** Including soft-deleted rows: the pull needs to see them to avoid resurrecting them. */
    @Query("SELECT * FROM habits WHERE code = :code")
    suspend fun allForCode(code: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE remote_id IS NULL AND deleted_at IS NULL ORDER BY id")
    suspend fun pendingCreates(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE remote_id IS NOT NULL AND deleted_at IS NOT NULL")
    suspend fun pendingDeletes(): List<HabitEntity>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("UPDATE habits SET remote_id = :remoteId, created_at = :createdAt WHERE id = :id")
    suspend fun markCreated(id: Long, remoteId: Long, createdAt: String)

    @Query("UPDATE habits SET deleted_at = :at WHERE id = :id")
    suspend fun markDeleted(id: Long, at: String)

    /** Logs go with it (foreign key cascade). */
    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(PENDING_COUNT)
    fun observePendingCount(): Flow<Int>

    @Query(PENDING_COUNT)
    suspend fun pendingCount(): Int
}

/** A log together with the server id of its habit, for the push. */
data class PendingLog(
    val id: Long,
    val remoteId: Long?,
    val habitRemoteId: Long?,
    val logDate: String,
)

@Dao
interface HabitLogDao {

    /** Days done for the visible habits of a code. */
    @Query(
        """
        SELECT l.* FROM habit_logs l JOIN habits h ON h.id = l.habit_id
        WHERE h.code = :code AND h.deleted_at IS NULL AND l.deleted_at IS NULL
        """,
    )
    fun observeActive(code: String): Flow<List<HabitLogEntity>>

    @Query(
        "SELECT * FROM habit_logs WHERE habit_id = :habitId AND log_date = :date AND deleted_at IS NULL",
    )
    suspend fun activeOn(habitId: Long, date: String): List<HabitLogEntity>

    @Query("SELECT l.* FROM habit_logs l JOIN habits h ON h.id = l.habit_id WHERE h.code = :code")
    suspend fun allForCode(code: String): List<HabitLogEntity>

    @Query(
        """
        SELECT l.id, l.remote_id AS remoteId, h.remote_id AS habitRemoteId, l.log_date AS logDate
        FROM habit_logs l JOIN habits h ON h.id = l.habit_id
        WHERE l.remote_id IS NULL AND l.deleted_at IS NULL
          AND h.remote_id IS NOT NULL AND h.deleted_at IS NULL
        ORDER BY l.id
        """,
    )
    suspend fun pendingCreates(): List<PendingLog>

    @Query(
        """
        SELECT l.id, l.remote_id AS remoteId, h.remote_id AS habitRemoteId, l.log_date AS logDate
        FROM habit_logs l JOIN habits h ON h.id = l.habit_id
        WHERE l.remote_id IS NOT NULL AND l.deleted_at IS NOT NULL AND h.deleted_at IS NULL
        """,
    )
    suspend fun pendingDeletes(): List<PendingLog>

    @Query("SELECT * FROM habit_logs WHERE remote_id = :remoteId")
    suspend fun findByRemoteId(remoteId: Long): HabitLogEntity?

    @Query(
        "SELECT * FROM habit_logs WHERE habit_id = :habitId AND log_date = :date " +
            "AND remote_id IS NULL AND deleted_at IS NULL LIMIT 1",
    )
    suspend fun pendingOn(habitId: Long, date: String): HabitLogEntity?

    @Insert
    suspend fun insert(log: HabitLogEntity): Long

    @Query("UPDATE habit_logs SET remote_id = :remoteId WHERE id = :id")
    suspend fun markCreated(id: Long, remoteId: Long)

    @Query("UPDATE habit_logs SET deleted_at = :at WHERE id = :id")
    suspend fun markDeleted(id: Long, at: String)

    @Query("DELETE FROM habit_logs WHERE id = :id")
    suspend fun delete(id: Long)

    /** Logs that never reached the server: nothing to push for them once their habit is gone. */
    @Query("DELETE FROM habit_logs WHERE habit_id = :habitId AND remote_id IS NULL")
    suspend fun deleteUnsyncedOf(habitId: Long)
}
