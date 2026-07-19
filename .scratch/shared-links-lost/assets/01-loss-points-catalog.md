# Catalog: link-loss points in the share pipeline (code trace)

Diagnosis only — traced by reading code, no device. Pipeline:
`AndroidManifest` intent-filter → `ShareUrlActivity.handleIntent` →
`MaakMaiNavigationActions.addFromShareRoute` (route encode) → Navigation-Compose
route parse → `AddEditBookmarkViewModel` (`URLDecoder` + `processSharedContent`) →
`saveBookmark`.

Each entry: **where** / **trigger** / **effect** (dropped | corrupted | wrong) /
**confidence** / **repro note** (feeds ticket 04).

---

## L1 — Intent filter accepts only `text/plain` + `ACTION_SEND`

- **Where:** `app/src/main/AndroidManifest.xml:34-38`
- **Trigger:** Source app shares via `ACTION_SEND_MULTIPLE` (multiple links), or with
  a MIME type other than `text/plain` (e.g. `text/uri-list`, `text/html`, or no
  type). The app is never offered / never invoked.
- **Effect:** dropped (share never reaches the app at all).
- **Confidence:** high (declarative manifest fact).
- **Repro:** `adb shell am start -a android.intent.action.SEND -t text/html --es
  android.intent.extra.TEXT "https://example.com" org.hahn.maakmai/.ShareUrlActivity`
  — with an explicit component it may still launch, but via the chooser the app
  won't appear. For `SEND_MULTIPLE`, share 2+ links from a gallery/browser.

## L2 — `handleIntent` action/type guard finishes silently on mismatch

- **Where:** `ShareUrlActivity.kt:30` (guard) → `:65` (`finish()` else-branch)
- **Trigger:** Intent reaches the activity but action != `ACTION_SEND` or
  `type` is null / not `text/*` (e.g. a redelivered/odd intent, or `onNewIntent`
  with a different action).
- **Effect:** dropped (activity finishes, no feedback).
- **Confidence:** medium (edge intents only; manifest usually pre-filters).
- **Repro:** send an `ACTION_SEND` with `-t null` / no type via `am`.

## L3 — `EXTRA_TEXT` read as String only; non-String or non-EXTRA_TEXT payloads dropped

- **Where:** `ShareUrlActivity.kt:32` (`getStringExtra(EXTRA_TEXT)`) → `:59-61`
  (`finish()` when null/empty)
- **Trigger:** Source app puts styled text (a `CharSequence`/`Spanned`, not a
  `String`) in `EXTRA_TEXT` — `getStringExtra` returns null; or the payload rides
  in `ClipData` / `EXTRA_STREAM` instead of `EXTRA_TEXT`.
- **Effect:** dropped (empty → `finish()`), or the styled URL is lost.
- **Confidence:** medium (depends on source app; some browsers/readers use
  CharSequence).
- **Repro:** craft an intent with a CharSequence extra (harder via `am`; via an
  app that shares styled text). Simplest: share with EXTRA_TEXT unset.

## L4 — `Patterns.WEB_URL.matcher(sharedText).matches()` requires the WHOLE string to be a URL  ★ TOP SUSPECT

- **Where:** `ShareUrlActivity.kt:45`
- **Trigger:** `sharedText` is not a bare, exact URL. Extremely common:
  - extra words: `"Check this out https://example.com via NewsApp"`
  - trailing newline/whitespace appended by the sharer: `"https://example.com\n"`
  - leading whitespace
  - internationalized-domain (IDN / unicode) URLs, or uncommon schemes
    (`market://`, `intent://`) that `Patterns.WEB_URL` does not match
- **Effect:** `.matches()` returns false → `urlParam = null`. See L5 — the URL is
  then discarded entirely.
- **Confidence:** high (full-string `.matches()` is the wrong matcher; trailing
  `\n` alone breaks it, and many apps append text/newlines).
- **Repro:** `adb shell am start -a android.intent.action.SEND -t text/plain --es
  android.intent.extra.TEXT "look https://example.com" org.hahn.maakmai/.ShareUrlActivity`
  → observe empty Add-Bookmark form (no URL). Repeat with a trailing `\n`.

## L5 — When `urlParam == null`, `sharedText` is never forwarded  ★ TOP SUSPECT (pairs with L4)

- **Where:** `ShareUrlActivity.kt:45-52` — the null branch (`:47-49`) plus the
  `addFromShareRoute(urlParam, title, subject)` call (`:52`).
- **Trigger:** any time L4 yields null. `sharedText` is used ONLY for the URL test;
  it is not passed as title/subject/anything. `title`/`subject` come solely from
  `EXTRA_TITLE`/`EXTRA_SUBJECT`, which many share sources do not set.
- **Effect:** dropped — the entire shared content (including the URL buried in the
  text) vanishes; the Add-Bookmark screen opens blank.
- **Confidence:** high.
- **Repro:** same as L4; confirm the form is completely empty (not even title/desc).

## L6 — Double decode of the route argument corrupts URLs with `%` or `+`  ★ TOP SUSPECT

- **Where:** encode at `MaakMaiNavigation.kt:74`
  (`URLEncoder.encode(url, UTF_8)`); Navigation-Compose decodes the arg once when
  it populates `SavedStateHandle`; then a SECOND decode at
  `AddEditBookmarkViewModel.kt:76` (`URLDecoder.decode(it, UTF_8)`).
- **Trigger:** URL already contains percent-encoding or `+`:
  - literal `+` (e.g. Google-style `?q=a+b`) → second `URLDecoder.decode` turns
    `+` into a space → corrupted URL.
  - any already-`%xx`-encoded segment (`.../%20`, `%2F`, `%25`) → over-decoded one
    level, changing the URL.
- **Effect:** corrupted (saved URL differs from shared URL; may 404 or point wrong).
- **Confidence:** medium-high (encode-once + framework-decode-once + manual-decode
  = one decode too many; exact framework behavior worth confirming on device).
- **Repro:** share `https://www.google.com/search?q=a+b+c` and inspect the saved
  URL for `a b c`; share `https://example.com/%2Ffoo` and inspect.

## L7 — `URLDecoder.decode` throws on malformed `%` → ViewModel construction crash  ★ TOP SUSPECT

- **Where:** `AddEditBookmarkViewModel.kt:76` (also `:77`, `:78` for title/subject)
- **Trigger:** After the framework's first decode, the value contains a bare `%`
  not followed by two hex digits (real URLs like `https://shop.com/100%off`, or any
  URL with a literal `%`). `URLDecoder.decode` throws `IllegalArgumentException`.
  Because this runs in a property initializer, the ViewModel fails to construct.
- **Effect:** dropped (crash) — the whole Add-Bookmark screen fails to open; URL
  lost, and likely a visible crash.
- **Confidence:** high (`URLDecoder.decode("...%of...")` throwing on bad hex is
  well-defined JDK behavior).
- **Repro:** `adb ... --es android.intent.extra.TEXT "https://shop.com/100%off"`
  (must pass L4 — keep it a bare URL) and watch for a crash / no screen.
  Note: whether it survives L4 depends on `Patterns.WEB_URL`; test with a bare URL
  that still contains a `%`.

## L8 — URL is set only AFTER the OpenGraph fetch completes (race)  ★ TOP SUSPECT

- **Where:** `AddEditBookmarkViewModel.kt:113-130` — `processSharedContent` launches
  a coroutine that first `await`s `OpenGraphUtils.extractUrlOpenGraphMetadata`
  (`:118`) and only then writes `url = sharedUrl` into `_uiState` (`:122-129`).
- **Trigger:** slow/hanging network. `OpenGraphUtils` uses a 10 s connect + 10 s
  read timeout (`OpenGraphUtils.kt:18`, `:42-43`), so the URL field can stay empty
  for up to ~20 s. If the user taps Save before the coroutine finishes,
  `saveBookmark` reads `uiState.value.url` (`AddEditBookmarkViewModel.kt:423`) which
  is still null.
- **Effect:** dropped — bookmark saved with `url = null` (and blank title/desc).
  Even without an early save, the URL field is empty/blank during the fetch window,
  looking like a loss.
- **Confidence:** high (URL assignment is strictly gated behind the network call).
- **Repro:** enable airplane mode or throttle network, share a URL, immediately tap
  Save → inspect the stored bookmark for a null/empty URL. Or point at a host that
  black-holes the connection to force the full timeout.

## L9 — OpenGraph fetch can hang (not a drop by itself; amplifies L8)

- **Where:** `OpenGraphUtils.kt:38-66`, timeouts at `:18`/`:42-43`
- **Trigger:** unresponsive server. Exceptions ARE caught (`:62-65`, returns empty
  metadata), so it does not throw — but it blocks the coroutine up to ~20 s.
- **Effect:** delay that enables the L8 race; no direct data loss on its own.
- **Confidence:** high (behavioral fact of the fetch).
- **Repro:** as L8.

## L10 — OpenGraph title/subject override shared metadata (wrong, not lost)

- **Where:** `AddEditBookmarkViewModel.kt:119-120`
- **Trigger:** page returns an `og:title`/`og:description`; it takes precedence over
  the app-supplied `sharedTitle`/`sharedSubject` (`openGraph.title ?: sharedTitle`).
- **Effect:** wrong (metadata mismatch) — the URL itself is preserved. Included for
  completeness of "mangled" cases; low relevance to link loss.
- **Confidence:** medium.
- **Repro:** share a URL with EXTRA_TITLE set to a known value and compare against
  the page's `<og:title>`.

---

### Summary of top suspects (highest user impact)

1. **L4 + L5** — non-bare / newline-suffixed shared text → URL silently dropped
   (most common real-world share shape).
2. **L8** — URL set only after network fetch → save-before-fetch drops the URL.
3. **L7** — bare `%` in URL crashes ViewModel construction → total loss.
4. **L6** — double decode corrupts URLs containing `+` or `%`.
5. **L1** — `SEND_MULTIPLE` / non-`text/plain` shares never reach the app.
