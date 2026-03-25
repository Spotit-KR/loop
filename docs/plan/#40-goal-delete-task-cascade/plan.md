# Goal/DailyGoal 삭제 시 연관 Task 삭제 계획

> Issue: #40

## 단계

- [x] 1단계: 이벤트 클래스 생성 — GoalDeletedEvent, DailyGoalRemovedEvent (common/domain/event/)
- [x] 2단계: TaskRepository 인터페이스에 deleteByGoalId, deleteByGoalIdAndTaskDate 추가
- [x] 3단계: GoalService 이벤트 발행 TDD — RED(테스트 작성) → GREEN(구현) → REFACTOR
- [x] 4단계: TaskService 이벤트 수신 TDD — RED(테스트 작성) → GREEN(구현) → REFACTOR
- [x] 5단계: ExposedTaskRepository에 deleteByGoalId, deleteByGoalIdAndTaskDate 구현
- [x] 6단계: 전체 테스트 통과 확인 및 검증 체크리스트 완료
- [x] 7단계: 코드 리뷰 피드백 반영 (FQN import 정리)
