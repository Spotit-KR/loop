# 일별 목표(DailyGoal) 구현 계획

> Issue: #38

## 단계

- [x] 1단계: Domain — DailyGoal 엔티티, VO, Command, Query, Repository 인터페이스 (TDD)
- [x] 2단계: Application — GoalService에 DailyGoal 메서드 추가 (TDD)
- [x] 3단계: Infrastructure — DailyGoalTable, ExposedDailyGoalRepository, Flyway 마이그레이션
- [x] 4단계: Presentation — GraphQL 스키마 확장, GoalDataFetcher 업데이트
- [x] 5단계: 검증 — 전체 테스트 통과, 아키텍처 준수 확인

## 리팩토링: DailyGoal API를 Goal 중심으로 단순화

GraphQL의 선택적 필드 조회 특성을 활용하여, 별도 DailyGoal 타입을 API에서 제거하고 Goal에 통합.
내부 도메인 모델(DailyGoal 엔티티)은 유지하되, 스키마 표면은 Goal 중심으로 변경.

- [x] 6단계: GraphQL 스키마 — DailyGoal 타입 제거, myGoals에 날짜 필터 추가, mutation 시그니처 변경
- [x] 7단계: Domain — DailyGoalCommand.Remove를 goalId+memberId+date 기반으로 변경, GoalQuery에 assignedDate 추가
- [x] 8단계: Application/Infrastructure — 변경된 Command/Query 반영
- [x] 9단계: Presentation — GoalDataFetcher 리팩토링 (DailyGoal 리졸버 제거, myGoals 필터 적용)
- [x] 10단계: 검증 — 전체 테스트 통과 확인

## GoalFilter 확장: AND 조건 필터 추가

GoalFilter에 ids 등 다양한 조건을 추가하고, 모든 조건은 AND로 결합.

- [x] 11단계: GoalFilter 확장 — 스키마, GoalQuery, Repository, DataFetcher 일괄 변경
- [x] 12단계: 검증 — 전체 테스트 통과 확인

## GoalFilter 추가 필드: id, title, 우선순위 규칙

id(단건) > ids(복수) > title 순 우선. assignedDate는 항상 AND. 이후 추가 조건도 AND.

- [x] 13단계: GoalFilter에 id, title 추가 및 우선순위 로직 구현
- [x] 14단계: 검증
