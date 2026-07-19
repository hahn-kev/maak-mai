# Reproduce the Google Search share failure on the emulator

Type: task
Status: resolved
Blocked by:

## Question

Confirm — with facts, not guesses — the one concrete user report: "shares from
Google Search don't work correctly."

On emulator `emulator-5554` (reachable via `adb`), fire `ACTION_SEND` intents at
`org.hahn.maakmai/.ShareUrlActivity` with the payloads the Google app / Google
Search / Chrome-via-Google actually produce. The leading hypothesis is that
Google shares `EXTRA_TEXT` as `"Page Title\nhttps://…"` (title + newline + URL),
which fails the full-string `Patterns.WEB_URL...matches()` check and drops the
URL. Test at least:
- `"Some Page Title\nhttps://example.com/article"` (title + newline + URL)
- a bare URL `"https://example.com/article"` (control — should work)
- a Google result URL with query params / redirects
  (`https://www.google.com/url?q=...&sa=...`) to also probe encoding.

For each payload, record: exact input, what reached the add-bookmark screen
(URL field populated? title? anything?), and whether the link was lost. Capture
the actual `EXTRA_TEXT` format Google produces if you can observe it.

Deliver findings as the resolution answer (link any script/log asset). Confirms
or refutes the report and pins down the exact trigger. Do NOT fix anything.

Build the app first if needed: `./gradlew installDebug` (or use an existing
build). Use `adb shell am start -a android.intent.action.SEND -t text/plain
--es android.intent.extra.TEXT "..." -n org.hahn.maakmai/.ShareUrlActivity`.

## Answer

**CONFIRMED.** Reproduced on `emulator-5554` (SDK 36). Exact trigger: an
`ACTION_SEND` `text/plain` whose `EXTRA_TEXT` is the `"Page Title\nURL"` shape
Google Search / the Google app produce. `ShareUrlActivity.kt:45` guards the URL
with `Patterns.WEB_URL.matcher(sharedText).matches()`, whose `.matches()`
requires the *entire* extra to be a URL. The multi-line "title + newline + URL"
string fails, `urlParam` becomes `null`, and the user lands on a **blank** Add
Bookmark screen — URL and Title both empty (the title half of `EXTRA_TEXT` is
never parsed out). The newline delivery was verified with `od -c`.

Controls behave as expected: a bare URL is preserved; a single-line
`google.com/url?q=...&sa=U` redirect passes the match but is stored verbatim as
the redirect wrapper rather than the destination. Extra finding: when the URL is
dropped, `processSharedContent` only recovers a title from `EXTRA_TITLE`, never
from `EXTRA_SUBJECT`, so a supplied subject is lost too.

Build note: `./gradlew installDebug` compiled OK but failed with
`INSTALL_FAILED_INSUFFICIENT_STORAGE`; installed via `pm install -r` of the
built APK pushed to `/data/local/tmp`. No user apps removed. No product code
changed.

Findings + table + screenshots:
[assets/02-google-repro.md](../assets/02-google-repro.md)
