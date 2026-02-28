# JWT 인증 ArgumentResolver 맥락

## 배경
회원가입/로그인으로 JWT를 발급하지만, 이후 요청에서 토큰을 검증하는 메커니즘이 없다. 서블릿 필터 대신 DGS ArgumentResolver로 DataFetcher 파라미터 단위 인증을 구현한다.

## 목표
- `@Authorize(require = true)` → 인증 필수, 토큰 없으면 예외
- `@Authorize(require = false)` → 토큰 없으면 null 반환
- DataFetcher 메서드 파라미터에 선언적으로 인증 처리

## 제약조건
- DGS `com.netflix.graphql.dgs.internal.method.ArgumentResolver` 인터페이스 구현
- `DgsContext.getRequestData(dfe)?.headers`로 헤더 접근
- `common/config/`에 위치 (모든 BC에서 사용)
- 서블릿 필터 방식 사용하지 않음

## 관련 문서
- docs/architecture.md — 아키텍처 규칙
- docs/spring-security-7.md — Security 7 API 레퍼런스
