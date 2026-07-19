# Share-payload matrix: what common Android apps put in an ACTION_SEND intent

Ticket: [03-share-payload-matrix](../issues/03-share-payload-matrix.md).
Feeds the frequency axis of the impact ranking (05) and the scope decision on
`ACTION_SEND_MULTIPLE` / non-`text/plain`.

## Method & sourcing

- **Documented baseline** from the official Android sharing docs: `EXTRA_TEXT` is
  the payload body, `EXTRA_SUBJECT` is intended for the email subject / title,
  `EXTRA_TITLE` is a preview label (API 29+), `text/plain` is the correct MIME
  type for plain text, and multi-item shares use `ACTION_SEND_MULTIPLE`
  ([Send simple data](https://developer.android.com/training/sharing/send),
  [Receive simple data](https://developer.android.com/training/sharing/receive)).
- **Per-app payload shapes** below are NOT specified by Android — each source app
  chooses its own `EXTRA_TEXT` layout. There is no authoritative doc for this;
  the shapes are established community/observed behavior. I have marked each row's
  confidence (**observed** = reproducible/first-hand-reported, **documented** =
  stated by a primary source, **inferred** = well-known convention but not pinned
  to a citation this pass). Rows marked inferred should be confirmed on the
  emulator (ticket 02 / 04) before load-bearing decisions.
- Corroborating note: even a *bare* URL can fail a strict full-string
  `Patterns.WEB_URL` match when it contains characters the regex mishandles, e.g.
  square brackets ([Google issue 67159235](https://issuetracker.google.com/issues/67159235)).

## Matrix

| # | Source app | Popularity as share source | MIME | `EXTRA_TEXT` shape | `EXTRA_SUBJECT` | `EXTRA_TITLE` | Action | Confidence |
|---|-----------|---------------------------|------|--------------------|-----------------|---------------|--------|-----------|
| 1 | **Chrome** (browser) | high | `text/plain` | bare URL | set = page title | sometimes = page title | `ACTION_SEND` | inferred (strong; widely observed) |
| 2 | **Google app / Google Search** | high | `text/plain` | **`"Title\nURL"`** (title + newline + URL); search-result shares can be prose + URL | usually set = title | sometimes | `ACTION_SEND` | inferred (this is the leading drop hypothesis — confirm in 02) |
| 3 | **YouTube** | high | `text/plain` | bare short URL (`https://youtu.be/…`); some versions prepend prose ("Check out this video…") + URL | often set = video title | sometimes | `ACTION_SEND` | inferred (bare-URL common; prose variant low-confidence) |
| 4 | **Reddit** | med | `text/plain` | **`"Post title\nURL"`** (title + newline + URL) | often set = post title | rarely | `ACTION_SEND` | inferred (low-med — layout varies by app version) |
| 5 | **Twitter / X** | med | `text/plain` | **prose + URL** (tweet text with an inline `https://…` / `t.co` link) | usually not set | rarely | `ACTION_SEND` | inferred (low-med) |
| 6 | **News app** (e.g. Google News, NYT, BBC) | med | `text/plain` | varies: `"Headline\nURL"` or **prose + URL** ("Headline — via App: URL") | often set = headline | sometimes | `ACTION_SEND` | inferred (low — highly app-dependent) |
| 7 | **Gmail** | low (as *source*) | `text/plain` | prose + URL (message body text, may embed a link) | set = message subject | rarely | `ACTION_SEND` | inferred (low — Gmail is usually a share *target*, not source) |
| 8 | **WhatsApp** | med | `text/plain` | message text as prose (may contain an inline URL); forwarded link = the message body | not set | not set | `ACTION_SEND` | inferred (low-med) |
| 9 | **Android system "Copy link"** | high | n/a (clipboard, not an intent) | bare URL placed on the **clipboard** — no `ACTION_SEND` is fired | n/a | n/a | none (clipboard) | documented behavior; **not a share intent** — flagged |

### Notes on individual rows

- **Row 2 (Google app)** is the marquee case for this whole effort: a
  `"Title\nURL"` payload is a single multi-line string, so the current
  full-string `Patterns.WEB_URL.matcher(sharedText).matches()` check in
  `ShareUrlActivity` (line ~45) returns false and the URL is dropped to `null`.
  Confirm the exact bytes in ticket 02.
- **Row 9 ("Copy link")** is not an `ACTION_SEND` at all — it writes to the
  system clipboard. It cannot reach `ShareUrlActivity` and is out of the intent
  pipeline entirely; included only to prevent it being miscounted as a share
  source. Popularity is "high" as a user action but zero as an intent into us.
- **`ACTION_SEND_MULTIPLE`**: none of the single-link share flows above use it.
  It appears for multi-item / attachment shares (e.g. sharing several photos, or
  a gallery selection), and typically with non-`text/plain` MIME types. For
  *link* sharing specifically it is rare-to-nonexistent. → supports treating
  `ACTION_SEND_MULTIPLE` as low frequency for our use case.

## Implications for our app

Our pipeline: `text/plain`-only intent filter → full-string
`Patterns.WEB_URL.matcher(sharedText).matches()` → URL URL-encoded into a
Navigation-Compose route → decoded in `AddEditBookmarkViewModel`.

What each shape does to us:

1. **`"Title\nURL"` (rows 2, 4; sometimes 6) — SILENTLY DROPPED.** The multi-line
   string is not a full-string URL match, so `urlParam` becomes `null` and the
   URL is lost. This is the highest-frequency, highest-severity failure because it
   hits **Google Search and Reddit**, both common sources. The title *is* passed
   through separately (`EXTRA_TITLE`/`EXTRA_SUBJECT`), but the actual link is gone.
   Fix direction: extract the URL from the string (e.g. `Patterns.WEB_URL` in
   *find* mode) rather than requiring the whole string to be a URL.

2. **prose + URL (rows 5, 7, 8; sometimes 3, 6) — SILENTLY DROPPED**, same root
   cause. Twitter/X, WhatsApp, Gmail, some news apps. Medium frequency,
   high severity.

3. **bare URL (rows 1, 3) — mostly works**, but is fragile: a valid URL
   containing characters `Patterns.WEB_URL` mishandles (square brackets, some
   Unicode, unusual query params) will still fail the strict `matches()` and be
   dropped — and even when it passes, the URL then has to survive URL-encoding
   into a Nav-Compose route and `URLDecoder` on the way out (`&`, `?`, `#`, `%`,
   `+`, spaces, encoded slashes are round-trip hazards; see ticket 01). So even
   the "happy path" apps (Chrome, YouTube) can lose or mangle certain links.

4. **`EXTRA_SUBJECT` vs `EXTRA_TITLE` inconsistency:** apps disagree on which
   carries the human title (most use `EXTRA_SUBJECT`; `EXTRA_TITLE` is spottier).
   The activity reads both, but only `title` (`EXTRA_TITLE`) and `subject`
   (`EXTRA_SUBJECT`) are forwarded — worth confirming both are actually used
   downstream, else titles from `EXTRA_SUBJECT`-only apps are under-used.

5. **`ACTION_SEND_MULTIPLE` / non-`text/plain`:** the filter accepts only
   `ACTION_SEND` + `text/`. For *link* sharing this excludes almost nothing (rare
   shape). Recommend keeping it **out of scope** unless 04 finds a real case —
   the matrix shows link shares are overwhelmingly single `ACTION_SEND` +
   `text/plain`.

**Bottom line for the impact ranking (05):** the dominant, most-frequent break is
the full-string URL match rejecting `"Title\nURL"` and prose+URL payloads, which
covers Google Search, Reddit, Twitter/X, WhatsApp, and many news apps. Bare-URL
apps (Chrome, YouTube, "copy link" → paste) mostly work but remain exposed to the
encoding/round-trip hazards.

## Sources

- [Send simple data to other apps — Android Developers](https://developer.android.com/training/sharing/send)
- [Receive simple data from other apps — Android Developers](https://developer.android.com/training/sharing/receive)
- [Patterns.WEB_URL square-bracket parsing issue — Google Issue Tracker 67159235](https://issuetracker.google.com/issues/67159235)
