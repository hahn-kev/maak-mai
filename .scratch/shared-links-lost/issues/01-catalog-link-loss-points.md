# Catalog every link-loss point in the share pipeline (code trace)

Type: research
Status: resolved
Blocked by:

## Question

By reading the code alone (no device), enumerate **every** point in the share
pipeline where a shared link can be silently dropped, corrupted, or mangled.

Trace the full path:
- `ShareUrlActivity.handleIntent` — the intent filter (`text/plain` only,
  `ACTION_SEND` only), the `Patterns.WEB_URL.matcher(sharedText).matches()`
  full-string check (line ~45), the `EXTRA_TEXT`/`EXTRA_SUBJECT`/`EXTRA_TITLE`
  handling, and what happens to `sharedText` when `urlParam` ends up `null`.
- `MaakMaiNavigation.addFromShareRoute` — URL-encoding of the url/title/subject
  into a Navigation-Compose route string, and how special URL characters
  (`&`, `?`, `#`, `%`, `+`, spaces, encoded slashes) survive round-tripping
  through the route and back out via `URLDecoder` in the ViewModel.
- `AddEditBookmarkViewModel.processSharedContent` — whether the
  `OpenGraphUtils.extractUrlOpenGraphMetadata(sharedUrl)` call can throw/hang and
  take the URL down with it (URL is only set *after* that fetch inside the
  coroutine), and the `sharedUrl == null` branch.

Deliver a **catalog** as a linked markdown asset: one entry per candidate
failure mode with { where (file:line), trigger condition, observed effect
(dropped / corrupted / wrong), confidence, note on how to reproduce }. This
catalog is the input to ticket 04 (reproduce & confirm the suspects).

Do NOT fix anything — this is diagnosis only.

## Answer

Traced the full pipeline by reading code. 10 candidate link-loss points catalogued
(full detail, with file:line, triggers, and repro notes in the asset below):

- **L1** `AndroidManifest.xml:34-38` — filter is `ACTION_SEND` + `text/plain` only;
  `SEND_MULTIPLE` and other MIME types never reach the app. *dropped* — high.
- **L2** `ShareUrlActivity.kt:30,65` — action/type guard `finish()`es silently on
  mismatch. *dropped* — medium.
- **L3** `ShareUrlActivity.kt:32,59-61` — `getStringExtra(EXTRA_TEXT)` returns null
  for styled `CharSequence` / `ClipData` / `EXTRA_STREAM` payloads. *dropped* — medium.
- **L4** `ShareUrlActivity.kt:45` — full-string `Patterns.WEB_URL...matches()` fails
  on extra words, trailing `\n`/whitespace, IDN/uncommon schemes. *dropped* — high. ★
- **L5** `ShareUrlActivity.kt:45-52` — when the URL test fails, `sharedText` is never
  forwarded (title/subject come only from EXTRA_TITLE/SUBJECT); whole share lost.
  *dropped* — high. ★ (pairs with L4)
- **L6** `MaakMaiNavigation.kt:74` + `AddEditBookmarkViewModel.kt:76` — encode-once +
  framework-decode + manual `URLDecoder.decode` = double decode; `+`→space and `%xx`
  over-decoded. *corrupted* — medium-high. ★
- **L7** `AddEditBookmarkViewModel.kt:76-78` — `URLDecoder.decode` throws on a bare
  `%` (e.g. `100%off`) in a property initializer → ViewModel construction crash.
  *dropped/crash* — high. ★
- **L8** `AddEditBookmarkViewModel.kt:113-130` — `url` is set only AFTER the
  OpenGraph fetch (up to ~20s, `OpenGraphUtils.kt:18`); save-before-fetch stores
  `url=null`. *dropped* — high. ★
- **L9** `OpenGraphUtils.kt:38-66` — fetch is caught (won't throw) but hangs ~20s,
  amplifying L8. *delay only* — high.
- **L10** `AddEditBookmarkViewModel.kt:119-120` — `og:title`/`og:description`
  override app-supplied title/subject. *wrong metadata, URL intact* — medium.

Top suspects: L4+L5, L8, L7, L6, L1.

Catalog asset: [01-loss-points-catalog.md](../assets/01-loss-points-catalog.md)
(feeds ticket 04 — reproduce & confirm).
