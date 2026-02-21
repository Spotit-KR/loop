# Sprint 1-2: Auth BC 구현 내용

## 개요

JWT 기반 인증(Auth BC) 구현. 회원가입, 로그인, 토큰 갱신, 로그아웃 기능 제공.

## 주요 변경 사항

### 리팩토링

- `Email`, `Nickname` VO를 `member/domain/model/` → `common/domain/`으로 이동 (Auth BC 공유 VO)
- `MembersTable`을 `member/infrastructure/persistence/` → `common/infrastructure/persistence/`로 이동 (Auth BC에서 사용)
- 관련 import 전체 업데이트 (Member BC, 테스트 포함)

### 새 파일

#### Domain Layer (`auth/domain/`)

| 파일 | 설명 |
|------|------|
| `model/Password.kt` | 원시 비밀번호 VO (8~100자) |
| `model/AccessToken.kt` | 액세스 토큰 VO |
| `model/RefreshToken.kt` | 리프레시 토큰 VO |
| `model/AuthCommand.kt` | Register/Login/Refresh/Logout sealed interface |
| `model/AuthMember.kt` | Auth BC 멤버 읽기 모델 |
| `model/StoredRefreshToken.kt` | DB에 저장된 리프레시 토큰 |
| `repository/AuthMemberRepository.kt` | 인터페이스 |
| `repository/RefreshTokenRepository.kt` | 인터페이스 |
| `service/TokenProvider.kt` | 토큰 생성/만료 추상화 인터페이스 |

#### Application Layer (`auth/application/`)

| 파일 | 설명 |
|------|------|
| `dto/AuthTokenDto.kt` | 토큰 쌍 반환 DTO |
| `service/RegisterService.kt` | 이메일 중복 검사 → 해싱 → 생성 → JWT 발급 |
| `service/LoginService.kt` | 이메일 조회 → 비밀번호 검증 → JWT 발급 |
| `service/RefreshService.kt` | 토큰 rotation → 새 쌍 발급 |
| `service/LogoutService.kt` | 리프레시 토큰 삭제 |

#### Infrastructure Layer (`auth/infrastructure/`)

| 파일 | 설명 |
|------|------|
| `security/JwtProperties.kt` | `@ConfigurationProperties(prefix="jwt")` |
| `security/JwtTokenProvider.kt` | JJWT 0.12.x 기반 토큰 생성/검증, `TokenProvider` 구현 |
| `security/JwtAuthenticationFilter.kt` | `OncePerRequestFilter`, Bearer 토큰 추출 → SecurityContext 설정 |
| `persistence/RefreshTokensTable.kt` | Exposed `LongIdTable("refresh_tokens")` |
| `persistence/RefreshTokenRepositoryImpl.kt` | `RefreshTokenRepository` 구현 |
| `persistence/AuthMemberRepositoryImpl.kt` | `AuthMemberRepository` 구현 (MembersTable 사용) |

#### Presentation Layer (`auth/presentation/`)

- `request/`: `RegisterRequest`, `LoginRequest`, `RefreshRequest`, `LogoutRequest`
- `response/`: `AuthTokenResponse`
- `controller/AuthController`: `POST /auth/register|login|refresh|logout`

#### Common 수정

- `SecurityConfig.kt`: `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 추가
- `CurrentMemberIdArgumentResolver.kt`: `authentication.principal as Long` 방식으로 변경
- `exception/DuplicateEmailException.kt`, `InvalidCredentialsException.kt`, `InvalidRefreshTokenException.kt`
- `exception/GlobalExceptionHandler.kt`: `@RestControllerAdvice` 전역 예외 처리

#### DB 마이그레이션

- `V4__create_refresh_tokens_table.sql`: `refresh_tokens` 테이블 + `member_id` 인덱스

### JWT 설정 (application.yml)

```yaml
jwt:
  secret: ${JWT_SECRET:dGVzdFNlY3JldEtleUZvclVuaXRUZXN0aW5nMTIzNDU=}
  access-token-expiry: 900000        # 15분
  refresh-token-expiry: 1296000000   # 15일
```

> **주의**: 운영 환경에서는 반드시 `JWT_SECRET` 환경변수를 256비트 이상의 Base64 인코딩된 값으로 설정해야 합니다.

## TDD 적용 내역

### Domain Layer (RED → GREEN → REFACTOR)

- `PasswordTest`: 8자 이상 성공, 7자 이하 실패, 100자 초과 실패
- `AccessTokenTest`: 비어있지 않은 문자열 성공, 빈/공백 문자열 실패
- `RefreshTokenTest`: 비어있지 않은 문자열 성공, 빈/공백 문자열 실패

### Application Layer (RED → GREEN → REFACTOR)

- `RegisterServiceTest`: 성공, 이메일 중복 시 `DuplicateEmailException`
- `LoginServiceTest`: 성공, 이메일 없음/비밀번호 불일치 시 `InvalidCredentialsException` (동일 예외로 보안 유지)
- `RefreshServiceTest`: 성공(토큰 rotation 검증), 토큰 없음/만료 시 `InvalidRefreshTokenException`
- `LogoutServiceTest`: 성공 (deleteByToken 호출 검증)

## API 엔드포인트

| Method | Path | 설명 | Auth |
|--------|------|------|------|
| POST | `/auth/register` | 회원가입 | 불필요 |
| POST | `/auth/login` | 로그인 | 불필요 |
| POST | `/auth/refresh` | 토큰 갱신 | 불필요 |
| POST | `/auth/logout` | 로그아웃 | 불필요 |

## 설계 결정 사항

1. **TokenProvider 인터페이스**: `JwtTokenProvider`는 Infrastructure 레이어에 있어 Application에서 직접 참조하면 의존성 방향이 역전됨. `auth/domain/service/TokenProvider` 인터페이스를 정의하여 Application → Domain 의존성을 유지하고, Infrastructure에서 구현체 제공.

2. **보안**: 이메일 없음과 비밀번호 불일치에 동일한 `InvalidCredentialsException` 사용하여 사용자 열거 공격 방지.

3. **토큰 Rotation**: `RefreshService`에서 기존 리프레시 토큰 삭제 후 새 토큰 발급 (단일 사용 원칙).

4. **`@ConfigurationPropertiesScan`**: `ServerApplication`에 추가하여 `JwtProperties` 자동 스캔.
