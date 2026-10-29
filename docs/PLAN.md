# V Court project plan

Updated: 2026-10-29. Current development uses **MuMu only**. Physical-phone
validation follows later. Work happens in this `v-court` repository, with
sibling desktop and lab folders as references. See [context](CONTEXT.md),
[progress](PROGRESS.md), [next-session plan](NEXT_SESSION.md) and
[workflow](WORKFLOW.md).

## Goal

V Court should book a configured tennis court at the opening time on an Android
phone without PC control and call it booked only after reading the ticket. It
should show readiness and distinguish booked, refused and unknown outcomes.

## Milestones

| Milestone | Exit condition |
|---|---|
| M0 — Foundation | Repo rules, source boundary, plan, build strategy and first branch-sized work are recorded in `v-court`. |
| M1 — Driver path | Imported driver core and Android-owned Shizuku session build here; on MuMu, the app handshakes, reads Vinhomes UI, safely taps its own target and cleans up without PC commands. |
| M2 — Manual booking | New Android engine uses current desktop behavior as reference. MuMu dry run stops before confirmation; a separately authorized real run reads its ticket. |
| M3 — Timed run | The Android app warms up and runs at a configured opening on MuMu, with bounded timeouts, one active run and clear failure states. |
| M4 — Mobile product | Native setup, choices, readiness, run state, history and notifications work on MuMu without FastAPI or a browser. |
| M5 — Phone validation | Repeat driver, booking, background, locked-screen and interruption tests on a chosen physical phone. Resolve Shizuku and reboot readiness there. |
| M6 — Release | Reviewed source, CI, signing/update path, privacy checks and fresh install/update evidence are available from this repo. |

MuMu success is emulator evidence, not phone compatibility. Split each milestone
into small parts with separate branches in [NEXT_SESSION.md](NEXT_SESSION.md)
or a linked task plan.

## Source decisions

- Retain and adapt the Java UiAutomation instrumentation driver and protocol.
- Use native Android Java, Android Views, Shizuku API 13.1.5 and the pinned
  SDK-tools builds for the first product slices. See [TECH_STACK.md](TECH_STACK.md).
- Translate desktop screen recognition, bilingual labels, retries, venue and
  calendar behavior, ticket verification and uncertain outcomes into Android.
  Check the latest desktop tests before porting.
- Build Android UI, engine, scheduling and persistence here. Leave Windows Task
  Scheduler, host ADB, Python/FastAPI and React out of the app runtime.
- Import source selectively with licenses and tests. Keep personal config,
  screenshots, UI dumps, logs, keys and generated APKs out of Git.

## Open decisions

Release signing and update method, minimum supported phone version after device
testing, and exact scheduling design. The development app ID is
`dev.vcourt.mobile`; the driver keeps `dev.courtbooker.driver`.
