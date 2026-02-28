# GraphQL 예외 처리 설계

이 문서는 Loop Server의 계층적 예외 구조와 GraphQL 표준 에러 응답 설계를 제안합니다.

**참고 스펙**:
- [GraphQL Spec — Errors](https://spec.graphql.org/draft/#sec-Errors)
- [Netflix DGS — Error Handling](https://netflix.github.io/dgs/error-handling/)

---

## 1. GraphQL 에러 스펙 요약

### 1.1 에러 응답 형식

GraphQL 스펙은 에러를 `errors` 배열로 반환합니다. `data`와 `errors`가 동시에 존재할 수 있어 **부분 성공(partial data)** 을 허용합니다.

```json
{
  "data": { "task": null },
  "errors": [
    {
      "message": "Task not found: 42",
      "locations": [{ "line": 2, "column": 3 }],
      "path": ["task"],
      "extensions": {
        "errorType": "NOT_FOUND",
        "errorDetail": "ENTITY_NOT_FOUND",
        "classification": "DataFetchingException"
      }
    }
  ]
}
```

### 1.2 에러 분류 (GraphQL Spec)

| 분류 | 발생 시점 | data 필드 |
|------|-----------|-----------|
| **Request Error** | 파싱/검증 단계 | `data` 없음 |
| **Field Error** | 리졸버 실행 중 | `data` 존재 (부분 성공) |

- Request Error: 잘못된 쿼리 문법, 검증 실패 → DGS 프레임워크가 자동 처리
- Field Error: 리졸버에서 throw된 예외 → **우리가 처리해야 하는 영역**

### 1.3 DGS ErrorType

DGS는 `com.netflix.graphql.types.errors.ErrorType`을 통해 표준 분류를 제공합니다.

| ErrorType | HTTP 유사 코드 | 용도 |
|-----------|---------------|------|
| `BAD_REQUEST` | 400 | 잘못된 입력값 |
| `UNAUTHENTICATED` | 401 | 인증 필요/실패 |
| `PERMISSION_DENIED` | 403 | 권한 부족 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `FAILED_PRECONDITION` | 409 | 상태 충돌 (중복, 비즈니스 규칙 위반) |
| `UNAVAILABLE` | 503 | 외부 서비스 장애 |
| `INTERNAL` | 500 | 예상치 못한 서버 오류 |

---

## 2. 현재 상태 분석

### 현재 예외 처리 방식

| 위치 | 현재 방식 | 문제점 |
|------|-----------|--------|
| VO (`init` 블록) | `require()` → `IllegalArgumentException` | 클라이언트에 `INTERNAL` 에러로 전달됨 |
| Service (`findById`) | `NoSuchElementException` | 클라이언트에 `INTERNAL` 에러로 전달됨 |
| 인증/인가 | 미구현 | — |
| DataFetcher | 예외 처리 없음 | 모든 예외가 `INTERNAL`로 노출 |

**핵심 문제**: DGS 기본 핸들러는 미등록 `RuntimeException`을 모두 `INTERNAL`로 처리합니다. 클라이언트가 입력값 오류와 서버 오류를 구분할 수 없습니다.

---

## 3. 예외 계층 설계

### 3.1 설계 원칙

1. **Domain 순수성**: Domain 예외는 외부 프레임워크에 의존하지 않음 (순수 Kotlin)
2. **단일 매핑 지점**: 예외 → GraphQL 에러 변환은 `@ControllerAdvice` 한 곳에서만
3. **최소 계층**: 과도한 예외 트리를 만들지 않음. 실제 필요한 분류만 도입
4. **extensions 활용**: `errorType`(대분류) + `errorDetail`(소분류)로 클라이언트 분기 지원

### 3.2 예외 클래스 계층

```text
LoopException (abstract, sealed)
├── InvalidInputException        → BAD_REQUEST
├── EntityNotFoundException      → NOT_FOUND
├── DuplicateEntityException     → FAILED_PRECONDITION
├── BusinessRuleException        → FAILED_PRECONDITION
├── AuthenticationException      → UNAUTHENTICATED
└── AccessDeniedException        → PERMISSION_DENIED
```

**미등록 RuntimeException** → `INTERNAL` (DGS 기본 핸들러 위임)

### 3.3 위치

```text
common/
└── domain/
    └── exception/
        └── Exceptions.kt    # 모든 예외 클래스를 한 파일에 정의
```

**이유**: 예외 클래스는 모든 BC에서 공통으로 사용하므로 `common/domain/`에 배치합니다. 파일이 짧으므로 (각 클래스가 1-5줄) 한 파일로 유지합니다.

### 3.4 코드

```kotlin
package kr.io.team.loop.common.domain.exception

/**
 * Loop 프로젝트 공통 예외 베이스.
 * Domain 레이어에 위치하므로 프레임워크 의존 없음.
 */
sealed class LoopException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

/** 잘못된 입력값. VO 검증 실패, 파라미터 오류. */
class InvalidInputException(
    message: String,
    cause: Throwable? = null,
) : LoopException(message, cause)

/** 엔티티를 찾을 수 없음. */
class EntityNotFoundException(
    message: String,
) : LoopException(message)

/** 엔티티 중복. 유니크 제약 위반. */
class DuplicateEntityException(
    message: String,
) : LoopException(message)

/** 비즈니스 규칙 위반. 도메인 불변식 위반. */
class BusinessRuleException(
    message: String,
) : LoopException(message)

/** 인증 실패. 토큰 만료, 잘못된 자격증명. */
class AuthenticationException(
    message: String,
    cause: Throwable? = null,
) : LoopException(message, cause)

/** 권한 부족. 인증은 되었으나 해당 리소스에 접근 불가. */
class AccessDeniedException(
    message: String,
) : LoopException(message)
```

> `sealed class`이므로 허용된 하위 타입이 컴파일 타임에 결정됩니다. 새 예외가 필요하면 이 파일에만 추가합니다.

---

## 4. 레이어별 예외 사용 패턴

### 4.1 Domain Layer — VO 검증

기존 `require()` 호출을 `InvalidInputException`으로 전환합니다.

```kotlin
@JvmInline
value class TaskTitle(val value: String) {
    init {
        if (value.isBlank()) throw InvalidInputException("TaskTitle must not be blank")
        if (value.length > 200) throw InvalidInputException("TaskTitle must not exceed 200 characters")
    }
}
```

**선택지 비교**:

| 방식 | 장점 | 단점 |
|------|------|------|
| A. `require()` 유지 + 핸들러에서 `IllegalArgumentException` 매핑 | 코드 변경 없음 | `IllegalArgumentException`이 범용이라 오탐 가능 |
| **B. `InvalidInputException` 직접 throw (권장)** | **의미가 명확, sealed로 안전** | VO마다 import 필요 |

**B 방식을 권장합니다.** `IllegalArgumentException`은 Kotlin 표준 라이브러리/프레임워크 내부에서도 발생하므로, 이를 무조건 `BAD_REQUEST`로 매핑하면 서버 버그가 클라이언트 입력 오류로 오인될 수 있습니다.

### 4.2 Application Layer — Service

```kotlin
@Service
class TaskService(
    private val taskRepository: TaskRepository,
) {
    @Transactional(readOnly = true)
    fun findById(id: TaskId): Task {
        return taskRepository.findById(id)
            ?: throw EntityNotFoundException("Task not found: ${id.value}")
    }

    @Transactional
    fun create(command: TaskCommand.Create): Task {
        if (taskRepository.existsByTitle(command.title)) {
            throw DuplicateEntityException("Task with title '${command.title.value}' already exists")
        }
        return taskRepository.save(command)
    }
}
```

### 4.3 Presentation Layer — DataFetcher

DataFetcher에서는 예외를 **잡지 않습니다**. Service/Domain에서 발생한 예외가 그대로 DGS로 전파되어 `@ControllerAdvice`에서 처리됩니다.

```kotlin
@DgsComponent
class TaskDataFetcher(
    private val taskService: TaskService,
) {
    @DgsMutation
    fun createTask(@InputArgument input: CreateTaskInput): Task {
        // InvalidInputException → VO 생성 시 자동 발생
        // DuplicateEntityException → Service에서 발생
        // 모두 @ControllerAdvice가 처리 → DataFetcher는 try-catch 불필요
        val command = TaskCommand.Create(
            title = TaskTitle(input.title),
            description = input.description?.let { TaskDescription(it) },
        )
        return taskService.create(command)
    }
}
```

---

## 5. GraphQL 에러 매핑 — `@ControllerAdvice`

### 5.1 핸들러 위치

```text
common/
└── config/
    └── GraphQlExceptionHandler.kt
```

### 5.2 코드

```kotlin
package kr.io.team.loop.common.config

import com.netflix.graphql.types.errors.ErrorType
import graphql.GraphQLError
import kr.io.team.loop.common.domain.exception.*
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler
import org.springframework.web.bind.annotation.ControllerAdvice

@ControllerAdvice
class GraphQlExceptionHandler {

    @GraphQlExceptionHandler
    fun handleInvalidInput(ex: InvalidInputException): GraphQLError {
        return GraphQLError.newError()
            .errorType(ErrorType.BAD_REQUEST)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleEntityNotFound(ex: EntityNotFoundException): GraphQLError {
        return GraphQLError.newError()
            .errorType(ErrorType.NOT_FOUND)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleDuplicateEntity(ex: DuplicateEntityException): GraphQLError {
        return GraphQLError.newError()
            .errorType(ErrorType.FAILED_PRECONDITION)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleBusinessRule(ex: BusinessRuleException): GraphQLError {
        return GraphQLError.newError()
            .errorType(ErrorType.FAILED_PRECONDITION)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleAuthentication(ex: AuthenticationException): GraphQLError {
        return GraphQLError.newError()
            .errorType(ErrorType.UNAUTHENTICATED)
            .message(ex.message)
            .build()
    }

    @GraphQlExceptionHandler
    fun handleAccessDenied(ex: AccessDeniedException): GraphQLError {
        return GraphQLError.newError()
            .errorType(ErrorType.PERMISSION_DENIED)
            .message(ex.message)
            .build()
    }
}
```

**미처리 예외**: 위 핸들러에 매핑되지 않는 `RuntimeException`은 DGS 기본 핸들러(`DefaultDataFetcherExceptionHandler`)가 `INTERNAL`로 처리합니다.

### 5.3 응답 예시

**InvalidInputException** (VO 검증 실패):
```json
{
  "errors": [{
    "message": "TaskTitle must not be blank",
    "path": ["createTask"],
    "extensions": { "errorType": "BAD_REQUEST", "classification": "DataFetchingException" }
  }],
  "data": { "createTask": null }
}
```

**EntityNotFoundException**:
```json
{
  "errors": [{
    "message": "Task not found: 42",
    "path": ["task"],
    "extensions": { "errorType": "NOT_FOUND", "classification": "DataFetchingException" }
  }],
  "data": { "task": null }
}
```

---

## 6. 예외 매핑 전체 요약

| 예외 클래스 | ErrorType | 발생 위치 | 예시 상황 |
|-------------|-----------|-----------|-----------|
| `InvalidInputException` | `BAD_REQUEST` | Domain VO, DataFetcher 인자 변환 | 빈 제목, 길이 초과, 잘못된 형식 |
| `EntityNotFoundException` | `NOT_FOUND` | Application Service | `findById` 결과 없음 |
| `DuplicateEntityException` | `FAILED_PRECONDITION` | Application Service | 중복 loginId로 회원가입 |
| `BusinessRuleException` | `FAILED_PRECONDITION` | Domain/Application | 완료된 Task 재완료 시도 |
| `AuthenticationException` | `UNAUTHENTICATED` | Application Service (auth BC) | 잘못된 비밀번호, 만료 토큰 |
| `AccessDeniedException` | `PERMISSION_DENIED` | Application Service | 타인의 Task 수정 시도 |
| 기타 `RuntimeException` | `INTERNAL` | 어디서든 | NPE, DB 연결 실패 등 |

---

## 7. 아키텍처 적합성

### Clean Architecture 준수 확인

```text
Domain Layer (common/domain/exception/)
  └── LoopException, InvalidInputException, ... (순수 Kotlin, 프레임워크 의존 없음)

Application Layer
  └── Service에서 Domain 예외를 throw (Domain에만 의존)

Presentation Layer (common/config/)
  └── @ControllerAdvice가 Domain 예외 → GraphQL 에러 변환 (Domain + Framework 의존)
```

- Domain 예외는 `RuntimeException`만 상속 → 프레임워크 무관
- GraphQL 에러 매핑은 Presentation/Config에서만 수행 → Domain 오염 없음
- `sealed class`로 예외 목록이 닫혀 있음 → `when` 분기 시 컴파일 안전성

### BC 간 규칙 준수

- 모든 예외가 `common/domain/exception/`에 위치 → 모든 BC에서 import 가능
- BC별 커스텀 예외 없음 → BC 간 예외 의존 발생하지 않음

---

## 8. 기존 코드 마이그레이션

현재 코드에서 변경이 필요한 부분:

| 현재 코드 | 변경 후 |
|-----------|---------|
| VO의 `require(cond) { msg }` | `if (!cond) throw InvalidInputException(msg)` |
| Service의 `?: throw NoSuchElementException(msg)` | `?: throw EntityNotFoundException(msg)` |

VO 마이그레이션 예시 (`LoginId.kt`):

```kotlin
// Before
@JvmInline
value class LoginId(val value: String) {
    init {
        require(value.isNotBlank()) { "LoginId must not be blank" }
        require(value.length <= 50) { "LoginId must not exceed 50 characters" }
    }
}

// After
@JvmInline
value class LoginId(val value: String) {
    init {
        if (value.isBlank()) throw InvalidInputException("LoginId must not be blank")
        if (value.length > 50) throw InvalidInputException("LoginId must not exceed 50 characters")
    }
}
```

---

## 9. 향후 확장 포인트

현재는 도입하지 않지만, 필요 시 추가할 수 있는 요소들:

| 확장 | 도입 시점 | 방식 |
|------|-----------|------|
| `errorDetail` (소분류 코드) | 클라이언트가 같은 ErrorType 내에서 세분화 필요 시 | `extensions`에 `errorDetail` 문자열 추가 |
| 에러 코드 enum | API가 안정화되고 클라이언트가 코드 기반 분기 필요 시 | `extensions`에 `code: "TASK_001"` 추가 |
| 다국어 메시지 | i18n 요구 시 | 예외에 `messageKey`를 두고 핸들러에서 번역 |
| 필드별 검증 에러 | 폼 검증에서 여러 필드 오류를 한 번에 반환할 때 | `extensions`에 `fieldErrors` 배열 추가 |
