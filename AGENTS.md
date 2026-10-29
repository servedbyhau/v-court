# AI development guidance for V Court

This repository, `D:\VH-Booker\mobile\v-court`, is the **main Android development
repository**. At the start of every session, read [context](docs/CONTEXT.md),
[progress](docs/PROGRESS.md), [next-session plan](docs/NEXT_SESSION.md),
[technology stack](docs/TECH_STACK.md) and [desktop source map](docs/DESKTOP_REFERENCE.md).
Use [setup](docs/SETUP.md) for build and device prerequisites and
[the migration roadmap](docs/ROADMAP.md) for slice order.

## Current desktop reference is mandatory

- Always consult `D:\VH-Booker\vh-tennis-booker` before translating booking
  behavior, labels, settings or tests. Its `develop` branch is the source of
  truth, and changes after any dated snapshot in V Court docs take precedence.
- Compare local `develop` with remote `refs/heads/develop` before relying on it.
  Fetch the remote branch when it is newer, then inspect `origin/develop` with
  `git show`; do not switch or edit the desktop working tree. If network access
  prevents verification, state the local commit and the freshness limit.
- Read the desktop `AGENTS.md` and `CLAUDE.md` plus the files relevant to the
  slice. The lab worktree has been removed; its reviewed source remains in
  desktop Git at commit `b85a70e1bc582020d7bea4dbe65175c18fdbf24a`
  (`feat/android-on-device`). Read it with `git show <commit>:<path>` as
  documented in [DESKTOP_REFERENCE.md](docs/DESKTOP_REFERENCE.md). Use it as
  compatibility evidence; current desktop `develop` owns booking behavior.

## Planning and branches

- State the outcome, small steps and validation before project work. Record the
  actionable plan in `docs/NEXT_SESSION.md` or a linked task plan before edits.
- Follow [workflow](docs/WORKFLOW.md): branch each reviewable part from V
  Court's `develop`, record its base, deliverable and exit check, then merge
  it into `develop` after validation. Promote stable `develop` to `main` after
  review. Preserve work before switching branches.
- Update progress after each part and the next-session plan before ending work.
  Record actual evidence and limitations, not only intentions.

## Product and source boundaries

- Current development and validation use **MuMu only**. The eventual product runs
  on an Android phone without PC control; physical-phone tests are a later phase.
- Keep the Java instrumentation driver core and protocol as the starting point.
  Build the Android booking engine, UI, scheduling and local data here. The
  native Java/Shizuku/SDK-tools stack is recorded in [TECH_STACK.md](docs/TECH_STACK.md).
  Refer to current desktop tests when porting behavior.
- The release target is one V Court APK. Python under `tools/` is build-time
  host tooling only, not an APK runtime. The current two-APK layout is
  temporary; prove combined app/Instrumentation packaging on MuMu before
  fixing the product Shizuku session design.
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
- Run `tools/check_env.py` and the affected build/tests. Never commit build
  outputs, debug keys, accounts, UI dumps or real booking records.
- Documentation and code use English; app labels may use the target app's
  language. [The plan](docs/PLAN.md) owns milestones.
