# Codex guidance for V Court

This repository, `D:\VH-Booker\mobile\v-court`, is the **main development repository**.
The sibling `vh-tennis-booker-android` folder is a mobile experiment inside the
original desktop repository; `vh-tennis-booker` is the desktop product. Read
[context](docs/CONTEXT.md), [progress](docs/PROGRESS.md) and
[next-session plan](docs/NEXT_SESSION.md) at the start of project work.

## Planning and branches

- State the outcome, small steps and validation before project work. Record the
  actionable plan in `docs/NEXT_SESSION.md` or a linked task plan before edits.
- Follow [workflow](docs/WORKFLOW.md): one reviewable part per named branch, with
  base, deliverable and exit check recorded. Preserve work before switching.
- Update progress after each part and the next-session plan before ending work.
  Record actual evidence and limitations, not only intentions.

## Product and source boundaries

- Current development and validation use **MuMu only**. The eventual product runs
  on an Android phone without PC control; physical-phone tests are a later phase.
- Keep the Java instrumentation driver core and protocol as the starting point.
  Build the Android booking engine, UI, scheduling and local data here. Refer to
  sibling sources and current desktop tests when porting behavior.
- Import only reviewed files needed for a mobile slice. Do not bulk-copy Python,
  FastAPI, React, Windows scheduling code, desktop history or private artifacts.
- A real booking is successful only after the target app shows its ticket.
  Distinguish refusal and unknown outcomes. Never submit a real booking without
  the user's explicit authorization for that test.
- Keep account data, UI dumps, screenshots, logs, keys and private configuration
  out of commits. Use synthetic examples and redacted evidence.

## Working practice

- Check `git status --short --branch` and `git remote -v` before Git operations.
  This repo's `origin` must be `servedbyhau/v-court`.
- Keep each implementation slice buildable. Use fake-driver tests for booking
  decisions and MuMu for the current device gate. Emulator evidence does not
  prove physical-phone compatibility.
- Documentation and code use English; app labels may use the target app's
  language. [The plan](docs/PLAN.md) owns milestones.
