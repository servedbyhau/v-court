# V Court next-session plan

Updated for CI and source-reference repair on 2026-10-30. Read
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
| C1 — Build readiness and source context | `fix/vcourt-build-readiness` from `develop` (`0c72e67`) | Replace setup-android's obsolete default `tools` package with explicit SDK packages; replace missing lab-folder references with pinned Git source; refresh merged-branch handoff. | Local environment and both APK builds pass; GitHub CI passes on a PR into `develop`; lab reference files can be read with `git show`; no product runtime change. |
| F2 — Safe protocol client | `feat/vcourt-driver-client` from current `develop` after C1 | Add a repeatable Java fake-test command; implement Android command IDs, reply matching, timeout/EOF/invalid-reply handling and broken-session state. | Fake-transport tests run locally and in CI; APK build passes; merge into `develop`. |
| F2a — One-APK architecture proof | `spike/vcourt-one-apk` from `develop` after F2 | Prove one installable V Court APK containing app and Instrumentation driver on MuMu, with Shizuku startup, hello, safe read/tap and cleanup. | Fresh single-APK install and redacted device evidence pass; merge into `develop`. |
| F3 — Owned Shizuku session | `feat/vcourt-driver-session` from `develop` after F2a | Adapt lab bridge into reusable Android-owned start/stop session with protocol/version check and cancellation. | Fake lifecycle tests and app build pass; no foreign driver stopped; merge into `develop`. |
| F4 — MuMu proof | `test/vcourt-mumu-session` from `develop` after F3 | Run hello, Vinhomes UI read, safe V Court test tap and cleanup from Android app. | Redacted evidence shows success with no host booking commands; merge into `develop`; no booking submitted. |

Record actual base and result commits in [PROGRESS.md](PROGRESS.md). If a part
grows too large, split it into new named branches here before coding.

## First actions after C1

1. Refresh the desktop `develop` reference as described in
   [DESKTOP_REFERENCE.md](DESKTOP_REFERENCE.md). Read its `driver.py` and
   `tests/test_driver.py` before F2; the lab's `ShellBridge` is only a transport
   reference, available from its archived Git commit rather than a folder.
2. Define the Android `DriverClient` interface and fake socket transport.
   Preserve IDs, matching replies, and broken-session behavior after timeout,
   EOF or invalid JSON. Keep app state and booking logic out of this slice.
3. Build both development APKs and run fake-transport tests. Before F3, prove
   the one-APK layout on MuMu and record its package/Instrumentation contract.
   MuMu was disconnected during F1; reconnect it for F2a and F4.

F2 can progress with fakes; F2a and F4 need MuMu and Shizuku online. Decide
the update-signing approach before release work.
