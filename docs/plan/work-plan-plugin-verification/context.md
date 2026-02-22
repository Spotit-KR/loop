# 작업 계획 플러그인 동작 확인 맥락

## 배경

OpenCode용 작업 계획 강제 플러그인(`work-plan-enforcer.js`, `plan-update-reminder.js`)이 이미 추가되어 있다. 실제 요청 흐름에서 의도대로 차단/안내가 동작하는지 재검증이 필요하다.

## 목표

플러그인 핵심 시나리오를 실행해 다음을 확인한다.

1. 활성 계획 없이 `src/` 수정 시 차단된다.
2. 예외 경로(`docs/`, `.claude/`, `.opencode/`, 설정 파일)는 차단되지 않는다.
3. Todo 상태 변경 이벤트에서 계획 문서 업데이트 리마인드가 올바르게 발생한다.

## 제약조건

- 기존 플러그인 코드는 변경하지 않고 검증 중심으로 진행한다.
- 검증은 로컬 실행 가능한 스크립트/명령으로 재현 가능해야 한다.

## 관련 문서

- `docs/work-planning-rules.md`
- `.opencode/plugins/work-plan-enforcer.js`
- `.opencode/plugins/plan-update-reminder.js`

## 검증 결과

- `bun -e` 기반 시나리오 테스트로 enforcer/reminder 핵심 동작(차단/허용/리마인드)을 확인했다.
- 검증 중 `work-plan-enforcer.js`가 상대 경로(`src/...`) 입력을 차단하지 않는 케이스를 발견했고, 경로 정규화 + `/(^|\/)src\//` 매칭으로 보완했다.
- 보완 후 절대/상대 경로 모두에서 동일하게 차단 동작이 재현됨을 확인했다.
- `opencode run` E2E 실행 시 수정된 플러그인이 즉시 로드되어 상대 경로 `src/...` 수정 요청에서 차단 메시지가 발생함을 확인했다.
