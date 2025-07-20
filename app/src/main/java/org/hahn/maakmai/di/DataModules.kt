package org.hahn.maakmai.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.hahn.maakmai.data.AttachmentRepository
import org.hahn.maakmai.data.AttachmentRepositoryRoom
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.data.BookmarkRepositoryMemory
import org.hahn.maakmai.data.BookmarkRepositoryRoom
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.data.FolderRepositoryRoom
import org.hahn.maakmai.data.source.local.AttachmentDao
import org.hahn.maakmai.data.source.local.MaakMaiDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryRoom): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryRoom): FolderRepository

    @Binds
    @Singleton
    abstract fun bindAttachmentRepository(impl: AttachmentRepositoryRoom): AttachmentRepository
}


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MaakMaiDatabase = Room.databaseBuilder(
        context,
        MaakMaiDatabase::class.java,
        "MaakMai.db"
    )
    .build()

    @Provides
    fun provideBookmarkDao(
        database: MaakMaiDatabase,
    ) = database.bookmarkDao()

    @Provides
    fun provideFolderDao(
        database: MaakMaiDatabase,
    ) = database.folderDao()

    @Provides
    fun provideAttachmentDao(
        database: MaakMaiDatabase,
    ) = database.attachmentDao()

}
