# 05 — (Optional) Unsorted inbox for auto-captured links

**What to build:** Auto-captured shared links (from ticket 02) land in an
"unsorted" / inbox area so that capturing every share doesn't clutter the main
bookmark list until the user has triaged them. The user can see what's been
captured-but-not-yet-organized and move items out of the inbox by tagging/filing
them.

This is the optional "+inbox" half of the auto-capture decision. Skip it if
auto-captured links surfacing in the normal list turns out to be fine in practice.

**Blocked by:** 02 (depends on auto-capture existing).

**Status:** ready-for-agent

- [ ] Auto-captured links are distinguishable from deliberately-saved bookmarks (an unsorted/inbox view)
- [ ] The inbox is reachable from the main UI and shows captured-but-untriaged links
- [ ] Filing/tagging a captured link removes it from the inbox
- [ ] Deliberately-created bookmarks do not appear in the inbox
