# V Court next-session plan

Updated after F1 commit `d76bba9` on 2026-10-29. Read
[context](CONTEXT.md), [progress](PROGRESS.md) and
[workflow](WORKFLOW.md) first. Check Git status and remotes before editing.
This plan authorizes no real booking or public push.

## Outcome

Build an Android-owned driver session and verify it on MuMu, using the
reproducible Java/SDK-tools foundation now in this repo. Recheck the latest
desktop `develop` before each behavior or protocol change.

## Small parts and branches

| Part | Branch and base | Deliverable | Exit check |
|---|---|---|---|
| F0 — Documentation foundation | `docs/vcourt-foundation` from `main` (`729defd`) | Completed locally at `4fe5014`; no push. | Reviewed docs only; links and staged diff checks passed. |
| F1 — Android development foundation | `chore/vcourt-android-foundation` from `docs/vcourt-foundation` (`a46633b`) | Completed locally at `d76bba9`; no push. | Both APKs built locally; CI defined but not run; no device attached. |
| F2 — Safe protocol client | `feat/vcourt-driver-client` from accepted F1 | Implement Android command IDs, reply matching, timeout/EOF/invalid-reply handling and broken-session state. | Fake-transport tests and APK build pass. |
| F3 — Owned Shizuku session | `feat/vcourt-driver-session` from accepted F2 | Adapt lab bridge into reusable Android-owned start/stop session with protocol/version check and cancellation. | Fake lifecycle tests and app build pass; no foreign driver stopped. |
| F4 — MuMu proof | `test/vcourt-mumu-session` from accepted F3 | Run hello, Vinhomes UI read, safe V Court test tap and cleanup from Android app. | Redacted evidence shows success with no host booking commands; no booking submitted. |

Record actual base and result commits in [PROGRESS.md](PROGRESS.md). If a part
grows too large, split it into new named branches here before coding.

## First actions after foundation

1. Refresh the desktop `develop` reference as described in
   [DESKTOP_REFERENCE.md](DESKTOP_REFERENCE.md). Read its `driver.py` and
   `tests/test_driver.py` before F2; the lab's `ShellBridge` is only a transport
   reference.
2. Define the Android `DriverClient` interface and fake socket transport.
   Preserve IDs, matching replies, and broken-session behavior after timeout,
   EOF or invalid JSON. Keep app state and booking logic out of this slice.
3. Build both APKs and run fake-transport tests. For F3, adapt Shizuku ownership
   and lifecycle without taking over a foreign driver. MuMu was disconnected
   during F1; reconnect it for F4.

MuMu was disconnected at the last lab check. F1–F3 can progress with builds
and fakes; F4 needs MuMu and Shizuku online. Choose the product application
ID and update-signing approach before settings or releases accumulate.
