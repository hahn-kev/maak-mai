package org.hahn.maakmai.bulkadd

import com.google.common.truth.Truth.assertThat
import org.hahn.maakmai.util.LinkPreview
import org.junit.Test

class BulkAddOrderTest {

    private fun loaded(url: String, error: String? = null) = BulkLinkItem(
        sourceUrl = url,
        preview = LinkPreview(url = url, title = url, description = "", imageUrl = null, error = error),
        isChecked = error == null
    )

    @Test
    fun `failed links go to the bottom and each group keeps its order`() {
        val items = listOf(
            loaded("a", error = "Timed out"),
            loaded("b"),
            BulkLinkItem(sourceUrl = "c"),
            loaded("d", error = "HTTP 404"),
            loaded("e")
        )

        assertThat(displayOrder(items).map { it.sourceUrl })
            .containsExactly("b", "c", "e", "a", "d").inOrder()
    }
}
