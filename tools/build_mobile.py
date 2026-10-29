"""Build the V Court development app with Android SDK tools.

Fetch the four pinned Shizuku AARs first with --fetch. Dependencies, APK and
development signing key stay under ignored android/mobile/build; no Gradle is needed.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path

import build_driver as sdk_tools

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "android" / "mobile" / "src" / "main"
OUTPUT = ROOT / "android" / "mobile" / "build"
VERSION = "13.1.5"
DEPENDENCIES = {
    "aidl": "33fe7191cdd69fcb66d649264f3b0c47acb2f3d6343afc05b98dbbff6f221963",
    "api": "4def9bde498ef8626614c2fc5db9af4749c86f16f6c33e3f5658d35e70bab59b",
    "provider": "b0f18cd9812464ec171c53cac93a819fe411718a3965c311f01eb4de265381b3",
    "shared": "4659642c9339be0a26e9c65bb8648f7ad6d8f4a465f557993ccbc78802381635",
}


def dependencies(fetch: bool) -> list[Path]:
    """Verify and extract the pinned Shizuku libraries."""
    folder = OUTPUT / "deps"
    folder.mkdir(parents=True, exist_ok=True)
    jars = []
    for name, expected in DEPENDENCIES.items():
        archive = folder / f"{name}.aar"
        if not archive.exists() and fetch:
            url = (
                "https://repo.maven.apache.org/maven2/dev/rikka/shizuku/"
                f"{name}/{VERSION}/{name}-{VERSION}.aar"
            )
            with urllib.request.urlopen(url, timeout=30) as response:
                archive.write_bytes(response.read())
        if not archive.exists():
            raise SystemExit(f"Missing {archive}; build again with --fetch")
        if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
            raise SystemExit(f"SHA-256 mismatch: {archive}")
        jar = folder / f"{name}.jar"
        with zipfile.ZipFile(archive) as library:
            jar.write_bytes(library.read("classes.jar"))
        jars.append(jar)
    return jars


def build(sdk: Path, fetch: bool) -> Path:
    """Build and sign the native Android app shell."""
    jars = dependencies(fetch)
    environ = dict(os.environ)
    windows = sys.platform == "win32"
    android = sdk / "platforms" / sdk_tools.PLATFORM / "android.jar"
    work = (OUTPUT / "work").resolve()
    if work.parent != OUTPUT.resolve():
        raise SystemExit("Build directory is outside android/mobile/build")
    if work.exists():
        shutil.rmtree(work)
    (work / "classes").mkdir(parents=True)
    (work / "dex").mkdir()

    def tool(name: str) -> Path:
        return sdk_tools.sdk_tool(sdk, name, windows)

    sdk_tools.run([
        tool("aapt2"), "link", "-I", android, "--manifest", SOURCE / "AndroidManifest.xml",
        "--min-sdk-version", "33", "--target-sdk-version", "35",
        "--version-code", "1", "--version-name", "0.1.0-dev", "-o", work / "unsigned.apk",
    ])  # fmt: skip
    sdk_tools.run([
        sdk_tools.jdk_tool("javac", environ, windows), "-source", "11", "-target", "11",
        "-Xlint:-options", "-classpath", os.pathsep.join(str(p) for p in [android, *jars]),
        "-d", work / "classes", *sorted((SOURCE / "java").rglob("*.java")),
    ])  # fmt: skip
    # A jar avoids Windows' command-line limit and D8's platform-dependent argfile parsing.
    compiled = work / "mobile.jar"
    with zipfile.ZipFile(compiled, "w") as archive:
        for path in sorted((work / "classes").rglob("*.class")):
            archive.write(path, path.relative_to(work / "classes").as_posix())
    sdk_tools.run([
        tool("d8"), "--lib", android, "--min-api", "33", "--output", work / "dex", compiled, *jars,
    ])  # fmt: skip
    with zipfile.ZipFile(work / "unsigned.apk", "a") as apk:
        apk.write(work / "dex" / "classes.dex", "classes.dex")
        apk.write(ROOT / "android" / "mobile" / "SHIZUKU-LICENSE.txt", "assets/SHIZUKU-LICENSE.txt")
    sdk_tools.run([tool("zipalign"), "-p", "-f", "4", work / "unsigned.apk", work / "aligned.apk"])
    keystore = OUTPUT / "debug.keystore"
    if not keystore.exists():
        sdk_tools.run([
            sdk_tools.jdk_tool("keytool", environ, windows), "-genkeypair",
            "-keystore", keystore,
            "-alias", "androiddebugkey", "-storepass", "android", "-keypass", "android",
            "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
            "-dname", "CN=V Court Development,O=Android,C=US",
        ])  # fmt: skip
    apk = OUTPUT / "v-court-dev.apk"
    sdk_tools.run([
        tool("apksigner"), "sign", "--ks", keystore, "--ks-key-alias", "androiddebugkey",
        "--ks-pass", "pass:android", "--key-pass", "pass:android",
        "--v4-signing-enabled", "false", "--out", apk, work / "aligned.apk",
    ])  # fmt: skip
    sdk_tools.run([tool("apksigner"), "verify", apk])
    return apk


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fetch", action="store_true", help="download missing pinned dependencies")
    parser.add_argument("--sdk", type=Path, default=sdk_tools.find_sdk(dict(os.environ)))
    args = parser.parse_args()
    print(f"Built {build(args.sdk, args.fetch).relative_to(ROOT)}")


if __name__ == "__main__":
    main()
