#!/usr/bin/env python3
"""
Claude Code 훅: PR 생성 / push 시 미완료 plan.md 차단

gh pr create 또는 git push 명령 실행 전,
docs/plan/**/plan.md에 미완료 항목(- [ ])이 있으면 차단합니다.

미완료 계획이 머지되는 것을 방지하여,
다른 작업의 work_plan_enforcer / plan_update_reminder 오작동을 예방합니다.

종료 코드:
  0 = 허용
  2 = 차단
"""
import json
import os
import re
import sys

PROJECT_DIR = os.environ.get("CLAUDE_PROJECT_DIR", "")
PLAN_BASE = os.path.join(PROJECT_DIR, "docs", "plan") if PROJECT_DIR else ""

REQUIRED_PLAN_FILES = ["plan.md", "context.md", "checklist.md"]

# gh pr create 또는 git push 감지
PR_CREATE_RE = re.compile(r"\bgh\s+pr\s+create\b")
GIT_PUSH_RE = re.compile(r"\bgit\s+push\b")


def find_incomplete_plans():
    """미완료 항목이 있는 plan.md 경로 목록 반환."""
    if not PLAN_BASE or not os.path.isdir(PLAN_BASE):
        return []

    try:
        entries = os.listdir(PLAN_BASE)
    except OSError:
        return []

    incomplete = []
    for entry in entries:
        plan_dir = os.path.join(PLAN_BASE, entry)
        if not os.path.isdir(plan_dir):
            continue

        if not all(
            os.path.isfile(os.path.join(plan_dir, f)) for f in REQUIRED_PLAN_FILES
        ):
            continue

        plan_md = os.path.join(plan_dir, "plan.md")
        try:
            with open(plan_md) as f:
                content = f.read()
        except OSError:
            continue

        if "- [ ]" in content:
            incomplete.append(plan_md)

    return incomplete


def main():
    try:
        data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)

    tool_name = data.get("tool_name", "")
    tool_input = data.get("tool_input", {})

    if tool_name != "Bash":
        sys.exit(0)

    command = tool_input.get("command", "")

    if not PR_CREATE_RE.search(command) and not GIT_PUSH_RE.search(command):
        sys.exit(0)

    incomplete = find_incomplete_plans()
    if not incomplete:
        sys.exit(0)

    plans = "\n".join(f"  - {p}" for p in incomplete)
    print(
        f"[plan-guard] 미완료 plan.md가 있어 PR 생성/push를 차단합니다.\n"
        f"다음 plan.md의 모든 항목을 완료([x])하거나 불필요한 계획을 정리하세요:\n"
        f"{plans}",
        file=sys.stderr,
    )
    sys.exit(2)


if __name__ == "__main__":
    main()
