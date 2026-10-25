# V Court project context

Last reviewed: 2026-10-25. Verify Git and source state at each new session.

## Decisions

- `v-court` is the main repository and working folder from now on.
  `vh-tennis-booker-android` is a mobile experiment in the original desktop
  repository; `vh-tennis-booker` remains the desktop product. Treat both
  sibling folders as read-only references unless the user asks otherwise.
- Develop and test on MuMu for the current phase. The eventual product runs
  completely on an Android phone without a PC; physical tests are deferred.
- Retain and adapt the Java instrumentation driver core. Implement Android
  booking, scheduling, UI, settings, history and notifications as V Court code.
  The old Python, FastAPI, React and Windows task code is reference only.
- Plan before work, track progress and next-session actions in files, and
  assign one named branch to each small reviewable part. See [workflow](WORKFLOW.md).
- Do not submit a real booking without explicit authorization. A ticket is
  the evidence for success; refusals and unknown results remain separate.

## Verified external baseline

The sibling Android lab has a Java UiAutomation instrumentation driver, protocol
1 over a local abstract socket, and a Java Shizuku shell bridge. On MuMu Android
15 it started the driver, read Vinhomes UI and verified taps on its own test
target. The documented real booking used host Python; it did not establish an
Android-controlled booking engine. Both lab and driver APKs were rebuilt from
source on 2026-10-25. MuMu was disconnected during that later review.

The lab is in `D:\VH-Booker\vh-tennis-booker-android\android`. Check the latest
desktop `develop` before porting: venue loading, quota notices and month
navigation changed after the lab branch.

## Current repo state

Before this handoff, `v-court` contained README, LICENSE, .gitignore and
`docs/PLAN.md`, with no Android source. The next code slice imports only the
reviewed driver core and builds a safe Android driver session. See
[progress](PROGRESS.md) and [next-session plan](NEXT_SESSION.md).

Open decisions: Android application ID, signing/update path, UI toolkit,
minimum Android version and exact scheduling approach.
