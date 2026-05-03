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


def create_plan_dir(base, name, complete=True, extra_files=None):
    """테스트용 plan 디렉토리 생성. 정책상 plan.md + checklist.md 2종이 필수."""
    plan_dir = os.path.join(base, "docs", "plan", name)
    os.makedirs(plan_dir, exist_ok=True)
    check = "[x]" if complete else "[ ]"
    with open(os.path.join(plan_dir, "plan.md"), "w") as f:
        f.write(f"# Test\n- {check} step 1\n")
    with open(os.path.join(plan_dir, "checklist.md"), "w") as f:
        f.write("# Checklist\n")
    for extra in extra_files or []:
        with open(os.path.join(plan_dir, extra), "w") as f:
            f.write(f"# {extra}\n")


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


def test_two_doc_incomplete_plan_blocked():
    """정책 정합성 회귀: plan.md + checklist.md 2종만 있는 미완료 plan은 차단되어야 함.

    이전 가드는 context.md까지 3종을 요구해, 정책에 맞춰 2종으로 작성된 미완료 plan이
    거짓통과(exit=0) 되는 결함이 있었다. (#44)
    """
    with tempfile.TemporaryDirectory() as tmp:
        create_plan_dir(tmp, "two-doc-wip", complete=False)
        code, stderr = run_hook(
            "Bash", {"command": 'gh pr create --title "test"'}, plan_base=tmp
        )
        assert code == 2, f"Should block 2-doc incomplete plan, got {code}"
        assert "two-doc-wip" in stderr


def test_two_doc_with_extra_files_still_blocked():
    """가드는 필수 2종만 보고, 추가 파일이 있어도 미완료면 차단되어야 함."""
    with tempfile.TemporaryDirectory() as tmp:
        create_plan_dir(
            tmp, "extras-wip", complete=False, extra_files=["context.md", "notes.md"]
        )
        code, _ = run_hook(
            "Bash", {"command": "git push origin main"}, plan_base=tmp
        )
        assert code == 2, f"Should block regardless of extra files, got {code}"


def test_plan_without_checklist_skipped():
    """필수 2종이 모두 갖춰지지 않은 디렉토리는 가드 대상이 아님."""
    with tempfile.TemporaryDirectory() as tmp:
        plan_dir = os.path.join(tmp, "docs", "plan", "no-checklist")
        os.makedirs(plan_dir, exist_ok=True)
        with open(os.path.join(plan_dir, "plan.md"), "w") as f:
            f.write("# Test\n- [ ] step 1\n")
        code, _ = run_hook(
            "Bash", {"command": 'gh pr create --title "test"'}, plan_base=tmp
        )
        assert code == 0, f"Incomplete-but-not-required dir should be skipped, got {code}"


if __name__ == "__main__":
    tests = [
        test_non_bash_allowed,
        test_non_pr_push_allowed,
        test_pr_create_blocked_with_incomplete_plan,
        test_git_push_blocked_with_incomplete_plan,
        test_pr_create_allowed_with_complete_plan,
        test_pr_create_allowed_with_no_plans,
        test_mixed_plans_blocked,
        test_two_doc_incomplete_plan_blocked,
        test_two_doc_with_extra_files_still_blocked,
        test_plan_without_checklist_skipped,
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
