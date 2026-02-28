# auth BC 검증 체크리스트

## 필수 항목
- [x] 아키텍처 원칙 준수 (docs/architecture.md 기준)
- [x] 레이어 의존성 규칙 위반 없음 (ArchitectureTest 통과)
- [x] 테스트 코드 작성 완료 (Domain, Application 필수)
- [x] 모든 테스트 통과
- [x] 기존 테스트 깨지지 않음
- [x] `./gradlew build` 성공

## 선택 항목 (해당 시)
- [x] Flyway 마이그레이션 작성 (V1__Create_member_table.sql)
- [x] GraphQL register/login mutation 스키마 정의 완료
