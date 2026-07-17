package org.hahn.maakmai.util

import com.google.common.truth.Truth.assertThat
import org.hahn.maakmai.util.OpenGraphEnricher.Edited
import org.hahn.maakmai.util.OpenGraphEnricher.Fields
import org.junit.Test

class OpenGraphEnricherTest {

    private val captured = Fields(
        title = "Text Derived Title",
        description = "",
        imageUri = null
    )

    @Test
    fun `successful metadata populates title description and image`() {
        val result = OpenGraphEnricher.enrich(
            current = captured,
            ogTitle = "OG Title",
            ogDescription = "OG Description",
            ogImage = "https://example.com/og.png",
            edited = Edited()
        )

        assertThat(result.title).isEqualTo("OG Title")
        assertThat(result.description).isEqualTo("OG Description")
        assertThat(result.imageUri).isEqualTo("https://example.com/og.png")
    }

    @Test
    fun `failed or empty metadata leaves the text-derived title unchanged`() {
        val result = OpenGraphEnricher.enrich(
            current = captured,
            ogTitle = null,
            ogDescription = null,
            ogImage = null,
            edited = Edited()
        )

        assertThat(result).isEqualTo(captured)
    }

    @Test
    fun `blank metadata values are treated as absent`() {
        val result = OpenGraphEnricher.enrich(
            current = captured,
            ogTitle = "   ",
            ogDescription = "",
            ogImage = "  ",
            edited = Edited()
        )

        assertThat(result).isEqualTo(captured)
    }

    @Test
    fun `never overwrites a title the user has already edited`() {
        val current = captured.copy(title = "User Typed This")

        val result = OpenGraphEnricher.enrich(
            current = current,
            ogTitle = "OG Title",
            ogDescription = "OG Description",
            ogImage = null,
            edited = Edited(title = true)
        )

        assertThat(result.title).isEqualTo("User Typed This")
        // Description wasn't edited, so it still gets enriched.
        assertThat(result.description).isEqualTo("OG Description")
    }

    @Test
    fun `never overwrites a description the user has already edited`() {
        val current = captured.copy(description = "User Description")

        val result = OpenGraphEnricher.enrich(
            current = current,
            ogTitle = "OG Title",
            ogDescription = "OG Description",
            ogImage = null,
            edited = Edited(description = true)
        )

        assertThat(result.description).isEqualTo("User Description")
        assertThat(result.title).isEqualTo("OG Title")
    }

    @Test
    fun `does not replace an image the user has chosen`() {
        val current = captured.copy(imageUri = "content://user/picked")

        val result = OpenGraphEnricher.enrich(
            current = current,
            ogTitle = null,
            ogDescription = null,
            ogImage = "https://example.com/og.png",
            edited = Edited(image = true)
        )

        assertThat(result.imageUri).isEqualTo("content://user/picked")
    }

    @Test
    fun `does not replace an existing image even if not explicitly edited`() {
        val current = captured.copy(imageUri = "content://existing")

        val result = OpenGraphEnricher.enrich(
            current = current,
            ogTitle = null,
            ogDescription = null,
            ogImage = "https://example.com/og.png",
            edited = Edited()
        )

        assertThat(result.imageUri).isEqualTo("content://existing")
    }
}
