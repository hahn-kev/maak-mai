package org.hahn.maakmai.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PageImagesTest {

    private val pageUrl = "https://example.com/films/stargate/"

    @Test
    fun `lists share images first, then img tags in page order`() {
        val html = """
            <html><head>
              <meta property="og:image" content="https://cdn.example.com/poster.jpg">
              <meta name="twitter:image" content="https://cdn.example.com/card.jpg" />
            </head><body>
              <img src="/images/still-1.jpg" alt="">
              <img alt="" src='still-2.png'>
            </body></html>
        """.trimIndent()

        assertEquals(
            listOf(
                "https://cdn.example.com/poster.jpg",
                "https://cdn.example.com/card.jpg",
                "https://example.com/images/still-1.jpg",
                "https://example.com/films/stargate/still-2.png"
            ),
            PageImages.extract(html, pageUrl)
        )
    }

    @Test
    fun `prefers the largest srcset entry and lazy-load attributes`() {
        val html = """
            <img src="small.jpg" srcset="small.jpg 320w, large.jpg 1280w, medium.jpg 640w">
            <img src="placeholder.gif" data-src="/real.jpg">
            <img src="a.jpg" srcset="a.jpg 1x, a@2x.jpg 2x">
        """.trimIndent()

        assertEquals(
            listOf(
                "https://example.com/films/stargate/large.jpg",
                "https://example.com/real.jpg",
                "https://example.com/films/stargate/a@2x.jpg"
            ),
            PageImages.extract(html, pageUrl)
        )
    }

    @Test
    fun `skips icons, tracking pixels, data uris and duplicates`() {
        val html = """
            <meta property="og:image" content="https://example.com/photo.jpg">
            <img src="https://example.com/photo.jpg">
            <img src="/logo.svg">
            <img src="/favicon-32.png">
            <img src="/track.gif" width="1" height="1">
            <img src="data:image/png;base64,AAAA">
            <img src="/ok.webp?w=800&amp;h=600">
        """.trimIndent()

        assertEquals(
            listOf("https://example.com/photo.jpg", "https://example.com/ok.webp?w=800&h=600"),
            PageImages.extract(html, pageUrl)
        )
    }

    @Test
    fun `keeps one copy of an image served at several sizes`() {
        val html = """
            <meta property="og:image" content="https://m.media-amazon.com/images/M/poster@._V1_FMjpg_UX1000_.jpg">
            <img src="https://m.media-amazon.com/images/M/poster@._V1_QL75_UX324_.jpg">
            <img src="https://m.media-amazon.com/images/M/still@._V1_QL75_UX324_.jpg">
        """.trimIndent()

        assertEquals(
            listOf(
                "https://m.media-amazon.com/images/M/poster@._V1_FMjpg_UX1000_.jpg",
                "https://m.media-amazon.com/images/M/still@._V1_QL75_UX324_.jpg"
            ),
            PageImages.extract(html, pageUrl)
        )
    }

    @Test
    fun `reads json-ld images`() {
        val html = """<script type="application/ld+json">{"@type":"Movie","image":"https:\/\/cdn.example.com\/ld.jpg"}</script>"""

        assertEquals(listOf("https://cdn.example.com/ld.jpg"), PageImages.extract(html, pageUrl))
    }
}
