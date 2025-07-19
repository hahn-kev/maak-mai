package org.hahn.maakmai.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.hahn.maakmai.model.Bookmark

@Database(entities = [Bookmark::class], version = 1)
@TypeConverters(Converters::class)
abstract class MaakMaiDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}
