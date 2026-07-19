# 01 — Capture the link from "Title + URL" / prose shares

**What to build:** When a user shares text that contains a URL alongside other
text — `"Page Title\nURL"` (Google, Reddit, X, WhatsApp, YouTube), `"Title URL"`,
or prose with a URL in it — the app captures the URL and a sensible title, instead
of landing the user on a blank Add Bookmark screen. A bare URL still works exactly
as before. When no title can be derived from the text, fall back to the share's
subject/title extras before falling back to the URL-derived title.

Prefactor first: pull the "raw share extras → parsed { url, title, description }"
logic into one pure, unit-testable unit. Today it's split between the share
activity (which rejects any text that isn't wholly a URL) and the add/edit view
model. Characterize current behaviour with tests, then change the URL detection
from "the whole text must be a URL" to "find the first URL within the text."

**Blocked by:** None — can start immediately.

**Status:** done

- [ ] Sharing `"Some Page Title\nhttps://example.com/article"` captures URL `https://example.com/article` and title "Some Page Title"
- [ ] Sharing `"Check this out https://example.com/x"` (prose + URL) captures the URL
- [ ] Sharing `"Title https://example.com/x"` (space-separated) captures the URL and title
- [ ] Sharing a bare `"https://example.com/article"` still works (regression)
- [ ] When the shared text yields no title, the share's subject/title extra is used before the URL-derived fallback
- [ ] Share-text parsing lives in one pure unit with unit tests covering the above shapes
