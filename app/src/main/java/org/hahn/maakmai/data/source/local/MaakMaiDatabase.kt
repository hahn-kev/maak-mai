package org.hahn.maakmai.data.source.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.hahn.maakmai.data.Folder
import org.hahn.maakmai.model.Attachment
import org.hahn.maakmai.model.Bookmark

@Database(
    entities = [Bookmark::class, Folder::class, Attachment::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4)
    ]
)
@TypeConverters(Converters::class)
abstract class MaakMaiDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun folderDao(): FolderDao
    abstract fun attachmentDao(): AttachmentDao
}
