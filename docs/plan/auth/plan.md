# auth BC 개발 계획

## 단계

- [x] 0단계: 사전 설정 (빌드/의존성/마이그레이션/스키마/application.properties)
- [x] 1단계: Common 모듈 (MemberId VO)
- [x] 2단계: Domain Layer (TDD — LoginId, Nickname, Member, MemberCommand, MemberRepository)
- [x] 3단계: Application Layer (TDD — AuthTokenDto, AuthService)
- [x] 4단계: Infrastructure Layer (MemberTable, ExposedMemberRepository, SecurityConfig, JwtTokenProvider)
- [x] 5단계: Presentation Layer (AuthDataFetcher)
- [x] 6단계: 검증 (전체 테스트, ArchitectureTest, 빌드)
