# 02 — Auto-capture shared links so an un-saved share isn't lost

**What to build:** Sharing a link into the app persists a bookmark *immediately*,
the moment the share arrives — not only when the user taps Save. The Add screen
then becomes an in-place edit of that already-saved bookmark: refining title, tags,
etc. updates it. If the user backs out, taps the top-left close, or swipes the app
away, the link is still saved. An explicit discard action deletes the
just-captured bookmark for the accidental-share case.

This restructures the share flow to **persist-in-share-handler, then open the
editor by bookmark id** (mirroring the existing edit-by-id path), rather than
threading the URL to the editor through a navigation route string. That removes the
route encode/decode round-trip entirely, so shared URLs with special characters
(`+`, `%`, `/`, `?`, `&`, `#`, `%20`, `%2F`) are stored byte-for-byte — this
ticket therefore also closes the special-character corruption bug (spec item c).

**Blocked by:** 01 (uses the parsed { url, title } it produces).

**Status:** done

- [x] Sharing a link creates a persisted bookmark before the user does anything
- [x] Editing on the Add screen updates that same bookmark (no duplicate created)
- [x] Pressing back / close / swiping the app away after sharing leaves the link saved
- [x] An explicit discard action removes the auto-captured bookmark
- [x] Shared URLs containing `+ % / ? & # %20 %2F` are stored byte-for-byte (special-char regression)
- [x] Re-sharing / re-entering the same share doesn't create duplicates within the same flow

## Comments

Implemented via a persist-first restructure:

- New pure `util/SharedBookmarkFactory.fromShare(parsed, id)` builds the bookmark
  captured the moment a share arrives (URL stored verbatim, sensible title,
  untagged). Title-from-URL fallback moved out of the view model into
  `util/UrlTitleExtractor`. Both covered by `SharedBookmarkFactoryTest`.
- `ShareUrlActivity` now injects `BookmarkRepository`, persists the bookmark on
  arrival, then opens the Add screen *by id* via the new
  `MaakMaiNavigationActions.shareCaptureRoute(id)`. A `savedInstanceState`-backed
  captured id prevents a second insert on rotation/recreation.
- The URL/subject/bookmarkTitle route args, `addFromShareRoute`, and the view
  model's `processSharedContent` were removed — the editor reads everything from
  the persisted record, so the shared URL is never encoded into and re-decoded out
  of a nav route. This closes the special-character corruption bug (spec item c).
- The Add screen's existing Delete action is the explicit discard (deletes the
  captured bookmark); `onBookmarkDelete` now closes the share activity.

Note: async OpenGraph enrichment is intentionally *not* re-added here — that is
ticket 03, which builds on this persist-first flow. End-to-end emulator
verification wasn't run (emulator tests are disabled in this repo); the capture
logic and byte-for-byte URL guarantee are covered by unit tests.
