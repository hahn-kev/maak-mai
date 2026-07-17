package org.hahn.maakmai.util

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // API 34
class GoogleUrlUnwrapperTest {

    @Test
    fun `unwraps a google url wrapper to its q destination`() {
        val result = GoogleUrlUnwrapper.unwrap("https://www.google.com/url?q=https://example.com/x&sa=U")

        assertThat(result).isEqualTo("https://example.com/x")
    }

    @Test
    fun `unwraps a percent-encoded destination`() {
        val result = GoogleUrlUnwrapper.unwrap(
            "https://www.google.com/url?q=https%3A%2F%2Fexample.com%2Fa%2Fb%3Fc%3D1&sa=U"
        )

        assertThat(result).isEqualTo("https://example.com/a/b?c=1")
    }

    @Test
    fun `leaves a normal url unchanged`() {
        val url = "https://example.com/x?a=1&b=2"

        assertThat(GoogleUrlUnwrapper.unwrap(url)).isEqualTo(url)
    }

    @Test
    fun `leaves a non-url q value unchanged`() {
        // A Google search page, not a redirect wrapper.
        val url = "https://www.google.com/search?q=stargate"

        assertThat(GoogleUrlUnwrapper.unwrap(url)).isEqualTo(url)
    }

    @Test
    fun `does not treat share_google as a query wrapper`() {
        // share.google needs network redirect resolution, not query unwrapping.
        val url = "https://share.google/ETUuyApubCcZReKwf"

        assertThat(GoogleUrlUnwrapper.unwrap(url)).isEqualTo(url)
    }

    @Test
    fun `only unwraps when the q value is an http url`() {
        val url = "https://www.google.com/url?q=mailto:someone@example.com"

        assertThat(GoogleUrlUnwrapper.unwrap(url)).isEqualTo(url)
    }
}
