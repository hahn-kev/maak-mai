# Reproduce & confirm the catalogued suspects on the emulator

Type: task
Status: resolved
Blocked by: 01

## Question

Take the failure-mode catalog from ticket 01 and, for each suspect that is
reproducible with a share intent, confirm on emulator `emulator-5554` whether it
is **real** or merely theoretical.

Likely suspects to exercise (final list comes from 01):
- URL corruption via route-encoding of special characters (`&`, `?`, `#`, `%`,
  `+`, spaces) round-tripping through the Navigation-Compose route.
- OpenGraph fetch failure/timeout taking the URL down with it (share while
  offline, or share a URL whose OG fetch throws).
- `ACTION_SEND_MULTIPLE` and non-`text/plain` MIME shares (does the app even
  appear / handle them?).

For each: exact input, observed result, real/theoretical verdict, and severity
(silent total loss > corruption > minor). Keep unconfirmable-by-intent items
flagged as "suspected" for 05.

Deliver as the resolution answer (link logs/assets). Do NOT fix anything —
diagnosis only.

## Answer

Confirmed on `emulator-5554` (SDK 36). Full table + screenshots:
[04-suspect-confirmation](../assets/04-suspect-confirmation.md).

**REAL — silent total loss (highest impact):**
- **L4+L5** — space-separated "Title URL" and prose+URL payloads both drop the
  URL entirely (blank Add screen), same failure as the confirmed Google
  `"Title\nURL"` case. This is the dominant, highest-frequency break.
- **L8** — the URL field is populated only *after* the OpenGraph fetch. With a
  hanging/black-holed fetch the URL field stays blank for the full ~10–20 s
  timeout window while the Save button is live, so an early Save persists
  `url=null`. (A *clean* offline network fast-fails and the URL populates fine —
  the drop needs a hang, not mere offline.)
- **L1** — `ACTION_SEND_MULTIPLE` and non-`text/plain` MIME (`text/html`,
  `image/png`) are never offered in the chooser and finish silently if forced.
  REAL but very low frequency for link sharing → supports keeping out of scope.

**REAL — corruption (medium):**
- **L6** — double-decode mangles special chars: `+` → space, `%2F` → `/`
  (structural), `%20` → space, `%25` → `%`. `?`, `&`, `=`, `#` round-trip fine.
  Invalid-UTF8 percent (`%c3`, `%ff`) is replaced with U+FFFD (also corruption).

**THEORETICAL / not reproducible by intent:**
- **L7** (URLDecoder throwing → ViewModel crash) — **not reproducible.** A bare
  `%`+non-hex is filtered out by L4's `Patterns.WEB_URL.matches()` before it
  reaches the decoder, and Android's `URLDecoder` is lenient (substitutes U+FFFD
  rather than throwing). No crash observed in any variant.
- **L3** (styled `CharSequence`/`ClipData`) and **L10** (OG metadata override)
  are not craftable via `am --es`; kept as "suspected" for 05.
