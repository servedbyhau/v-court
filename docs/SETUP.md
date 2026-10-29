# Android development setup

This repository is the Android source of truth. Current builds require Windows
or Linux with Python 3.9+, JDK 17+, Android SDK platform 35 and Build Tools
35.0.0. The driver build has no Python package or Gradle dependency. The app
build needs four pinned Shizuku AARs. Neither build installs an APK or runs a
booking.

Python here is host-side build tooling. It is not packaged into an APK or
needed to run V Court on Android. The current two APK outputs are a development
scaffold; [the roadmap](ROADMAP.md) requires a one-APK architecture proof.

## Local setup on the current Windows machine

The checked installation on 2026-10-29 was Python 3.14.4, JDK 21.0.10,
`D:\Tools\android-sdk\platforms\android-35` and
`D:\Tools\android-sdk\build-tools\35.0.0`. The `python`/`py` WindowsApps
aliases did not start inside this shell, so use the actual executable:

```powershell
$PythonExe = 'C:\Users\HauPham\AppData\Local\Python\pythoncore-3.14-64\python.exe'
& $PythonExe tools/check_env.py
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

## Device prerequisites for the next gate

MuMu, Vinhomes Resident, Shizuku in non-root shell mode and the separate
driver APK are needed for live tests. `adb` is optional for the build and is
not on `PATH` in the checked shell; MuMu provides it under
`C:\Program Files\Netease\MuMuPlayer\nx_main\adb.exe`. Use the intended
MuMu instance explicitly. The compatibility lab's
`android/mobile/README.md` documents its setup and observations, but the lab
probe is not V Court's product app.

Do not run a real booking without explicit authorization. A successful build
does not establish an Android-controlled session or booking. The next gate is
an Android app that owns the Shizuku session, performs `hello`, reads a screen,
taps only its own safe target and cleans up. See [NEXT_SESSION.md](NEXT_SESSION.md).

## Continuous integration

`.github/workflows/android-driver.yml` installs the pinned SDK packages,
checks the toolchain and builds both APKs from source. CI does not upload
or commit APKs, install them on a device or prove the MuMu gate. Run the same
local commands after changes to the driver, app or build scripts.
