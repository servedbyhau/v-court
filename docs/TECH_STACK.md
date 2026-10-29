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
| Privileged execution | Shizuku API 13.1.5, UserService in shell mode (UID 2000) | App builds with pinned Shizuku AARs and can request permission. Product UserService/session is not yet implemented. Shizuku is separately installed. |
| UI driver | Java Android Instrumentation using `UiAutomation` | `android/app` and `tools/build_driver.py` currently build a separate development APK. It is not an `AccessibilityService`. Release packaging must be proven as one APK. |
| Driver protocol | Newline-delimited JSON protocol 1 over local abstract socket `court-driver` | Implemented by the driver. The Android client and ownership rules are next. |
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

The two APKs built today are a development scaffold. The product target is
**one V Court APK** containing its native app and Instrumentation driver.
`tools/*.py` runs on a development machine or CI to produce compiled Android
code; no Python source, interpreter or process is required in the APK runtime.
Shizuku remains separately installed under this stack. The one-APK layout
requires a MuMu proof before the product session design is fixed; see
[the migration roadmap](ROADMAP.md).
