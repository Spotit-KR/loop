# Goal/DailyGoal 삭제 시 연관 Task 삭제 검증 체크리스트

## 필수 항목
- [x] 아키텍처 원칙 준수 (docs/architecture.md 기준)
- [x] BC 간 이벤트 통신 규칙 준수 (docs/layers/bc-event.md 기준)
- [x] 레이어 의존성 규칙 위반 없음
- [x] 테스트 코드 작성 완료 (Domain, Application 필수)
- [x] 모든 테스트 통과
- [x] 기존 테스트 깨지지 않음

## 선택 항목 (해당 시)
- [x] BC 간 통신 규칙 준수 (이벤트 클래스는 common/domain/event/에 배치)
