#!/usr/bin/env python3
"""
Claude Code 훅: 위험한 명령어 차단
데이터 손실을 유발할 수 있는 파괴적인 git 및 파일 작업을 방지합니다.
종료 코드:
  0 = 명령어 허용
  2 = 명령어 차단 (stderr가 에러 메시지로 표시됨)
"""
import json
import re
import sys

# 🚫 차단할 명령어 패턴 리스트
BLOCKED_PATTERNS = [
    # Git 히스토리 파괴 방지
    (r"git\s+reset\s+--hard", "git reset --hard는 커밋되지 않은 작업을 삭제합니다"),
    (r"git\s+push\s+.*--force", "git push --force는 원격 히스토리를 덮어씁니다"),
    (r"git\s+push\s+.*-f\b", "git push -f는 원격 히스토리를 덮어씁니다"),

    # Git 작업 디렉토리 파괴 방지
    (r"git\s+clean\s+-.*f", "git clean -f는 추적되지 않은 파일을 영구 삭제합니다"),
    (r"git\s+checkout\s+\.\s*$", "git checkout .은 모든 커밋되지 않은 변경사항을 삭제합니다"),
    (r"git\s+restore\s+\.\s*$", "git restore .은 모든 커밋되지 않은 변경사항을 삭제합니다"),
    (r"git\s+stash\s+drop", "git stash drop은 스태시된 변경사항을 영구 삭제합니다"),
    (r"git\s+stash\s+clear", "git stash clear는 모든 스태시를 삭제합니다"),

    # 시스템/파일 파괴 방지
    (r"\brm\s+-rf\s+/", "rm -rf /는 매우 위험합니다"),
    (r"\brm\s+-rf\s+~", "rm -rf ~는 홈 디렉토리를 삭제합니다"),
    (r"\brm\s+-rf\s+\.\.", "rm -rf ..은 상위 디렉토리를 삭제할 수 있습니다"),
    (r"\brm\s+-rf\s+\*", "rm -rf *는 위험합니다"),

    # 데이터베이스 파괴 방지
    (r"DROP\s+DATABASE", "DROP DATABASE는 파괴적입니다"),
    (r"DROP\s+TABLE", "DROP TABLE은 파괴적입니다"),
    (r"TRUNCATE\s+TABLE", "TRUNCATE TABLE은 모든 데이터를 삭제합니다"),
]


def main():
    try:
        # Claude로부터 입력받은 JSON 파싱
        data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)  # 파싱 실패시 기본적으로 허용

    tool_name = data.get("tool_name", "")
    if tool_name != "Bash":
        sys.exit(0)

    command = data.get("tool_input", {}).get("command", "")
    if not command:
        sys.exit(0)

    # 패턴 매칭 검사
    for pattern, reason in BLOCKED_PATTERNS:
        if re.search(pattern, command, re.IGNORECASE):
            print(f"🚫 [Safety Hook] 차단됨: {reason}", file=sys.stderr)
            print(f"", file=sys.stderr)
            print(f"시도된 명령어: {command}", file=sys.stderr)
            print(f"정말 실행이 필요하다면, 터미널에서 직접 실행하세요.", file=sys.stderr)
            sys.exit(2)  # 종료 코드 2 = 차단

    sys.exit(0)  # 허용


if __name__ == "__main__":
    main()
