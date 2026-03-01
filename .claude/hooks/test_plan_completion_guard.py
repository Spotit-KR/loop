#!/usr/bin/env python3
"""plan_completion_guard.py 단위 테스트."""
import json
import os
import subprocess
import sys
import tempfile

SCRIPT = os.path.join(os.path.dirname(__file__), "plan_completion_guard.py")


def run_hook(tool_name, tool_input, plan_base=None):
    """훅 스크립트를 실행하고 (exit_code, stderr)를 반환."""
    data = json.dumps({"tool_name": tool_name, "tool_input": tool_input})
    env = os.environ.copy()
    if plan_base is not None:
        env["CLAUDE_PROJECT_DIR"] = plan_base
    result = subprocess.run(
        [sys.executable, SCRIPT],
        input=data,
        capture_output=True,
        text=True,
        env=env,
    )
    return result.returncode, result.stderr


def create_plan_dir(base, name, complete=True):
    """테스트용 plan 디렉토리 생성."""
    plan_dir = os.path.join(base, "docs", "plan", name)
    os.makedirs(plan_dir, exist_ok=True)
    check = "[x]" if complete else "[ ]"
    with open(os.path.join(plan_dir, "plan.md"), "w") as f:
        f.write(f"# Test\n- {check} step 1\n")
    with open(os.path.join(plan_dir, "context.md"), "w") as f:
        f.write("# Context\n")
    with open(os.path.join(plan_dir, "checklist.md"), "w") as f:
        f.write("# Checklist\n")


def test_non_bash_allowed():
    code, _ = run_hook("Edit", {"file_path": "src/main/Test.kt"})
    assert code == 0, f"Non-Bash should be allowed, got {code}"


def test_non_pr_push_allowed():
    code, _ = run_hook("Bash", {"command": "git status"})
    assert code == 0, f"Non PR/push should be allowed, got {code}"


def test_pr_create_blocked_with_incomplete_plan():
    with tempfile.TemporaryDirectory() as tmp:
        create_plan_dir(tmp, "test-task", complete=False)
        code, stderr = run_hook(
            "Bash", {"command": 'gh pr create --title "test"'}, plan_base=tmp
        )
        assert code == 2, f"Should block, got {code}"
        assert "plan-guard" in stderr


def test_git_push_blocked_with_incomplete_plan():
    with tempfile.TemporaryDirectory() as tmp:
        create_plan_dir(tmp, "test-task", complete=False)
        code, stderr = run_hook(
            "Bash", {"command": "git push -u origin main"}, plan_base=tmp
        )
        assert code == 2, f"Should block, got {code}"
        assert "plan-guard" in stderr


def test_pr_create_allowed_with_complete_plan():
    with tempfile.TemporaryDirectory() as tmp:
        create_plan_dir(tmp, "test-task", complete=True)
        code, _ = run_hook(
            "Bash", {"command": 'gh pr create --title "test"'}, plan_base=tmp
        )
        assert code == 0, f"Should allow, got {code}"


def test_pr_create_allowed_with_no_plans():
    with tempfile.TemporaryDirectory() as tmp:
        os.makedirs(os.path.join(tmp, "docs", "plan"), exist_ok=True)
        code, _ = run_hook(
            "Bash", {"command": 'gh pr create --title "test"'}, plan_base=tmp
        )
        assert code == 0, f"Should allow with no plans, got {code}"


def test_mixed_plans_blocked():
    with tempfile.TemporaryDirectory() as tmp:
        create_plan_dir(tmp, "done-task", complete=True)
        create_plan_dir(tmp, "wip-task", complete=False)
        code, stderr = run_hook(
            "Bash", {"command": 'gh pr create --title "test"'}, plan_base=tmp
        )
        assert code == 2, f"Should block with any incomplete, got {code}"
        assert "wip-task" in stderr


if __name__ == "__main__":
    tests = [
        test_non_bash_allowed,
        test_non_pr_push_allowed,
        test_pr_create_blocked_with_incomplete_plan,
        test_git_push_blocked_with_incomplete_plan,
        test_pr_create_allowed_with_complete_plan,
        test_pr_create_allowed_with_no_plans,
        test_mixed_plans_blocked,
    ]
    failed = 0
    for test in tests:
        try:
            test()
            print(f"  PASS: {test.__name__}")
        except AssertionError as e:
            print(f"  FAIL: {test.__name__}: {e}")
            failed += 1
    print(f"\n{len(tests) - failed}/{len(tests)} passed")
    sys.exit(1 if failed else 0)
