# Build the share-payload matrix across common source apps

Type: research
Status: resolved
Blocked by:

## Question

Ground the impact ranking (ticket 05) in what real apps actually send when a
user shares a link. Build a **matrix** of common Android share sources and the
`ACTION_SEND` payload each produces.

For each source app (at least: Chrome, the Google app / Google Search, YouTube,
Reddit, Twitter/X, a news app, Gmail, WhatsApp, the Android system "copied
link"), record:
- MIME type sent (`text/plain` vs other)
- `EXTRA_TEXT` shape: bare URL / `"Title\nURL"` / `"Title URL"` / prose + URL /
  something else
- whether `EXTRA_SUBJECT` / `EXTRA_TITLE` are set
- whether it's `ACTION_SEND` or `ACTION_SEND_MULTIPLE`

Prefer primary sources (official Android docs, app behavior) and reproduce on the
emulator where practical; use `/research` for documented conventions and note
confidence per row (observed vs documented vs inferred). Add a rough popularity
weight per source so 05 can estimate frequency.

Deliver the matrix as a linked markdown asset. This feeds both the frequency axis
of the impact ranking and the scope decision on `ACTION_SEND_MULTIPLE` /
non-`text/plain`. Do NOT fix anything.

## Answer

Matrix built for 9 sources: [assets/03-share-payload-matrix.md](../assets/03-share-payload-matrix.md).

Key patterns:
- **All link shares are `ACTION_SEND` + `text/plain`.** `ACTION_SEND_MULTIPLE`
  and non-`text/plain` do not occur for single-link sharing — recommend keeping
  them out of scope pending 04.
- **Three `EXTRA_TEXT` shapes dominate:**
  - **bare URL** — Chrome, YouTube (mostly work; still exposed to route
    encoding/round-trip hazards and `Patterns.WEB_URL` quirks like `[]`).
  - **`"Title\nURL"`** (title + newline + URL) — Google app/Search, Reddit,
    some news apps. **Silently dropped** by our full-string
    `Patterns.WEB_URL...matches()` check.
  - **prose + URL** — Twitter/X, WhatsApp, Gmail, some news apps. Also
    **silently dropped**, same root cause.
- `EXTRA_SUBJECT` usually carries the human title; `EXTRA_TITLE` is spottier —
  apps are inconsistent about which they set.
- "Copy link" is clipboard, not an intent — never reaches `ShareUrlActivity`.

Dominant break = the full-string URL match rejecting multi-line / prose payloads,
hitting several high/medium-popularity sources.

**Confidence caveat:** per-app payload shapes are established community/observed
convention, not Android-documented; rows for Reddit, Twitter/X, news apps, Gmail,
WhatsApp, and the YouTube prose variant are low-to-medium confidence and should be
confirmed on the emulator (02/04). Only the documented Android extras semantics
are high confidence.
