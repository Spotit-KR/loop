# AI 작업 일관성 강화 후속 작업 맥락

## 배경

작업 계획 강제 플러그인 검증 이후, 일관성 강화 관점에서 추가 보완 포인트가 도출되었다. 특히 OpenCode 플러그인 쪽은 Claude 훅 대비 자동 테스트 체계가 약하고, bash 경유 파일 수정 우회 여지가 있다.

## 목표

다음 3가지를 이번 작업에서 구현한다.

1. active plan 식별 로직 정밀화
2. bash 기반 src 수정 우회 차단
3. OpenCode 플러그인 테스트 스위트 추가

추가로 다음 2가지는 구현 대신 TODO로 기록한다.

4. CI 일관성 검증 추가
5. 브랜치 규칙 자동 가드

## 제약조건

- 기존 플러그인 아키텍처와 이벤트 모델(`tool.execute.before`, `todo.updated`)을 유지한다.
- 테스트는 로컬에서 재현 가능한 `bun test` 기반으로 작성한다.

## 관련 문서

- `docs/work-planning-rules.md`
- `.opencode/plugins/work-plan-enforcer.js`
- `.opencode/plugins/plan-update-reminder.js`

## 결과

- active plan 판별을 `work-plan-utils.js`로 공통화했다.
- active plan은 문서 3종(`plan.md`, `context.md`, `checklist.md`)이 모두 존재하고, 미완료 체크박스가 있는 계획만 인정하도록 강화했다.
- 활성 계획이 여러 개일 때는 최신 수정 계획을 우선(plan reminder 대상)으로 선택하도록 정밀화했다.
- `work-plan-enforcer.js`에 bash 기반 src 수정 감지를 추가해 파일 경로 인자 없이도 우회 시도를 차단한다.
- `bun test plugins/__tests__` 기준 13개 테스트가 통과했다.
- `opencode run` E2E에서 활성 계획 없는 `bash` src 쓰기(`echo hi > src/...`)가 즉시 차단되는 것을 확인했다.
