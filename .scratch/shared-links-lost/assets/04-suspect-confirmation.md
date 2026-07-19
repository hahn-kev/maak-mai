# Suspect confirmation on emulator `emulator-5554` (SDK 36)

Diagnosis only — no product code changed. Package `org.hahn.maakmai`, share
activity `.ShareUrlActivity` (already installed from a prior session; no
reinstall needed, so the INSTALL_FAILED_INSUFFICIENT_STORAGE issue did not
block this pass).

**Method.** Fire `ACTION_SEND` `text/plain` intents straight at the activity:
`adb shell "am start -a android.intent.action.SEND -t text/plain --es
android.intent.extra.TEXT '<payload>' -n org.hahn.maakmai/.ShareUrlActivity"`.
Read fields with `uiautomator dump` (empty field ⇒ only the hint label
"Title"/"URL"/"Description" appears) plus `screencap`. The already-CONFIRMED
Google `"Title\nURL"` full-match drop (asset 02) was **not** redone.

Control (bare URL `https://example.com/article`): URL field =
`https://example.com/article`, Title = `Article`. Pipeline healthy. XML:
`04-control.xml`.

## Results

| Suspect | Input `EXTRA_TEXT` | Observed result | Verdict | Severity |
|---|---|---|---|---|
| **L4+L5** space-separated "Title URL" | `Some Title https://example.com/x` | All fields **empty** — URL dropped, blank Add screen (`04-L45-space.xml`) | **REAL** | silent total loss |
| **L4+L5** prose + URL | `Check this out https://example.com/x via App` | All fields **empty** — URL dropped (`04-L45-prose.xml`) | **REAL** | silent total loss |
| **L6** `+` in query | `https://www.google.com/search?q=a+b+c` | Stored `https://www.google.com/search?q=a b c` — `+` → space (`04-L6-plus.xml`) | **REAL** | corruption |
| **L6** percent-encoded segments | `https://example.com/%2Ffoo%20bar%25baz` | Stored `https://example.com//foo bar%baz` — `%2F`→`/` (structural), `%20`→space, `%25`→`%` (`04-L6-enc.xml`) | **REAL** | corruption (can change target/path) |
| **L7** bare `%`+non-hex (crash) | `https://shop.com/100%off`, `https://example.com/100%off`, `https://example.com/a%zzb` | All **dropped at L4** (blank screen), no crash — `Patterns.WEB_URL.matches()` rejects a bare `%` not followed by valid escaping, so the input never reaches `URLDecoder` (`04-L7-percent.xml`, `04-L7-b.xml`, `04-L7-c.xml`) | **THEORETICAL** (L4 gates it) | n/a (unreachable) |
| **L7** valid-hex-but-invalid-UTF8 `%` | `https://example.com/x%c3y`, `.../p%ffq` | **No crash.** Android `URLDecoder` is lenient — substitutes U+FFFD (`�`) instead of throwing. Stored `https://example.com/x�y` (`04-L7-utf8.xml`, `04-L7-utf8b.xml`) | **THEORETICAL** as a crash; behaves as **REAL corruption** | corruption |
| **L8** OpenGraph fetch hang (URL set only after fetch) | `https://10.255.255.1/…` (black-hole host, connect stalls to 10 s timeout) | URL field **empty for the whole ~10 s fetch window** (`04-L8-hang-t2.xml`, screenshot `04-L8-hang-empty.png`); populates only after timeout (`04-L8-hang-after.xml`). Save button is live during the blank window → tapping Save persists `url=null` (see `saveBookmark` reads `uiState.value.url`) | **REAL** | silent total loss (if user saves during window) / confusing blank field |
| **L8/offline** fetch on cleanly-unreachable network | `https://example.com/offline-test` with wifi+data off / airplane on | URL **populated immediately** — the fetch fast-fails ("Network unreachable"), is caught, and `url = sharedUrl` is then set (`04-L8-offline-t0.xml`) | **THEORETICAL** for clean-offline; the drop needs a *hang*, not a fast fail | n/a for fast-fail |
| **L1** `ACTION_SEND_MULTIPLE` | explicit component, `text/plain` | Activity does **not** foreground (finishes) — `handleIntent` requires `action == ACTION_SEND`. Not offered in the `text/plain` chooser at all (manifest declares only `ACTION_SEND`) | **REAL** (drop) | total loss, but **very low frequency** (link shares are single `ACTION_SEND`) |
| **L1** non-`text/plain` MIME | `image/png` explicit; chooser query for `text/html`, `image/png` | `image/png` explicit → activity finishes (`type.startsWith("text/")` false). `cmd package query-activities`: MaakMai listed for `text/plain` only; **not** for `text/html` or `image/png` | **REAL** (never offered) | total loss, low frequency |

## Special-character round-trip summary (bare URLs through the Nav-Compose route)

| Char | Input | Stored | Effect |
|---|---|---|---|
| `?` `&` `=` | `https://example.com/s?a=1&b=2&c=3` | identical (`&amp;` in dump = XML escaping only) | OK (`04-chars-qs.xml`) |
| `#` fragment | `https://example.com/page#sec1` | identical | OK (`04-chars-frag.xml`) |
| `+` | `?q=a+b+c` | `?q=a b c` | **corrupted** |
| `%xx` | `%2F` `%20` `%25` | `/` space `%` | **corrupted / over-decoded** |
| `%` + non-hex | `100%off` | (dropped at L4) | dropped, not corrupted |

Root cause of the `+`/`%` corruption: `MaakMaiNavigation.kt:74`
`URLEncoder.encode` → Navigation-Compose decodes the route arg once → a **second**
`URLDecoder.decode` at `AddEditBookmarkViewModel.kt:76`. One decode too many.

## Notes for ticket 05 (kept as "suspected" — not intent-reproducible here)

- **L3** styled `CharSequence`/`Spanned` in `EXTRA_TEXT`, or payload in
  `ClipData`/`EXTRA_STREAM`: cannot be crafted via `am --es` (String only).
  Suspected, source-app-dependent.
- **L10** OpenGraph title/description overriding app-supplied metadata: needs a
  live page with `og:` tags; wrong-metadata, not link loss. Suspected.
- **L8 early-save data loss** was demonstrated as far as the blank-URL window +
  code path (`saveBookmark` reads the still-null `uiState.url`); an
  end-to-end "saved a null-URL bookmark to the DB" assertion was not run
  (would need DB inspection). The window and mechanism are confirmed.

## Screenshots / artifacts
All under `.scratch/shared-links-lost/assets/`:
- `04-L8-hang-empty.png` — Add screen with URL blank during the OG fetch hang (Save button live).
- XML dumps: `04-control.xml`, `04-L45-space.xml`, `04-L45-prose.xml`,
  `04-L6-plus.xml`, `04-L6-enc.xml`, `04-L7-*.xml`, `04-L8-*.xml`,
  `04-chars-qs.xml`, `04-chars-frag.xml`.
