"""Build the V Court instrumentation driver with pinned Android SDK tools.

Requires JDK 17+, Android SDK platform 35 and build tools 35.0.0. Generated
APKs, source hashes and the development signing key stay in ignored android/build.
The driver is a separate APK; this command does not install or run it.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
SOURCES = REPO / "android" / "app" / "src" / "main"
APK_NAME = "court-driver.apk"
HASH_NAME = "court-driver.json"
BUILD_OUT = REPO / "android" / "build"
DEFAULT_SDK = Path("D:/Tools/android-sdk")
PLATFORM = "android-35"
BUILD_TOOLS = "35.0.0"
MIN_SDK = 30
TARGET_SDK = 35
JAVA_RELEASE = "11"
VERSION_CODE = 1
"""Kept at 1: a driver is replaced by its file's hash, and Android installs an equal version
code over the old one but refuses a lower one."""
KEYSTORE = BUILD_OUT / "debug.keystore"
KEY_ALIAS = "androiddebugkey"
KEY_PASSWORD = "android"
# Build tools that are batch files on Windows and shell scripts elsewhere.
SCRIPT_TOOLS = {"d8", "apksigner"}


def sources_hash(sources: Path = SOURCES) -> str:
    """Return the SHA-256 of every file under ``sources``, with their paths."""
    digest = hashlib.sha256()
    for path in sorted(p for p in sources.rglob("*") if p.is_file()):
        digest.update(path.relative_to(sources).as_posix().encode() + b"\0")
        digest.update(path.read_bytes() + b"\0")
    return digest.hexdigest()


def find_sdk(environ: dict[str, str]) -> Path:
    """Return the Android SDK folder named by the environment, else the default one."""
    for name in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if environ.get(name):
            return Path(environ[name])
    return DEFAULT_SDK


def sdk_tool(sdk: Path, name: str, windows: bool) -> Path:
    """Return the path of build tool ``name`` (aapt2, d8, zipalign, apksigner)."""
    suffix = (".bat" if name in SCRIPT_TOOLS else ".exe") if windows else ""
    return sdk / "build-tools" / BUILD_TOOLS / f"{name}{suffix}"


def jdk_tool(name: str, environ: dict[str, str], windows: bool) -> str:
    """Return the JDK tool ``name`` (javac, keytool) from JAVA_HOME, else from PATH."""
    if environ.get("JAVA_HOME"):
        return str(Path(environ["JAVA_HOME"]) / "bin" / (f"{name}.exe" if windows else name))
    found = shutil.which(name)
    if found is None:
        raise SystemExit(f"{name} not found: install a JDK 17 or later or set JAVA_HOME")
    return found


def run(command: list[object]) -> None:
    subprocess.run([str(part) for part in command], check=True)


def build(sdk: Path, out: Path, environ: dict[str, str], windows: bool) -> Path:
    """Build the signed APK into ``out`` and return its path."""
    android_jar = sdk / "platforms" / PLATFORM / "android.jar"
    if not android_jar.exists():
        raise SystemExit(
            f"{android_jar} not found: install the SDK packages "
            f'"platforms;{PLATFORM}" and "build-tools;{BUILD_TOOLS}"'
        )
    work = BUILD_OUT / "work"
    if work.exists():
        shutil.rmtree(work)
    (work / "classes").mkdir(parents=True)
    (work / "dex").mkdir()

    def tool(name: str) -> Path:
        return sdk_tool(sdk, name, windows)

    run([
        tool("aapt2"), "link", "-I", android_jar, "--manifest", SOURCES / "AndroidManifest.xml",
        "--min-sdk-version", MIN_SDK, "--target-sdk-version", TARGET_SDK,
        "--version-code", VERSION_CODE, "--version-name", sources_hash()[:12],
        "-o", work / "unsigned.apk",
    ])  # fmt: skip
    sources = sorted((SOURCES / "java").rglob("*.java"))
    run([
        jdk_tool("javac", environ, windows), "-source", JAVA_RELEASE, "-target", JAVA_RELEASE,
        "-Xlint:-options", "-classpath", android_jar, "-d", work / "classes", *sources,
    ])  # fmt: skip
    classes = sorted((work / "classes").rglob("*.class"))
    dex = work / "dex"
    run([tool("d8"), "--lib", android_jar, "--min-api", MIN_SDK, "--output", dex, *classes])
    with zipfile.ZipFile(work / "unsigned.apk", "a") as apk:
        apk.write(work / "dex" / "classes.dex", "classes.dex")
    run([tool("zipalign"), "-p", "-f", "4", work / "unsigned.apk", work / "aligned.apk"])
    if not KEYSTORE.exists():
        KEYSTORE.parent.mkdir(parents=True, exist_ok=True)
        run([
            jdk_tool("keytool", environ, windows), "-genkeypair", "-keystore", KEYSTORE,
            "-alias", KEY_ALIAS, "-storepass", KEY_PASSWORD, "-keypass", KEY_PASSWORD,
            "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
            "-dname", "CN=Android Debug,O=Android,C=US",
        ])  # fmt: skip
    out.mkdir(parents=True, exist_ok=True)
    apk = out / APK_NAME
    run([
        tool("apksigner"), "sign", "--ks", KEYSTORE, "--ks-key-alias", KEY_ALIAS,
        "--ks-pass", f"pass:{KEY_PASSWORD}", "--key-pass", f"pass:{KEY_PASSWORD}",
        "--v4-signing-enabled", "false", "--out", apk, work / "aligned.apk",
    ])  # fmt: skip
    run([tool("apksigner"), "verify", apk])
    shutil.rmtree(work)
    return apk


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--sdk", type=Path, help="Android SDK folder (default: see above)")
    args = parser.parse_args()
    environ = dict(os.environ)
    apk = build(args.sdk or find_sdk(environ), BUILD_OUT, environ, windows=sys.platform == "win32")
    record = {"sources_sha256": sources_hash()}
    with open(BUILD_OUT / HASH_NAME, "w", encoding="utf-8", newline="\n") as file:
        file.write(json.dumps(record, indent=2) + "\n")
    print(f"Built {apk.relative_to(REPO)} ({apk.stat().st_size // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
