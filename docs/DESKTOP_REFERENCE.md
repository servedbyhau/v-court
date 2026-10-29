# Current desktop reference for Android work

Always read the latest `develop` source at `D:\VH-Booker\vh-tennis-booker`
before translating behavior into V Court. Do not use the old Python tree in
`vh-tennis-booker-android` as the specification. The desktop checkout is a
read-only reference for V Court work; its daily runner and schedule are separate.

## Freshness check

At the 2026-10-29 review, local `develop` and GitHub `develop` both pointed to
`50ed350e8585c4f4316ad8162f5df525bf0a2354` (desktop release 0.18.0).
That is a dated observation, not a pinned requirement. At each new session:

```powershell
git -C D:\VH-Booker\vh-tennis-booker status --short --branch
git -C D:\VH-Booker\vh-tennis-booker rev-parse develop
git -C D:\VH-Booker\vh-tennis-booker ls-remote origin refs/heads/develop
```

If the remote hash differs, fetch `origin develop`, then read files from
`origin/develop` with `git show origin/develop:<path>` or update the local
reference without switching the desktop working tree. If the remote cannot be
checked, report the local commit and the freshness limit. Before modifying a
slice, read desktop `AGENTS.md`, its referenced `CLAUDE.md`, the current
`CHANGELOG.md`, and relevant files below.

## Source map

| V Court concern | Latest desktop sources and tests to inspect |
|---|---|
| Driver protocol, command IDs, timeout and version | `android/app/`, `src/court_booker/driver.py`, `tests/test_driver.py`, `tests/test_build_driver.py` |
| Screen parsing and safe target selection | `src/court_booker/screen.py`, `geometry.py`, `tests/test_screen.py`, `tests/test_geometry.py` |
| Booking state machine and outcomes | `src/court_booker/flow.py`, `runner.py`, `tests/test_flow.py`, `tests/test_runner.py` |
| Languages, sports, courts and venues | `src/court_booker/app_language.py`, `catalog.py`, `tests/test_app_language.py`, `tests/test_catalog.py` |
| Timing, runs and failure handling | `src/court_booker/clock.py`, `watchdog.py`, `lock.py`, `events.py`, matching tests |
| Settings and history behavior | `src/court_booker/config.py`, `history.py`, `history_read.py`, matching tests |
| Observed target app behavior | `docs/field-notes.md`, `docs/how-it-works.md`, `docs/architecture.md` |

The desktop UI and API can inform wording and settings behavior. Do not copy
FastAPI, React, Windows scheduling, ADB forwarding or the desktop SQLite
schema into the Android runtime. For example, desktop 0.18.0 added a second
MuMu machine tab and automatic MuMu/wake-timer setup; those are desktop
features, not phone requirements. Current booking invariants include a
verified ticket for success, separate refusal and unknown outcomes, optional
venue handling, quota notices and recovery from a moving court list. Confirm
their implementation against the current desktop source and tests each time.
