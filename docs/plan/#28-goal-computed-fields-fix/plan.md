# goal BC computed fields 하드코딩 수정 계획

> Issue: #28

## 분석 결과

`GoalDataFetcher.toGraphql()`에서 `totalTaskCount = 0`, `completedTaskCount = 0`, `achievementRate = 0.0`을 하드코딩하고 있음.

task BC에 이미 `GoalTaskStatsDataFetcher`(`@DgsData(parentType = "Goal")`)와 `GoalTaskStatsDataLoader`가 구현되어 있어, DGS의 field-level resolver가 하드코딩 값을 오버라이드함. 그러나 goal BC 코드만 보면 거짓 데이터를 반환하는 것처럼 보이는 문제가 있음.

**근본 원인**: computed fields가 `goal.graphqls`에 정의되어 있어 DGS Codegen이 `Goal` 클래스 생성자에 필수 파라미터로 포함시킴. 이로 인해 `GoalDataFetcher`가 불필요하게 이 필드들의 값을 설정해야 함.

## 단계

- [x] 1단계: 분석 및 계획 수립
- [x] 2단계: GoalDataFetcher의 하드코딩 값에 @DgsData 오버라이드 동작 문서화
- [x] 3단계: `GoalDataFetcher.toGraphql()`에서 하드코딩 값 제거 (Codegen 제약으로 comment 처리)
- [x] 4단계: Presentation 통합 테스트 작성 (computed fields가 DataLoader로 해석되는지 검증)
- [x] 5단계: 전체 테스트 통과 확인 및 checklist 검증
