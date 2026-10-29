# Desktop-to-Android migration roadmap

Reviewed against `D:\VH-Booker\vh-tennis-booker` local and remote `develop`
at `50ed350` (v0.18.0) on 2026-10-29. This is a dated baseline: before each
implementation slice, compare `develop` with the remote and read the latest
source and tests named in [DESKTOP_REFERENCE.md](DESKTOP_REFERENCE.md). The
Android lab is evidence for Shizuku/UiAutomation compatibility, not the
booking behavior specification.

## Destination and scope

V Court is an Android-owned tennis booker. It must start and run without a PC,
show readiness and run results, and report **booked** only after reading the
target app's ticket. A refusal and an uncertain post-confirmation outcome are
distinct. The release is **one V Court APK**, containing the app and its
Instrumentation driver; Shizuku remains a separately installed prerequisite
under the chosen stack. Python scripts are build tools on the host/CI only,
never an APK runtime dependency.

The current repository builds two development APKs and shows Shizuku permission
status. It has no Android-owned driver session, booking engine or scheduler.
First develop and validate on MuMu; physical-phone evidence is a later gate.

## What to translate

| Desktop behavior/source | Android destination | First proof |
|---|---|---|
| `driver.py`, `tests/test_driver.py`, `android/app/` | Java protocol client, one owned Shizuku session and the Instrumentation driver | Correlated commands, timeouts, ownership, hello and safe MuMu read/tap. |
| `screen.py`, `geometry.py`, `app_language.py`, `catalog.py` and their tests | Pure Java screen model, matching, bilingual labels and tennis court/venue catalog | Synthetic UI trees cover Vietnamese/English, off-screen nodes and coordinate fallback. |
| `flow.py`, `tests/test_flow.py`, `docs/field-notes.md` | Android booking state machine behind a small device interface | Fake-driver rounds cover calendar, venue, moving court list, notices, confirmation and ticket. |
| `runner.py`, `clock.py`, `watchdog.py`, `lock.py` and tests | Android run coordinator, monotonic timing, one-run guard, cancellation and bounded device calls | Manual and timed MuMu runs with failure injection; no duplicate submission. |
| `config.py`, `events.py`, `history.py` and tests | Android settings, stable events and local persistence | Settings survive restart; failures in reporting/history never turn a booking into a false success. |
| `api/`, React UI, `schedule.py`, `power.py`, `device_setup.py`, `emulator.py`, `adb.py` | Product wording and behavior reference only; replace with native UI and Android lifecycle/scheduling | No FastAPI, browser, Windows Task Scheduler, host ADB or Python process in the APK runtime. |

Desktop 0.18.0 also has multi-sport and multi-MuMu behavior. The first mobile
booking gate is **one device, one account, tennis**. Retain a data model that
can grow, but do not copy desktop machine tabs or parallel host processes as
phone requirements without a separate product decision.

## Ordered slices

Each row is one reviewable branch from V Court's current `develop`, after the
preceding slice has merged there. Put the concrete branch/base/exit check into
[NEXT_SESSION.md](NEXT_SESSION.md) before implementation. Keep the two APK
development build working until the one-APK proof replaces it; do not delay
the proof until release.

| Slice | Deliverable | Exit evidence |
|---|---|---|
| 0. Foundation — done locally | Java driver, native app shell, pinned SDK build and AI context | Both development APKs build; no device result claimed. |
| 1. Protocol client | Java command IDs, matching replies, timeout/EOF/invalid-JSON handling and broken-session state | Fake transport tests for all commands and failure paths; app build passes. |
| 2. One-APK architecture proof | Prototype app and Instrumentation in one installable V Court APK; establish package/target, Shizuku launch and signing contract | Fresh install of one V Court APK on MuMu; hello, safe app read/tap and cleanup work without host booking commands. Record a blocker and revise the design if this fails. |
| 3. Owned driver session | Reusable start/stop, permission/version checks, cancellation and cleanup; never take over or stop another owner | Fake lifecycle tests plus MuMu stop/disconnect/restart checks. |
| 4. Pure booking model | UI tree parser, geometry, Vietnamese/English labels, dates, tennis venues/courts, minimal validated settings | Pure tests adapted from current desktop cases; no device or account data required. |
| 5. Manual dry-run engine | Prepare, reach Utilities, select date/slot/venue/court, verify details and consent; stop before final Confirm | Fake rounds for normal, slow, missing/full and moving UI; MuMu dry run reaches the correct confirmation without submitting. |
| 6. Manual real booking | Confirm once under safe retry rules; read ticket, refusal or unknown; return for next slot | Fake tests for all outcomes; **separately authorized** MuMu real booking shows the ticket. A sent tap alone never passes. |
| 7. Timed run coordinator | Opening-time warm-up, monotonic timing, bounded commands, one active run, event/result model | Fake clock and interruption tests; timed MuMu dry run records trigger, first tap and outcome. |
| 8. Background scheduling | Android trigger, foreground execution, wake/notification handling and readiness after interruption/reboot | MuMu scheduled dry runs from background; missing Shizuku/permission, late start and duplicate trigger have explicit results. |
| 9. Mobile product | Native settings/readiness, run state, local history and notifications | Fresh install/restart/update on MuMu; saved choices and truthful history; UI observation does not delay the run. |
| 10. Phone and release | Chosen phone validation, signing/update path, CI, privacy review and one-APK release build | Repeat driver, dry run, authorized real ticket, locked-screen/background/reboot and update gates on a phone; only then claim phone readiness. |

## Rules every slice preserves

- Read each screen once per decision, act on current labels/bounds and wait for
  a verified next state. A swallowed tap can be retried only while its target
  still shows; a slow loading screen is waited out.
- Distinguish **booked with ticket**, **refused**, **unknown after Confirm**,
  **dry run**, and **not ready**. Never infer success from a tap or disappearing
  confirmation page; do not blindly repeat an uncertain submission.
- Keep driver, screen model and booking decisions behind interfaces so pure
  tests need neither MuMu nor network. Device tests use redacted evidence;
  real bookings require explicit authorization.
- Replace desktop-specific scheduling, process locks, ADB forwarding, NTP/
  Windows clock handling, backend and UI with Android equivalents. Validate
  Android background and power behavior rather than assuming desktop timing
  carries over.

## Gate ownership

[PLAN.md](PLAN.md) holds product milestones; this file orders the migration.
[NEXT_SESSION.md](NEXT_SESSION.md) holds the next branch-sized tasks, and
[PROGRESS.md](PROGRESS.md) records actual evidence. A build or fake test is not
an on-device booking proof, and MuMu proof is not physical-phone proof.
