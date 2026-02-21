# Sprint 1-2: Auth BC 구현 계획

## 개요

Auth BC의 구현 계획서. auth-planner(opus)가 수립, 리더가 승인.

## 파일 구조

### Domain Layer (`auth/domain/`)
- `model/AccessToken.kt` — Access Token VO
- `model/RefreshToken.kt` — Refresh Token VO
- `model/Password.kt` — 비밀번호 VO (원본, 유효성 검증)
- `model/AuthCommand.kt` — Command sealed interface (Register, Login, Refresh, Logout)
- `model/AuthMember.kt` — Auth BC의 멤버 읽기 모델
- `model/StoredRefreshToken.kt` — 저장된 리프레시 토큰 모델
- `repository/AuthMemberRepository.kt` — 멤버 데이터 접근 인터페이스
- `repository/RefreshTokenRepository.kt` — 리프레시 토큰 저장소 인터페이스

### Application Layer (`auth/application/`)
- `dto/AuthTokenDto.kt` — 토큰 응답 DTO
- `service/RegisterService.kt` — 회원가입
- `service/LoginService.kt` — 로그인
- `service/RefreshService.kt` — 토큰 갱신
- `service/LogoutService.kt` — 로그아웃

### Infrastructure Layer (`auth/infrastructure/`)
- `persistence/AuthMemberRepositoryImpl.kt` — 멤버 저장소 구현
- `persistence/RefreshTokenRepositoryImpl.kt` — 리프레시 토큰 저장소 구현
- `persistence/RefreshTokensTable.kt` — Exposed 테이블 정의
- `security/JwtTokenProvider.kt` — JWT 생성/검증
- `security/JwtAuthenticationFilter.kt` — JWT 인증 필터
- `security/JwtProperties.kt` — JWT 설정 프로퍼티

### Presentation Layer (`auth/presentation/`)
- `request/RegisterRequest.kt`, `LoginRequest.kt`, `RefreshRequest.kt`
- `response/AuthTokenResponse.kt`
- `controller/AuthController.kt`

### Common 추가
- `common/presentation/CurrentMemberId.kt` — 어노테이션
- `common/config/CurrentMemberIdArgumentResolver.kt` — 리졸버

### DB 마이그레이션
- `V4__create_refresh_tokens_table.sql` (V1~V3은 Foundation에서 사용)

## JWT 전략

| 항목 | 설정 |
|------|------|
| 라이브러리 | JJWT 0.12.6 |
| 알고리즘 | HMAC-SHA256 |
| Access Token 만료 | 15분 |
| Refresh Token 만료 | 15일 |
| Refresh Token Rotation | 갱신 시 기존 삭제 + 새 발급 |
| 저장소 | DB (refresh_tokens 테이블) |

## 보안 고려사항

- JWT Secret: 환경변수 관리, 256-bit 이상
- 로그인 실패: 이메일/비밀번호 구분 없이 동일 에러 메시지 (정보 노출 방지)
- BCrypt: Spring Security 기본 강도 10
- CSRF 비활성화 (Stateless API)
- Session: STATELESS

## 주요 설계 결정

| 결정 | 내용 |
|------|------|
| Email/Nickname | common/domain/으로 이동 (Member BC와 공유) |
| MembersTable | common/infrastructure/persistence/로 이동 (중복 방지) |
| 예외 처리 | common에 공통 예외 베이스 + @RestControllerAdvice |
| BC 간 통신 | Auth가 자체 AuthMember 모델로 members 테이블 직접 접근 |

## TDD 테스트 대상

- Domain: PasswordTest, AccessTokenTest, RefreshTokenTest
- Application: RegisterServiceTest, LoginServiceTest, RefreshServiceTest, LogoutServiceTest
