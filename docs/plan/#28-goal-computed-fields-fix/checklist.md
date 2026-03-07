# goal BC computed fields 하드코딩 수정 검증 체크리스트

## 필수 항목
- [x] 아키텍처 원칙 준수 (docs/architecture.md 기준)
- [x] 레이어 의존성 규칙 위반 없음
- [x] GoalDataFetcher에서 하드코딩 0 값 문서화 (Codegen 제약으로 constructor 값 제거 불가, @DgsData 오버라이드 동작 명시)
- [x] computed fields가 task BC의 DataLoader를 통해 실제 값으로 해석됨 (GoalDataFetcherTest로 검증)
- [x] 모든 테스트 통과
- [x] 기존 테스트 깨지지 않음

## 선택 항목 (해당 시)
- [ ] GraphQL 스키마 타입 소유권 정리 (goal BC → task BC extend) — goal BC만 수정 가능 제약으로 미수행
