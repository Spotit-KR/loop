# Spring Security 7 API 레퍼런스

이 프로젝트는 Spring Security 7.x를 사용합니다. 6.x 대비 주요 변경사항입니다.
Security 관련 코드(`common/config/` 등) 작성 전 반드시 참고합니다.

## HttpSecurity DSL

`and()` 체이닝 제거. **lambda DSL만 사용합니다.**

```kotlin
http {
    authorizeHttpRequests {
        authorize("/api/auth/**", permitAll)
        authorize(anyRequest, authenticated)
    }
    csrf { disable() }
    sessionManagement {
        sessionCreationPolicy = SessionCreationPolicy.STATELESS
    }
    oauth2ResourceServer {
        jwt { }
    }
}
```

## Request Matcher

`antMatchers()`, `mvcMatchers()` 제거 → `requestMatchers()` (PathPatternRequestMatcher)

```kotlin
// 금지
.antMatchers("/api/**")
.mvcMatchers("/users/{id}")

// 사용
.requestMatchers("/api/**")
.requestMatchers("/users/{id}")
```

## Authorization

`authorizeRequests()` 제거 → `authorizeHttpRequests()`

```kotlin
// 금지
http.authorizeRequests()

// 사용
http.authorizeHttpRequests()
```

## JWT

- JWT 타입 검증(`typ` 헤더)이 기본 활성화
- `BearerTokenAuthenticationFilter`에서 `setBearerTokenResolver()` deprecated → `BearerTokenAuthenticationConverter`에서 설정

## Jackson 3

Security Jackson 모듈이 Jackson 3 기반으로 변경.

- `SecurityJackson2Modules` → `SecurityJacksonModules`
- `ObjectMapper` → `JsonMapper.Builder`

> 전체 마이그레이션 가이드: https://deepwiki.com/spring-projects/spring-security/9-migration-to-spring-security-7.0
