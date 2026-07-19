# Repro: Google Search share failure (emulator `emulator-5554`, SDK 36)

**Package:** `org.hahn.maakmai`  ·  **Share activity:** `.ShareUrlActivity`
**Build/install:** `./gradlew installDebug` compiled OK but failed with
`INSTALL_FAILED_INSUFFICIENT_STORAGE`. Worked around by pushing the built APK
(`app/build/outputs/apk/debug/app-debug.apk`) to `/data/local/tmp` and running
`pm install -r` (Success). No user apps were removed.

**Method:** fire `ACTION_SEND` `text/plain` intents straight at
`org.hahn.maakmai/.ShareUrlActivity`, then read the Add Bookmark screen with
`uiautomator dump` (field hint text such as "Title"/"URL" shows only when the
field is empty) plus `screencap`.

Literal newlines were delivered by embedding a real newline inside the
single-quoted `--es` value. Verified the newline survives the adb pipeline with
`od -c` (output showed `Some Page Title \n https://example.com/article`).

## Results

| # | Input `EXTRA_TEXT` (extras) | URL field | Title field | Description | Link lost? | Notes |
|---|---|---|---|---|---|---|
| 1 | `Some Page Title\nhttps://example.com/article` (title + newline + URL) — the suspected Google shape | **empty** | **empty** | empty | **YES** | `Patterns.WEB_URL.matcher(sharedText).matches()` fails on the multi-line string → `urlParam=null`. Title portion of EXTRA_TEXT is never parsed out, so it is lost too. Screenshot: `02-google-repro-titlenl.png` |
| 2 | `https://example.com/article` (bare URL — control) | `https://example.com/article` | `Article` (derived from last path segment) | empty | no | Full string matches WEB_URL → URL preserved. Screenshot: `02-google-repro-control-bareurl.png` |
| 3 | `https://www.google.com/url?q=https://example.com&sa=U` (Google redirect w/ query params) | `https://www.google.com/url?q=https://example.com&sa=U` | `Url` (from `/url` path segment) | empty | no (but wrong target) | Single-line string passes the full match, so it is kept verbatim — the app stores the **google.com redirect wrapper**, not the destination `example.com`. `&` survived intact. |
| 4 | `EXTRA_TEXT` = `Some Page Title\nhttps://...` **and** `EXTRA_SUBJECT` = `Some Page Title` | **empty** | **empty** | **empty** | **YES** | Same drop as #1. Additionally, when the URL is dropped `processSharedContent` only recovers a title from `EXTRA_TITLE` (`sharedTitle`), never from `EXTRA_SUBJECT` — so even a supplied subject populates nothing. |

## Root cause (code)

`ShareUrlActivity.kt:45`
```kotlin
val urlParam = if (Patterns.WEB_URL.matcher(sharedText).matches()) sharedText else null
```
`Matcher.matches()` requires the **entire** `EXTRA_TEXT` to be a URL. Google
Search / the Google app share `"Page Title\nURL"`, so the whole-string match
fails and the URL is set to `null` — the link is silently dropped. The title
half of the payload is never extracted, so nothing usable reaches the screen.

Downstream: `MaakMaiNavigation.kt:73 addFromShareRoute` and
`AddEditBookmarkViewModel.kt:113 processSharedContent` only populate fields from
`url` / `EXTRA_TITLE`; neither salvages a URL embedded inside a longer
`EXTRA_TEXT`, and `processSharedContent` ignores `EXTRA_SUBJECT` when `url` is null.

## Conclusion

**CONFIRMED.** The report "shares from Google Search don't work correctly" is
real. Exact trigger: any `ACTION_SEND` whose `EXTRA_TEXT` is not a bare URL —
specifically the `"Title\nURL"` shape Google produces — fails the full-string
`Patterns.WEB_URL...matches()` check and the link is dropped, landing the user
on a blank Add Bookmark screen.
