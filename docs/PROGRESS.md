# V Court progress

Last updated: 2026-10-29. Read with [context](CONTEXT.md) and
[next-session plan](NEXT_SESSION.md). Progress is based on evidence in this repo.

## Current position

- Main development folder: `D:\VH-Booker\mobile\v-court`; Git remote:
  `servedbyhau/v-court`. Current branch `chore/vcourt-android-foundation`
  from `docs/vcourt-foundation` (`a46633b`); not pushed.
- This repo now has a buildable Java driver and a minimal native Android app
  shell. The app reports Shizuku readiness and can request permission. It has
  no owned driver session, booking engine, daily scheduler or history yet.
- Current development target: MuMu emulator. Physical-phone validation is later.
- The sibling lab proved Android-controlled UI read/tap on MuMu. The only
  documented real booking used Python on the host.
- Desktop `D:\VH-Booker\vh-tennis-booker` local and remote `develop` were both
  `50ed350` (release 0.18.0) at this review. Refresh before each porting slice.

## Foundation work

| Part | Branch | Status | Evidence and limit |
|---|---|---|---|
| F0 — Establish V Court as the main development repo | `docs/vcourt-foundation` from `main` (`729defd`) | Complete locally at `4fe5014`; not pushed. | README, plan, AGENTS and context/handoff docs committed. Local links and staged diff checks passed. No source imported or booking behavior proven. |
| F1 — Android build and AI context | `chore/vcourt-android-foundation` from `docs/vcourt-foundation` (`a46633b`) | Complete locally at `d76bba9`; not pushed. | Python 3.14.4, JDK 21.0.10 and SDK/Build Tools 35.0.0 passed `check_env.py`. Both APK builds and signature verification passed locally; app min SDK 33, driver min SDK 30, both target 35. Python scripts compiled; staged diff checks passed. CI workflow added but not run remotely. No MuMu device was attached, so no live session or booking is claimed. |

## Next gate

Follow [NEXT_SESSION.md](NEXT_SESSION.md) for the safe protocol client and
owned Shizuku session on separate branches. MuMu must be online for live
validation. A build does not complete the M1 device gate.
