# OpenCode 작업 계획 플러그인 추가 맥락

## 배경

Claude Code에는 work_plan_enforcer.py와 plan_update_reminder.py 훅이 있지만, OpenCode에는 대응하는 플러그인이 없다. 두 도구 간 동일한 작업 계획 프로세스 강제가 필요하다.

## 목표

OpenCode 플러그인으로 다음 2개를 추가한다:
1. `work-plan-enforcer.js` — 소스 코드 수정 시 활성 작업 계획 존재 강제
2. `plan-update-reminder.js` — Todo 생성/완료 시 plan.md 관련 리마인더

## 제약조건

- OpenCode 플러그인 API 사용 (`tool.execute.before`, `todo.updated` 이벤트)
- 기존 플러그인 패턴(layer-doc-reminder.js, block-dangerous.js) 참고
- `.opencode/plugins/` 디렉토리에 배치
- 기존 Claude Code 훅과 동일한 로직

## 관련 문서

- .claude/hooks/work_plan_enforcer.py — Claude Code 대응 훅
- .claude/hooks/plan_update_reminder.py — Claude Code 대응 훅
- .opencode/plugins/layer-doc-reminder.js — 기존 OpenCode 플러그인 패턴
- https://opencode.ai/docs/ko/plugins/ — OpenCode 플러그인 문서
