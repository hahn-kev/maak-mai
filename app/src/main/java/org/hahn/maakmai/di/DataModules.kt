package org.hahn.maakmai.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.data.BookmarkRepositoryMemory
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.data.FolderRepositoryMemory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryMemory): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryMemory): FolderRepository

}