#!/usr/bin/env python3
"""Fails if a Play artifact (.aab or .apk) is not fit for Google Play.

Google Play forbids apps that install or update themselves outside Play. The
direct-install updater lives in shared source and is compiled out of Play
builds through a constant BuildConfig flag, so this check proves R8 actually
removed it rather than trusting that it did.

Play also rejects updates targeting Android 15+ whose native libraries do not
support 16 KB memory pages. The app ships only small AndroidX libraries today,
but a dependency bump can bring in one built for 4 KB pages, so every native
library must have 16 KB-aligned LOAD segments, and an uncompressed one inside an
APK must start on a 16 KB boundary.

Usage:  scripts/check-play-artifact.py <artifact> [<artifact> ...]
"""
import struct
import sys
import zipfile

PAGE_16_KB = 16384
PT_LOAD = 1

# Byte markers that only exist when the updater survives in the artifact.
FORBIDDEN = {
    b"application/vnd.android.package-archive": "APK install intent",
    b"android.settings.MANAGE_UNKNOWN_APP_SOURCES": "unknown-sources settings link",
    b"canRequestPackageInstalls": "install-permission check",
    b"REQUEST_INSTALL_PACKAGES": "REQUEST_INSTALL_PACKAGES permission",
    "REQUEST_INSTALL_PACKAGES".encode("utf-16-le"): "REQUEST_INSTALL_PACKAGES permission",
}


def elf_load_alignments(data):
    """The p_align of every LOAD segment in an ELF file, or None if it is not ELF."""
    if data[:4] != b"\x7fELF":
        return None
    is_64 = data[4] == 2
    order = "<" if data[5] == 1 else ">"
    if is_64:
        phoff, = struct.unpack_from(order + "Q", data, 0x20)
        phentsize, phnum = struct.unpack_from(order + "HH", data, 0x36)
    else:
        phoff, = struct.unpack_from(order + "I", data, 0x1C)
        phentsize, phnum = struct.unpack_from(order + "HH", data, 0x2A)
    alignments = []
    for index in range(phnum):
        start = phoff + index * phentsize
        p_type, = struct.unpack_from(order + "I", data, start)
        if p_type != PT_LOAD:
            continue
        align_offset = start + (0x30 if is_64 else 0x1C)
        p_align, = struct.unpack_from(order + ("Q" if is_64 else "I"), data, align_offset)
        alignments.append(p_align)
    return alignments


def zip_data_offset(path, info):
    """Where an entry's bytes start in the archive, after its local header."""
    with open(path, "rb") as raw:
        raw.seek(info.header_offset)
        header = raw.read(30)
    name_length, extra_length = struct.unpack_from("<HH", header, 26)
    return info.header_offset + 30 + name_length + extra_length


def page_size_problems(path, archive):
    found = []
    for info in archive.infolist():
        if not info.filename.endswith(".so"):
            continue
        alignments = elf_load_alignments(archive.read(info))
        if alignments is None:
            found.append(f"{path}: {info.filename} is not an ELF library")
        elif any(a < PAGE_16_KB for a in alignments):
            found.append(f"{path}: {info.filename} has LOAD segments aligned below 16 KB "
                         f"({', '.join(hex(a) for a in alignments)}); rebuild or update the library")
        if (path.endswith(".apk") and info.compress_type == zipfile.ZIP_STORED
                and zip_data_offset(path, info) % PAGE_16_KB):
            found.append(f"{path}: uncompressed {info.filename} is not zip-aligned to 16 KB")
    return found


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
        found += page_size_problems(path, archive)
    return found


def main(paths):
    if not paths:
        print(__doc__.strip().splitlines()[-1], file=sys.stderr)
        return 2
    failures = [p for path in paths for p in problems(path)]
    for failure in failures:
        print(f"::error::{failure}")
    if not failures:
        print("Play artifact check passed: no self-update code or install permission, "
              "and native libraries support 16 KB pages.")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
