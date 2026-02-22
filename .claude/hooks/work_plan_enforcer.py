#!/usr/bin/env python3
"""
Claude Code 훅: 작업 계획 강제
소스 코드(src/) 수정 시 docs/plan/ 하위에 활성 작업 계획이 존재하는지 검사합니다.
활성 계획이 없으면 차단(exit code 2)하여 계획 문서를 먼저 생성하도록 유도합니다.

예외 (work-planning-rules.md 기준):
- 문서 작성·수정 (docs/, README, CLAUDE.md 등)
- 설정 파일 변경 (build.gradle.kts, application.yml 등)
- .claude/ 디렉토리 파일
- 빌드·CI/CD 설정

종료 코드:
  0 = 허용
  2 = 차단 (작업 계획을 먼저 생성하라는 메시지)
"""
import glob
import json
import os
import sys

PROJECT_DIR = os.environ.get("CLAUDE_PROJECT_DIR", "")
PLAN_BASE = os.path.join(PROJECT_DIR, "docs", "plan") if PROJECT_DIR else ""

# 예외 경로 패턴: 이 문자열이 파일 경로에 포함되면 검사하지 않음
EXEMPT_PATTERNS = [
    "/docs/",
    "/.claude/",
    "README",
    "CLAUDE.md",
]

# 예외 파일 확장자 (설정/빌드 파일)
EXEMPT_EXTENSIONS = [
    ".gradle.kts",
    ".gradle",
    ".yml",
    ".yaml",
    ".properties",
    ".toml",
    ".xml",
]


def is_exempt(file_path):
    """작업 계획 프로세스 예외 대상인지 확인."""
    for pattern in EXEMPT_PATTERNS:
        if pattern in file_path:
            return True

    for ext in EXEMPT_EXTENSIONS:
        if file_path.endswith(ext):
            return True

    return False


def has_active_plan():
    """docs/plan/ 하위에 활성 작업 계획(미완료 항목이 있는 plan.md)이 존재하는지 확인."""
    if not PLAN_BASE or not os.path.isdir(PLAN_BASE):
        return False

    for plan_md in glob.glob(os.path.join(PLAN_BASE, "*", "plan.md")):
        with open(plan_md) as f:
            content = f.read()
        if "- [ ]" in content:
            return True

    return False


def main():
    try:
        data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)

    file_path = data.get("tool_input", {}).get("file_path", "")
    if not file_path:
        sys.exit(0)

    # 예외 대상이면 허용
    if is_exempt(file_path):
        sys.exit(0)

    # 소스 코드가 아니면 허용 (src/ 하위가 아닌 경우)
    if "/src/" not in file_path:
        sys.exit(0)

    # 활성 작업 계획이 있으면 허용
    if has_active_plan():
        sys.exit(0)

    # 차단: 작업 계획 없이 소스 코드 수정 시도
    print(
        "[work-plan] 소스 코드 수정이 차단되었습니다. "
        "docs/plan/{작업명}/ 에 plan.md, context.md, checklist.md 를 먼저 생성하세요. "
        "(docs/work-planning-rules.md 참고)",
        file=sys.stderr,
    )
    sys.exit(2)


if __name__ == "__main__":
    main()
