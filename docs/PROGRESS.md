# V Court progress

Last updated: 2026-10-25. Read with [context](CONTEXT.md) and
[next-session plan](NEXT_SESSION.md). Progress is based on evidence in this repo.

## Current position

- Main development folder: `D:\VH-Booker\mobile\v-court`; Git remote:
  `servedbyhau/v-court`. Initial `main` commit: `729defd`.
- This repo had documentation only before the foundation branch. No driver,
  Android app, booking engine or scheduler has been imported here.
- Current development target: MuMu emulator. Physical-phone validation is later.
- The sibling lab proved Android-controlled UI read/tap on MuMu. The only
  documented real booking used Python on the host.

## Foundation work

| Part | Branch | Status | Evidence and limit |
|---|---|---|---|
| F0 — Establish V Court as the main development repo | `docs/vcourt-foundation` from `main` (`729defd`) | Complete locally at `4fe5014`; not pushed. | README, plan, AGENTS and context/handoff docs committed. Local links and staged diff checks passed. No source imported or booking behavior proven. |

## Next gate

Follow [NEXT_SESSION.md](NEXT_SESSION.md) to bring in the driver core and
implement a safe Android session on separate branches.
MuMu must be online for live validation. Record the branch and commit of
each completed part here; a plan or build alone does not complete a milestone.
