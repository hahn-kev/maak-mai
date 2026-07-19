package org.hahn.maakmai.util

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // API 34
class SharedBookmarkFactoryTest {

    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `captures the url and derived title from a title-plus-url share`() {
        val parsed = ShareTextParser.ParsedShare(
            url = "https://example.com/article",
            title = "Some Page Title",
            description = null
        )

        val bookmark = SharedBookmarkFactory.fromShare(parsed, id)

        assertThat(bookmark.id).isEqualTo(id)
        assertThat(bookmark.url).isEqualTo("https://example.com/article")
        assertThat(bookmark.title).isEqualTo("Some Page Title")
        assertThat(bookmark.tags).isEmpty()
    }

    @Test
    fun `uses the subject as the description`() {
        val parsed = ShareTextParser.ParsedShare(
            url = "https://example.com/x",
            title = "Title",
            description = "A subject line"
        )

        val bookmark = SharedBookmarkFactory.fromShare(parsed, id)

        assertThat(bookmark.description).isEqualTo("A subject line")
    }

    @Test
    fun `derives a title from the url when the share has none`() {
        val parsed = ShareTextParser.ParsedShare(
            url = "https://example.com/some-great-article",
            title = null,
            description = null
        )

        val bookmark = SharedBookmarkFactory.fromShare(parsed, id)

        // Falls back to the last path segment, cleaned up.
        assertThat(bookmark.title).isEqualTo("Some Great Article")
    }

    @Test
    fun `stores urls with special characters byte-for-byte`() {
        // The persist-first flow passes only the bookmark id to the editor, so the
        // url is never round-tripped through a navigation route: it must survive
        // verbatim, closing the special-character corruption bug.
        val trickyUrls = listOf(
            "https://example.com/a+b",
            "https://example.com/a%20b",
            "https://example.com/a%2Fb",
            "https://example.com/a%25b",
            "https://example.com/x?a=1&b=2",
            "https://example.com/page#frag",
            "https://example.com/p?q=a+b%2Fc#frag",
        )

        for (url in trickyUrls) {
            val bookmark = SharedBookmarkFactory.fromShare(
                ShareTextParser.ParsedShare(url = url, title = "T", description = null),
                id
            )
            assertThat(bookmark.url).isEqualTo(url)
        }
    }

    @Test
    fun `title-less url-less share still yields an empty-string title`() {
        val parsed = ShareTextParser.ParsedShare(url = null, title = null, description = null)

        val bookmark = SharedBookmarkFactory.fromShare(parsed, id)

        assertThat(bookmark.url).isNull()
        assertThat(bookmark.title).isEqualTo("")
    }
}
