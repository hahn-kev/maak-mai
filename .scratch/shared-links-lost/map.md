# Shared links are being lost

Label: wayfinder:map

## Destination

A shared map that diagnoses *how* links shared into Bookmark Tags get dropped or
corrupted, and produces a **fix plan ranked by user impact**, ready to hand to an
implementation session. Planning only — no code fixes land in this effort.

## Notes

- Domain: Android `ACTION_SEND` share-intent handling for the "Bookmark Tags" app.
- Pipeline under investigation:
  `ShareUrlActivity` → `MaakMaiNavigation.addFromShareRoute` (route encoding) →
  `AddEditBookmarkViewModel.processSharedContent`.
- Method: hybrid — reproduce marquee/uncertain cases on emulator `emulator-5554`
  (SDK 36) via `adb` (AFK); reason about the long tail.
- Ranking yardstick: **pure user impact** (frequency × severity). Confirmed
  failures get ranked; unconfirmed ones sit in a "suspected" bucket until proven.
- Scope is deliberately wide: silent drops, corruption/mangling, save failures,
  and unsupported share shapes are all in play until repro findings let us rule
  some out.
- Consult `/grilling` + `/domain-modeling` for decisions; `/research` for Android
  share conventions.
- Planning only: produce decisions, not code changes.

## Decisions so far

<!-- one line per closed ticket: gist of the answer, then link -->

- [Catalog every link-loss point in the share pipeline](issues/01-catalog-link-loss-points.md)
  — 10 loss points found; top suspects: non-bare/newline text drops the URL (the
  full-match check), URL set only *after* the OpenGraph fetch, and double-decode
  corruption/crash on special chars. See [catalog](assets/01-loss-points-catalog.md).
- [Build the share-payload matrix across common source apps](issues/03-share-payload-matrix.md)
  — 9 sources, all share links as `SEND` + `text/plain`; "Title\nURL" and prose+URL
  shapes (Google/Reddit/X/WhatsApp) are silently dropped by the full-string match.
  Per-app shapes mostly inferred; several rows flagged for emulator confirmation in 04.
  See [matrix](assets/03-share-payload-matrix.md).
- [Reproduce the Google Search share failure on the emulator](issues/02-reproduce-google-search.md)
  — **CONFIRMED** on `emulator-5554`: Google's "Title\nURL" `EXTRA_TEXT` fails the
  full-match `WEB_URL` check; URL *and* title both dropped → blank Add screen. Also
  found: the dropped-URL fallback recovers a title from `EXTRA_TITLE` only, never
  `EXTRA_SUBJECT`; and a `google.com/url?q=` redirect is stored verbatim (wrong
  target, not dropped). See [repro](assets/02-google-repro.md).
- [Reproduce & confirm the catalogued suspects](issues/04-reproduce-confirm-suspects.md)
  — REAL: space/prose "Title URL" shapes drop the URL (same as Google, highest
  frequency); hanging OpenGraph fetch leaves URL blank so an early Save persists
  `url=null` (a *clean* offline fast-fails fine); `+`/`%` double-decode corruption;
  `SEND_MULTIPLE`/non-`text/plain` unhandled (real but rare). THEORETICAL: the
  `%`→URLDecoder crash (filtered by the match + lenient decoder). See
  [confirmation](assets/04-suspect-confirmation.md).
- [Rank the confirmed failures by user impact](issues/05-rank-by-user-impact.md)
  — ranked P0→P3 with a fix direction agreed per mode; new mode **(g)** added
  (link lost when Save never tapped → auto-capture on share). Fix approaches were
  clear enough to decide inline. Assembled into the handoff [spec.md](spec.md).

## Not yet specified

<!-- The way to the destination is clear. Every confirmed failure mode is
diagnosed, ranked (05), and has an agreed fix direction; the fix approaches were
clear enough to decide inline rather than as separate tickets. The consolidated
handoff plan is spec.md. Nothing left to decide — ready for an implementation
session. -->

_(empty — map has reached its destination)_

## Out of scope

<!-- work ruled beyond the destination; closed, never graduates -->

- **Handling `ACTION_SEND_MULTIPLE` and non-`text/plain` MIME shares** — confirmed
  real in [04](issues/04-reproduce-confirm-suspects.md) (app isn't offered in the
  chooser / finishes silently) but rare for link sharing and it's add-a-capability
  work, not a lost-link regression. Ruled out of *this* effort; recorded as a
  separate backlog task outside this session for later consideration.
