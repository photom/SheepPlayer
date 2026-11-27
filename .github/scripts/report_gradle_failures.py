#!/usr/bin/env python3
"""Surface Gradle build/test failures as GitHub Actions annotations.

Job logs are not always reachable (expired retention, restricted networks),
but annotations are always visible on the run summary and through the
check-runs API. This script turns the most useful parts of a failed Gradle
run into annotations:

* Kotlin compiler errors (``e: file:///path/File.kt:LINE:COL message``)
  become file/line annotations.
* Failed JUnit test cases from ``**/build/test-results/**/*.xml``.
* The ``* What went wrong:`` section of the Gradle log.

Usage: report_gradle_failures.py <gradle-log> [<search-root>]
"""

from __future__ import annotations

import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

MAX_ANNOTATIONS = 10
MAX_MESSAGE_CHARS = 4000

KOTLIN_ERROR = re.compile(r"^e: (?:file://)?(?P<path>[^:]+\.kts?):(?P<line>\d+):(?P<col>\d+) (?P<msg>.*)$")


def escape_data(value: str) -> str:
    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def escape_property(value: str) -> str:
    return escape_data(value).replace(":", "%3A").replace(",", "%2C")


def annotate(message: str, title: str, file: str | None = None, line: str | None = None) -> None:
    props = [f"title={escape_property(title)}"]
    if file:
        props.append(f"file={escape_property(file)}")
    if line:
        props.append(f"line={line}")
    print(f"::error {','.join(props)}::{escape_data(message[:MAX_MESSAGE_CHARS])}")


def relative(path: str) -> str:
    workspace = os.environ.get("GITHUB_WORKSPACE", os.getcwd())
    try:
        return str(Path(path).resolve().relative_to(Path(workspace).resolve()))
    except ValueError:
        return path


def kotlin_errors(log_lines: list[str]) -> list[tuple[str, str, str, str]]:
    found = []
    for raw in log_lines:
        match = KOTLIN_ERROR.match(raw.strip())
        if match:
            found.append(("Kotlin compile error", match["msg"], relative(match["path"]), match["line"]))
    return found


def what_went_wrong(log_lines: list[str]) -> list[str]:
    sections, current = [], None
    for raw in log_lines:
        line = raw.rstrip("\n")
        if line.startswith("* What went wrong:"):
            current = []
            continue
        if current is not None:
            if line.startswith("* Try:") or line.startswith("* Exception is:"):
                sections.append("\n".join(current).strip())
                current = None
            else:
                current.append(line)
    if current:
        sections.append("\n".join(current).strip())
    return [s for s in sections if s]


def failed_tests(root: Path) -> list[tuple[str, str]]:
    results = []
    for report in sorted(root.glob("**/build/test-results/**/*.xml")):
        try:
            tree = ET.parse(report)
        except ET.ParseError:
            continue
        for case in tree.getroot().iter("testcase"):
            for problem in list(case.findall("failure")) + list(case.findall("error")):
                name = f"{case.get('classname', '?')}.{case.get('name', '?')}"
                detail = (problem.get("message") or "") + "\n" + (problem.text or "")
                results.append((name, detail.strip()))
    return results


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    log_path = Path(argv[1])
    root = Path(argv[2]) if len(argv) > 2 else Path(".")
    log_lines = log_path.read_text(errors="replace").splitlines() if log_path.exists() else []

    emitted = 0
    for title, msg, file, line in kotlin_errors(log_lines):
        if emitted >= MAX_ANNOTATIONS:
            break
        annotate(msg, title, file, line)
        emitted += 1

    for name, detail in failed_tests(root):
        if emitted >= MAX_ANNOTATIONS:
            break
        annotate(detail, f"Test failed: {name}")
        emitted += 1

    for section in what_went_wrong(log_lines):
        if emitted >= MAX_ANNOTATIONS:
            break
        annotate(section, "Gradle: what went wrong")
        emitted += 1

    if emitted == 0 and log_lines:
        annotate("\n".join(log_lines[-60:]), "Gradle log tail")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
