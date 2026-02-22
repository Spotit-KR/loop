#!/usr/bin/env python3
"""
Claude Code 훅: 작업 계획 문서 업데이트 리마인더
TaskCreate/TaskUpdate 호출 후 plan 문서 관련 피드백을 제공합니다.

- TaskCreate 후: plan 디렉토리·문서 생성 여부 확인 리마인드
- TaskUpdate(status=completed) 후: plan.md 체크 표시 업데이트 리마인드
"""
import json
import sys
import glob
import os

PROJECT_DIR = os.environ.get("CLAUDE_PROJECT_DIR", "")
PLAN_BASE = os.path.join(PROJECT_DIR, "docs", "plan") if PROJECT_DIR else ""

REQUIRED_PLAN_FILES = ["plan.md", "context.md", "checklist.md"]


def find_active_plan_dirs():
    """활성 작업 계획 디렉토리 목록 반환.

    활성 계획 조건: plan.md, context.md, checklist.md 3종 모두 존재 + plan.md에 미완료 항목.
    """
    if not PLAN_BASE or not os.path.isdir(PLAN_BASE):
        return []
    dirs = []
    try:
        entries = os.listdir(PLAN_BASE)
    except OSError:
        return []
    for entry in entries:
        plan_dir = os.path.join(PLAN_BASE, entry)
        if not os.path.isdir(plan_dir):
            continue
        # 3종 문서 존재 확인
        if not all(
            os.path.isfile(os.path.join(plan_dir, f)) for f in REQUIRED_PLAN_FILES
        ):
            continue
        plan_md = os.path.join(plan_dir, "plan.md")
        with open(plan_md) as f:
            content = f.read()
        if "- [ ]" in content:
            dirs.append(plan_dir)
    return dirs


def handle_task_create(tool_input):
    subject = tool_input.get("subject", "")
    active_dirs = find_active_plan_dirs()
    if not active_dirs:
        msg = (
            f"[plan] TaskCreate 감지: \"{subject}\"\n"
            f"docs/plan/{{작업명}}/ 에 plan.md, context.md, checklist.md 를 생성했는지 확인하세요. "
            f"(docs/work-planning-rules.md 참고)"
        )
        return msg
    return None


def handle_task_update(tool_input):
    status = tool_input.get("status", "")
    if status != "completed":
        return None

    active_dirs = find_active_plan_dirs()
    if active_dirs:
        plan_list = ", ".join(
            os.path.join(d, "plan.md") for d in active_dirs
        )
        return (
            f"[plan] Task 완료 감지. "
            f"다음 plan.md 의 해당 단계를 [x]로 업데이트하세요: {plan_list}"
        )
    return None


def main():
    try:
        data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)

    tool_name = data.get("tool_name", "")
    tool_input = data.get("tool_input", {})

    message = None
    if tool_name == "TaskCreate":
        message = handle_task_create(tool_input)
    elif tool_name == "TaskUpdate":
        message = handle_task_update(tool_input)

    if message:
        # stderr → Claude에게 피드백 (PostToolUse exit code 2)
        print(message, file=sys.stderr)
        sys.exit(2)

    sys.exit(0)


if __name__ == "__main__":
    main()
