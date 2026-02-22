#!/usr/bin/env python3
import json
import os
import subprocess
import sys

import pytest

SCRIPT_PATH = os.path.join(os.path.dirname(__file__), "block_dangerous.py")


def run_hook(command):
    data = {
        "tool_name": "Bash",
        "tool_input": {"command": command},
    }
    result = subprocess.run(
        [sys.executable, SCRIPT_PATH],
        input=json.dumps(data),
        capture_output=True,
        text=True,
    )
    return result


# ── 차단 (exit code 2) ──


class TestHardBlock:
    def test_rm_rf_root(self):
        result = run_hook("rm -rf /")
        assert result.returncode == 2
        assert "시스템 루트" in result.stderr

    def test_rm_rf_root_wildcard(self):
        result = run_hook("rm -rf /*")
        assert result.returncode == 2

    def test_rm_rf_home(self):
        result = run_hook("rm -rf ~/")
        assert result.returncode == 2
        assert "홈 디렉토리" in result.stderr

    def test_rm_rf_home_no_slash(self):
        result = run_hook("rm -rf ~")
        assert result.returncode == 2

    def test_rm_rf_star(self):
        result = run_hook("rm -rf *")
        assert result.returncode == 2
        assert "현재 디렉토리" in result.stderr

    def test_rm_rf_root_chained(self):
        result = run_hook("rm -rf / && echo done")
        assert result.returncode == 2


# ── 경고 (exit code 0 + additionalContext) ──


class TestSoftWarn:
    def test_rm_rf_project_path(self):
        result = run_hook("rm -rf /Users/imsubin/project/temp")
        assert result.returncode == 0
        output = json.loads(result.stdout)
        assert "additionalContext" in output["hookSpecificOutput"]
        assert "rm -rf" in output["hookSpecificOutput"]["additionalContext"]

    def test_rm_rf_relative_path(self):
        result = run_hook("rm -rf ./build")
        assert result.returncode == 0
        output = json.loads(result.stdout)
        assert "additionalContext" in output["hookSpecificOutput"]

    def test_rm_rf_parent_dir(self):
        result = run_hook("rm -rf ../sibling")
        assert result.returncode == 0
        output = json.loads(result.stdout)
        assert "상위 디렉토리" in output["hookSpecificOutput"]["additionalContext"]

    def test_git_reset_hard(self):
        result = run_hook("git reset --hard HEAD~1")
        assert result.returncode == 0
        output = json.loads(result.stdout)
        assert "additionalContext" in output["hookSpecificOutput"]

    def test_git_push_force(self):
        result = run_hook("git push --force origin feature")
        assert result.returncode == 0
        output = json.loads(result.stdout)
        assert "force" in output["hookSpecificOutput"]["additionalContext"].lower()

    def test_git_clean_f(self):
        result = run_hook("git clean -fd")
        assert result.returncode == 0
        assert result.stdout.strip() != ""

    def test_git_checkout_dot(self):
        result = run_hook("git checkout .")
        assert result.returncode == 0
        assert result.stdout.strip() != ""

    def test_git_stash_drop(self):
        result = run_hook("git stash drop")
        assert result.returncode == 0
        assert result.stdout.strip() != ""

    def test_drop_table(self):
        result = run_hook("psql -c 'DROP TABLE users'")
        assert result.returncode == 0
        assert result.stdout.strip() != ""


# ── 허용 (exit code 0, 출력 없음) ──


class TestAllowed:
    def test_ls(self):
        result = run_hook("ls -la")
        assert result.returncode == 0
        assert result.stdout.strip() == ""

    def test_git_status(self):
        result = run_hook("git status")
        assert result.returncode == 0
        assert result.stdout.strip() == ""

    def test_git_push_no_force(self):
        result = run_hook("git push origin main")
        assert result.returncode == 0
        assert result.stdout.strip() == ""

    def test_rm_single_file(self):
        result = run_hook("rm file.txt")
        assert result.returncode == 0
        assert result.stdout.strip() == ""

    def test_gradle_build(self):
        result = run_hook("./gradlew build")
        assert result.returncode == 0
        assert result.stdout.strip() == ""

    def test_non_bash_tool(self):
        data = {
            "tool_name": "Edit",
            "tool_input": {"file_path": "/some/file"},
        }
        result = subprocess.run(
            [sys.executable, SCRIPT_PATH],
            input=json.dumps(data),
            capture_output=True,
            text=True,
        )
        assert result.returncode == 0
        assert result.stdout.strip() == ""

    def test_invalid_json(self):
        result = subprocess.run(
            [sys.executable, SCRIPT_PATH],
            input="not json",
            capture_output=True,
            text=True,
        )
        assert result.returncode == 0
