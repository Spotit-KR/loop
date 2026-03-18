# 로그인 회원 본인 정보 조회 (me) Query 검증 체크리스트

## 필수 항목
- [x] 아키텍처 원칙 준수 (docs/architecture.md 기준)
- [x] 레이어 의존성 규칙 위반 없음
- [x] 테스트 코드 작성 완료 (Domain — 해당 없음, Application 필수)
- [x] 모든 테스트 통과
- [x] 기존 테스트 깨지지 않음
- [x] password 필드가 GraphQL 응답에 노출되지 않음
- [x] DGS Codegen으로 GraphQL 타입 자동 생성 (수동 작성 없음)
