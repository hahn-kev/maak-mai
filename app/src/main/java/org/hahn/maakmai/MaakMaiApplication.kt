package org.hahn.maakmai

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hahn.maakmai.data.AttachmentRepository
import org.hahn.maakmai.images.AttachmentFetcher
import org.hahn.maakmai.images.AttachmentKeyer
import org.hahn.maakmai.images.ImageOptimizer
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MaakMaiApplication : Application(), SingletonImageLoader.Factory {
    @Inject
    lateinit var attachmentRepository: AttachmentRepository

    @Inject
    lateinit var imageOptimizer: ImageOptimizer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Cards size their image from the recorded size; without it the list jumps as images load
        appScope.launch {
            runCatching { imageOptimizer.recordMissingSizes() }
                .onFailure { Timber.e(it, "Could not record image sizes") }
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
