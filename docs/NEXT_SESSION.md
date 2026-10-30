# V Court next-session plan

Updated when phase A (F3, F4) was planned on 2026-10-30. Read
[context](CONTEXT.md), [progress](PROGRESS.md) and
[workflow](WORKFLOW.md) first. Check Git status and remotes before editing.
Real booking still requires explicit authorization for that test.

## Outcome

Use V Court's `develop` as the integration base, then build an Android-owned
driver session and verify it on MuMu. Recheck the latest desktop `develop` before
each behavior or protocol change. The release target is one V Court APK;
Python is build-time tooling only. V Court feature branches start from its own
`develop`, merge there after checks, and reach `main` when stable.

## Small parts and branches

| Part | Branch and base | Deliverable | Exit check |
|---|---|---|---|
| F0 — Documentation foundation | `docs/vcourt-foundation` from `main` (`729defd`) | Completed at `4fe5014`; included in `develop` and `main`; old task branch removed. | Reviewed docs only; links and staged diff checks passed. |
| F1 — Android development foundation | `chore/vcourt-android-foundation` from `docs/vcourt-foundation` (`a46633b`) | Completed at `d76bba9`; included in `develop` and `main`; old task branch removed. | Both APKs built locally. Initial GitHub CI failed in SDK setup; C1 addresses it. |
| R1 — Migration roadmap | `docs/vcourt-mobile-roadmap` from F1 HEAD (`757f95c`) | Complete at `2b47979`; included in `develop` and `main`; old task branch removed. | Desktop source mapped to ordered Android slices and one-APK gate; docs checked. |
| B1 — `develop` workflow | `docs/vcourt-develop-workflow` from `develop` (`6d55713`) | Merged into `develop` at `0c72e67`, then into `main` through PR #1; old task branch removed. | Branch topology recorded; subsequent work branches target `develop`. |
| C1 — Build readiness and source context | `fix/vcourt-build-readiness` from `develop` (`0c72e67`) | Fix at `68de4bb`; merged into `develop` through [PR #2](https://github.com/servedbyhau/v-court/pull/2) at `f9fe06b`. | GitHub CI and both local APK builds passed; lab references and Markdown links verified. No product runtime change. |
| S1 — Desktop sync | `docs/vcourt-desktop-sync` from `develop` (`f9fe06b`) | Merged through PR #3 at `930fdf4`. | Desktop changes from `50ed350` to `a16240a` recorded. |
| F2 — Safe protocol client | `feat/vcourt-driver-client` stacked on S1 | Merged through PR #4 at `cc472db`; promoted to `main` through PR #5. | 57 JVM tests and both APK builds pass in CI. |
| S2 — Session context | `docs/vcourt-session-context` from `develop` (`cc472db`) | Merged through PR #6 at `3a3baca`. | Docs agree; links checked. |
| F2a — One-APK architecture proof | From `develop` (`3a3baca`); merged through PR #7 at `333f2cd` | One APK with app and Instrumentation driver; Shizuku shell service starts it with `--no-restart`; the in-app proof runs hello, safe read/tap and cleanup. CI builds it. | Open: fresh single-APK install on MuMu and two passing proof reports, recorded on a `docs/` branch. |
| D1 — F2a status and branch names | `docs/vcourt-f2a-status` from `develop` (`333f2cd`) | Status notes say F2a merged with its MuMu run open; AGENTS.md states the branch naming rule. | Docs agree; links checked; merge into `develop`. |
| F3 — Owned driver session | `feat/vcourt-driver-session` from `develop` (`734a531`) | Implemented. `DriverSession` in the engine: refuses a driver it did not start, starts one through a `DriverLauncher`, waits for `hello` (protocol and version), stops it and ends it if it does not stop, can be cancelled from another thread; Android `DriverLauncher` and `DriverConnector` over `ShellBridge` and `LocalSocket`; the one-APK proof runs on it. | Fake lifecycle tests pass on the JVM; app compile check and CI build pass; merge into `develop`. |
| F4 — Vinhomes read check | `feat/vcourt-vinhomes-read` stacked on F3 | In-app check: open a session, open Vinhomes Resident, read its screen and report only counts and whether the home tile label shows, return to V Court, tap V Court's test target, stop. | Compile check and CI build pass; merge into `develop` after F3. |
| F2a/F4 device evidence | `test/vcourt-mumu-session` from `develop` after F4 | Run the one-APK proof twice and the Vinhomes read check once on MuMu; record the redacted reports. Completes M1. | Reports pass; no booking submitted. |
| Two-APK removal | `chore/vcourt-drop-driver-apk` from `develop` after the device evidence | Stop building the separate driver APK in CI and docs once one APK is proven. | CI passes; docs agree. |

Record actual base and result commits in [PROGRESS.md](PROGRESS.md). If a part
grows too large, split it into new named branches here before coding.

## Phase A (M1): F3 and F4 code, then the device evidence

Planned 2026-10-30. F3 ports the desktop's start and stop rules (`connect_driver`,
`start_driver`, `stop_driver` in `driver.py`: start, `hello` every 0.1 s for up to
10 s, `stop`, then end the driver when it does not stop) with V Court's ownership
rule: a driver this session did not start is refused, never stopped. The proof's
own start and stop code moves into it. F4 adds the Vinhomes read check, whose
report never holds screen text. Both are coded in the cloud; the device evidence
needs MuMu.

## F2a, one-APK architecture proof

The code merged into `develop` through PR #7 (`333f2cd`) before the device run,
so that run is what remains; its result goes on its own branch from `develop`.

1. Code, done: the driver sources in `android/app` are compiled
   into the V Court APK, which declares them as an Instrumentation targeting
   itself. A Shizuku user service (`ShellService`, shell UID) runs
   `am instrument -w --no-restart`: Android then loads the driver into the
   running V Court process instead of force-stopping the package (checked in
   the Android 15 framework, `INSTR_FLAG_NO_RESTART`; other versions unchecked). The app
   connects a `LocalSocket` to `court-driver`, wraps it in `StreamTransport` and
   runs `hello`, `current`, `dump`, one `click` on V Court's own test target,
   then `stop`, and checks that the driver ended and the socket is gone. The
   lab's `ShellBridge` could not be read in the cloud; this one is new.
2. Checks, done: engine tests and a compile against an Android 15 framework jar
   in the cloud; CI built and signed the APK on PR #7 and on `develop`.
3. Device gate, open, on the development machine: install the one APK on MuMu with
   Shizuku running, run the proof twice ([SETUP.md](SETUP.md), "One-APK proof"),
   and record the redacted report in [PROGRESS.md](PROGRESS.md). No booking.
4. Then record the package and Instrumentation contract in
   [TECH_STACK.md](TECH_STACK.md) and [CONTEXT.md](CONTEXT.md). If
   `--no-restart` is refused, record the blocker; the fallback is calling
   `IActivityManager.startInstrumentation` with the same flag from the shell
   service, then a driver run without the instrumentation (UiAutomation made in
   the shell service).

F2a and F4 need MuMu and Shizuku online. Decide the update-signing approach
before release work.
