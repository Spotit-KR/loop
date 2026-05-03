# plan_completion_guard 정책 정합성 회복 및 위험 권한 제거 계획

> Issue: #44

## 단계

- [x] 1단계: 회귀 재현 — 2종 문서(plan.md + checklist.md)만 있는 미완료 plan에서 가드가 거짓통과(exit=0)하는 것을 임시 디렉토리로 재현
- [x] 2단계: `plan_completion_guard.py`의 `REQUIRED_PLAN_FILES`에서 `context.md` 제거
- [x] 3단계: `test_plan_completion_guard.py`의 `create_plan_dir` 헬퍼를 2종 구조로 갱신 (extra_files 인자로 부수 파일 추가 지원)
- [x] 4단계: 회귀 방지 테스트 3건 추가 — 2종만 있는 미완료 차단, extra 파일 있어도 차단, 필수 2종 미충족 디렉토리는 가드 대상 외
- [x] 5단계: `.claude/settings.json`의 와일드카드 삭제 allow 항목 제거 (allow 빈 배열로)
- [x] 6단계: 가드 테스트 실행 — `test_plan_completion_guard.py` 10/10, `test_work_plan_enforcer.py` 34/34 통과
- [x] 7단계: 검증 체크리스트 확인 후 커밋/PR 생성

## 비고

- `test_block_dangerous.py`(pytest 기반) 12건 실패는 본 변경 이전부터 존재한 결함(soft-warn 케이스가 hard-block으로 동작). 별도 이슈로 분리 예정.
