#!/usr/bin/env python3
"""
Claude Code 훅: 작업 계획 강제
소스 코드(src/) 수정 시 docs/plan/ 하위에 활성 작업 계획이 존재하는지 검사합니다.
활성 계획이 없으면 차단(exit code 2)하여 계획 문서를 먼저 생성하도록 유도합니다.

활성 계획 조건: plan.md, context.md, checklist.md 3종이 모두 존재하고,
plan.md에 미완료 항목(- [ ])이 있어야 합니다.

예외 (work-planning-rules.md 기준):
- 문서 작성·수정 (docs/, README, CLAUDE.md 등)
- 설정 파일 변경 (build.gradle.kts, application.yml 등)
- .claude/ 디렉토리 파일
- 빌드·CI/CD 설정

종료 코드:
  0 = 허용
  2 = 차단 (작업 계획을 먼저 생성하라는 메시지)
"""
import json
import os
import re
import sys

PROJECT_DIR = os.environ.get("CLAUDE_PROJECT_DIR", "")
PLAN_BASE = os.path.join(PROJECT_DIR, "docs", "plan") if PROJECT_DIR else ""

REQUIRED_PLAN_FILES = ["plan.md", "context.md", "checklist.md"]

# 예외 경로 패턴: 이 문자열이 파일 경로에 포함되면 검사하지 않음
EXEMPT_PATTERNS = [
    "/docs/",
    "/.claude/",
    "/.opencode/",
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

# Bash 쓰기 명령 패턴
BASH_WRITE_PATTERNS = [
    re.compile(r"\b(touch|mkdir|cp|mv|rm|install|truncate|dd)\b", re.I),
    re.compile(r"\b(sed|perl)\b[\s\S]*\s-i\b", re.I),
    re.compile(r"\btee\b", re.I),
    re.compile(r"\b(cat|echo|printf)\b[\s\S]*>{1,2}", re.I),
    re.compile(r">{1,2}\s*[\"']?[^\s\"']*src/", re.I),
    re.compile(
        r"\bpython(?:3)?\b[\s\S]*open\(\s*[\"'][^\"']*src/[^\"']*[\"']\s*,\s*[\"'][wa]",
        re.I,
    ),
]

SRC_PATH_RE = re.compile(r"(^|/)src/")


def normalize_path(file_path):
    """경로 구분자를 통일."""
    return file_path.replace("\\", "/")


def is_exempt(file_path):
    """작업 계획 프로세스 예외 대상인지 확인."""
    normalized = normalize_path(file_path)

    for pattern in EXEMPT_PATTERNS:
        if pattern in normalized:
            return True

    if normalized.startswith("docs/"):
        return True
    if normalized.startswith(".claude/"):
        return True
    if normalized.startswith(".opencode/"):
        return True

    for ext in EXEMPT_EXTENSIONS:
        if normalized.endswith(ext):
            return True

    return False


def is_src_path(file_path):
    """src/ 하위 경로인지 정규식으로 확인."""
    return bool(SRC_PATH_RE.search(normalize_path(file_path)))


def has_active_plan():
    """docs/plan/ 하위에 활성 작업 계획이 존재하는지 확인.

    활성 계획 조건: plan.md, context.md, checklist.md 3종 모두 존재 + plan.md에 미완료 항목.
    """
    if not PLAN_BASE or not os.path.isdir(PLAN_BASE):
        return False

    try:
        entries = os.listdir(PLAN_BASE)
    except OSError:
        return False

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
            return True

    return False


def extract_src_path_candidates(command):
    """Bash 명령어에서 src/ 경로 후보를 추출."""
    paths = []
    for match in re.finditer(r"(?:^|[\s\"'])([^\s\"'`]*src/[^\s\"'`]+)", command):
        path = re.sub(r"[;|,&]+$", "", match.group(1))
        paths.append(path)
    return paths


def is_bash_source_mutation(command):
    """Bash 명령어가 src/ 파일을 변경하는 쓰기 명령인지 감지."""
    if not re.search(r"(^|\W)src/", command):
        return False
    return any(pattern.search(command) for pattern in BASH_WRITE_PATTERNS)


def should_block_file_path(file_path):
    """파일 경로가 차단 대상인지 확인."""
    normalized = normalize_path(file_path)
    if is_exempt(normalized):
        return False
    return is_src_path(normalized)


def block_exit():
    """차단 메시지를 출력하고 종료."""
    print(
        "[work-plan] 소스 코드 수정이 차단되었습니다. "
        "docs/plan/{작업명}/ 에 plan.md, context.md, checklist.md 를 먼저 생성하세요. "
        "(docs/work-planning-rules.md 참고)",
        file=sys.stderr,
    )
    sys.exit(2)


def main():
    try:
        data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)

    tool_name = data.get("tool_name", "")
    tool_input = data.get("tool_input", {})

    # Bash 도구: 쓰기 명령 감지
    if tool_name == "Bash":
        command = tool_input.get("command", "")
        if not is_bash_source_mutation(command):
            sys.exit(0)

        src_paths = extract_src_path_candidates(command)
        has_non_exempt = any(should_block_file_path(p) for p in src_paths)
        if not has_non_exempt:
            sys.exit(0)

        if has_active_plan():
            sys.exit(0)

        block_exit()

    # Edit/Write 도구: 파일 경로 검사
    if tool_name not in ("Edit", "Write"):
        sys.exit(0)

    file_path = tool_input.get("file_path", "")
    if not file_path:
        sys.exit(0)

    if not should_block_file_path(file_path):
        sys.exit(0)

    if has_active_plan():
        sys.exit(0)

    block_exit()


if __name__ == "__main__":
    main()
