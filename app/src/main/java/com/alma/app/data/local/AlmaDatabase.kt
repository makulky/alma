package com.alma.app.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec

@RenameColumn(tableName = "books", fromColumnName = "googleVolumeId", toColumnName = "sourceId")
class RenameVolumeIdToSourceId : AutoMigrationSpec

@Database(
    entities = [BookEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = RenameVolumeIdToSourceId::class)],
)
@TypeConverters(Converters::class)
abstract class AlmaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        fun create(context: Context): AlmaDatabase =
            Room.databaseBuilder(context, AlmaDatabase::class.java, "alma.db").build()
    }
}
