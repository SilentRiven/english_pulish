package com.workplat.englishpulish.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordEntity::class,
        ReviewStateEntity::class,
        ReviewLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun reviewStateDao(): ReviewStateDao
    abstract fun reviewLogDao(): ReviewLogDao
}
