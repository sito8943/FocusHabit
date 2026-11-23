package dev.sito8943.focushabit.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Local source of truth. Schema changes need a `Migration` (never
 * `fallbackToDestructiveMigration`): unsynced habits only live here.
 */
@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, CategoryEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class FocusHabitDatabase : RoomDatabase() {

    abstract fun habits(): HabitDao

    abstract fun logs(): HabitLogDao

    abstract fun categories(): CategoryDao

    companion object {
        fun build(context: Context): FocusHabitDatabase =
            Room.databaseBuilder(context, FocusHabitDatabase::class.java, "focus-habit.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

        /** Icon, kind, frequency, goal and start day. Existing habits stay daily "build" habits. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN icon TEXT")
                db.execSQL("ALTER TABLE habits ADD COLUMN kind TEXT NOT NULL DEFAULT 'build'")
                db.execSQL("ALTER TABLE habits ADD COLUMN frequency TEXT NOT NULL DEFAULT 'daily'")
                db.execSQL("ALTER TABLE habits ADD COLUMN frequency_value INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE habits ADD COLUMN goal INTEGER")
                db.execSQL("ALTER TABLE habits ADD COLUMN start_date TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE habits SET start_date = substr(created_at, 1, 10)")
            }
        }

        /** Optional time of day. Existing habits have none. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN time TEXT")
            }
        }

        /** Categories table and an optional category per habit. Existing habits have none. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`code` TEXT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL, `icon` TEXT, " +
                        "`created_at` TEXT NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_code` ON `categories` (`code`)")
                db.execSQL("ALTER TABLE habits ADD COLUMN category_id INTEGER")
            }
        }
    }
}
