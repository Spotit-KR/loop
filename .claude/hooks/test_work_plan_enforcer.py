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


def run_hook(tool_name, tool_input, project_dir=None):
    """훅을 실행하고 (exit_code, stderr) 반환."""
    data = {"tool_name": tool_name, "tool_input": tool_input, "session_id": "test-session"}
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


def create_active_plan(plan_dir):
    """3종 문서를 모두 생성하여 활성 계획을 만든다."""
    os.makedirs(plan_dir, exist_ok=True)
    with open(os.path.join(plan_dir, "plan.md"), "w") as f:
        f.write("# Test Plan\n\n- [ ] 1단계: 구현\n- [ ] 2단계: 검증\n")
    with open(os.path.join(plan_dir, "context.md"), "w") as f:
        f.write("# Test Context\n\n## 배경\n테스트용\n")
    with open(os.path.join(plan_dir, "checklist.md"), "w") as f:
        f.write("# Test Checklist\n\n- [ ] 테스트 통과\n")


def main():
    global passed, failed

    tmp_dir = tempfile.mkdtemp(prefix="hook_test_")
    plan_dir = os.path.join(tmp_dir, "docs", "plan", "test-task")
    os.makedirs(os.path.join(tmp_dir, "docs", "plan"), exist_ok=True)

    try:
        # ── Edit/Write: 계획 없는 상태 ──────────────────────────
        print("\n[Edit/Write - 계획 없는 상태]")

        code, err = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("Edit src/ 소스 코드 → 차단", code, 2, err)

        code, err = run_hook(
            "Write",
            {"file_path": f"{tmp_dir}/src/main/kotlin/NewFile.kt"},
            project_dir=tmp_dir,
        )
        test("Write src/ 소스 코드 → 차단", code, 2, err)

        # ── Edit/Write: 예외 경로 ──────────────────────────────
        print("\n[Edit/Write - 예외 경로 (계획 없어도 허용)]")

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/docs/architecture.md"},
            project_dir=tmp_dir,
        )
        test("docs/ 문서 수정 → 허용", code, 0)

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/.claude/hooks/test.py"},
            project_dir=tmp_dir,
        )
        test(".claude/ 파일 수정 → 허용", code, 0)

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/.opencode/plugins/test.js"},
            project_dir=tmp_dir,
        )
        test(".opencode/ 파일 수정 → 허용", code, 0)

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/build.gradle.kts"},
            project_dir=tmp_dir,
        )
        test("build.gradle.kts → 허용", code, 0)

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/src/main/resources/application.yml"},
            project_dir=tmp_dir,
        )
        test("application.yml → 허용", code, 0)

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/CLAUDE.md"},
            project_dir=tmp_dir,
        )
        test("CLAUDE.md → 허용", code, 0)

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/README.md"},
            project_dir=tmp_dir,
        )
        test("README.md → 허용", code, 0)

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/settings.gradle.kts"},
            project_dir=tmp_dir,
        )
        test("settings.gradle.kts → 허용", code, 0)

        # ── 경로 정규화 ──────────────────────────────────────
        print("\n[경로 정규화 (백슬래시)]")

        code, err = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}\\src\\main\\kotlin\\Task.kt"},
            project_dir=tmp_dir,
        )
        test("백슬래시 src 경로 → 차단", code, 2, err)

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}\\docs\\architecture.md"},
            project_dir=tmp_dir,
        )
        test("백슬래시 docs 경로 → 허용", code, 0)

        # ── 활성 계획 존재 (3종 문서 완비) ────────────────────────
        print("\n[활성 계획 존재 - 3종 문서 완비]")

        create_active_plan(plan_dir)

        code, _ = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("3종 문서 완비 + 미완료 항목 → src/ 수정 허용", code, 0)

        # ── 3종 문서 불완전 ──────────────────────────────────
        print("\n[3종 문서 불완전 - 활성 계획 아님]")

        # context.md 삭제
        os.remove(os.path.join(plan_dir, "context.md"))
        code, err = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("context.md 없으면 → 차단", code, 2, err)

        # context.md 복구, checklist.md 삭제
        with open(os.path.join(plan_dir, "context.md"), "w") as f:
            f.write("# Context\n")
        os.remove(os.path.join(plan_dir, "checklist.md"))
        code, err = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("checklist.md 없으면 → 차단", code, 2, err)

        # plan.md만 있는 경우
        shutil.rmtree(plan_dir)
        os.makedirs(plan_dir)
        with open(os.path.join(plan_dir, "plan.md"), "w") as f:
            f.write("# Plan\n\n- [ ] TODO\n")
        code, err = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("plan.md만 있으면 → 차단", code, 2, err)

        # ── 모든 계획 완료 ──────────────────────────────────
        print("\n[모든 계획 완료]")

        shutil.rmtree(plan_dir)
        create_active_plan(plan_dir)
        with open(os.path.join(plan_dir, "plan.md"), "w") as f:
            f.write("# Plan\n\n- [x] 1단계: 완료\n- [x] 2단계: 완료\n")

        code, err = run_hook(
            "Edit",
            {"file_path": f"{tmp_dir}/src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("모든 항목 완료 → src/ 수정 차단", code, 2, err)

        # ── Bash 우회 차단 ──────────────────────────────────
        print("\n[Bash 우회 차단 - 계획 없는 상태]")

        # 3종 완비 상태 제거
        shutil.rmtree(plan_dir, ignore_errors=True)

        code, err = run_hook(
            "Bash",
            {"command": "echo hello > src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("echo > src/ → 차단", code, 2, err)

        code, err = run_hook(
            "Bash",
            {"command": "cat data.txt >> src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("cat >> src/ → 차단", code, 2, err)

        code, err = run_hook(
            "Bash",
            {"command": "sed -i 's/old/new/g' src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("sed -i src/ → 차단", code, 2, err)

        code, err = run_hook(
            "Bash",
            {"command": "tee src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("tee src/ → 차단", code, 2, err)

        code, err = run_hook(
            "Bash",
            {"command": "cp template.kt src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("cp → src/ → 차단", code, 2, err)

        code, err = run_hook(
            "Bash",
            {"command": "mv old.kt src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("mv → src/ → 차단", code, 2, err)

        code, err = run_hook(
            "Bash",
            {"command": "rm src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("rm src/ → 차단", code, 2, err)

        # ── Bash 허용 케이스 ─────────────────────────────────
        print("\n[Bash 허용 케이스]")

        code, _ = run_hook(
            "Bash",
            {"command": "cat src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("cat src/ (읽기 전용) → 허용", code, 0)

        code, _ = run_hook(
            "Bash",
            {"command": "grep -r 'class' src/"},
            project_dir=tmp_dir,
        )
        test("grep src/ (읽기 전용) → 허용", code, 0)

        code, _ = run_hook(
            "Bash",
            {"command": "ls src/main/kotlin/"},
            project_dir=tmp_dir,
        )
        test("ls src/ (읽기 전용) → 허용", code, 0)

        code, _ = run_hook(
            "Bash",
            {"command": "./gradlew build"},
            project_dir=tmp_dir,
        )
        test("gradlew build (src 없음) → 허용", code, 0)

        code, _ = run_hook(
            "Bash",
            {"command": "echo hello > docs/output.md"},
            project_dir=tmp_dir,
        )
        test("echo > docs/ → 허용", code, 0)

        # ── Bash + 활성 계획 ─────────────────────────────────
        print("\n[Bash + 활성 계획 존재]")

        create_active_plan(plan_dir)

        code, _ = run_hook(
            "Bash",
            {"command": "echo hello > src/main/kotlin/Task.kt"},
            project_dir=tmp_dir,
        )
        test("활성 계획 있으면 Bash src/ 쓰기 → 허용", code, 0)

        shutil.rmtree(plan_dir, ignore_errors=True)

        # ── Bash 예외 경로 ───────────────────────────────────
        print("\n[Bash 예외 경로]")

        code, _ = run_hook(
            "Bash",
            {"command": "echo hello > src/main/resources/application.yml"},
            project_dir=tmp_dir,
        )
        test("Bash yml 설정 파일 → 허용", code, 0)

        # ── 엣지 케이스 ──────────────────────────────────────
        print("\n[엣지 케이스]")

        code, _ = run_hook("Edit", {"file_path": ""}, project_dir=tmp_dir)
        test("빈 file_path → 허용", code, 0)

        code, _ = run_hook("Edit", {}, project_dir=tmp_dir)
        test("file_path 없음 → 허용", code, 0)

        code, _ = run_hook("Bash", {"command": ""}, project_dir=tmp_dir)
        test("빈 command → 허용", code, 0)

        code, _ = run_hook("Read", {"file_path": "src/main/kotlin/Task.kt"}, project_dir=tmp_dir)
        test("Read 도구 (비수정) → 허용", code, 0)

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
