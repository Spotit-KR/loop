# Review 수정 Mutation 추가 계획

> Issue: #19

## 단계

- [x] 1단계: Domain — ReviewCommand.Update 추가 + Review 도메인 테스트
- [x] 2단계: Domain — ReviewRepository에 update 메서드 추가
- [x] 3단계: Application — ReviewService.update 구현 (소유권 검증 포함) + 테스트
- [x] 4단계: GraphQL 스키마 — UpdateReviewInput, updateReview Mutation 추가
- [x] 5단계: Infrastructure — ExposedReviewRepository.update 구현
- [x] 6단계: Presentation — ReviewDataFetcher.updateReview 구현
- [x] 7단계: 빌드 및 전체 테스트 통과 확인
