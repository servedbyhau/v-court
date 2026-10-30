# V Court next-session plan

Updated after C1 merged (PR #2, `f9fe06b`) and the desktop sync S1 on 2026-10-30. Read
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
| S1 — Desktop sync | `docs/vcourt-desktop-sync` from `develop` (`f9fe06b`) | Record desktop changes from `50ed350` to `a16240a` in DESKTOP_REFERENCE and refresh the dated baselines. | Docs agree on the new baseline; links checked; merge into `develop`. |
| F2 — Safe protocol client | `feat/vcourt-driver-client` stacked on S1 (`40930fc`) | Implemented: `tools/test_java.py` (JUnit on the JVM), `DriverClient`, `StreamTransport` and JSON codec in `dev.vcourt.engine.driver`. | 57 tests pass locally; CI must pass the tests and both APK builds; merge into `develop` after S1. |
| F2a — One-APK architecture proof | `spike/vcourt-one-apk` from `develop` after F2 | Prove one installable V Court APK containing app and Instrumentation driver on MuMu, with Shizuku startup, hello, safe read/tap and cleanup. | Fresh single-APK install and redacted device evidence pass; merge into `develop`. |
| F3 — Owned Shizuku session | `feat/vcourt-driver-session` from `develop` after F2a | Adapt lab bridge into reusable Android-owned start/stop session with protocol/version check and cancellation. | Fake lifecycle tests and app build pass; no foreign driver stopped; merge into `develop`. |
| F4 — MuMu proof | `test/vcourt-mumu-session` from `develop` after F3 | Run hello, Vinhomes UI read, safe V Court test tap and cleanup from Android app. | Redacted evidence shows success with no host booking commands; merge into `develop`; no booking submitted. |

Record actual base and result commits in [PROGRESS.md](PROGRESS.md). If a part
grows too large, split it into new named branches here before coding.

## First actions after F2

1. Merge S1, then F2, into V Court's `develop` once CI passes on each; F2's CI
   run is the first to build both APKs with the engine package inside.
2. F2a needs MuMu and Shizuku online: prove the one-APK layout and record its
   package/Instrumentation contract before F3. The app reaches the driver
   through a `LocalSocket` to `court-driver`; wrap its streams in
   `StreamTransport` and talk through `DriverClient`, so F2a reuses F2 as is.
3. Refresh the desktop `develop` reference as described in
   [DESKTOP_REFERENCE.md](DESKTOP_REFERENCE.md) before F3; the lab's
   `ShellBridge` is the transport reference, available from its archived Git
   commit rather than a folder.

F2a and F4 need MuMu and Shizuku online. Decide the update-signing approach
before release work.
