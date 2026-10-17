package org.hahn.maakmai.util

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // API 34
class LinkTextParserTest {

    @Test
    fun `finds every link in prose, in order`() {
        val urls = LinkTextParser.extractUrls(
            """
            Some reading for the week:
            - First https://example.com/one is good
            - Then http://example.org/two?x=1&y=2
            Also see https://example.net/three#part
            """.trimIndent()
        )

        assertThat(urls).containsExactly(
            "https://example.com/one",
            "http://example.org/two?x=1&y=2",
            "https://example.net/three#part"
        ).inOrder()
    }

    @Test
    fun `returns each link once`() {
        val urls = LinkTextParser.extractUrls("https://example.com/a and again https://example.com/a")

        assertThat(urls).containsExactly("https://example.com/a")
    }

    @Test
    fun `drops sentence punctuation after a link`() {
        val urls = LinkTextParser.extractUrls("Read https://example.com/a. Then https://example.com/b, ok?")

        assertThat(urls).containsExactly("https://example.com/a", "https://example.com/b").inOrder()
    }

    @Test
    fun `drops a closing bracket that is not part of the link`() {
        val urls = LinkTextParser.extractUrls("(see https://example.com/a) and [x](https://example.com/b)")

        assertThat(urls).containsExactly("https://example.com/a", "https://example.com/b").inOrder()
    }

    @Test
    fun `keeps brackets that belong to the link`() {
        val urls = LinkTextParser.extractUrls("https://en.wikipedia.org/wiki/Mercury_(planet)")

        assertThat(urls).containsExactly("https://en.wikipedia.org/wiki/Mercury_(planet)")
    }

    @Test
    fun `adds a scheme to bare domains`() {
        val urls = LinkTextParser.extractUrls("go to www.example.com/page today")

        assertThat(urls).containsExactly("https://www.example.com/page")
    }

    @Test
    fun `unwraps google redirect links`() {
        val urls = LinkTextParser.extractUrls(
            "https://www.google.com/url?q=https://example.com/dest&sa=D"
        )

        assertThat(urls).containsExactly("https://example.com/dest")
    }

    @Test
    fun `text without links gives no links`() {
        assertThat(LinkTextParser.extractUrls("nothing to see here")).isEmpty()
        assertThat(LinkTextParser.extractUrls("")).isEmpty()
    }
}
