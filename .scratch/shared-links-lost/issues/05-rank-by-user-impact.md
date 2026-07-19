# Rank the confirmed failures by user impact

Type: grilling
Status: resolved
Blocked by: 02, 03, 04

## Question

With the confirmed failure modes (02, 04) and the share-payload matrix (03) in
hand, produce the **impact-ranked list** that the fix plan is built around.

Yardstick (from the map): **pure user impact = frequency × severity**. Do NOT
discount by fix cost — this is a simple app.
- Frequency: how often real shares hit this path, estimated from the 03 matrix
  and source-app popularity.
- Severity: silent total loss > corruption/mangling > minor/cosmetic.

Confidence gate: only reproduced-and-confirmed failures get ranked. Unconfirmed
suspicions go in a separate "suspected" bucket, not the ranked list.

This is HITL — grill the human on the frequency/severity weightings and the
final ordering; do not decide it unilaterally.

Deliver the ranked list + suspected bucket as the resolution answer. On resolve,
this graduates the "fix-approach per confirmed failure mode" fog into concrete
fix-decision tickets, and settles the scope question on `ACTION_SEND_MULTIPLE` /
non-`text/plain`.

## Answer

Ranked by user impact (frequency × severity), agreed with the human. Fix
approaches were clear enough per mode that they were decided inline rather than
spun into separate fix-decision tickets. Consolidated into the handoff spec:
[spec.md](../spec.md).

A new failure mode **(g)** surfaced during ranking: a shared link is lost when
the user never taps the Save FAB (the top-left X and system back `finish()` and
discard silently, with no auto-save and no unsaved-changes prompt). Confirmed by
code inspection. Fix direction chosen by the human: **auto-capture on share**
(persist immediately, edit-in-place), optionally into an unsorted/inbox area. No
prototype requested.

Scope: **(f)** `ACTION_SEND_MULTIPLE` / non-`text/plain` ruled **out of scope**
(see map Out of scope; recorded as a separate backlog task).

**Ranked list:**

| Rank | Failure | Severity × frequency | Fix direction |
|------|---------|----------------------|---------------|
| P0 | (a) "Title+URL" / prose / space-separated share drops the URL (full-string `WEB_URL.matches()`) | total loss × high | Extract the first URL *within* the shared text instead of requiring the whole string to match |
| P0 | (g) Link lost when Save is never tapped (X/back/swipe discards silently) | total loss × every share | Auto-capture on share (persist immediately, edit-in-place); optional unsorted/inbox area |
| P1 | (c) Special-char double-decode corruption (`+`→space, `%2F`→`/`, `%20`→space, `%25`→`%`) | corruption × medium | Fix the Navigation-Compose route encode/decode round-trip so the URL isn't double-decoded |
| P2 | (b) Hanging OpenGraph fetch → early Save persists `url=null` | total loss × low–med | Populate the URL field immediately, independent of the OG metadata fetch |
| P3 | (d) `google.com/url?q=…` redirect stored verbatim (wrong target) | wrong target × uncertain | Unwrap known redirect wrappers — only if it proves common |
| P3 | (e) `EXTRA_SUBJECT` never used as title fallback when the URL is dropped | minor × low | Cheap rider on the (a) fix — fall back to `EXTRA_SUBJECT`/`EXTRA_TITLE` for the title |

**Suspected (unconfirmed, not ranked):** styled `CharSequence`/`ClipData` handling,
OpenGraph metadata overriding user-supplied title/description (L3, L10, per the 01
catalog) — carry into implementation as things to watch, not confirmed bugs.
