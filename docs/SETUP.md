# Android development setup

This repository is the Android source of truth. Current builds require Windows
or Linux with Python 3.9+, JDK 17+, Android SDK platform 35 and Build Tools
35.0.0. The driver build has no Python package or Gradle dependency. The app
build needs four pinned Shizuku AARs. Neither build installs an APK or runs a
booking.

Python here is host-side build tooling. It is not packaged into an APK or
needed to run V Court on Android. The V Court APK holds the app and the driver
(the one-APK layout under proof); the separate driver APK is still built for the
two-APK scaffold until that proof passes on MuMu.

## Local setup on the current Windows machine

The checked installation on 2026-10-29 was Python 3.14.4, JDK 21.0.10,
`D:\Tools\android-sdk\platforms\android-35` and
`D:\Tools\android-sdk\build-tools\35.0.0`. The `python`/`py` WindowsApps
aliases did not start inside this shell, so use the actual executable:

```powershell
$PythonExe = 'C:\Users\HauPham\AppData\Local\Python\pythoncore-3.14-64\python.exe'
& $PythonExe tools/check_env.py
& $PythonExe tools/test_java.py --fetch
& $PythonExe tools/build_driver.py
& $PythonExe tools/build_mobile.py --fetch
```

On another machine, use its real Python executable. Set `ANDROID_HOME` or
`ANDROID_SDK_ROOT`, or pass `--sdk <path>` to either script when the SDK is not
at the default `D:\Tools\android-sdk`. `JAVA_HOME` selects the JDK; otherwise
the scripts search `PATH`. `tools/check_env.py` reports missing pieces without
changing the machine. Build output and a development-only signing key stay in
ignored `android/build/` and `android/mobile/build/`. The app build fetches
Shizuku API 13.1.5 AARs from Maven Central only when missing and checks their
SHA-256 hashes. Later builds can omit `--fetch` and work offline. A fresh
clone creates new keys; update an installed APK only with the same signing
key, or uninstall the older development APK first. Release signing is a
separate decision. Outputs are `android/build/court-driver.apk` and
`android/mobile/build/v-court-dev.apk`.

`tools/test_java.py` compiles the engine (`dev.vcourt.engine`, plain Java) and
its tests in `android/mobile/src/test/java` against Java 11's API and runs them
on the JVM with JUnit 5. It needs only the JDK: no Android SDK or device. It
refuses an engine source that imports an Android class, so the engine stays
testable off the device. `--fetch` downloads the pinned JUnit console launcher
1.11.3 (checked by SHA-256) into ignored `android/mobile/build/deps/` once.

## Device prerequisites for the next gate

MuMu, Vinhomes Resident and Shizuku in non-root shell mode are needed for live
tests; the V Court APK carries its own driver. `adb` is optional for the build and is
not on `PATH` in the checked shell; MuMu provides it under
`C:\Program Files\Netease\MuMuPlayer\nx_main\adb.exe`. Use the intended
MuMu instance explicitly. The archived compatibility lab's
`android/mobile/README.md` documents its setup and observations. Read it
from desktop Git using [DESKTOP_REFERENCE.md](DESKTOP_REFERENCE.md); the lab
worktree folder has been removed. The probe is not V Court's product app.

Do not run a real booking without explicit authorization. A successful build
does not establish an Android-controlled session or booking. The next gate is
an Android app that owns the Shizuku session, performs `hello`, reads a screen,
taps only its own safe target and cleans up. See [NEXT_SESSION.md](NEXT_SESSION.md).

## Driver checks on MuMu (phase A)

Two checks in V Court make phase A's device evidence: the one-APK proof (F2a) and
the Vinhomes read check (F4). Both run the driver inside the V Court APK through
Shizuku and stop it at the end; neither books anything.

### One-APK proof

On MuMu with Shizuku running (adb mode) and nothing else serving `court-driver`
(stop the separate driver APK's instrumentation if it runs):

1. Install only the V Court APK (uninstall an older development build first if
   its signing key differs) and open V Court.
2. Tap **Authorize Shizuku** and allow V Court.
3. Tap **Run one-APK proof**. The report under the test target lists each step as
   `PASS` or `FAIL` and ends with `RESULT:`; the same lines go to logcat with the
   tag `VCourtProof` (`adb logcat -s VCourtProof VCourtShell CourtDriver`).
4. Run it a second time without restarting V Court: the driver must start again
   in the same app process (the report shows the app pid).

The report reads only V Court's own screen, so it holds no personal data; copy it
into [PROGRESS.md](PROGRESS.md). A `FAIL` at the driver's start shows what `am`
printed; record it as the blocker ([NEXT_SESSION.md](NEXT_SESSION.md) names the
fallbacks).

### Vinhomes read check

After the proof passes, with Vinhomes Resident installed and signed in on the
same MuMu instance:

1. Open V Court and tap **Read Vinhomes screen**. V Court starts the driver,
   opens Vinhomes and reads it until the home screen's "Tiện ích" (or
   "Utilities") tile shows, for up to 30 s. Nothing in Vinhomes is tapped.
2. V Court comes back to the front by itself (or by one Back press, which the
   report names), taps its own test target and stops the driver.
3. The report ends with `RESULT:`; logcat tag `VCourtVinhomes`.

The report holds only counts, timings and which home label showed, never text
from the Vinhomes screen, so it can be copied into [PROGRESS.md](PROGRESS.md).
If Vinhomes shows a sign-in or a notice instead of its home screen, close that by
hand and run the check again.

## Cloud sessions

A cloud session (Claude Code on the web) has a clone of this repository and a
JDK, but no `D:\` drive, MuMu or device. Checked on 2026-10-30:

- `tools/test_java.py --fetch` works: JUnit comes from Maven Central, which can
  answer 429 for a while; run it again later, or download the same file from
  Google's mirror `https://maven-central.storage-download.googleapis.com/maven2/`
  into `android/mobile/build/deps/` (the script still checks its SHA-256). The
  engine and its tests can be written and verified there.
- App and driver code can be compiled, not built: `javac` against
  `org.robolectric:android-all-instrumented` 15 (from the same mirror), the
  Shizuku jars and two empty stand-ins for the
  `org.robolectric.internal.bytecode` interfaces `InstrumentedInterface` and
  `ShadowedObject`. That jar holds hidden APIs too, so it catches compile errors,
  not the use of a non-public API.
- The Android SDK cannot be downloaded (`dl.google.com` is refused), so the APK
  builds run in CI only: open the pull request and read its `Android builds` run.
- Pushing branches works once the repository is attached to the session with write
  access; deleting a branch on GitHub is refused, so the user deletes merged
  branches.
- Commit identity and signing: see [AGENTS.md](../AGENTS.md), "Commits and pull
  requests". The container's global Git config signs commits otherwise.

## Continuous integration

`.github/workflows/android-driver.yml` explicitly installs `platform-tools`,
`platforms;android-35` and `build-tools;35.0.0` through setup-android,
checks the toolchain, runs the engine tests (`tools/test_java.py`) and builds
both APKs from source. CI does not upload
or commit APKs, install them on a device or prove the MuMu gate. Run the same
local commands after changes to the driver, app or build scripts.

The first GitHub run failed before compilation because setup-android's default
package list requested the unavailable legacy `tools` package. Keep the
explicit package list rather than falling back to that default.
