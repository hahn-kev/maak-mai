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

**Status:** ready-for-agent

- [ ] Sharing a link creates a persisted bookmark before the user does anything
- [ ] Editing on the Add screen updates that same bookmark (no duplicate created)
- [ ] Pressing back / close / swiping the app away after sharing leaves the link saved
- [ ] An explicit discard action removes the auto-captured bookmark
- [ ] Shared URLs containing `+ % / ? & # %20 %2F` are stored byte-for-byte (special-char regression)
- [ ] Re-sharing / re-entering the same share doesn't create duplicates within the same flow
