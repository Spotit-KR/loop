# plan_completion_guard 정책 정합성 회복 및 위험 권한 제거 검증 체크리스트

## 필수 항목
- [x] `plan_completion_guard.py`가 `plan.md` + `checklist.md`만 필수로 요구
- [x] 정책 문서(`docs/work-planning-rules.md`)와 가드의 필수 문서 목록이 일치
- [x] 회귀 테스트 추가: `context.md` 없이 미완료 plan이 차단되는지 확인하는 케이스 존재
- [x] `test_plan_completion_guard.py` 모든 테스트 통과 (10/10)
- [x] `test_work_plan_enforcer.py` 회귀 없음 (34/34)
- [x] `.claude/settings.json`에서 와일드카드 삭제 allow 제거됨 (allow: [])
- [x] `block_dangerous.py`가 여전히 보조 방어선으로 동작 (PreToolUse Bash 매처 유지)

## 선택 항목 (해당 시)
- [x] 가드 메시지가 사용자에게 명확한 안내 제공 (기존 메시지 유지)
