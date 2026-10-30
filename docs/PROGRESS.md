# V Court progress

Last updated: 2026-10-30. Read with [context](CONTEXT.md) and
[next-session plan](NEXT_SESSION.md). Progress is based on evidence in this repo.

## Current position

- Main development folder: `D:\VH-Booker\mobile\v-court` (a cloud session uses
  its own clone); Git remote: `servedbyhau/v-court`. V Court's `develop` was created on GitHub from
  `docs/vcourt-mobile-roadmap` at `6d55713`. Work branches now start from
  `develop` and merge there; stable work is promoted to `main`.
- Foundation and roadmap reached `main` through merged PR #1 (`557efd5`).
  The four completed task branches were removed after ancestry checks found
  no unmerged commits. C1 merged into `develop` through PR #2 (`f9fe06b`), S1
  through PR #3 (`930fdf4`) and F2 through PR #4 (`cc472db`). `develop` was
  promoted to `main` through PR #5 (`8afc3ef`); `main`'s CI, red since PR #1 from
  the old SDK package list, passed there. Merging PR #5 deleted `develop` (head
  branches are deleted on merge); it was restored at `cc472db` the same minute.
  S2 merged into `develop` through PR #6 (`3a3baca`) and F2a's code through
  PR #7 (`333f2cd`), before its MuMu run. That run is the open gate. Phase A's
  code is on `feat/vcourt-driver-session` (F3) and `feat/vcourt-vinhomes-read`
  (F4), from `develop` (`734a531`).
- This repo has a buildable Java driver, a native Android app shell and, from
  F2, a plain-Java protocol client with JVM tests. Since F2a the app APK also
  holds the driver and can run the one-APK proof; CI builds it, but it has not
  run on a device. F3 adds the reusable driver session the proof now runs on.
- The full app is coded on fourteen local branches after F4 (not pushed; list and
  test order in [NEXT_SESSION.md](NEXT_SESSION.md)): the engine ported from desktop
  `develop` `9a8e088` (screen model, languages, catalog, settings, booking flow, run
  clock and rounds) with 152 JVM tests, and the Android product (driver device, stored
  settings, history, foreground run service, exact daily alarm, home screen). The
  Android code compiles against the Android 15 framework jar; no APK of it was built
  and nothing of it ran on a device.
- Current development target: MuMu emulator. Physical-phone validation is later.
- The sibling lab proved Android-controlled UI read/tap on MuMu. The only
  documented real booking used Python on the host.
- Desktop GitHub `develop` was `a16240a` (release 0.18.1 plus unreleased tooling
  and docs) at the 2026-10-30 sync; [DESKTOP_REFERENCE.md](DESKTOP_REFERENCE.md)
  lists what changed since `50ed350`. Refresh before each porting slice.
- The lab folder is absent. Its README, bridge, probe, controller and build
  scripts were verified in desktop Git at `b85a70e1bc582020d7bea4dbe65175c18fdbf24a`.
  [DESKTOP_REFERENCE.md](DESKTOP_REFERENCE.md) provides working `git show` commands.
- [ROADMAP.md](ROADMAP.md) maps desktop behavior to Android slices. The release
  target is one V Court APK without Python runtime; the separate driver APK is
  kept until the one-APK proof passes on MuMu. Shizuku remains separately installed under this stack.

## Foundation work

| Part | Branch | Status | Evidence and limit |
|---|---|---|---|
| F0 — Establish V Court as the main development repo | `docs/vcourt-foundation` from `main` (`729defd`) | Complete at `4fe5014`; branch pushed. | README, plan, AGENTS and context/handoff docs committed. Local links and staged diff checks passed. No source imported or booking behavior proven. |
| F1 — Android build and AI context | `chore/vcourt-android-foundation` from `docs/vcourt-foundation` (`a46633b`) | Complete at `d76bba9`; branch pushed. | Python 3.14.4, JDK 21.0.10 and SDK/Build Tools 35.0.0 passed `check_env.py`. Both APK builds and signature verification passed locally; app min SDK 33, driver min SDK 30, both target 35. Python scripts compiled; staged diff checks passed. CI workflow added; its GitHub result was not checked in this record. No MuMu device was attached, so no live session or booking is claimed. |
| R1 — Desktop-to-Android migration roadmap | `docs/vcourt-mobile-roadmap` from F1 HEAD (`757f95c`) | Complete at `2b47979`; branch pushed. | Local and remote desktop `develop` matched at `50ed350`; reviewed current desktop roadmap, architecture, flow and driver tests. Source map, ordered slices, one-APK proof and validation gates recorded. Documentation diff check passed; no Android implementation or device test in this slice. |
| B1 — `develop` integration workflow | `docs/vcourt-develop-workflow` from `develop` (`6d55713`) | Merged into `develop` at `0c72e67`; included in `main` through PR #1. | Branch topology and future bases recorded; documentation diff check passed. Old task branch removed. |
| C1 — Build readiness and source context | `fix/vcourt-build-readiness` from `develop` (`0c72e67`) | Fix at `68de4bb`; merged into `develop` through [PR #2](https://github.com/servedbyhau/v-court/pull/2) at `f9fe06b`. | [GitHub CI passed](https://github.com/servedbyhau/v-court/actions/runs/40929071381): SDK setup, environment check and both APK builds. Local builds/signature verification, lab source-file checks, Markdown links and diff checks passed. Replaced unavailable SDK package `tools` and missing lab folders with verified Git references. No device test. |
| S1 — Desktop sync to 0.18.1 | `docs/vcourt-desktop-sync` from `develop` (`f9fe06b`) | Merged into `develop` through [PR #3](https://github.com/servedbyhau/v-court/pull/3) at `930fdf4`. | Desktop GitHub `develop` `a16240a` compared with the `50ed350` baseline: the calendar day-tap fix and the `pick_date` label recorded for slices 4 and 5; driver protocol and sources unchanged (V Court `Driver.java` differs in comments only). No code or device change. |
| F2 — Safe protocol client | `feat/vcourt-driver-client` stacked on S1 | Merged into `develop` through [PR #4](https://github.com/servedbyhau/v-court/pull/4) at `cc472db`. | `DriverClient`, `StreamTransport` and a dependency-free JSON codec in `dev.vcourt.engine.driver`, ported from desktop `driver.py` with its messages and broken-connection rules. `tools/test_java.py` runs 57 JUnit tests on JDK 21 (`--release 11`; engine compiled with `-Xlint:all -Werror`), including a client session over a loopback socket against a stand-in driver; breaking the timeout or id rule fails them. CI ran the tests and built both APKs with the engine inside. No device test. |
| P1 — Promote `develop` to `main` | `develop` (`cc472db`) into `main` | Merged through [PR #5](https://github.com/servedbyhau/v-court/pull/5) at `8afc3ef`. | `main`'s CI passed for the first time (its only earlier run failed in SDK setup, fixed by C1). The merge deleted `develop`; it was pushed back at `cc472db`. Protect `develop` to prevent a repeat. |
| S2 — Session context | `docs/vcourt-session-context` from `develop` (`cc472db`) | Merged into `develop` through [PR #6](https://github.com/servedbyhau/v-court/pull/6) at `3a3baca`. | `CLAUDE.md` imports `AGENTS.md`; commit and pull request rules, cloud session limits and a GitHub route to the desktop reference recorded; status notes brought up to date. No code change. |
| F2a — One-APK proof | `claude/modest-einstein-8rwk32` from `develop` (`3a3baca`), a session-generated name instead of `spike/vcourt-one-apk` | Code at `5d44964`, merged into `develop` through [PR #7](https://github.com/servedbyhau/v-court/pull/7) at `333f2cd`; the MuMu run is still open. | The app APK compiles `android/app`'s driver in and declares it as an Instrumentation targeting `dev.vcourt.mobile`. `ShellService`, a Shizuku user service, runs `am instrument -w --no-restart`; the Android 15 framework (Robolectric `android-all` 15) shows that flag keeps the running process instead of force-stopping the package, at start and at finish. `OneApkProof` checks the free socket, starts the driver, runs `hello`, `current`, `dump`, a `click` on V Court's own test target (and that the tap arrived), `stop`, and that `am` ended and the socket closed. Evidence: the app and driver compile against that jar with `-Xlint:all` and no warning in this repo's code; the 57 engine tests pass; [CI on the pull request](https://github.com/servedbyhau/v-court/actions/runs/41018714495) and [on `develop`](https://github.com/servedbyhau/v-court/actions/runs/41018859919) ran the tests and built and signed both APKs, the V Court one with the driver inside. Nothing has run on a device: whether `am` accepts `--no-restart` there is unproven. |
| F3 — Owned driver session | `feat/vcourt-driver-session` from `develop` (`734a531`) | Implemented; merge into `develop` after CI is the exit. | `DriverSession`, `DriverLauncher`, `DriverConnector`, `LauncherState`, `DriverUnavailableException` and `DriverInUseException` in `dev.vcourt.engine.driver`, ported from the desktop's `connect_driver` and `stop_driver` with V Court's ownership rule; `ShizukuDriverLauncher` and `LocalSocketConnector` on Android; `OneApkProof` runs on the session. 14 new JVM tests (71 in all) cover refusing a foreign driver, the start poll, a dropped first hello, an early exit, the start timeout, another protocol or version (never stopped), a failed start, an interrupted start, stop and its timeout, cancel, and a stop that cannot reach the driver. App compile check against Android 15 clean. No device run. |
| F4 — Vinhomes read check | `feat/vcourt-vinhomes-read` stacked on F3 (`8198a50`) | Implemented; merge into `develop` after F3 and CI is the exit. | `VinhomesReadCheck` opens a `DriverSession`, starts Vinhomes Resident (`com.vinhomes.resident`, declared in `<queries>`), waits for it in front, reads it every 0.25 s for up to 30 s until "Tiện ích" or "Utilities" shows (desktop `home` label, exact match), comes back to V Court by an activity start or one Back, taps V Court's test target and stops. Nothing in Vinhomes is tapped; the report holds counts, timings and the label only. `ScreenProbe` and `CheckSupport` are shared with the one-APK proof. App compile check against Android 15 clean; 71 engine tests pass. No device run. |
| Full build | 14 local branches from `docs/vcourt-full-build-plan` to `docs/vcourt-full-build-status`, stacked on F4 | Coded and committed locally; not pushed. | Engine: `dev.vcourt.engine.screen`, `.booking`, `.events`, `.flow`, `.clock`; 152 JVM tests pass (`-Xlint:all -Werror`), among them 23 flow cases on the desktop's fake app (a full round costs 16 commands, as on the desktop) and 5 run cases. Android: `DriverDevice`, `SettingsStore`/`SettingsActivity`, `HistoryStore`/`HistoryActivity`, `RunService`, `DailySchedule`, `MainActivity`, `DriverChecksActivity`; compile check clean. Not built with the SDK, not run. Known gaps: English-only UI; on a phone the screen and keyguard are not handled yet (MuMu's screen stays on); Shizuku without root stops at reboot. |
| MuMu run 1 and fix | `fix/vcourt-transport-close` stacked on `docs/vcourt-full-build-status` | Fix committed locally; the proof is to be run again on it. | The one-APK proof on MuMu (Android 15, SDK 35), 2026-10-30, twice: Shizuku as uid 2000; `am instrument -w --no-restart` loaded the driver into the running V Court process (greeted in 593 and 1013 ms, app pid unchanged); `current` 11-17 ms; a dump in 4-22 ms; the accessibility click reached the test target. Then the proof hung: the driver stopped and `am` ended with 0, but closing the connection waited forever. `StreamTransport.close()` closed the reader before the socket, and the reader's lock is held by the read blocked on a connection the in-app driver never closes; `LocalSocket.close()` alone does not wake that read. Now the socket goes first, with its input shut down. A test that stands for Android's socket hung on the old code and passes; 153 tests pass. |
| MuMu run 2: phase A passes (M1) | `fix/vcourt-transport-close` (`ffce783`), built on the development machine | Done on MuMu, 2026-10-30. | The one-APK proof twice in one app process (pid unchanged): `RESULT: PASS` both, the driver greeted in 690 and 628 ms, a dump in 15-17 ms, stopped with `am` ending 0 and the socket closed, about 0.8 s a proof. The Vinhomes read check three times, `RESULT: PASS` each: from a cold start Vinhomes came to the front in 1.8 s and showed its home tile after 20 s and 69 reads, through screens with few labels; with Vinhomes already open, at the first read in 0.1-0.15 s; dumps of its screen 5-30 ms. V Court came back by an activity start each time (no Back). The app showed English ("Utilities"). Nothing in Vinhomes was tapped; the reports hold counts and timings only. Next: Dry run now. |
| MuMu run 3: dry run passes | `fix/vcourt-transport-close` (`ffce783`) | Done on MuMu, 2026-10-30. | **Dry run now**, two slots on today + 2: NTP offset +0.218 s (time.google.com, 57 ms round trip); Vinhomes restarted, English found, account subdivision Origami, ready on its home screen in about 4 s; each round went Utilities, Tennis, the date, the slot, Next, S10, Next, read the confirmation page (date, slot and court as asked), ticked the consent and stopped before Confirm, in 3.7 and 3.5 s (72 and 81 device commands, most of them reads while waiting); the second round came back to the utilities list by itself; the driver stopped cleanly; outcome dry_run. The booking flow ported from the desktop ran unchanged on the real app. Next: a separately authorized real booking. |
| MuMu run 4: real booking (M2) | `fix/vcourt-transport-close` (`ffce783`) | Done on MuMu, 2026-10-30, authorized by the user. | **Book now**, two consecutive slots on today + 2 on S10: both rounds booked, the ticket read after "Confirm" each time (6.2 and 4.0 s); the app's ticket showed status Registered with the date, slot and court asked. History recorded `booked` with both rounds; the notification read "đã đặt 2/2". The user cancelled both bookings in the app. Next: the daily run with "Dry run only". |
| MuMu run 5: daily run, dry run only | `fix/vcourt-transport-close` (`ffce783`) | Done on MuMu, 2026-10-30. | The daily run set a few minutes ahead with "Dry run only": the exact alarm woke V Court by itself, the run waited and booked for today + 2, both rounds stopped before "Confirm" (4.3 and 3.1 s), History recorded it as `scheduled · dry run`, and the next alarm moved to the next day. The opening-time accuracy was not measured (no log taken). The user counts this test as passed. |
| MuMu run 6: reboot | `fix/vcourt-transport-close` (`ffce783`) | Done on MuMu, 2026-10-30. | After MuMu restarted, Shizuku (no root) had stopped and V Court showed it (Shizuku running ✗, authorized ✗, both run buttons disabled). With Shizuku started again, a daily run set for 17:20 (opening 17:22, "Dry run only") woke V Court and passed. Decision: V Court adds no reminder for a phone restarted, updated or run flat overnight; starting Shizuku again after a restart is the user's task (Wireless debugging without a PC, or a PC's adb). |

## Next gate

Device tests 1 (driver checks, M1), 2 (dry run), 3 (real booking, M2) and 4 (the
daily run, dry run only) passed on `fix/vcourt-transport-close`. Next, device test
5, a MuMu reboot, then the rest of [NEXT_SESSION.md](NEXT_SESSION.md),
"Device tests after the build"; push and merge the stacked branches in order when
the user asks.
