## Agent skills

### Issue tracker

Issues live as markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Uses the default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout: root `CONTEXT.md` + `docs/adr/`. See `docs/agents/domain.md`.

## Signing and releases

`app/signing/maakmai.keystore` is committed on purpose and is public. Both build types sign with it so every build installs over existing installs. Keep it as the only signing key: a new or rotated key makes every installed copy refuse updates until users uninstall, which loses their data.

Releases publish from `.github/workflows/ci.yml` when a `v*` tag is pushed; see README "Releases and signing".
