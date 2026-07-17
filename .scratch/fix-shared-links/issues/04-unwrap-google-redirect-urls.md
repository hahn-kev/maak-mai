# 04 — Unwrap Google redirect share URLs

**What to build:** When a shared link is a known redirect wrapper —
`https://www.google.com/url?q=<destination>&sa=...` and similar — the app captures
and stores the `<destination>`, not the wrapper. Non-wrapper URLs are stored
unchanged.

Note: confidence that this shape is actually common is low (the Google app usually
shares the destination directly). Keep the unwrapping list small and conservative;
this is a nice-to-have, not a load-bearing fix.

**Blocked by:** 01 (extends the parsed-URL step).

**Status:** ready-for-agent

- [ ] Sharing `https://www.google.com/url?q=https://example.com/x&sa=U` captures `https://example.com/x`
- [ ] A normal `https://example.com/x` is stored unchanged
- [ ] Unwrapping is limited to a small, documented set of known redirect wrappers
- [ ] Unit tests cover wrapped and unwrapped inputs
