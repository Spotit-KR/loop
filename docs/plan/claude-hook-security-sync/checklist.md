# Claude Code 훅 보안 동기화 검증 체크리스트

## 필수 항목

- [x] 경로 정규화 (백슬래시 → 슬래시) 적용
- [x] 접두사 기반 예외 (docs/, .claude/) 적용
- [x] 정규식 기반 src 경로 검사 적용
- [x] 활성 계획 3종 문서 (plan.md, context.md, checklist.md) 존재 검증
- [x] Bash 경유 src 수정 우회 차단
- [x] plan_update_reminder.py도 3종 문서 검증 적용
- [x] 테스트 통과
- [x] 기존 기능 깨지지 않음
