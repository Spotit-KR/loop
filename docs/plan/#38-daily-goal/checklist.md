# 일별 목표(DailyGoal) 검증 체크리스트

## 필수 항목
- [x] 아키텍처 원칙 준수 (docs/architecture.md 기준)
- [x] 레이어 의존성 규칙 위반 없음
- [x] Domain, Application 테스트 코드 작성 완료 (TDD)
- [x] 모든 테스트 통과
- [x] 기존 테스트 깨지지 않음
- [x] DGS Codegen으로 GraphQL 타입 자동 생성 (수동 작성 금지)

## 선택 항목
- [x] Flyway 마이그레이션 작성 (V5__Create_daily_goal_table.sql)
- [x] Goal + Date + Member 유니크 제약 적용
