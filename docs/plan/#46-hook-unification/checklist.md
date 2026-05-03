# 훅 로직 단일화 검증 체크리스트

> Issue: #46

## 필수 항목

### 코어 모듈
- [x] `hooks/core/block-dangerous.ts` 단위 테스트 통과
- [x] `hooks/core/work-plan-enforcer.ts` 단위 테스트 통과
- [x] `hooks/core/plan-completion-guard.ts` 단위 테스트 통과
- [x] `hooks/core/plan-update-reminder.ts` 단위 테스트 통과
- [x] `hooks/core/layer-doc-reminder.ts` 단위 테스트 통과
- [x] `bun test` 전체 통과 (127 pass / 0 fail)

### 기능 동등성 (Claude Code & OpenCode 양쪽)
- [x] `git push --force` → 차단 메시지 (block-dangerous 테스트)
- [x] `rm -rf ~` → 차단 (block-dangerous 테스트)
- [x] 활성 plan 없는 상태에서 `src/` 파일 편집 → 차단 (work-plan-enforcer 테스트)
- [x] 활성 plan 없는 상태에서 `docs/` 파일 편집 → 통과 (work-plan-enforcer 테스트)
- [x] 활성 plan 있는 상태에서 `src/` 파일 편집 → 통과 (work-plan-enforcer 테스트)
- [x] 미완료 `plan.md` 있는 상태에서 `gh pr create`/`git push` → 차단 (plan-completion-guard 테스트)
- [x] `domain/model/` 파일 편집 시 레이어 문서 리마인더 출력 (layer-doc-reminder 테스트)
- [x] `TaskCreate`/`TaskUpdate` 이후 plan 업데이트 리마인더 출력 (plan-update-reminder 테스트 + 라이브 동작으로 본 세션에서 반복 확인)
- [x] OpenCode 어댑터 시나리오 14건 통과 (`.opencode/plugins/__tests__/loop-hooks.test.js`)
- [x] Claude dispatcher 라이브 스모크 테스트 (rm -rf / 차단, ls -la 통과)

### 구조 정리
- [x] `.claude/hooks/`의 가드·리마인더 Python 5종 + 대응 테스트 삭제
- [x] `.opencode/plugins/`의 가드·리마인더 JS 4종 + 대응 테스트 삭제, `loop-hooks.js` 한 개로 통합
- [x] `.opencode/lib/work-plan-utils.js` 삭제 (`hooks/core/lib/plan-scanner.ts`로 이전)
- [x] `.claude/settings.json`이 `bun run.mjs <hook>` 형태로 호출
- [x] `.opencode` 패키지가 단일 플러그인(loop-hooks)만 등록 (markdown-formatter는 포매팅 범위 외 보존)

### 회귀 검증
- [x] OpenCode SDK의 `todo.updated` 공식 shape(`properties.todos`) 테스트 추가
- [x] pending todo 이벤트가 `TaskCreate` 리마인더를 발생시킴
- [x] completed todo 이벤트가 활성 plan 경로 리마인더를 발생시킴
- [x] in_progress todo 이벤트는 silent
- [x] 관련 테스트 통과

### 범위 외 보존 확인
- [x] 포매팅 훅(`markdown_formatter.py`, `ktlint-format.sh`, `markdown-formatter.js`)은 변경되지 않음

## 선택 항목
- [x] CLAUDE.md / README에 Bun 1.x 의존성 명시
- [ ] 글로벌 hook(`~/.claude/`, `~/.config/opencode/`) 통합 여부 결정
