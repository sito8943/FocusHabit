package dev.sito8943.focushabit.core.sync

import androidx.room.withTransaction
import dev.sito8943.focushabit.core.data.FocusHabitDatabase
import dev.sito8943.focushabit.core.data.HabitEntity
import dev.sito8943.focushabit.core.data.HabitLogEntity
import java.time.Instant

/**
 * Push what only exists on this phone, then pull the server copy of a code.
 *
 * Order of the push is about foreign keys: a log needs its habit's server id.
 *
 * 1. Soft-deleted habits (and their logs) on the server, then gone here.
 * 2. New habits; each gets its `remote_id`.
 * 3. Soft-deleted logs.
 * 4. New logs of habits that are on the server.
 *
 * Creates are idempotent by construction: only rows without `remote_id` are
 * sent, and the id is saved as soon as the server returns it.
 *
 * The pull treats the server as the truth for synced rows: anything with a
 * `remote_id` the server no longer lists was deleted elsewhere (the web, the
 * other phone) and goes away here too. Rows still waiting to be pushed are
 * never touched.
 */
class SyncEngine(
    private val database: FocusHabitDatabase,
    private val api: HabitEndpoints,
    private val now: () -> Instant = Instant::now,
) {

    private val habits = database.habits()
    private val logs = database.logs()

    suspend fun push() {
        habits.pendingDeletes().forEach { habit ->
            api.softDeleteHabit(habit.remoteId!!, habit.code, habit.deletedAt ?: now().toString())
            habits.delete(habit.id)
        }
        habits.pendingCreates().forEach { habit ->
            val created = api.createHabit(habit.code, habit.name, habit.color)
            habits.markCreated(habit.id, created.id, created.createdAt)
        }
        logs.pendingDeletes().forEach { log ->
            api.softDeleteLog(log.remoteId!!, now().toString())
            logs.delete(log.id)
        }
        logs.pendingCreates().forEach { log ->
            val created = api.createLog(log.habitRemoteId!!, log.logDate)
            // The server already had that day (web, other phone) and it is
            // already here: this row is a duplicate of it.
            if (logs.findByRemoteId(created.id) != null) {
                logs.delete(log.id)
            } else {
                logs.markCreated(log.id, created.id)
            }
        }
    }

    /** Downloads [code] and merges it. Throws without touching Room if the download fails. */
    suspend fun pull(code: String) {
        val remoteHabits = api.listHabits(code)
        val remoteLogs = api.listLogs(code)
        merge(code, remoteHabits, remoteLogs)
    }

    private suspend fun merge(
        code: String,
        remoteHabits: List<HabitDto>,
        remoteLogs: List<HabitLogDto>,
    ) = database.withTransaction {
        val localHabits = habits.allForCode(code).filter { it.remoteId != null }
            .associateBy { it.remoteId!! }
        val serverHabitIds = remoteHabits.mapTo(HashSet()) { it.id }

        localHabits.values
            .filter { it.remoteId !in serverHabitIds }
            .forEach { habits.delete(it.id) }

        val localIdByRemote = HashMap<Long, Long>()
        val deletedHere = HashSet<Long>()
        remoteHabits.forEach { dto ->
            val local = localHabits[dto.id]
            when {
                local == null -> localIdByRemote[dto.id] = habits.insert(
                    HabitEntity(
                        remoteId = dto.id,
                        code = code,
                        name = dto.name,
                        color = dto.color,
                        createdAt = dto.createdAt,
                        startDate = dto.createdAt.take(10),
                    ),
                )

                else -> {
                    localIdByRemote[dto.id] = local.id
                    if (local.deletedAt != null) {
                        deletedHere += dto.id
                    } else if (local.name != dto.name || local.color != dto.color) {
                        habits.update(local.copy(name = dto.name, color = dto.color))
                    }
                }
            }
        }

        val localLogs = logs.allForCode(code).filter { it.remoteId != null }
            .associateBy { it.remoteId!! }
        val serverLogIds = remoteLogs.mapTo(HashSet()) { it.id }

        localLogs.values
            .filter { it.remoteId !in serverLogIds }
            .forEach { logs.delete(it.id) }

        remoteLogs.forEach { dto ->
            if (dto.id in localLogs || dto.habitId in deletedHere) return@forEach
            val habitId = localIdByRemote[dto.habitId] ?: return@forEach
            // Same day marked here and not pushed yet: adopt the server row.
            val pending = logs.pendingOn(habitId, dto.logDate)
            if (pending != null) {
                logs.markCreated(pending.id, dto.id)
            } else {
                logs.insert(HabitLogEntity(remoteId = dto.id, habitId = habitId, logDate = dto.logDate))
            }
        }
    }
}
