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
  PR #7 (`333f2cd`), before its MuMu run. That run is the open gate.
- This repo has a buildable Java driver, a native Android app shell and, from
  F2, a plain-Java protocol client with JVM tests. Since F2a the app APK also
  holds the driver and can run the one-APK proof; CI builds it, but it has not
  run on a device.
  There is no reusable driver session, booking engine, daily scheduler or
  history yet.
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

## Next gate

F2a's device run: build the APK on the development machine from `develop`,
install it on MuMu with Shizuku running and follow [SETUP.md](SETUP.md),
"One-APK proof". Then F3, the owned Shizuku session, per
[NEXT_SESSION.md](NEXT_SESSION.md). A build does not complete M1.
