package org.hahn.maakmai.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShortLinksTest {

    @Test
    fun `recognises share_google as a short link`() {
        assertThat(ShortLinks.isShortLink("https://share.google/68O7dzVQBj04q18pQ")).isTrue()
        assertThat(ShortLinks.isShortLink("https://SHARE.GOOGLE/abc")).isTrue()
    }

    @Test
    fun `does not treat ordinary urls as short links`() {
        assertThat(ShortLinks.isShortLink("https://example.com/x")).isFalse()
        assertThat(ShortLinks.isShortLink("https://www.google.com/url?q=https://example.com")).isFalse()
        assertThat(ShortLinks.isShortLink("https://www.imdb.com/title/tt0111282/")).isFalse()
    }

    @Test
    fun `handles malformed input safely`() {
        assertThat(ShortLinks.isShortLink("not a url")).isFalse()
        assertThat(ShortLinks.isShortLink("")).isFalse()
    }
}
