# V Court next-session plan

Updated for the Android development foundation on 2026-10-29. Read
[context](CONTEXT.md), [progress](PROGRESS.md) and
[workflow](WORKFLOW.md) first. Check Git status and remotes before editing.
This plan authorizes no real booking or public push.

## Outcome

Make this repo a reproducible starting point for Android development and AI
sessions. Keep the lab's proven Java/SDK-tools stack, import only the driver
core, and identify the latest desktop `develop` as the behavioral reference.
Then build an Android-owned driver session and verify it on MuMu.

## Small parts and branches

| Part | Branch and base | Deliverable | Exit check |
|---|---|---|---|
| F0 — Documentation foundation | `docs/vcourt-foundation` from `main` (`729defd`) | Completed locally at `4fe5014`; no push. | Reviewed docs only; links and staged diff checks passed. |
| F1 — Android development foundation | `chore/vcourt-android-foundation` from `docs/vcourt-foundation` (`a46633b`) | Verify latest desktop `develop`; import the reviewed driver core; add a minimal native Java app shell with Shizuku permission/status; make both SDK-tools builds reproducible; add environment checks, CI and concise AI/source context. | Both APKs build locally and in CI definition; environment check reports installed tools; no desktop runtime, secrets or generated APK committed. |
| F2 — Safe protocol client | `feat/vcourt-driver-client` from accepted F1 | Implement Android command IDs, reply matching, timeout/EOF/invalid-reply handling and broken-session state. | Fake-transport tests and APK build pass. |
| F3 — Owned Shizuku session | `feat/vcourt-driver-session` from accepted F2 | Adapt lab bridge into reusable Android-owned start/stop session with protocol/version check and cancellation. | Fake lifecycle tests and app build pass; no foreign driver stopped. |
| F4 — MuMu proof | `test/vcourt-mumu-session` from accepted F3 | Run hello, Vinhomes UI read, safe V Court test tap and cleanup from Android app. | Redacted evidence shows success with no host booking commands; no booking submitted. |

Record actual base and result commits in [PROGRESS.md](PROGRESS.md). If a part
grows too large, split it into new named branches here before coding.

## First actions after foundation

1. Verify the desktop source at `D:\VH-Booker\vh-tennis-booker`: inspect Git
   `develop`, the current architecture/flow/tests and the lab's stack document.
2. Build F1 with a workspace-local debug key and no committed binary. Check the
   installed JDK, Python and Android SDK before building.
3. Record a compact source map and session start instructions so future AI work
   checks the desktop branch again instead of trusting a frozen snapshot.

MuMu was disconnected at the last lab check. F1–F3 can progress with builds
and fakes; F4 needs MuMu and Shizuku online. Choose the product application
ID and update-signing approach before settings or releases accumulate.
