# Goal 타입에 달성률/통계 computed fields 추가 계획

> Issue: #18

## 설계 방향

- Goal GraphQL 타입에 `completedTaskCount`, `totalTaskCount`, `achievementRate` 필드 추가
- DB 저장 없이 응답 시 Task 데이터 기반으로 계산
- DataLoader로 N+1 방지 (Goal 목록 조회 시 한 번의 batch 쿼리)
- **BC 간 참조 규칙 준수**: Task BC의 DataFetcher가 Goal 타입의 통계 필드를 resolve
  - Goal BC는 Task BC를 참조하지 않음
  - Task BC의 Presentation 레이어에서 `@DgsData(parentType = "Goal")` 사용

## 단계

- [x] 1단계: GraphQL 스키마 — Goal 타입에 통계 필드 추가
- [x] 2단계: Domain — GoalTaskStats 모델 + TaskRepository에 `countByGoalIds` 추가 (TDD)
- [x] 3단계: Application — TaskService에 `getStatsByGoalIds` 메서드 추가 (TDD)
- [x] 4단계: Infrastructure — ExposedTaskRepository에 집계 쿼리 구현
- [x] 5단계: Presentation — GoalTaskStatsDataLoader + GoalTaskStatsDataFetcher 구현
- [x] 6단계: 전체 테스트 통과 확인 및 검증
