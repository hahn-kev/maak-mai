package org.hahn.maakmai

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import org.hahn.maakmai.data.AttachmentRepository
import org.hahn.maakmai.images.AttachmentFetcher
import org.hahn.maakmai.images.AttachmentKeyer
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MaakMaiApplication : Application(), SingletonImageLoader.Factory {
    @Inject
    lateinit var attachmentRepository: AttachmentRepository

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(AttachmentFetcher.Factory(attachmentRepository))
                add(AttachmentKeyer())
            }
            .build()
    }
}
