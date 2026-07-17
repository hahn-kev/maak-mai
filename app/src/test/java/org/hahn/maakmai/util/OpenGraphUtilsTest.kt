package org.hahn.maakmai.util

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // API 34
class OpenGraphUtilsTest {

    @Test
    fun `test HTML entity decoding in meta tags`() {
        // Create HTML with encoded entities
        val html = """
            <html>
            <head>
                <meta property="og:title" content="Test &quot;quoted&quot; title" />
                <meta property="og:description" content="Description with &amp; and &lt;tags&gt;" />
                <title>Regular &quot;title&quot; with entities</title>
            </head>
            <body>
                <p>Some content</p>
            </body>
            </html>
        """.trimIndent()

        // Parse the HTML
        val metadata = OpenGraphUtils.parseOpenGraphMetadata(html)

        // Verify the entities are decoded
        assertEquals("Test \"quoted\" title", metadata.title)
        assertEquals("Description with & and <tags>", metadata.description)
    }

    @Test
    fun `test HTML entity decoding in title tag`() {
        // Create HTML with encoded entities in title but no OG tags
        val html = """
            <html>
            <head>
                <title>Regular &quot;title&quot; with &amp; entities</title>
            </head>
            <body>
                <p>Some content</p>
            </body>
            </html>
        """.trimIndent()

        // Parse the HTML
        val metadata = OpenGraphUtils.parseOpenGraphMetadata(html)

        // Verify the entities in the title are decoded
        assertEquals("Regular \"title\" with & entities", metadata.title)
    }
}