#!/usr/bin/env python3
"""Surface Gradle build/test failures as GitHub Actions annotations.

Job logs are not always reachable (expired retention, restricted networks),
but annotations are always visible on the run summary and through the
check-runs API. This script turns the most useful parts of a failed Gradle
run into annotations:

* Kotlin compiler errors (``e: file:///path/File.kt:LINE:COL message``)
  become file/line annotations.
* Failed JUnit test cases from ``**/build/test-results/**/*.xml`` under the
  current directory.
* The ``* What went wrong:`` section of the Gradle log.

Usage: report_gradle_failures.py <gradle-log> [<gradle-log>...]
Missing log files are skipped.
"""

from __future__ import annotations

import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

MAX_ANNOTATIONS = 10
MAX_MESSAGE_CHARS = 4000
LOG_TAIL_LINES = 60

KOTLIN_ERROR = re.compile(r"^e: (?:file://)?(?P<path>[^:]+\.kts?):(?P<line>\d+):(?P<col>\d+) (?P<msg>.*)$")


def escape_data(value: str) -> str:
    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def escape_property(value: str) -> str:
    return escape_data(value).replace(":", "%3A").replace(",", "%2C")


def format_annotation(message: str, title: str, file: str | None = None, line: str | None = None) -> str:
    props = [f"title={escape_property(title)}"]
    if file:
        props.append(f"file={escape_property(file)}")
    if line:
        props.append(f"line={line}")
    return f"::error {','.join(props)}::{escape_data(message[:MAX_MESSAGE_CHARS])}"


def relative_to(path: str, workspace: str) -> str:
    try:
        return str(Path(path).resolve().relative_to(Path(workspace).resolve()))
    except ValueError:
        return path


def kotlin_errors(log_lines: list[str], workspace: str) -> list[tuple[str, str, str, str]]:
    found: list[tuple[str, str, str, str]] = []
    for raw in log_lines:
        match = KOTLIN_ERROR.match(raw.strip())
        if not match:
            continue
        error = ("Kotlin compile error", match["msg"], relative_to(match["path"], workspace), match["line"])
        if error not in found:
            found.append(error)
    return found


def what_went_wrong(log_lines: list[str]) -> list[str]:
    sections: list[str] = []
    current: list[str] | None = None
    for line in log_lines:
        if line.startswith("* What went wrong:"):
            current = []
        elif current is not None and line.startswith(("* Try:", "* Exception is:")):
            sections.append("\n".join(current).strip())
            current = None
        elif current is not None:
            current.append(line)
    if current:
        sections.append("\n".join(current).strip())
    return [section for section in sections if section]


def failed_tests(root: Path) -> list[tuple[str, str]]:
    results: list[tuple[str, str]] = []
    for report in sorted(root.glob("**/build/test-results/**/*.xml")):
        try:
            suite = ET.parse(report).getroot()
        except ET.ParseError:
            continue
        for case in suite.iter("testcase"):
            for problem in [*case.findall("failure"), *case.findall("error")]:
                name = f"{case.get('classname', '?')}.{case.get('name', '?')}"
                detail = f"{problem.get('message') or ''}\n{problem.text or ''}".strip()
                results.append((name, detail))
    return results


def read_logs(paths: list[str]) -> list[str]:
    lines: list[str] = []
    for path in map(Path, paths):
        if path.is_file():
            lines.extend(path.read_text(errors="replace").splitlines())
    return lines


def build_annotations(log_lines: list[str], root: Path, workspace: str) -> list[str]:
    annotations = [format_annotation(msg, title, file, line) for title, msg, file, line in kotlin_errors(log_lines, workspace)]
    annotations += [format_annotation(detail, f"Test failed: {name}") for name, detail in failed_tests(root)]
    annotations += [format_annotation(section, "Gradle: what went wrong") for section in what_went_wrong(log_lines)]
    if not annotations and log_lines:
        annotations.append(format_annotation("\n".join(log_lines[-LOG_TAIL_LINES:]), "Gradle log tail"))
    return annotations[:MAX_ANNOTATIONS]


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    workspace = os.environ.get("GITHUB_WORKSPACE", os.getcwd())
    for annotation in build_annotations(read_logs(argv[1:]), Path("."), workspace):
        print(annotation)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
