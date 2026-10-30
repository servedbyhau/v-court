# V Court Android technology stack

Decisions recorded 2026-10-29 from the completed compatibility lab assessment.
These adopted choices are preserved here. The lab implementation and build
scripts are available at the pinned Git commit in
[DESKTOP_REFERENCE.md](DESKTOP_REFERENCE.md). The
**implemented** part in this repository is currently the driver and a minimal
native app shell with reproducible builds; the driver session and booking engine
are next slices.

| Layer | Chosen technology | Current V Court state |
|---|---|---|
| App language and UI | Native Android Java, Android Views for the initial product screens | Minimal `dev.vcourt.mobile` app shell shows Shizuku readiness. The lab's `MainActivity` was not imported. |
| Privileged execution | Shizuku API 13.1.5, UserService in shell mode (UID 2000) | `ShellService` (F2a) is a hand-written binder user service that only runs `am instrument -w --no-restart` for V Court's driver and reports its state; the reusable product session is F3. Shizuku is separately installed. |
| UI driver | Java Android Instrumentation using `UiAutomation` | The driver source in `android/app` is compiled into the V Court APK, declared as an Instrumentation targeting `dev.vcourt.mobile` and loaded into the running app with `--no-restart` (F2a, awaiting its MuMu proof); `tools/build_driver.py` still builds it as a separate APK. It is not an `AccessibilityService`. |
| Driver protocol | Newline-delimited JSON protocol 1 over local abstract socket `court-driver` | Implemented by the driver. The client (`dev.vcourt.engine.driver`, F2) numbers commands, matches replies and treats a timeout, end of stream, failed connection, unreadable or mismatched reply as a lost connection; a refusal leaves it usable. `DriverSession` (F3) owns one driver: it refuses a socket it did not start, starts the driver through a `DriverLauncher` (Shizuku's `ShellService` on Android), greets it every 0.1 s for up to 10 s (protocol 1 and this APK's version), stops it, ends `am` when it does not stop within 5 s, and can be cancelled from another thread. |
| Engine tests | JUnit 5 (console launcher 1.11.3, pinned by SHA-256) on the JVM through `tools/test_java.py` | The engine under `dev.vcourt.engine` is plain Java 11 without Android imports, so booking logic and the protocol client are tested without a device; the script refuses an Android import there. |
| Active-run lifecycle | Android foreground service, notification, partial wake lock and monotonic clock | Proven for a user-started 90-second lab probe; daily scheduling and product lifecycle remain to be designed. |
| Android build | Python 3.9+ scripts calling SDK 35, Build Tools 35.0.0 (`aapt2`, `d8`, `zipalign`, `apksigner`) and JDK 17+; Java source/target 11 | Both APKs build locally. No Gradle project. Shizuku AARs are pinned by SHA-256. |
| Android versions | Driver minimum SDK 30, target SDK 35; app minimum SDK 33, target SDK 35 | Both APK builds set these values. Phone support remains unvalidated. |
| Current device gate | MuMu Player on Android 15 / SDK 35 | Lab evidence exists; V Court has not yet run its own session. |
| External setup helper | GeTo, separately installed in the lab | Not embedded in V Court. Its launch/developer-option behavior needs a product test before unattended booking. |

The desktop's Python booking engine, FastAPI, React, SQLite schema, Windows Task
Scheduler and ADB control are **references only**. V Court will implement its own
Android booking logic, local data and scheduling. The exact scheduler, product
update signing and release build remain open decisions in
[the plan](PLAN.md). See [the desktop map](DESKTOP_REFERENCE.md) before porting.

## Distribution boundary

The product target is **one V Court APK** containing its native app and
Instrumentation driver; F2a builds it, and the separate driver APK stays until
the MuMu proof passes.
`tools/*.py` runs on a development machine or CI to produce compiled Android
code; no Python source, interpreter or process is required in the APK runtime.
Shizuku remains separately installed under this stack. The one-APK layout
requires a MuMu proof before the product session design is fixed; see
[the migration roadmap](ROADMAP.md).
