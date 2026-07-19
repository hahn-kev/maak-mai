package org.hahn.maakmai.util

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // API 34
class ShareTextParserTest {

    @Test
    fun `title on its own line before the url is captured with the url`() {
        val parsed = ShareTextParser.parse("Some Page Title\nhttps://example.com/article")

        assertThat(parsed.url).isEqualTo("https://example.com/article")
        assertThat(parsed.title).isEqualTo("Some Page Title")
    }

    @Test
    fun `prose containing a url captures the url`() {
        val parsed = ShareTextParser.parse("Check this out https://example.com/x")

        assertThat(parsed.url).isEqualTo("https://example.com/x")
        assertThat(parsed.title).isEqualTo("Check this out")
    }

    @Test
    fun `space separated title and url captures both`() {
        val parsed = ShareTextParser.parse("Title https://example.com/x")

        assertThat(parsed.url).isEqualTo("https://example.com/x")
        assertThat(parsed.title).isEqualTo("Title")
    }

    @Test
    fun `bare url is captured with no derived title`() {
        val parsed = ShareTextParser.parse("https://example.com/article")

        assertThat(parsed.url).isEqualTo("https://example.com/article")
        assertThat(parsed.title).isNull()
    }

    @Test
    fun `url with query and fragment is captured whole`() {
        val parsed = ShareTextParser.parse("Look https://example.com/x?a=1&b=2#frag")

        assertThat(parsed.url).isEqualTo("https://example.com/x?a=1&b=2#frag")
        assertThat(parsed.title).isEqualTo("Look")
    }

    @Test
    fun `subject is used as the title when the text has no title`() {
        val parsed = ShareTextParser.parse(
            text = "https://example.com/article",
            subject = "My Subject"
        )

        assertThat(parsed.url).isEqualTo("https://example.com/article")
        assertThat(parsed.title).isEqualTo("My Subject")
    }

    @Test
    fun `title extra takes precedence over subject as the title fallback`() {
        val parsed = ShareTextParser.parse(
            text = "https://example.com/article",
            subject = "Subject",
            titleExtra = "Title Extra"
        )

        assertThat(parsed.title).isEqualTo("Title Extra")
    }

    @Test
    fun `text derived title beats both subject and title extra`() {
        val parsed = ShareTextParser.parse(
            text = "Real Title\nhttps://example.com/x",
            subject = "Subject",
            titleExtra = "Title Extra"
        )

        assertThat(parsed.title).isEqualTo("Real Title")
    }

    @Test
    fun `text with no url keeps the text as the title and no url`() {
        val parsed = ShareTextParser.parse("just some words with no link")

        assertThat(parsed.url).isNull()
        assertThat(parsed.title).isEqualTo("just some words with no link")
    }

    @Test
    fun `null or blank text yields nulls`() {
        assertThat(ShareTextParser.parse(null).url).isNull()
        assertThat(ShareTextParser.parse(null).title).isNull()
        assertThat(ShareTextParser.parse("   ").url).isNull()
        assertThat(ShareTextParser.parse("   ").title).isNull()
    }

    @Test
    fun `subject is exposed as the description`() {
        val parsed = ShareTextParser.parse(
            text = "Title https://example.com/x",
            subject = "A subject line"
        )

        assertThat(parsed.description).isEqualTo("A subject line")
    }
}
