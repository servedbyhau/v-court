# V Court next-session plan

Updated after local foundation commit `4fe5014` on 2026-10-25. Read
[context](CONTEXT.md), [progress](PROGRESS.md) and
[workflow](WORKFLOW.md) first. Check Git status and remotes before editing.
This plan authorizes no real booking or public push.

## Outcome

Build the first Android-owned driver path in this repo and verify it on MuMu.
Use the sibling lab's Java driver and the desktop client's tested protocol
behavior as references. Import only needed mobile files after review.

## Small parts and branches

| Part | Branch and base | Deliverable | Exit check |
|---|---|---|---|
| F0 — Documentation foundation | `docs/vcourt-foundation` from `main` (`729defd`) | Completed locally at `4fe5014`; no push. | Reviewed docs only; links and staged diff checks passed. |
| F1 — Driver core and build | `feat/vcourt-driver-core` from the accepted `docs/vcourt-foundation` HEAD | Import Java instrumentation driver, manifest, needed license/build files and reproducible APK build. | Clean checkout builds driver; no desktop backend or private artifact included. |
| F2 — Safe protocol client | `feat/vcourt-driver-client` from accepted F1 | Implement Android command IDs, reply matching, timeout/EOF/invalid-reply handling and broken-session state. | Fake-transport tests and APK build pass. |
| F3 — Owned Shizuku session | `feat/vcourt-driver-session` from accepted F2 | Adapt lab bridge into reusable Android-owned start/stop session with protocol/version check and cancellation. | Fake lifecycle tests and app build pass; no foreign driver stopped. |
| F4 — MuMu proof | `test/vcourt-mumu-session` from accepted F3 | Run hello, Vinhomes UI read, safe V Court test tap and cleanup from Android app. | Redacted evidence shows success with no host booking commands; no booking submitted. |

Record actual base and result commits in [PROGRESS.md](PROGRESS.md). If a part
grows too large, split it into new named branches here before coding.

## First actions after foundation

1. Verify F0's branch and commit `4fe5014`; check for user changes before F1.
2. Review driver source, manifest, build script and licenses in the sibling
   lab. Compare protocol expectations with desktop `DriverClient` and tests.
3. Create F1's branch from accepted F0 and import driver core only. Add the
   smallest repeatable build check before app/session work.

MuMu was disconnected at the last lab check. F1–F3 can progress with builds
and fakes; F4 needs MuMu and Shizuku online. Choose the product application
ID and update-signing approach before settings or releases accumulate.
