# Review(회고) BC 구현 계획

> Issue: #16

## 단계

- [x] Step 0: 준비 — 마이그레이션, GraphQL 스키마, Codegen 실행
- [x] Step 1: Domain Layer (TDD) — ReviewId, PeriodKey, Review, ReviewType, StepType, ReviewStep, ReviewCommand, ReviewQuery, ReviewRepository
- [x] Step 2: Application Layer (TDD) — ReviewStatsDto, ReviewService (create, findAll, getStats)
- [x] Step 3: Infrastructure Layer — ReviewTable (JSONB), ExposedReviewRepository
- [x] Step 4: Presentation Layer — ReviewDataFetcher
- [x] Step 5: 검증 — 전체 테스트 통과, 빌드 성공
