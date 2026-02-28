# JWT 인증 ArgumentResolver 구현 계획

## 단계

- [x] 1단계: JjwtTokenProvider 테스트 작성 (RED) — validateToken, getMemberIdFromToken
- [x] 2단계: JwtTokenProvider 인터페이스 확장 + JjwtTokenProvider 구현 (GREEN)
- [x] 3단계: @Authorize 어노테이션 + AuthorizeArgumentResolver 생성
- [x] 4단계: 빌드 검증 (./gradlew clean build)
