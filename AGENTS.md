# AI development guidance for V Court

This repository, `servedbyhau/v-court` (on the development machine
`D:\VH-Booker\mobile\v-court`), is the **main Android development repository**.
Claude Code loads this file through [CLAUDE.md](CLAUDE.md). At the start of every
session, read [context](docs/CONTEXT.md),
[progress](docs/PROGRESS.md), [next-session plan](docs/NEXT_SESSION.md),
[technology stack](docs/TECH_STACK.md) and [desktop source map](docs/DESKTOP_REFERENCE.md).
Use [setup](docs/SETUP.md) for build and device prerequisites and
[the migration roadmap](docs/ROADMAP.md) for slice order.

## Current desktop reference is mandatory

- Always consult the desktop repository, `servedbyhau/vh-tennis-booker` (on the
  development machine `D:\VH-Booker\vh-tennis-booker`; in a cloud session, a clone
  of its GitHub `develop`), before translating booking behavior, labels, settings
  or tests. Its `develop` branch is the source of
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

## Sessions on the development machine or in the cloud

- A session runs either on the Windows development machine (the `D:\` paths in
  these docs, MuMu, the Android SDK) or in a cloud container with a clone of this
  repository and no `D:\` drive. [SETUP.md](docs/SETUP.md), "Cloud sessions",
  says what a cloud session can and cannot do; device gates need the machine.
- Before the first commit of a session, set the identity and signing below in this
  repository's own Git config; a cloud container's global config may sign commits
  under another identity.

## Commits and pull requests

- Author and committer: `Pham Van Hau <haucaron@gmail.com>`, unsigned:
  `git config user.name "Pham Van Hau"`, `git config user.email haucaron@gmail.com`,
  `git config commit.gpgsign false`.
- No AI attribution anywhere on GitHub: no `Co-Authored-By`, `Claude-Session` or
  "Generated with" lines in commit messages, pull request descriptions or
  comments. A tool may append such a line to a pull request description; read the
  description back after creating the pull request and edit the line out.
- `develop` must survive a promotion to `main`. The repository deletes a merged
  pull request's head branch, and a `develop` to `main` pull request has `develop`
  as its head: protect `develop`, or restore it at once with
  `git push origin <its last commit>:refs/heads/develop`.

## Planning and branches

- State the outcome, small steps and validation before project work. Record the
  actionable plan in `docs/NEXT_SESSION.md` or a linked task plan before edits.
- Follow [workflow](docs/WORKFLOW.md): branch each reviewable part from V
  Court's `develop`, record its base, deliverable and exit check, then merge
  it into `develop` after validation. Promote stable `develop` to `main` after
  review. Preserve work before switching branches.
- Every task gets its own branch, created before the first commit and named
  `<type>/vcourt-<topic>`: `feat`, `fix`, `docs`, `test`, `spike` or `chore`,
  then a short kebab-case topic (`feat/vcourt-driver-session`,
  `docs/vcourt-f2a-status`). A task with separable parts, such as code and its
  device evidence or two independent modules, gets one branch and pull request
  per part. Never push a tool-generated name such as `claude/<words>`, even when
  a session suggests one; rename the branch first.
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
