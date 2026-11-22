package dev.sito8943.focushabit.core.sync

import dev.sito8943.focushabit.core.network.SupabaseRest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HabitDto(
    val id: Long,
    val code: String,
    val name: String,
    val color: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class HabitLogDto(
    val id: Long,
    @SerialName("habit_id") val habitId: Long,
    @SerialName("log_date") val logDate: String,
)

@Serializable
private data class NewHabit(val name: String, val color: String, val code: String)

@Serializable
private data class NewLog(
    @SerialName("habit_id") val habitId: Long,
    @SerialName("log_date") val logDate: String,
)

@Serializable
private data class SoftDelete(@SerialName("deleted_at") val deletedAt: String)

/** The server side of sync. Abstract so the engine can be tested without HTTP. */
interface HabitEndpoints {
    suspend fun listHabits(code: String): List<HabitDto>
    suspend fun listLogs(code: String): List<HabitLogDto>
    suspend fun createHabit(code: String, name: String, color: String): HabitDto
    suspend fun createLog(habitRemoteId: Long, logDate: String): HabitLogDto
    suspend fun softDeleteHabit(remoteId: Long, code: String, at: String)
    suspend fun softDeleteLog(remoteId: Long, at: String)
}

/**
 * Same tables and filters as the web app (`entities/habits`, `entities/logs`).
 * Codes are `[A-Z0-9]{4}` and ids are numbers, so nothing here needs URL
 * escaping.
 */
class HabitApi(
    private val rest: SupabaseRest,
    private val json: Json,
) : HabitEndpoints {

    override suspend fun listHabits(code: String): List<HabitDto> = json.decodeFromString(
        rest.get(
            "habits?select=id,code,name,color,created_at" +
                "&code=eq.$code&deleted_at=is.null&order=created_at.asc,id.asc",
        ),
    )

    /**
     * Logs have no `code` column: they are scoped through the inner join to
     * their habit. Paged, because PostgREST caps a response at 1000 rows and a
     * daily habit passes that in under three years.
     */
    override suspend fun listLogs(code: String): List<HabitLogDto> {
        val all = mutableListOf<HabitLogDto>()
        while (true) {
            val page: List<HabitLogDto> = json.decodeFromString(
                rest.get(
                    "habit_logs?select=id,habit_id,log_date,habits!inner(code)" +
                        "&habits.code=eq.$code&deleted_at=is.null" +
                        "&order=id.asc&limit=$PAGE&offset=${all.size}",
                ),
            )
            all += page
            if (page.size < PAGE) return all
        }
    }

    override suspend fun createHabit(code: String, name: String, color: String): HabitDto =
        json.decodeFromString<List<HabitDto>>(
            rest.post("habits", json.encodeToString(NewHabit(name, color, code))),
        ).single()

    /**
     * Reuses an active log for the same habit and day if there is one: the
     * table has no unique key, and a retry after a lost response, or the web
     * marking the same day, would otherwise leave duplicates.
     */
    override suspend fun createLog(habitRemoteId: Long, logDate: String): HabitLogDto {
        val existing: List<HabitLogDto> = json.decodeFromString(
            rest.get(
                "habit_logs?select=id,habit_id,log_date" +
                    "&habit_id=eq.$habitRemoteId&log_date=eq.$logDate&deleted_at=is.null&limit=1",
            ),
        )
        existing.firstOrNull()?.let { return it }
        return json.decodeFromString<List<HabitLogDto>>(
            rest.post("habit_logs", json.encodeToString(NewLog(habitRemoteId, logDate))),
        ).single()
    }

    /**
     * Logs first, then the habit. The web does it the other way round and its
     * log step then finds no habit and skips, leaving the logs alive.
     */
    override suspend fun softDeleteHabit(remoteId: Long, code: String, at: String) {
        val body = json.encodeToString(SoftDelete(at))
        rest.patch("habit_logs?habit_id=eq.$remoteId&deleted_at=is.null", body)
        rest.patch("habits?id=eq.$remoteId&code=eq.$code&deleted_at=is.null", body)
    }

    override suspend fun softDeleteLog(remoteId: Long, at: String) {
        rest.patch(
            "habit_logs?id=eq.$remoteId&deleted_at=is.null",
            json.encodeToString(SoftDelete(at)),
        )
    }

    private companion object {
        const val PAGE = 1000
    }
}
