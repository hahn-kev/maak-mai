package org.hahn.maakmai.util

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.hahn.maakmai.util.OpenGraphUtils.OpenGraphMetadata
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // API 34
class LinkPreviewTest {

    @Test
    fun `uses the page metadata`() {
        val preview = LinkPreview.from(
            "https://example.com/a",
            OpenGraphMetadata(title = "Title", description = "About it", image = "https://example.com/i.png")
        )

        assertThat(preview).isEqualTo(
            LinkPreview(
                url = "https://example.com/a",
                title = "Title",
                description = "About it",
                imageUrl = "https://example.com/i.png",
                error = null
            )
        )
    }

    @Test
    fun `a failed fetch keeps the link, takes a title from it and reports the error`() {
        val preview = LinkPreview.from(
            "https://example.com/some-article",
            OpenGraphMetadata(error = "HTTP 404")
        )

        assertThat(preview.url).isEqualTo("https://example.com/some-article")
        assertThat(preview.title).isEqualTo("Some Article")
        assertThat(preview.description).isEmpty()
        assertThat(preview.imageUrl).isNull()
        assertThat(preview.error).isEqualTo("HTTP 404")
    }

    @Test
    fun `a short link takes the address it redirects to`() {
        val preview = LinkPreview.from(
            "https://share.google/abc",
            OpenGraphMetadata(finalUrl = "https://example.com/real-page")
        )

        assertThat(preview.url).isEqualTo("https://example.com/real-page")
        assertThat(preview.title).isEqualTo("Real Page")
    }

    @Test
    fun `an ordinary link keeps its address after a redirect`() {
        val preview = LinkPreview.from(
            "https://example.com/a",
            OpenGraphMetadata(title = "T", finalUrl = "https://example.com/b")
        )

        assertThat(preview.url).isEqualTo("https://example.com/a")
    }
}
