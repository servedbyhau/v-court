"""Check the local toolchain needed for the V Court Android driver build."""

from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

import build_driver


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, help="Android SDK folder")
    args = parser.parse_args()
    sdk = args.sdk or build_driver.find_sdk(dict(os.environ))
    windows = sys.platform == "win32"
    failed = False

    def report(name: str, value: str, ok: bool) -> None:
        nonlocal failed
        print(f"{'OK' if ok else 'MISSING'} {name}: {value}")
        failed |= not ok

    report("Python", f"{sys.version_info.major}.{sys.version_info.minor} ({sys.executable})",
           sys.version_info >= (3, 9))

    try:
        javac = build_driver.jdk_tool("javac", dict(os.environ), windows)
    except SystemExit:
        javac = "javac"
    if Path(javac).is_file() or shutil.which(javac):
        version = subprocess.run([javac, "-version"], capture_output=True, text=True, check=False)
        value = (version.stdout or version.stderr).strip()
        match = re.search(r"javac (\d+)", value)
        report("JDK", f"{value} ({javac})", version.returncode == 0 and bool(match)
               and int(match.group(1)) >= 17)
    else:
        report("JDK", f"javac missing ({javac})", False)
    try:
        keytool = build_driver.jdk_tool("keytool", dict(os.environ), windows)
    except SystemExit:
        keytool = "keytool"
    report("keytool", keytool, Path(keytool).is_file() or bool(shutil.which(keytool)))

    platform = sdk / "platforms" / build_driver.PLATFORM / "android.jar"
    report("Android SDK platform", str(platform), platform.is_file())
    for name in ("aapt2", "d8", "zipalign", "apksigner"):
        tool = build_driver.sdk_tool(sdk, name, windows)
        report(name, str(tool), tool.is_file())

    adb = shutil.which("adb")
    print(f"OPTIONAL adb: {adb or 'not on PATH; needed only for MuMu install/tests'}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
