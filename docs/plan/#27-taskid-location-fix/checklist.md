# TaskId 위치 수정 검증 체크리스트

## 필수 항목
- [x] 아키텍처 원칙 준수 (common/domain에는 2개 이상 BC에서 사용하는 VO만 배치)
- [x] 레이어 의존성 규칙 위반 없음
- [x] 모든 테스트 통과
- [x] 기존 테스트 깨지지 않음
- [x] common/domain/TaskId.kt 삭제됨
- [x] task/domain/model/TaskId.kt 생성됨
- [x] 모든 import 경로 변경됨
