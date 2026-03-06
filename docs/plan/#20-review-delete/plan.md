# Review 삭제 Mutation 추가 계획

> Issue: #20

## 단계

- [x] 1단계: Domain — ReviewCommand.Delete 추가
- [x] 2단계: Domain — ReviewRepository에 delete 메서드 추가
- [x] 3단계: Application — ReviewService.delete 테스트 작성 (RED)
- [x] 4단계: Application — ReviewService.delete 구현 (GREEN)
- [x] 5단계: Infrastructure — ExposedReviewRepository.delete 구현
- [x] 6단계: Presentation — GraphQL 스키마에 deleteReview Mutation 추가
- [x] 7단계: Presentation — ReviewDataFetcher.deleteReview 구현
- [x] 8단계: DGS Codegen 빌드 확인
- [x] 9단계: 전체 테스트 통과 확인
