#!/usr/bin/env python3
"""Fails if a Play artifact (.aab or .apk) carries the self-update code path.

Google Play forbids apps that install or update themselves outside Play. The
direct-install updater lives in shared source and is compiled out of Play
builds through a constant BuildConfig flag, so this check proves R8 actually
removed it rather than trusting that it did.

Usage:  scripts/check-play-artifact.py <artifact> [<artifact> ...]
"""
import sys
import zipfile

# Byte markers that only exist when the updater survives in the artifact.
FORBIDDEN = {
    b"application/vnd.android.package-archive": "APK install intent",
    b"android.settings.MANAGE_UNKNOWN_APP_SOURCES": "unknown-sources settings link",
    b"canRequestPackageInstalls": "install-permission check",
    b"REQUEST_INSTALL_PACKAGES": "REQUEST_INSTALL_PACKAGES permission",
    "REQUEST_INSTALL_PACKAGES".encode("utf-16-le"): "REQUEST_INSTALL_PACKAGES permission",
}


def problems(path):
    found = []
    with zipfile.ZipFile(path) as archive:
        names = [n for n in archive.namelist()
                 if n.endswith(".dex") or n.endswith("AndroidManifest.xml")]
        if not any(n.endswith(".dex") for n in names):
            return [f"{path}: no dex files found; is this an Android artifact?"]
        for name in names:
            data = archive.read(name)
            for marker, meaning in FORBIDDEN.items():
                if marker in data:
                    found.append(f"{path}: {name} contains {meaning}")
    return found


def main(paths):
    if not paths:
        print(__doc__.strip().splitlines()[-1], file=sys.stderr)
        return 2
    failures = [p for path in paths for p in problems(path)]
    for failure in failures:
        print(f"::error::{failure}")
    if not failures:
        print("Play artifact check passed: no self-update code or install permission.")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
