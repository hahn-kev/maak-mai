package org.hahn.maakmai.util

import java.net.URI

/**
 * Classifies redirect *shortener* URLs whose real destination is only known after
 * following redirects — e.g. `share.google/<code>`.
 *
 * The actual resolution is done for free by [OpenGraphUtils], whose fetch already
 * follows redirects and reports the final URL; this only decides *when* it is safe
 * to adopt that resolved URL in place of the shared one. The host set is
 * intentionally small and conservative so ordinary links are never rewritten.
 */
object ShortLinks {

    private val HOSTS = setOf("share.google")

    fun isShortLink(url: String): Boolean {
        return try {
            URI(url).host?.lowercase() in HOSTS
        } catch (e: Exception) {
            false
        }
    }
}
