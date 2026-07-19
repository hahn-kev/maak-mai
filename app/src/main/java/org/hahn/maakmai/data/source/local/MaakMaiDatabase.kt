package org.hahn.maakmai.data.source.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import org.hahn.maakmai.data.Folder
import org.hahn.maakmai.model.Attachment
import org.hahn.maakmai.model.Bookmark

@Database(
    entities = [Bookmark::class, Folder::class, Attachment::class],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = Migration6To7::class)
    ]
)
@TypeConverters(Converters::class)
abstract class MaakMaiDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun folderDao(): FolderDao
    abstract fun attachmentDao(): AttachmentDao
}

class Migration6To7 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE bookmarks SET createdAt = ? WHERE createdAt = 0",
            arrayOf(System.currentTimeMillis())
        )
    }
}
