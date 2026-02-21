#!/usr/bin/env python3
"""
Claude Code 훅: 레이어 문서 리마인더
해당 레이어 파일을 세션 내 처음 수정할 때, 관련 문서를 먼저 읽도록 차단합니다.
같은 세션에서 두 번째 수정부터는 통과합니다.

종료 코드:
  0 = 허용
  2 = 차단 (문서를 먼저 읽으라는 메시지)
"""
import json
import os
import sys

REMINDER_DIR = "/tmp/claude_layer_reminders"

# 파일 경로 패턴 → 읽어야 할 문서
LAYER_DOCS = {
    "domain": "docs/layers/domain.md",
    "application": "docs/layers/application.md",
    "infrastructure": "docs/layers/infrastructure.md",
    "presentation": "docs/layers/presentation.md",
}

SECURITY_DOC = "docs/spring-security-7.md"


def get_marker_path(session_id, key):
    return os.path.join(REMINDER_DIR, f"{session_id}_{key}")


def should_remind(session_id, key):
    return not os.path.exists(get_marker_path(session_id, key))


def mark_reminded(session_id, key):
    os.makedirs(REMINDER_DIR, exist_ok=True)
    with open(get_marker_path(session_id, key), "w") as f:
        f.write("")


def detect_layer(file_path):
    for layer in LAYER_DOCS:
        if f"/{layer}/" in file_path:
            return layer
    return None


def is_security_related(file_path):
    return "common/config" in file_path and "security" in file_path.lower()


def main():
    try:
        data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)

    session_id = data.get("session_id", "unknown")
    file_path = data.get("tool_input", {}).get("file_path", "")
    if not file_path:
        sys.exit(0)

    # docs/ 자체를 수정하는 경우는 무시
    if "/docs/" in file_path:
        sys.exit(0)

    # test 파일은 무시
    if "/test/" in file_path or "Test.kt" in file_path:
        sys.exit(0)

    messages = []

    # 레이어 문서 리마인더
    layer = detect_layer(file_path)
    if layer and should_remind(session_id, layer):
        mark_reminded(session_id, layer)
        doc = LAYER_DOCS[layer]
        messages.append(f"[{layer}] 이 레이어 첫 수정입니다. 먼저 {doc} 를 읽으세요.")

    # Security 리마인더
    if is_security_related(file_path) and should_remind(session_id, "security"):
        mark_reminded(session_id, "security")
        messages.append(f"[security] Security 설정 첫 수정입니다. 먼저 {SECURITY_DOC} 를 읽으세요.")

    if messages:
        for msg in messages:
            print(msg, file=sys.stderr)
        sys.exit(2)  # 차단 → 문서 읽은 뒤 재시도

    sys.exit(0)


if __name__ == "__main__":
    main()
