#!/usr/bin/env python3
"""Verify that every class in a JAR targets at most a given Java release.

A JAR built with -source/-target 8 on a newer JDK still *claims* Java 8 while
possibly linking against newer APIs, so this checks the shipped artifact rather
than trusting the build flags.

Usage:
    scripts/verify-bytecode.py <java-release> <jar> [<jar> ...]

Example:
    scripts/verify-bytecode.py 8 bungeecord/target/LiteBansDiscordBridge-bungeecord-2.0.0.jar
"""

import sys
import zipfile

# Class file major version 52 is Java 8, 53 is Java 9, and so on.
MAJOR_VERSION_OFFSET = 44


def java_release(major_version):
    return major_version - MAJOR_VERSION_OFFSET


def check(jar_path, max_release):
    """Return a dict of {java release: [class names]} for classes that are too new."""
    offenders = {}
    with zipfile.ZipFile(jar_path) as jar:
        for entry in jar.namelist():
            if not entry.endswith(".class"):
                continue
            # Multi-release JARs deliberately ship newer copies here; the JVM only
            # loads them when it is new enough, so they are not a compatibility risk.
            if entry.startswith("META-INF/versions/"):
                continue
            with jar.open(entry) as class_file:
                header = class_file.read(8)
            if len(header) < 8 or header[:4] != b"\xca\xfe\xba\xbe":
                continue
            release = java_release(int.from_bytes(header[6:8], "big"))
            if release > max_release:
                offenders.setdefault(release, []).append(entry)
    return offenders


def main(argv):
    if len(argv) < 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2

    max_release = int(argv[1])
    failed = False

    for jar_path in argv[2:]:
        offenders = check(jar_path, max_release)
        if offenders:
            failed = True
            print("FAIL  {}: classes newer than Java {}".format(jar_path, max_release))
            for release in sorted(offenders):
                classes = offenders[release]
                print("        Java {}: {} class(es), e.g. {}".format(
                    release, len(classes), ", ".join(sorted(classes)[:3])))
        else:
            print("OK    {}: all classes target Java {} or older".format(jar_path, max_release))

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
