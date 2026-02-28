# GraphQL 예외 처리 표준 적용 계획

## 단계

- [x] 1단계: `Exceptions.kt` 생성 (sealed LoopException + 6개 하위 클래스)
- [x] 2단계: Domain VO 마이그레이션 (MemberId, LoginId, Nickname) + 테스트 업데이트
- [x] 3단계: AuthService 마이그레이션 + 테스트 업데이트
- [x] 4단계: AuthorizeArgumentResolver 마이그레이션
- [x] 5단계: `GraphQlExceptionHandler.kt` 생성
- [x] 6단계: 전체 테스트 실행 검증
