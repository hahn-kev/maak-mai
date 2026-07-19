# Spec: Stop losing shared links

Status: ready-for-agent

Handoff spec for fixing the "shared links are being lost" reports. Produced by the
`/wayfinder` effort in this directory — see [map.md](map.md) for how each finding
was reached, and the `assets/` dir for the code-trace catalog, emulator repros,
screenshots, and the share-payload matrix that back every claim below.

## Problem

Users report links they share into Bookmark Tags going missing. Investigation
found this is **not one bug** — it's several independent failure modes in the
share pipeline:

`ShareUrlActivity` → `MaakMaiNavigation.addFromShareRoute` (route encoding) →
`AddEditBookmarkViewModel.processSharedContent` → user taps Save.

The dominant cause (confirmed against the one concrete report, "Google Search
shares don't work") is that the share handler only accepts text that is *entirely*
a URL, so the common "Title + URL" share shape drops the link. A second, likely
larger cause is that a shared link is never persisted unless the user taps a small
Save button — closing or swiping away discards it silently.

## Fixes, in priority order (user impact = frequency × severity)

Ship top-down; P0s are the point of this effort.

### P0 — (a) "Title + URL" shares drop the link

- **Where:** [ShareUrlActivity.kt:45](../../app/src/main/java/org/hahn/maakmai/ShareUrlActivity.kt#L45)
- **Cause:** `Patterns.WEB_URL.matcher(sharedText).matches()` requires the *whole*
  `EXTRA_TEXT` to be a URL. Google/Reddit/X/WhatsApp/YouTube commonly send
  `"Page Title\nURL"`, `"Title URL"`, or prose containing a URL → match fails →
  `urlParam = null` → URL (and title) dropped, blank Add screen.
- **Fix:** Extract the first URL *found within* the shared text (e.g. `Patterns.WEB_URL`
  as a `find()`/`Matcher` scan, not `matches()`). Use the surrounding text (the
  part that isn't the URL) as the title candidate.
- **Confirmed:** [assets/02-google-repro.md](assets/02-google-repro.md),
  [assets/04-suspect-confirmation.md](assets/04-suspect-confirmation.md).

### P0 — (g) Link lost when Save is never tapped

- **Where:** [AddEditBookmarkScreen.kt:106](../../app/src/main/java/org/hahn/maakmai/addeditbookmark/AddEditBookmarkScreen.kt#L106)
  (X / back → `finish()`), [:120](../../app/src/main/java/org/hahn/maakmai/addeditbookmark/AddEditBookmarkScreen.kt#L120)
  (Save is a small checkmark FAB); `saveBookmark()` is the only write path.
- **Cause:** Nothing is persisted unless the user taps the Save FAB. The top-left
  X, system back, and app-swipe all discard the shared link silently — no
  auto-save, no "unsaved changes" prompt, no "Saved" confirmation. For a
  share-to-save app this feels like losing links even when the pipeline worked.
- **Fix (agreed direction):** **Auto-capture on share** — persist the bookmark
  immediately when a share arrives (URL + derived title, untagged), then let the
  Add screen *edit* that record. Optionally land auto-captured links in a
  lightweight unsorted/"inbox" area so they don't clutter the main list until
  triaged. Make the discard action explicit (delete the just-created bookmark).
- **Confirmed:** by code inspection (see above).

### P1 — (c) Special characters corrupted in the URL

- **Where:** [MaakMaiNavigation.kt:73-83](../../app/src/main/java/org/hahn/maakmai/MaakMaiNavigation.kt#L73)
  (encode into route) ↔ [AddEditBookmarkViewModel.kt:76-78](../../app/src/main/java/org/hahn/maakmai/addeditbookmark/AddEditBookmarkViewModel.kt#L76) (decode).
- **Cause:** The URL is URL-encoded into a Navigation-Compose route string and then
  decoded again, but Nav-Compose also decodes route args — a double-decode.
  Observed: `+`→space, `%2F`→`/` (structural), `%20`→space, `%25`→`%`; invalid
  percent sequences become U+FFFD.
- **Fix:** Stop the double-decode round-trip. Pass the shared URL to the Add screen
  without encoding it into and re-decoding it out of the route (e.g. via a mechanism
  that doesn't re-decode, or encode exactly once). Verify against the char set below.
- **Confirmed:** [assets/04-suspect-confirmation.md](assets/04-suspect-confirmation.md).

### P2 — (b) Hanging OpenGraph fetch → early Save saves `url=null`

- **Where:** [AddEditBookmarkViewModel.kt:113-130](../../app/src/main/java/org/hahn/maakmai/addeditbookmark/AddEditBookmarkViewModel.kt#L113)
- **Cause:** In `processSharedContent`, the URL field is set only *after*
  `OpenGraphUtils.extractUrlOpenGraphMetadata(sharedUrl)` returns. A stalling /
  black-holed host leaves the URL field blank for the whole timeout window while
  Save is live, so an early tap persists `url=null`. (A *clean* offline network
  fast-fails and the URL populates fine — the bug needs a hang.)
- **Fix:** Populate the URL field immediately, independent of the OG fetch; let OG
  metadata (title/description/image) fill in asynchronously when it arrives.
- **Confirmed:** [assets/04-suspect-confirmation.md](assets/04-suspect-confirmation.md)
  (screenshot `assets/04-L8-hang-empty.png`).

### P3 — (d) Google redirect URL stored verbatim

- **Where:** `ShareUrlActivity` / share handling.
- **Cause:** A `https://www.google.com/url?q=<dest>&sa=U` wrapper passes the URL
  check and is stored as-is (wrong target). **Frequency uncertain** — the Google
  app usually shares the destination, not the wrapper.
- **Fix (only if it proves common):** Detect known redirect wrappers and unwrap to
  the `q`/destination param. Don't over-engineer; confirm real prevalence first.

### P3 — (e) `EXTRA_SUBJECT` not used as a title fallback

- **Where:** [AddEditBookmarkViewModel.kt:131-139](../../app/src/main/java/org/hahn/maakmai/addeditbookmark/AddEditBookmarkViewModel.kt#L131)
- **Cause:** When the URL is dropped, the fallback recovers a title from
  `EXTRA_TITLE` only, never `EXTRA_SUBJECT`.
- **Fix:** Cheap rider on (a) — fall back to `EXTRA_SUBJECT`/`EXTRA_TITLE` for the
  title. Largely moot once (a) preserves the text.

## Regression test payloads

The repros double as the test spec. Fire `ACTION_SEND` `text/plain` at
`org.hahn.maakmai/.ShareUrlActivity` (or unit-test the extraction directly). After
each, the URL must be captured intact and the bookmark must survive back/exit.

- `"Some Page Title\nhttps://example.com/article"` → URL `https://example.com/article`, title "Some Page Title" (covers a, e)
- `"Check this out https://example.com/x"` (prose + URL) → URL captured (a)
- `"Title https://example.com/x"` (space-separated) → URL captured (a)
- `"https://example.com/article"` (bare URL) → still works (control)
- URLs containing `+`, `%20`, `%2F`, `%25`, `?a=1&b=2`, `#frag` → stored **byte-for-byte** (c)
- Share a valid URL against a hanging host, tap Save immediately → URL persisted, not `null` (b)
- Share, then press back / X / swipe away without tapping Save → link still captured (g)

## Out of scope

- **`ACTION_SEND_MULTIPLE` and non-`text/plain` MIME shares** — real but rare for
  links, and add-a-capability rather than a regression. Recorded as a separate
  backlog task. See [map.md](map.md) Out of scope.

## Watch list (suspected, unconfirmed)

Carry into implementation as things to verify, not confirmed bugs:
- Styled `CharSequence` / `ClipData` shares (couldn't craft via `adb`).
- OpenGraph metadata overriding user-supplied title/description.
