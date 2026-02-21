# Sprint 1 리뷰 — 잔여 이슈 (배포 전 / 추후 개선)

> Sprint 1 아키텍처·보안 리뷰에서 발견된 이슈 중 즉시 수정 4건은 처리 완료.
> 아래는 배포 전 수정 권장 및 추후 개선 항목.

---

## 배포 전 수정 권장 (Medium)

### 1. CORS 전체 허용
- **위치**: `common/config/WebMvcConfig.kt`
- **문제**: `allowedOriginPatterns("*")` + `allowCredentials(true)` — 모든 도메인에서 인증 요청 가능
- **권고**: 프로덕션 배포 전 허용 오리진을 명시적으로 지정 (`*.loop.io` 등)

### 2. /auth/logout, /auth/refresh 인증 없이 접근 가능
- **위치**: `SecurityConfig.kt` (`/auth/**` 전체 permitAll)
- **문제**: 타인의 refresh token 값을 알면 강제 로그아웃 가능 (추측 어렵지만 원칙적 위험)
- **권고**: `/auth/login`, `/auth/register`만 permitAll, 나머지는 설계 결정 문서화

### 3. 비밀번호 복잡도 정책 미흡
- **위치**: `auth/domain/model/Password.kt`, `member/domain/model/Password.kt`
- **문제**: 길이(8~100자)만 검증, 대소문자·숫자·특수문자 조합 없음
- **권고**: 최소 숫자+영문 조합 등 기본 복잡도 정책 추가

### 4. 내부 오류 메시지 클라이언트 노출
- **위치**: `GlobalExceptionHandler.kt`
- **문제**: VO init 블록의 IllegalArgumentException 메시지가 그대로 응답 (내부 검증 규칙 노출)
- **권고**: 에러 코드(enum) + 사용자 친화적 메시지 분리

---

## 추후 개선 (Low / Minor)

### 5. Auth 전용 예외가 common에 위치 [아키텍처 Minor]
- **위치**: `common/exception/`
- **문제**: `InvalidCredentialsException`, `InvalidRefreshTokenException`은 Auth BC 전용
- **권고**: `auth/domain/` 또는 `auth/application/`으로 이동 (DuplicateEmailException은 공유 가능하므로 common 유지 가능)

### 6. ProfileImageUrl URL 형식 검증 없음 [보안 Low]
- **위치**: `member/domain/model/ProfileImageUrl.kt`
- **문제**: 공백 아님 + 길이만 검증, `javascript:alert(1)` 등 입력 가능
- **권고**: `http://` 또는 `https://` 시작 여부 검증 추가

### 7. NoSuchElementException에 내부 ID 노출 [보안 Low]
- **위치**: 여러 서비스/리포지토리
- **문제**: `"Member not found: ${memberId.value}"` — 내부 DB ID가 에러 응답에 포함 가능
- **권고**: ID 값 없는 일반 메시지 사용

### 8. 중복된 Password VO [아키텍처 Minor]
- **문제**: `member/domain/model/Password`와 `auth/domain/model/Password`가 동일 로직
- **권고**: common/domain/으로 통합할지 팀 내 판단 필요

### 9. RefreshService 시간 의존성 [아키텍처 Info]
- **문제**: `LocalDateTime.now()` 직접 호출, 테스트에서 시간 제어 어려움
- **권고**: Clock 의존성 주입 고려
