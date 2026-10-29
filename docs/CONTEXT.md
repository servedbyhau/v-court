# V Court project context

Last reviewed: 2026-10-29. Verify Git and source state at each new session.

## Decisions

- `v-court` is the main repository and working folder from now on.
  `vh-tennis-booker-android` is a mobile experiment in the original desktop
  repository; `vh-tennis-booker` remains the desktop product. Treat both
  sibling folders as read-only references unless the user asks otherwise.
- V Court's `develop` is the integration branch. New task branches start there
  and merge back after validation; stable `develop` work is promoted to
  `main`. This branch is separate from the desktop's `develop` reference.
- Always consult the latest desktop `develop` at
  `D:\VH-Booker\vh-tennis-booker` when porting behavior. Check the remote
  branch first; [DESKTOP_REFERENCE.md](DESKTOP_REFERENCE.md) maps the current
  source and tests. The 2026-10-29 local and remote heads were both `50ed350`
  (release 0.18.0), but that observation must be refreshed.
- Develop and test on MuMu for the current phase. The eventual product runs
  completely on an Android phone without a PC; physical tests are deferred.
- Use the completed lab's [Java/Shizuku/UiAutomation stack](TECH_STACK.md) as
  the Android foundation. Native Java Views and SDK-tools builds are chosen
  for this phase; app identity is `dev.vcourt.mobile`. The development driver
  currently uses `dev.courtbooker.driver`; one-APK packaging may change its
  package identity while preserving the protocol behavior.
- The release must be one V Court APK with no Python runtime. The two current
  APKs are a temporary build scaffold. Prove combined app/Instrumentation
  packaging on MuMu before fixing the product session design. Shizuku remains
  separately installed under the chosen stack.
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

The Android foundation branch imports the current desktop driver source and
adds a minimal V Court app shell. Both APKs build from source with SDK 35 and
JDK 21 on the development machine. The app only shows Shizuku readiness and
requests permission: no driver session, booking engine or scheduler is present.
See [progress](PROGRESS.md) and [next-session plan](NEXT_SESSION.md) for the
verified state and next code slice.

Open decisions: release signing/update path, exact scheduling approach and
physical-phone minimum version after testing. The development keys stay in
ignored build directories.
