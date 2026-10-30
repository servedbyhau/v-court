# V Court next-session plan

Updated after S2 merged (PR #6) and F2a was implemented on 2026-10-30. Read
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
| F2a — One-APK architecture proof | `spike/vcourt-one-apk` from `develop` (`3a3baca`) | Implemented, not committed: one APK with app and Instrumentation driver; Shizuku shell service starts it with `--no-restart`; the in-app proof runs hello, safe read/tap and cleanup. | CI builds the APK; fresh single-APK install on MuMu and two passing proof reports; merge into `develop`. |
| F3 — Owned Shizuku session | `feat/vcourt-driver-session` from `develop` after F2a | Adapt lab bridge into reusable Android-owned start/stop session with protocol/version check and cancellation. | Fake lifecycle tests and app build pass; no foreign driver stopped; merge into `develop`. |
| F4 — MuMu proof | `test/vcourt-mumu-session` from `develop` after F3 | Run hello, Vinhomes UI read, safe V Court test tap and cleanup from Android app. | Redacted evidence shows success with no host booking commands; merge into `develop`; no booking submitted. |

Record actual base and result commits in [PROGRESS.md](PROGRESS.md). If a part
grows too large, split it into new named branches here before coding.

## Next: F2a, one-APK architecture proof

Base: `develop` at `3a3baca` (S2 merged through PR #6). Branch name per the table;
nothing is committed until the user asks.

1. Code, done in a cloud session: the driver sources in `android/app` are compiled
   into the V Court APK, which declares them as an Instrumentation targeting
   itself. A Shizuku user service (`ShellService`, shell UID) runs
   `am instrument -w --no-restart`: Android 13 and later then load the driver
   into the running V Court process instead of force-stopping the package
   (checked in the Android 15 framework: `INSTR_FLAG_NO_RESTART`). The app
   connects a `LocalSocket` to `court-driver`, wraps it in `StreamTransport` and
   runs `hello`, `current`, `dump`, one `click` on V Court's own test target,
   then `stop`, and checks that the driver ended and the socket is gone. The
   lab's `ShellBridge` could not be read in the cloud; this one is new.
2. Checks in the cloud: engine tests, and a compile of the app and driver against
   an Android 15 framework jar. CI builds the APK.
3. Device gate, on the development machine: install the one APK on MuMu with
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
