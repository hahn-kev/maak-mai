# 04 — Unwrap Google redirect share URLs

**What to build:** When a shared link is a known redirect wrapper —
`https://www.google.com/url?q=<destination>&sa=...` and similar — the app captures
and stores the `<destination>`, not the wrapper. Non-wrapper URLs are stored
unchanged.

Note: confidence that this shape is actually common is low (the Google app usually
shares the destination directly). Keep the unwrapping list small and conservative;
this is a nice-to-have, not a load-bearing fix.

**Blocked by:** 01 (extends the parsed-URL step).

**Status:** done

- [x] Sharing `https://www.google.com/url?q=https://example.com/x&sa=U` captures `https://example.com/x`
- [x] A normal `https://example.com/x` is stored unchanged
- [x] Unwrapping is limited to a small, documented set of known redirect wrappers
- [x] Unit tests cover wrapped and unwrapped inputs

## Comments

Two distinct redirect mechanisms, handled separately:

**Query wrappers (`google.com/url?q=<dest>`)** — pure, synchronous string
unwrap. New `util/GoogleUrlUnwrapper.unwrap(url)` returns the `q`/`url`
destination when the host is `www.google.com`/`google.com`, the path is `/url`,
and the destination is an http(s) URL; everything else passes through unchanged.
Wired into `ShareTextParser` so the parsed URL is already the destination.
Covered by `GoogleUrlUnwrapperTest`.

**Redirect shorteners (`share.google/<code>`)** — added on top of the original
ticket at the user's request. Verified live that these are NOT query wrappers:
`share.google/<code>` 302s to `www.google.com/share.google?q=<code>` (where `q`
is the opaque code, not a URL) and then 301s on to the real destination. The
chain is plain HTTP 3xx (no JavaScript), so it resolves with a normal HTTP
client. Rather than a separate resolver round-trip, the existing OpenGraph fetch
(which already follows redirects) now reports the final landed URL via
`OpenGraphMetadata.finalUrl`; `AddEditBookmarkViewModel.enrichCapturedShare`
adopts it for known short-link hosts only (`util/ShortLinks.isShortLink`, unit-
tested) and re-derives the URL-based fallback title from the resolved destination
(so a share.google code no longer shows as the title). On any network failure the
original URL is kept — the link is never lost. A spinner in the URL field
(`isEnriching`) shows while the fetch is in flight.

Verified end-to-end on the emulator with the two real links the user provided:
`share.google/68O7dzVQBj04q18pQ` → `www.imdb.com/title/tt0111282/`, and
`share.google/ETUuyApubCcZReKwf` → a `google.com/search?...q=Stargate` page.

Note: sites with aggressive bot protection (e.g. IMDb returns HTTP 202 to any
non-browser client) don't yield OpenGraph metadata to our lightweight fetcher, so
the title falls back to the URL-derived value ("Imdb"). That's an inherent OG
limitation, not specific to redirect handling.
