"""Compile and run V Court's engine tests on the JVM, with no Android SDK or device.

The engine (android/mobile/src/main/java/dev/vcourt/engine) is plain Java: it must not
use Android classes, so its tests run on the development machine and in CI, and this
script refuses an engine source that imports one. Fetch the pinned JUnit console launcher
first with --fetch; it is checked by SHA-256 and stays in ignored android/mobile/build.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
import urllib.request
from pathlib import Path

import build_driver

ROOT = Path(__file__).resolve().parents[1]
MOBILE = ROOT / "android" / "mobile"
ENGINE = MOBILE / "src" / "main" / "java" / "dev" / "vcourt" / "engine"
TESTS = MOBILE / "src" / "test" / "java"
OUTPUT = MOBILE / "build" / "test"
JAVA_RELEASE = build_driver.JAVA_RELEASE
JUNIT_VERSION = "1.11.3"
JUNIT_SHA256 = "33440476714985bda2584ed6c70d0d877085012343be67e961dcf80dac596227"
JUNIT_URL = (
    "https://repo.maven.apache.org/maven2/org/junit/platform/"
    f"junit-platform-console-standalone/{JUNIT_VERSION}/"
    f"junit-platform-console-standalone-{JUNIT_VERSION}.jar"
)
_ANDROID_IMPORT = re.compile(r"^\s*import\s+(static\s+)?(android|androidx|dalvik)\..*$", re.M)


def android_imports(sources: list[Path]) -> list[str]:
    """Return ``file: line`` for each engine source line importing an Android class."""
    found = []
    for path in sources:
        for match in _ANDROID_IMPORT.finditer(path.read_text(encoding="utf-8")):
            found.append(f"{path.relative_to(ROOT).as_posix()}: {match.group(0).strip()}")
    return found


def junit(fetch: bool) -> Path:
    """Return the verified JUnit console launcher, downloading it when allowed."""
    jar = MOBILE / "build" / "deps" / f"junit-platform-console-standalone-{JUNIT_VERSION}.jar"
    if not jar.exists() and fetch:
        jar.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(JUNIT_URL, timeout=60) as response:
            jar.write_bytes(response.read())
    if not jar.exists():
        raise SystemExit(f"Missing {jar.relative_to(ROOT)}; run again with --fetch")
    if hashlib.sha256(jar.read_bytes()).hexdigest() != JUNIT_SHA256:
        raise SystemExit(f"SHA-256 mismatch: {jar.relative_to(ROOT)}")
    return jar


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--fetch", action="store_true", help="download the pinned JUnit jar")
    args = parser.parse_args()
    environ = dict(os.environ)
    windows = sys.platform == "win32"

    sources = sorted(ENGINE.rglob("*.java"))
    tests = sorted(TESTS.rglob("*.java"))
    if not sources or not tests:
        raise SystemExit("No engine sources or tests found")
    found = android_imports(sources)
    if found:
        print("The engine must not use Android classes:", *found, sep="\n  ")
        return 1
    launcher = junit(args.fetch)

    work = OUTPUT.resolve()
    if work.parent != (MOBILE / "build").resolve():
        raise SystemExit("Test output is outside android/mobile/build")
    if work.exists():
        shutil.rmtree(work)
    classes, test_classes = work / "classes", work / "test-classes"
    javac = build_driver.jdk_tool("javac", environ, windows)
    # --release checks the engine against Java 11's API, the level the app is built at.
    build_driver.run([
        javac, "--release", JAVA_RELEASE, "-Xlint:all", "-Werror", "-encoding", "UTF-8",
        "-d", classes, *sources,
    ])  # fmt: skip
    build_driver.run([
        javac, "--release", JAVA_RELEASE, "-encoding", "UTF-8",
        "-classpath", os.pathsep.join(str(path) for path in (classes, launcher)),
        "-d", test_classes, *tests,
    ])  # fmt: skip
    java = build_driver.jdk_tool("java", environ, windows)
    finished = subprocess.run([
        java, "-jar", str(launcher), "execute", "--disable-banner",
        "--class-path", os.pathsep.join(str(path) for path in (classes, test_classes)),
        "--scan-class-path", "--fail-if-no-tests",
    ], check=False)  # fmt: skip
    return finished.returncode


if __name__ == "__main__":
    sys.exit(main())
