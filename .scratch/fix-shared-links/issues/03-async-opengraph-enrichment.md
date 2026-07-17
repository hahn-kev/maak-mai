# 03 — OpenGraph enrichment runs async and never blocks or nulls the URL

**What to build:** After a shared link is captured, the OpenGraph metadata fetch
(title, description, image) runs asynchronously and only *enriches* the already-set
URL and fields — it never gates them. A slow or black-holed host must not leave the
URL blank, and saving during the fetch must never persist a null URL. Metadata
fills in when (and if) it arrives; if it fails, the captured URL and any
text-derived title remain intact.

**Blocked by:** 02 (builds on the persist-first flow).

**Status:** ready-for-agent

- [ ] The URL is present immediately on the Add screen, before any metadata fetch completes
- [ ] Sharing a link whose host hangs, then saving right away, persists the real URL (never null)
- [ ] Successful OpenGraph metadata still populates title/description/image
- [ ] Failed/timed-out metadata leaves the URL and text-derived title unchanged
- [ ] Metadata never overwrites a title/description the user has already edited
