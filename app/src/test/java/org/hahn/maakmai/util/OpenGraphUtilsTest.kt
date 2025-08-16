package org.hahn.maakmai.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

        // Use reflection to access private methods for testing
        val parseMethod = OpenGraphUtils::class.java.getDeclaredMethod("parseOpenGraphMetadata", String::class.java)
        parseMethod.isAccessible = true

        // Parse the HTML
        val metadata = parseMethod.invoke(OpenGraphUtils, html) as OpenGraphUtils.OpenGraphMetadata

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

        // Use reflection to access private methods for testing
        val parseMethod = OpenGraphUtils::class.java.getDeclaredMethod("parseOpenGraphMetadata", String::class.java)
        parseMethod.isAccessible = true

        // Parse the HTML
        val metadata = parseMethod.invoke(OpenGraphUtils, html) as OpenGraphUtils.OpenGraphMetadata

        // Verify the entities in the title are decoded
        assertEquals("Regular \"title\" with & entities", metadata.title)
    }
}