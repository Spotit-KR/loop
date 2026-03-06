# Goal 타입 달성률/통계 검증 체크리스트

## 필수 항목
- [x] 아키텍처 원칙 준수 (docs/architecture.md 기준)
- [x] 레이어 의존성 규칙 위반 없음
- [x] BC 간 참조 규칙 준수 (Task BC가 Goal 타입 필드 resolve)
- [x] 테스트 코드 작성 완료 (Domain, Application 필수)
- [x] 모든 테스트 통과
- [x] 기존 테스트 깨지지 않음

## 선택 항목
- [x] DataLoader로 N+1 방지 확인
