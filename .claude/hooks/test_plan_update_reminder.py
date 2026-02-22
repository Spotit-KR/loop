#!/usr/bin/env python3
import json
import os
import subprocess
import sys

import pytest

sys.path.insert(0, os.path.dirname(__file__))
import plan_update_reminder as sut

SCRIPT_PATH = os.path.join(os.path.dirname(__file__), "plan_update_reminder.py")


@pytest.fixture()
def plan_base(tmp_path, monkeypatch):
    """격리된 임시 PLAN_BASE 디렉토리."""
    base = str(tmp_path / "docs" / "plan")
    monkeypatch.setattr(sut, "PLAN_BASE", base)
    return base


def _make_plan_dir(plan_base, name, unchecked=True):
    """plan 디렉토리와 plan.md 생성. unchecked=True이면 미완료 항목 포함."""
    d = os.path.join(plan_base, name)
    os.makedirs(d, exist_ok=True)
    content = "- [ ] 1단계\n- [ ] 2단계" if unchecked else "- [x] 1단계\n- [x] 2단계"
    with open(os.path.join(d, "plan.md"), "w") as f:
        f.write(content)
    return d


# ── find_active_plan_dirs ──


class TestFindActivePlanDirs:
    def test_empty_when_no_plan_base(self, monkeypatch):
        monkeypatch.setattr(sut, "PLAN_BASE", "")
        assert sut.find_active_plan_dirs() == []

    def test_empty_when_dir_not_exists(self, monkeypatch, tmp_path):
        monkeypatch.setattr(sut, "PLAN_BASE", str(tmp_path / "nonexistent"))
        assert sut.find_active_plan_dirs() == []

    def test_finds_dir_with_unchecked_items(self, plan_base):
        _make_plan_dir(plan_base, "feature-a", unchecked=True)
        result = sut.find_active_plan_dirs()
        assert len(result) == 1
        assert "feature-a" in result[0]

    def test_ignores_fully_checked_plan(self, plan_base):
        _make_plan_dir(plan_base, "feature-b", unchecked=False)
        assert sut.find_active_plan_dirs() == []

    def test_mixed_plans(self, plan_base):
        _make_plan_dir(plan_base, "active", unchecked=True)
        _make_plan_dir(plan_base, "done", unchecked=False)
        result = sut.find_active_plan_dirs()
        assert len(result) == 1
        assert "active" in result[0]


# ── handle_task_create ──


class TestHandleTaskCreate:
    def test_reminds_when_no_active_plan(self, plan_base):
        msg = sut.handle_task_create({"subject": "새 기능 구현"})
        assert msg is not None
        assert "새 기능 구현" in msg
        assert "plan.md" in msg

    def test_no_reminder_when_active_plan_exists(self, plan_base):
        _make_plan_dir(plan_base, "existing", unchecked=True)
        msg = sut.handle_task_create({"subject": "추가 작업"})
        assert msg is None


# ── handle_task_update ──


class TestHandleTaskUpdate:
    def test_reminds_on_completed_with_active_plan(self, plan_base):
        _make_plan_dir(plan_base, "feature-x", unchecked=True)
        msg = sut.handle_task_update({"taskId": "1", "status": "completed"})
        assert msg is not None
        assert "plan.md" in msg
        assert "[x]" in msg

    def test_no_reminder_on_non_completed_status(self, plan_base):
        _make_plan_dir(plan_base, "feature-x", unchecked=True)
        msg = sut.handle_task_update({"taskId": "1", "status": "in_progress"})
        assert msg is None

    def test_no_reminder_when_no_active_plan(self, plan_base):
        msg = sut.handle_task_update({"taskId": "1", "status": "completed"})
        assert msg is None

    def test_no_reminder_when_all_plans_done(self, plan_base):
        _make_plan_dir(plan_base, "done", unchecked=False)
        msg = sut.handle_task_update({"taskId": "1", "status": "completed"})
        assert msg is None


# ── main (subprocess 통합 테스트) ──


def run_hook(input_data, env_override=None):
    env = os.environ.copy()
    if env_override:
        env.update(env_override)
    result = subprocess.run(
        [sys.executable, SCRIPT_PATH],
        input=json.dumps(input_data),
        capture_output=True,
        text=True,
        env=env,
    )
    return result


class TestMain:
    def test_task_create_blocks_with_stderr(self, tmp_path):
        env = {"CLAUDE_PROJECT_DIR": str(tmp_path)}
        data = {
            "tool_name": "TaskCreate",
            "tool_input": {"subject": "인증 구현"},
        }
        result = run_hook(data, env)
        assert result.returncode == 2
        assert "인증 구현" in result.stderr
        assert "plan.md" in result.stderr

    def test_task_update_completed_with_active_plan(self, tmp_path):
        plan_dir = tmp_path / "docs" / "plan" / "feature"
        plan_dir.mkdir(parents=True)
        (plan_dir / "plan.md").write_text("- [ ] 1단계\n- [x] 2단계")

        data = {
            "tool_name": "TaskUpdate",
            "tool_input": {"taskId": "1", "status": "completed"},
        }
        result = run_hook(data, {"CLAUDE_PROJECT_DIR": str(tmp_path)})
        assert result.returncode == 2
        assert "plan.md" in result.stderr

    def test_task_update_in_progress_no_output(self, tmp_path):
        plan_dir = tmp_path / "docs" / "plan" / "feature"
        plan_dir.mkdir(parents=True)
        (plan_dir / "plan.md").write_text("- [ ] 1단계")

        data = {
            "tool_name": "TaskUpdate",
            "tool_input": {"taskId": "1", "status": "in_progress"},
        }
        result = run_hook(data, {"CLAUDE_PROJECT_DIR": str(tmp_path)})
        assert result.returncode == 0
        assert result.stdout.strip() == ""

    def test_invalid_json_passes(self):
        result = subprocess.run(
            [sys.executable, SCRIPT_PATH],
            input="not json",
            capture_output=True,
            text=True,
        )
        assert result.returncode == 0

    def test_unknown_tool_no_output(self, tmp_path):
        data = {
            "tool_name": "Edit",
            "tool_input": {"file_path": "/some/file.kt"},
        }
        result = run_hook(data, {"CLAUDE_PROJECT_DIR": str(tmp_path)})
        assert result.returncode == 0
        assert result.stdout.strip() == ""
