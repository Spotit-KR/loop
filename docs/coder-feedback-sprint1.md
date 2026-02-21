# Sprint 1 Coder 피드백 (검토 필요)

> Sprint 1의 foundation-coder, member-coder, auth-coder가 작업 중 겪은 이슈와 개선 제안.
> 리더 검토 후 프로젝트 컨벤션/문서에 반영 여부 결정 필요.

---

## foundation-coder 피드백

### 1. CORS: allowedOrigins vs allowedOriginPatterns
- **상황**: 계획에 "모든 origin 허용 + credentials 허용"이 함께 명시됨
- **문제**: Spring은 `allowCredentials(true)` + `allowedOrigins("*")` 조합을 허용하지 않음
- **해결**: `allowedOriginPatterns("*")`로 자체 판단하여 변경
- **제안**: CORS 설정 시 이 제약을 명시할 것

### 2. 기본 실행 프로파일 미지정
- **상황**: `application.yml`에는 datasource 없고 `application-dev.yml`에만 H2 설정
- **문제**: 프로파일 없이 실행하면 datasource 오류
- **제안**: `SPRING_PROFILES_ACTIVE=dev` 기본 사용 여부 등 팀 컨벤션 필요

### 3. Spring Security 7 API 변경
- **문제**: `authorizeRequests()`는 deprecated, `authorizeHttpRequests()` 사용 필요
- **제안**: Spring Boot 4 / Security 7 기준임을 계획에 명시

### 4. ktlint 자동 포맷
- **상황**: 빌드 시 ktlint가 코드 스타일을 자동 변경 (예: value class 파라미터 멀티라인화)
- **제안**: 의도된 동작임을 coder에게 안내

---

## member-coder 피드백

### 1. Exposed 1.0.0 패키지 변경 (가장 큰 병목)
- **문제**: 1.0.0에서 패키지가 `org.jetbrains.exposed.sql.*` → `org.jetbrains.exposed.v1.*`로 변경
- **추가 이슈**: EntityID 컬럼 비교 시 `EntityID(value, Table)` 래핑 필요
- **제안**: architecture.md에 Exposed 1.0.0 패키지 규칙 명시

### 2. kotlinx.datetime ↔ java.time 변환
- **문제**: 도메인 모델은 `java.time.LocalDateTime`, Exposed는 `kotlinx.datetime.LocalDateTime` 사용
- **해결**: `toJavaLocalDateTime()`, `toKotlinLocalDateTime()` 변환 함수 사용
- **제안**: Infrastructure 구현 패턴으로 문서화
```kotlin
// Exposed → Domain
row[table.createdAt].toJavaLocalDateTime()
// 저장 시
Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
```

### 3. @CurrentMemberId stub 범위
- **문제**: "하드코딩 또는 예외" 중 어떤 방식인지 불명확
- **제안**: "예외를 던진다"로 확정 권장 (하드코딩 시 통합테스트 혼선)

### 4. MemberId 양수 제약과 테스트 데이터
- **문제**: `MemberId(0L)`이나 음수 사용 시 예외 발생
- **제안**: 테스트 데이터 작성 가이드에 VO 제약 사항 명시

### 5. Repository save() 멀티 커맨드 처리
- **문제**: `save(command: MemberCommand)`가 Register와 UpdateProfile 모두 처리하는 설계 의도가 계획에 없음
- **제안**: when 분기 패턴이 의도된 설계임을 명시

---

## auth-coder 피드백

### 1. JWT_SECRET 기본값 형식
- **문제**: 계획의 기본값 "your-256-bit-base64-encoded-secret-key-for-dev"가 유효한 Base64가 아니라 디코딩 실패
- **제안**: "기본값은 반드시 유효한 Base64 인코딩 문자열이어야 함" 명시

### 2. @ConfigurationPropertiesScan 추가 필요
- **문제**: `@ConfigurationProperties` 클래스 사용 시 `ServerApplication`에 `@ConfigurationPropertiesScan` 필요
- **제안**: 계획에 Application 클래스 수정 사항 포함

### 3. 테스트 application.yml 업데이트
- **문제**: Infrastructure에서 `@ConfigurationProperties` Bean 추가 시 테스트 yml에도 설정 필요
- **제안**: 새 설정 추가 시 테스트 yml도 함께 업데이트하도록 안내

### 4. Kotlin non-nullable Long + Spring Config Binding
- **문제**: Kotlin data class의 non-nullable `Long` 필드에 null 바인딩 시 NPE 발생, 디버깅 어려움
- **제안**: "ConfigurationProperties 클래스의 non-nullable 필드는 설정값 누락 시 NPE 발생 주의" 경고 추가
