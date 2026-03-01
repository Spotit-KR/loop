좋은 방향 전환입니다. 요청하신 3가지 관점 기준으로 보면, 현재 변경은 **의도 대부분을 잘 달성**하고 있습니다.

**핵심 평가**
- `이슈 생성 → 계획 → 실행 → 검증`으로 선행 이슈를 강제한 점은 작업 공유 측면에서 확실히 개선입니다 (`CLAUDE.md:7`, `docs/work-planning-rules.md:3`, `docs/work-planning-rules.md:15`).
- 이슈 번호를 계획 디렉토리/브랜치에 통일한 점도 추적성 향상에 효과적입니다 (`CLAUDE.md:10`, `docs/work-planning-rules.md:55`, `docs/work-planning-rules.md:57`, `docs/work-planning-rules.md:58`).
- `context.md` 제거 후 이슈 본문으로 대체한 방향은 타당합니다 (`docs/work-planning-rules.md:23`, `docs/work-planning-rules.md:67`).
- 비코드 작업 예외를 명시해 범위를 분리한 것도 명확합니다 (`docs/work-planning-rules.md:7`).

**중요 보완점 (실제 동작 불일치)**
- 현재 훅/플러그인 로직은 아직 `context.md` 3종 구성을 요구합니다. 즉, 문서 규칙은 2종인데 자동 검사 코드는 3종이라 충돌합니다.
  - `.claude/hooks/work_plan_enforcer.py:28`
  - `.claude/hooks/plan_update_reminder.py:23`
  - `.opencode/lib/work-plan-utils.js:4`
  - `.opencode/plugins/plan-update-reminder.js:60`
- 이 상태면 새 규칙대로 작업해도 차단/경고가 발생할 수 있어서, 이번 변경의 효과를 떨어뜨립니다.

**context.md 제거 관련 코멘트**
- 제거 자체는 적절합니다.
- 다만 “초기 컨텍스트”는 이슈 본문으로 대체되지만, **진행 중 의사결정/스코프 변경**이 누락될 수 있습니다. 이를 방지하려면:
  - 이슈 본문 갱신 or 이슈 댓글로 변경 이력 기록 규칙 추가
  - 세션 복원 명령을 `gh issue view #N`에서 `gh issue view #N --comments`로 확장 권장 (`docs/work-planning-rules.md:63`)

**추가 개선 제안**
- 이미 이슈 번호가 주어진 요청은 `gh issue create`를 생략하고 재사용한다는 규칙 추가 권장.
- `gh issue create` 실패 시(권한/네트워크) 대체 플로우를 명시하면 운영 안정성이 좋아집니다.
- `chore` 라벨 설명(설정 변경)과 “설정만 변경 시 예외” 규칙이 헷갈릴 수 있어, “설정-only는 예외, 코드 동반 설정 변경은 chore”로 문구 명확화 권장 (`docs/work-planning-rules.md:10`, `docs/work-planning-rules.md:51`).
