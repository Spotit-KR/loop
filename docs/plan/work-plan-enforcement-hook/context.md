# 작업 계획 강제 훅 맥락

## 배경

작업 계획 프로세스(docs/work-planning-rules.md)에 따르면 모든 코드 작업은 계획 → 실행 → 검증 순서로 진행해야 한다. 하지만 현재는 이를 강제하는 메커니즘이 없어 Claude가 계획 문서 없이 바로 코드를 수정하는 실수가 발생했다.

## 목표

PreToolUse 훅을 통해 소스 코드 파일(src/) 수정 시 `docs/plan/` 하위에 활성 작업 계획(plan.md)이 존재하는지 검사하고, 없으면 차단하여 계획 문서를 먼저 생성하도록 유도한다.

## 제약조건

- work-planning-rules.md의 예외 항목 준수:
  - 문서 작성·수정 (CLAUDE.md, docs/, README 등) → 차단 안 함
  - 설정 파일만 변경 (build.gradle.kts, application.yml 등) → 차단 안 함
  - 빌드·CI/CD 설정 수정 → 차단 안 함
- .claude/ 디렉토리 파일(훅, 설정 등) 수정도 예외
- 기존 layer_doc_reminder.py 훅과 동일한 PreToolUse Edit|Write 매처에 추가
- 세션 내에서 한번 계획 확인이 통과하면 이후 수정은 허용 (세션 마커 방식, layer_doc_reminder.py 패턴 참고)

## 관련 문서

- docs/work-planning-rules.md — 작업 계획 프로세스 규칙
- .claude/hooks/layer_doc_reminder.py — 유사 패턴 참고 (세션 마커 방식)
- .claude/settings.json — 훅 설정
