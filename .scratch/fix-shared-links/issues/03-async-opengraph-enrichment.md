# 03 — OpenGraph enrichment runs async and never blocks or nulls the URL

**What to build:** After a shared link is captured, the OpenGraph metadata fetch
(title, description, image) runs asynchronously and only *enriches* the already-set
URL and fields — it never gates them. A slow or black-holed host must not leave the
URL blank, and saving during the fetch must never persist a null URL. Metadata
fills in when (and if) it arrives; if it fails, the captured URL and any
text-derived title remain intact.

**Blocked by:** 02 (builds on the persist-first flow).

**Status:** done

- [x] The URL is present immediately on the Add screen, before any metadata fetch completes
- [x] Sharing a link whose host hangs, then saving right away, persists the real URL (never null)
- [x] Successful OpenGraph metadata still populates title/description/image
- [x] Failed/timed-out metadata leaves the URL and text-derived title unchanged
- [x] Metadata never overwrites a title/description the user has already edited

## Comments

Built on the persist-first flow from ticket 02:

- New pure `util/OpenGraphEnricher.enrich(...)` holds the merge rules: OG values
  only *fill in* or *improve* fields, blank OG values are ignored, user-edited
  fields are never overwritten, and an image slot is filled only when empty and
  untouched. Covered by `OpenGraphEnricherTest`. The URL is deliberately not part
  of the merge.
- `AddEditBookmarkViewModel` now fetches OG metadata in a *separate* coroutine
  (`enrichFromOpenGraph`) launched only after the captured bookmark has loaded and
  only when the new `enrich` flag is set. The URL is set from the persisted record
  first and is never touched by enrichment, so it shows immediately and an early
  Save can't persist null. `OpenGraphUtils` already does the network on
  `Dispatchers.IO`, so the main thread is never blocked.
- The view model tracks `titleEdited`/`descriptionEdited`/`imageEdited` (set by the
  `update*` handlers) and passes them to the enricher so in-flight metadata can't
  clobber user edits.
- Enrichment is opt-in via `MaakMaiNavigationActions.shareCaptureRoute` adding
  `enrich=true`; the normal edit-by-id path (Browse → edit) defaults to `false`,
  so opening an existing bookmark never refetches and overwrites saved fields.

Scope note: enrichment updates the on-screen editing session (and is persisted by
Save). It is intentionally *not* written straight to the DB mid-fetch — a back-out
before Save keeps the ticket-02 text-derived title, which still satisfies the
"link is saved" guarantee. Live emulator verification of a real OG fetch wasn't
run here; the merge rules are unit-tested.
