# Claude Code 훅 보안 동기화 맥락

## 배경

OpenCode 플러그인에 적용된 보안 강화 사항이 Claude Code 훅에는 아직 반영되지 않았다.
주요 보완점: 경로 정규화, 활성 계획 3종 문서 존재 검증, Bash 경유 우회 차단, 정규식 기반 src 검사.

## 목표

OpenCode 커밋 9c4bd59, 4788e85의 보안 개선을 Claude Code 훅에 동일하게 적용한다.

## 변경 대상

- `.claude/hooks/work_plan_enforcer.py` — 경로 정규화, 접두사 예외, 정규식 src 검사, 3종 문서 검증, Bash 우회 차단
- `.claude/hooks/plan_update_reminder.py` — 3종 문서 검증
- `.claude/settings.json` — Bash 매처에 work_plan_enforcer.py 추가
- `.claude/hooks/test_work_plan_enforcer.py` — 새 테스트 케이스 추가

## 관련 문서

- 커밋 9c4bd59: src 경로 검사 보완
- 커밋 4788e85: 일관성 가드 강화 (공통 유틸, bash 감지, 테스트)
- `.opencode/plugins/work-plan-enforcer.js` — OpenCode 측 구현 참고
- `.opencode/lib/work-plan-utils.js` — 활성 계획 판별 로직 참고
