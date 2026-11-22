package dev.sito8943.focushabit.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local source of truth. Schema changes need a `Migration` (never
 * `fallbackToDestructiveMigration`): unsynced habits only live here.
 */
@Database(
    entities = [HabitEntity::class, HabitLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FocusHabitDatabase : RoomDatabase() {

    abstract fun habits(): HabitDao

    abstract fun logs(): HabitLogDao

    companion object {
        fun build(context: Context): FocusHabitDatabase =
            Room.databaseBuilder(context, FocusHabitDatabase::class.java, "focus-habit.db").build()
    }
}
