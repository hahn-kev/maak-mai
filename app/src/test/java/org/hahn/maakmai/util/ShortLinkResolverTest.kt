package org.hahn.maakmai.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShortLinkResolverTest {

    @Test
    fun `recognises share_google as a short link`() {
        assertThat(ShortLinkResolver.isShortLink("https://share.google/68O7dzVQBj04q18pQ")).isTrue()
        assertThat(ShortLinkResolver.isShortLink("https://SHARE.GOOGLE/abc")).isTrue()
    }

    @Test
    fun `does not treat ordinary urls as short links`() {
        assertThat(ShortLinkResolver.isShortLink("https://example.com/x")).isFalse()
        assertThat(ShortLinkResolver.isShortLink("https://www.google.com/url?q=https://example.com")).isFalse()
        assertThat(ShortLinkResolver.isShortLink("not a url")).isFalse()
    }

    @Test
    fun `follows a redirect chain to its terminal url`() {
        val hops = mapOf(
            "https://share.google/CODE" to "https://www.google.com/share.google?q=CODE",
            "https://www.google.com/share.google?q=CODE" to "https://m.imdb.com/title/tt0111282/",
            "https://m.imdb.com/title/tt0111282/" to "https://www.imdb.com/title/tt0111282/"
        )

        val result = ShortLinkResolver.followChain("https://share.google/CODE") { hops[it] }

        assertThat(result).isEqualTo("https://www.imdb.com/title/tt0111282/")
    }

    @Test
    fun `stops when there is no further redirect`() {
        val result = ShortLinkResolver.followChain("https://example.com/final") { null }

        assertThat(result).isEqualTo("https://example.com/final")
    }

    @Test
    fun `resolves a relative location against the current url`() {
        val hops = mapOf(
            "https://host.test/a" to "/b/c",
            "https://host.test/b/c" to null
        )

        val result = ShortLinkResolver.followChain("https://host.test/a") { hops[it] }

        assertThat(result).isEqualTo("https://host.test/b/c")
    }

    @Test
    fun `guards against redirect loops`() {
        val hops = mapOf(
            "https://a.test/" to "https://b.test/",
            "https://b.test/" to "https://a.test/"
        )

        // Should terminate rather than spin forever.
        val result = ShortLinkResolver.followChain("https://a.test/") { hops[it] }

        assertThat(result).isAnyOf("https://a.test/", "https://b.test/")
    }

    @Test
    fun `caps the number of redirects followed`() {
        // An endless chain of unique urls; must stop at maxRedirects.
        val result = ShortLinkResolver.followChain("https://loop.test/0", maxRedirects = 3) { current ->
            val n = current.substringAfterLast('/').toInt()
            "https://loop.test/${n + 1}"
        }

        assertThat(result).isEqualTo("https://loop.test/3")
    }
}
