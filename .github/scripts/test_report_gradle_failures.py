"""Unit tests for report_gradle_failures.py (run: python3 -m unittest discover -s .github/scripts)."""

import io
import os
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

import report_gradle_failures as rgf


def run_main(args):
    out = io.StringIO()
    with redirect_stdout(out):
        code = rgf.main(["report_gradle_failures.py", *args])
    return code, out.getvalue().splitlines()


class EscapingTest(unittest.TestCase):
    def test_escape_data_encodes_percent_and_newlines(self):
        self.assertEqual(rgf.escape_data("50%\r\nx"), "50%25%0D%0Ax")

    def test_escape_property_also_encodes_colon_and_comma(self):
        self.assertEqual(rgf.escape_property("a:b,c"), "a%3Ab%2Cc")


class CompileErrorsTest(unittest.TestCase):
    def test_parses_file_line_and_message_relative_to_workspace(self):
        with tempfile.TemporaryDirectory() as ws:
            line = f"e: file://{ws}/app/src/A.kt:12:5 Unresolved reference 'foo'."
            errors = rgf.compile_errors([line], workspace=ws)
        self.assertEqual(errors, [("Kotlin compile error", "Unresolved reference 'foo'.", "app/src/A.kt", "12")])

    def test_duplicate_errors_are_reported_once(self):
        line = "e: file:///w/A.kt:1:1 boom"
        self.assertEqual(len(rgf.compile_errors([line, line], workspace="/w")), 1)

    def test_ignores_warnings(self):
        self.assertEqual(rgf.compile_errors(["w: file:///w/A.kt:1:1 deprecated"], workspace="/w"), [])

    def test_decodes_url_escaped_paths(self):
        errors = rgf.compile_errors(["e: file:///w/My%20Dir/A.kt:3:1 boom"], workspace="/w")
        self.assertEqual(errors, [("Kotlin compile error", "boom", "My Dir/A.kt", "3")])

    def test_parses_ksp_errors(self):
        errors = rgf.compile_errors(["e: [ksp] /w/app/Dao.kt:20: There is no column"], workspace="/w")
        self.assertEqual(errors, [("KSP error", "There is no column", "app/Dao.kt", "20")])

    def test_parses_javac_errors(self):
        errors = rgf.compile_errors(["/w/app/Legacy.java:7: error: cannot find symbol"], workspace="/w")
        self.assertEqual(errors, [("Java compile error", "cannot find symbol", "app/Legacy.java", "7")])

    def test_omits_location_for_paths_outside_workspace(self):
        errors = rgf.compile_errors(["e: file:///elsewhere/Gen.kt:1:1 boom"], workspace="/w")
        self.assertEqual(errors, [("Kotlin compile error", "/elsewhere/Gen.kt:1: boom", None, None)])


class WhatWentWrongTest(unittest.TestCase):
    def test_extracts_section_until_try(self):
        lines = ["* What went wrong:", "Execution failed for task ':app:test'.", "", "* Try:", "> Run with --info"]
        self.assertEqual(rgf.what_went_wrong(lines), ["Execution failed for task ':app:test'."])


class FailedTestsTest(unittest.TestCase):
    def test_reads_failures_and_errors_from_junit_xml(self):
        with tempfile.TemporaryDirectory() as root:
            report = Path(root, "app/build/test-results/testDebugUnitTest/TEST-a.xml")
            report.parent.mkdir(parents=True)
            report.write_text(
                '<testsuite><testcase classname="a.B" name="ok"/>'
                '<testcase classname="a.B" name="fails"><failure message="boom">trace</failure></testcase>'
                '<testcase classname="a.B" name="errs"><error message="npe"/></testcase></testsuite>'
            )
            self.assertEqual(rgf.failed_tests(Path(root)), [("a.B.fails", "boom\ntrace"), ("a.B.errs", "npe")])

    def test_same_failure_in_several_variants_is_reported_once(self):
        with tempfile.TemporaryDirectory() as root:
            for variant in ("testDebugUnitTest", "testReleaseUnitTest"):
                report = Path(root, f"app/build/test-results/{variant}/TEST-a.xml")
                report.parent.mkdir(parents=True)
                report.write_text('<testsuite><testcase classname="a.B" name="fails"><failure message="boom"/></testcase></testsuite>')
            self.assertEqual(rgf.failed_tests(Path(root)), [("a.B.fails", "boom")])

    def test_skips_unparsable_reports(self):
        with tempfile.TemporaryDirectory() as root:
            report = Path(root, "build/test-results/t/bad.xml")
            report.parent.mkdir(parents=True)
            report.write_text("<testsuite>")
            self.assertEqual(rgf.failed_tests(Path(root)), [])


class MainTest(unittest.TestCase):
    def setUp(self):
        # main() searches the current directory for JUnit reports; isolate it.
        self._cwd = os.getcwd()
        self._tmp = tempfile.TemporaryDirectory()
        os.chdir(self._tmp.name)

    def tearDown(self):
        os.chdir(self._cwd)
        self._tmp.cleanup()

    def test_reads_multiple_logs_and_skips_missing_ones(self):
        with tempfile.TemporaryDirectory() as d:
            log = Path(d, "gradle-test.log")
            log.write_text("* What went wrong:\nTask failed.\n* Try:\n")
            code, lines = run_main([str(log), str(Path(d, "missing.log"))])
        self.assertEqual(code, 0)
        self.assertEqual(lines, ["::error title=Gradle%3A what went wrong::Task failed."])

    def test_falls_back_to_log_tail_when_nothing_recognised(self):
        with tempfile.TemporaryDirectory() as d:
            log = Path(d, "gradle.log")
            log.write_text("something odd\n")
            _, lines = run_main([str(log)])
        self.assertEqual(lines, ["::error title=Gradle log tail::something odd"])

    def test_caps_number_of_annotations(self):
        with tempfile.TemporaryDirectory() as d:
            log = Path(d, "gradle.log")
            log.write_text("".join(f"e: file:///w/A.kt:{i}:1 err {i}\n" for i in range(20)))
            _, lines = run_main([str(log)])
        self.assertEqual(len(lines), rgf.MAX_ANNOTATIONS)

    def test_what_went_wrong_is_kept_when_compile_errors_exceed_the_cap(self):
        with tempfile.TemporaryDirectory() as d:
            log = Path(d, "gradle.log")
            errors = "".join(f"e: file:///w/A.kt:{i}:1 err {i}\n" for i in range(20))
            log.write_text(errors + "* What went wrong:\nCompilation failed.\n* Try:\n")
            _, lines = run_main([str(log)])
        self.assertEqual(lines[0], "::error title=Gradle%3A what went wrong::Compilation failed.")
        self.assertEqual(len(lines), rgf.MAX_ANNOTATIONS)

    def test_log_tail_keeps_the_end_of_long_logs(self):
        with tempfile.TemporaryDirectory() as d:
            log = Path(d, "gradle.log")
            log.write_text("x" * (rgf.MAX_MESSAGE_CHARS * 2) + "\nBUILD FAILED\n")
            _, lines = run_main([str(log)])
        self.assertTrue(lines[0].endswith("%0ABUILD FAILED"), lines[0][-40:])

    def test_no_arguments_is_usage_error(self):
        code, _ = run_main([])
        self.assertEqual(code, 2)


if __name__ == "__main__":
    unittest.main()
