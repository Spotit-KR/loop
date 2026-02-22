#!/usr/bin/env python3
"""
work_plan_enforcer.py 훅 테스트
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile

SCRIPT = os.path.join(os.path.dirname(__file__), "work_plan_enforcer.py")

passed = 0
failed = 0


def run_hook(tool_input, project_dir=None):
    """훅을 실행하고 (exit_code, stderr) 반환."""
    data = {"tool_input": tool_input, "session_id": "test-session"}
    env = os.environ.copy()
    if project_dir:
        env["CLAUDE_PROJECT_DIR"] = project_dir
    else:
        env.pop("CLAUDE_PROJECT_DIR", None)

    result = subprocess.run(
        [sys.executable, SCRIPT],
        input=json.dumps(data),
        capture_output=True,
        text=True,
        env=env,
    )
    return result.returncode, result.stderr


def test(name, exit_code, expected_code, stderr=""):
    global passed, failed
    if exit_code == expected_code:
        print(f"  PASS: {name}")
        passed += 1
    else:
        print(f"  FAIL: {name} (expected {expected_code}, got {exit_code})")
        if stderr:
            print(f"        stderr: {stderr}")
        failed += 1


def main():
    global passed, failed

    # 임시 프로젝트 디렉토리 생성
    tmp_dir = tempfile.mkdtemp(prefix="hook_test_")
    plan_dir = os.path.join(tmp_dir, "docs", "plan", "test-task")
    os.makedirs(plan_dir, exist_ok=True)

    try:
        # ── 계획 없는 상태 테스트 ──────────────────────────────
        print("\n[계획 없는 상태]")

        # src/ 소스 코드 수정 → 차단
        code, err = run_hook(
            {"file_path": f"{tmp_dir}/src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("src/ 소스 코드 수정 → 차단", code, 2, err)

        # ── 예외 경로 테스트 ──────────────────────────────────
        print("\n[예외 경로 - 계획 없어도 허용]")

        code, _ = run_hook(
            {"file_path": f"{tmp_dir}/docs/architecture.md"},
            project_dir=tmp_dir,
        )
        test("docs/ 문서 수정 → 허용", code, 0)

        code, _ = run_hook(
            {"file_path": f"{tmp_dir}/.claude/hooks/test.py"},
            project_dir=tmp_dir,
        )
        test(".claude/ 파일 수정 → 허용", code, 0)

        code, _ = run_hook(
            {"file_path": f"{tmp_dir}/build.gradle.kts"},
            project_dir=tmp_dir,
        )
        test("build.gradle.kts 설정 파일 → 허용", code, 0)

        code, _ = run_hook(
            {"file_path": f"{tmp_dir}/src/main/resources/application.yml"},
            project_dir=tmp_dir,
        )
        test("application.yml 설정 파일 → 허용", code, 0)

        code, _ = run_hook(
            {"file_path": f"{tmp_dir}/CLAUDE.md"},
            project_dir=tmp_dir,
        )
        test("CLAUDE.md → 허용", code, 0)

        code, _ = run_hook(
            {"file_path": f"{tmp_dir}/README.md"},
            project_dir=tmp_dir,
        )
        test("README.md → 허용", code, 0)

        # src/ 외 파일
        code, _ = run_hook(
            {"file_path": f"{tmp_dir}/settings.gradle.kts"},
            project_dir=tmp_dir,
        )
        test("settings.gradle.kts → 허용", code, 0)

        # ── 활성 계획 존재 시 테스트 ─────────────────────────────
        print("\n[활성 계획 존재]")

        # plan.md 생성 (미완료 항목 포함)
        with open(os.path.join(plan_dir, "plan.md"), "w") as f:
            f.write("# Test Plan\n\n- [ ] 1단계: 구현\n- [ ] 2단계: 검증\n")

        code, _ = run_hook(
            {"file_path": f"{tmp_dir}/src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("활성 계획 있으면 src/ 수정 → 허용", code, 0)

        # ── 모든 계획 완료 시 테스트 ─────────────────────────────
        print("\n[모든 계획 완료 상태]")

        # plan.md를 모두 완료 상태로 변경
        with open(os.path.join(plan_dir, "plan.md"), "w") as f:
            f.write("# Test Plan\n\n- [x] 1단계: 구현\n- [x] 2단계: 검증\n")

        code, err = run_hook(
            {"file_path": f"{tmp_dir}/src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("모든 계획 완료 → src/ 수정 차단", code, 2, err)

        # ── 빈 입력 테스트 ──────────────────────────────────
        print("\n[엣지 케이스]")

        code, _ = run_hook({"file_path": ""}, project_dir=tmp_dir)
        test("빈 file_path → 허용", code, 0)

        code, _ = run_hook({}, project_dir=tmp_dir)
        test("file_path 없음 → 허용", code, 0)

    finally:
        shutil.rmtree(tmp_dir, ignore_errors=True)

    # ── 결과 요약 ──────────────────────────────────────────
    print(f"\n{'='*50}")
    print(f"결과: {passed} passed, {failed} failed")
    if failed:
        sys.exit(1)
    print("All tests passed!")


if __name__ == "__main__":
    main()
