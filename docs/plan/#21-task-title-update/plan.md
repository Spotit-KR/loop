# Task 제목 수정 Mutation 추가 계획

> Issue: #21

## 단계

- [x] 1단계: Domain — TaskCommand.UpdateTitle 추가
- [x] 2단계: Domain — TaskRepository.updateTitle 추가
- [x] 3단계: Application — TaskService.updateTitle TDD (RED → GREEN → REFACTOR)
- [x] 4단계: Infrastructure — ExposedTaskRepository.updateTitle 구현
- [x] 5단계: Presentation — GraphQL 스키마 + DataFetcher 추가
- [x] 6단계: DGS Codegen 빌드 및 전체 테스트 검증

## PR 리뷰 반영 — updateTask 통합 리팩토링

- [x] 7단계: Domain — TaskCommand.UpdateStatus + UpdateTitle → Update 통합
- [x] 8단계: Domain — TaskRepository.updateStatus + updateTitle → update 통합
- [x] 9단계: Application — TaskService TDD (기존 테스트 수정 + 새 케이스)
- [x] 10단계: Infrastructure — ExposedTaskRepository.update 통합 구현
- [x] 11단계: Presentation — GraphQL 스키마 updateTask 통합 + DataFetcher 수정
- [x] 12단계: DGS Codegen 빌드 및 전체 테스트 검증
