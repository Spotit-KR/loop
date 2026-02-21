# Sprint 2 아키텍처 규칙 위반 리팩토링

## 작업 개요

외부 분석에서 발견된 아키텍처 규칙 위반 3개 카테고리를 수정했습니다.
`./gradlew build` BUILD SUCCESSFUL 확인 완료.

---

## 카테고리 1: TaskQuery non-nullable 필드

### 문제

`TaskQuery`의 필드가 non-nullable로 선언되어 Query data class 규칙(`nullable 필드, 기본값 null`) 위반.
`MemberQuery`, `CategoryQuery`는 이미 규칙을 준수하고 있었으나 `TaskQuery`만 위반 상태였음.

### 수정 전/후

**`task/domain/model/TaskQuery.kt`**
```kotlin
// Before
data class TaskQuery(
    val memberId: MemberId,
    val taskDate: TaskDate,
)

// After
data class TaskQuery(
    val memberId: MemberId? = null,
    val taskDate: TaskDate? = null,
)
```

### 연쇄 수정 파일

| 파일 | 변경 내용 |
|------|-----------|
| `task/infrastructure/persistence/ExposedTaskRepository.kt` | `findAllWithCategoryInfo`: 직접 `.value` 접근 → `andWhere` 패턴으로 null 처리 |
| `task/application/service/GetTasksByDateService.kt` | `query.taskDate.value` → `requireNotNull(query.taskDate).value` |

---

## 카테고리 2: Service/Repository가 VO 직접 입력 수신

### 문제

Service 메서드가 VO(`MemberId`, `Email` 등)를 직접 파라미터로 받는 것은 아키텍처 규칙 위반.
"Presentation 이후 공용 입력은 Command/Query"여야 함.

### 수정 내역

#### 2-1. `GetCategoriesService`

```kotlin
// Before
fun execute(memberId: MemberId): List<CategoryDto>

// After
fun execute(query: CategoryQuery): List<CategoryDto>
```

- `CategoryController`: `getCategoriesService.execute(MemberId(memberId))` → `execute(CategoryQuery(memberId = MemberId(memberId)))`

#### 2-2. `GetMemberService`

```kotlin
// Before
fun execute(memberId: MemberId): MemberDto {
    val member = memberRepository.findById(memberId) ?: throw ...
    return MemberDto.from(member)
}

// After
fun execute(query: MemberQuery): MemberDto {
    val member = memberRepository.findAll(query).firstOrNull() ?: throw ...
    return MemberDto.from(member)
}
```

- `MemberController`: `getMemberService.execute(MemberId(memberId))` → `execute(MemberQuery(memberId = MemberId(memberId)))`

#### 2-3. `AuthMemberRepository` — `existsByEmail` 제거 + `AuthMemberQuery` 신규 생성

Auth BC에 Query 클래스가 없었으므로 신규 생성.

**신규: `auth/domain/model/AuthMemberQuery.kt`**
```kotlin
data class AuthMemberQuery(
    val email: Email? = null,
)
```

**`auth/domain/repository/AuthMemberRepository.kt`**
```kotlin
// Before
interface AuthMemberRepository {
    fun findByEmail(email: Email): AuthMember?
    fun existsByEmail(email: Email): Boolean
    fun save(command: AuthMemberCommand.Create): AuthMember
}

// After
interface AuthMemberRepository {
    fun findByEmail(email: Email): AuthMember?
    fun findAll(query: AuthMemberQuery): List<AuthMember>
    fun save(command: AuthMemberCommand.Create): AuthMember
}
```

**`auth/application/service/RegisterService.kt`**
```kotlin
// Before
if (authMemberRepository.existsByEmail(command.email)) { throw DuplicateEmailException() }

// After
if (authMemberRepository.findByEmail(command.email) != null) { throw DuplicateEmailException() }
```

---

## 카테고리 3: Repository에서 엔티티 외 타입 반환

### 문제

Repository 메서드가 `Boolean`, `Int`, `Pair<Int, Int>` 등 엔티티가 아닌 타입을 반환하는 규칙 위반.
Repository의 반환 타입은 엔티티 또는 `List<Entity>`여야 함.

### 수정 내역

#### 3-1. `CategoryRepository` — 3개 메서드 제거

```kotlin
// Before (제거된 메서드들)
fun countByMemberId(memberId: MemberId): Int
fun existsByMemberIdAndName(memberId: MemberId, name: CategoryName): Boolean
fun existsTasksByCategory(categoryId: CategoryId): Boolean

// After (추가된 메서드)
fun findTaskIdsByCategoryId(categoryId: CategoryId): List<TaskId>
```

- `existsTasksByCategory`는 BC 간 조회라 `CategoryRepository`에서 `TasksTable`을 직접 조회하던 기존 구조를 유지하되, `Boolean` 대신 `List<TaskId>` (`common/domain`의 공유 VO) 반환으로 변경.
- `CategoryQuery`에 `name: CategoryName? = null` 필드 추가 → `existsByMemberIdAndName` 대체.

**`CreateCategoryService`** 변경:
```kotlin
// Before
require(categoryRepository.countByMemberId(command.memberId) < 10) { ... }
require(!categoryRepository.existsByMemberIdAndName(command.memberId, command.name)) { ... }

// After
require(categoryRepository.findAll(CategoryQuery(memberId = command.memberId)).size < 10) { ... }
require(categoryRepository.findAll(CategoryQuery(memberId = command.memberId, name = command.name)).isEmpty()) { ... }
```

**`UpdateCategoryService`** 변경:
```kotlin
// Before
require(!categoryRepository.existsByMemberIdAndName(command.memberId, command.name)) { ... }

// After
require(categoryRepository.findAll(CategoryQuery(memberId = command.memberId, name = command.name)).isEmpty()) { ... }
```

**`DeleteCategoryService`** 변경:
```kotlin
// Before
require(!categoryRepository.existsTasksByCategory(command.categoryId)) { ... }

// After
require(categoryRepository.findTaskIdsByCategoryId(command.categoryId).isEmpty()) { ... }
```

#### 3-2. `TaskRepository` — `countByQuery` 제거

```kotlin
// Before
fun countByQuery(query: TaskQuery): Pair<Int, Int>

// After (메서드 제거, Service에서 findAllWithCategoryInfo 결과로 계산)
```

**`GetTaskStatsService`** 변경:
```kotlin
// Before
fun execute(query: TaskQuery): TaskStatsDto {
    val (total, completed) = taskRepository.countByQuery(query)
    val rate = if (total == 0) 0.0 else (completed.toDouble() / total * 100)
    return TaskStatsDto(total = total, completed = completed, rate = rate)
}

// After
fun execute(query: TaskQuery): TaskStatsDto {
    val tasksWithInfo = taskRepository.findAllWithCategoryInfo(query)
    val total = tasksWithInfo.size
    val completed = tasksWithInfo.count { it.completed }
    val rate = if (total == 0) 0.0 else (completed.toDouble() / total * 100)
    return TaskStatsDto(total = total, completed = completed, rate = rate)
}
```

---

## 변경된 파일 전체 목록

### 메인 코드

| 파일 | 변경 유형 |
|------|-----------|
| `task/domain/model/TaskQuery.kt` | 수정 |
| `task/domain/repository/TaskRepository.kt` | 수정 (countByQuery 제거) |
| `task/infrastructure/persistence/ExposedTaskRepository.kt` | 수정 |
| `task/application/service/GetTasksByDateService.kt` | 수정 |
| `task/application/service/GetTaskStatsService.kt` | 수정 |
| `task/presentation/controller/TaskController.kt` | 미변경 (non-null 값으로 Query 생성 중이라 호환됨) |
| `category/domain/model/CategoryQuery.kt` | 수정 (name 필드 추가) |
| `category/domain/repository/CategoryRepository.kt` | 수정 |
| `category/infrastructure/persistence/ExposedCategoryRepository.kt` | 수정 |
| `category/application/service/GetCategoriesService.kt` | 수정 |
| `category/application/service/CreateCategoryService.kt` | 수정 |
| `category/application/service/UpdateCategoryService.kt` | 수정 |
| `category/application/service/DeleteCategoryService.kt` | 수정 |
| `category/presentation/controller/CategoryController.kt` | 수정 |
| `member/application/service/GetMemberService.kt` | 수정 |
| `member/presentation/controller/MemberController.kt` | 수정 |
| `auth/domain/model/AuthMemberQuery.kt` | **신규 생성** |
| `auth/domain/repository/AuthMemberRepository.kt` | 수정 |
| `auth/infrastructure/persistence/AuthMemberRepositoryImpl.kt` | 수정 |
| `auth/application/service/RegisterService.kt` | 수정 |

### 테스트 코드

| 파일 | 변경 내용 |
|------|-----------|
| `task/application/service/GetTaskStatsServiceTest.kt` | `countByQuery` mock → `findAllWithCategoryInfo` mock으로 변경 |
| `category/application/service/GetCategoriesServiceTest.kt` | `service.execute(memberId)` → `service.execute(CategoryQuery(...))` |
| `category/application/service/CreateCategoryServiceTest.kt` | `countByMemberId`, `existsByMemberIdAndName` mock → `findAll` mock |
| `category/application/service/UpdateCategoryServiceTest.kt` | `existsByMemberIdAndName` mock → `findAll` mock |
| `category/application/service/DeleteCategoryServiceTest.kt` | `existsTasksByCategory` mock → `findTaskIdsByCategoryId` mock |
| `member/application/service/GetMemberServiceTest.kt` | `findById` mock → `findAll` mock, `execute(memberId)` → `execute(MemberQuery(...))` |
| `auth/application/service/RegisterServiceTest.kt` | `existsByEmail` mock → `findByEmail` mock |

---

# Sprint 2 2차 아키텍처 규칙 엄격 모드 리팩토링

## 작업 개요

1차 리팩토링 이후 남아 있던 규칙 위반 4개 항목을 추가 수정했습니다.
`./gradlew build` BUILD SUCCESSFUL 확인 완료.

---

## 항목 1: 개별 VO 입력 메서드 완전 제거 → findAll(Query) 통일

### 문제

1차 리팩토링 후에도 Repository 인터페이스에 `findById`, `findByEmail`, `findByToken` 같은 개별 VO 입력 메서드가 잔존.

### 수정 내역

#### 1-1. `MemberRepository`

```kotlin
// Before
interface MemberRepository {
    fun save(command: MemberCommand): Member
    fun findById(id: MemberId): Member?
    fun findByEmail(email: Email): Member?
    fun findAll(query: MemberQuery): List<Member>
}

// After
interface MemberRepository {
    fun save(command: MemberCommand): Member
    fun findAll(query: MemberQuery): List<Member>
}
```

#### 1-2. `CategoryRepository`

```kotlin
// Before
interface CategoryRepository {
    fun save(command: CategoryCommand): Category
    fun findById(id: CategoryId): Category?
    fun findAll(query: CategoryQuery): List<Category>
    fun delete(command: CategoryCommand.Delete)
    fun findTaskIdsByCategoryId(categoryId: CategoryId): List<TaskId>
}

// After
interface CategoryRepository {
    fun save(command: CategoryCommand): Category
    fun findAll(query: CategoryQuery): List<Category>
    fun delete(command: CategoryCommand.Delete): Category
}
```

#### 1-3. `TaskRepository`

```kotlin
// Before
interface TaskRepository {
    fun save(command: TaskCommand): Task
    fun findById(id: TaskId): Task?
    fun findAllWithCategoryInfo(query: TaskQuery): List<TaskWithCategoryInfo>
    fun delete(command: TaskCommand.Delete)
}

// After
interface TaskRepository {
    fun save(command: TaskCommand): Task
    fun findAll(query: TaskQuery): List<Task>
    fun delete(command: TaskCommand.Delete): Task
}
```

- `TaskQuery`에 `taskId: TaskId? = null` 필드 추가.

#### 1-4. `AuthMemberRepository`

```kotlin
// Before
interface AuthMemberRepository {
    fun findByEmail(email: Email): AuthMember?
    fun findAll(query: AuthMemberQuery): List<AuthMember>
    fun save(command: AuthMemberCommand.Create): AuthMember
}

// After
interface AuthMemberRepository {
    fun findAll(query: AuthMemberQuery): List<AuthMember>
    fun save(command: AuthMemberCommand.Create): AuthMember
}
```

#### 1-5. `RefreshTokenRepository`

기존 메서드 전면 교체. `RefreshTokenQuery` 신규 생성.

**신규: `auth/domain/model/RefreshTokenQuery.kt`**
```kotlin
data class RefreshTokenQuery(
    val token: RefreshToken? = null,
    val memberId: MemberId? = null,
)
```

```kotlin
// Before
interface RefreshTokenRepository {
    fun save(command: RefreshTokenCommand.Save)
    fun findByToken(token: RefreshToken): StoredRefreshToken?
    fun deleteByToken(token: RefreshToken)
    fun deleteAllByMemberId(memberId: MemberId)
}

// After
interface RefreshTokenRepository {
    fun save(command: RefreshTokenCommand.Save): StoredRefreshToken
    fun findAll(query: RefreshTokenQuery): List<StoredRefreshToken>
    fun delete(command: RefreshTokenCommand.Delete): StoredRefreshToken
    fun deleteAllByMemberId(command: RefreshTokenCommand.DeleteAllByMemberId): List<StoredRefreshToken>
}
```

- `RefreshTokenCommand`에 `Delete(token)`, `DeleteAllByMemberId(memberId)` 추가.

### 서비스 레이어 변경

모든 서비스에서 개별 VO 메서드 → `findAll(Query).firstOrNull()` 패턴으로 전환.

| 서비스 | 변경 전 | 변경 후 |
|--------|---------|---------|
| `UpdateTaskService` | `taskRepository.findById(command.taskId)` | `taskRepository.findAll(TaskQuery(taskId = command.taskId)).firstOrNull()` |
| `DeleteTaskService` | `taskRepository.findById(command.taskId)` | `taskRepository.findAll(TaskQuery(taskId = command.taskId)).firstOrNull()` |
| `ToggleTaskCompleteService` | `taskRepository.findById(command.taskId)` | `taskRepository.findAll(TaskQuery(taskId = command.taskId)).firstOrNull()` |
| `UpdateCategoryService` | `categoryRepository.findById(command.categoryId)` | `categoryRepository.findAll(CategoryQuery(categoryId = command.categoryId)).firstOrNull()` |
| `DeleteCategoryService` | `categoryRepository.findById(command.categoryId)` | `categoryRepository.findAll(CategoryQuery(categoryId = command.categoryId)).firstOrNull()` |
| `UpdateMemberProfileService` | `memberRepository.findById(command.memberId)` | `memberRepository.findAll(MemberQuery(memberId = command.memberId)).firstOrNull()` |
| `RegisterMemberService` | `memberRepository.findByEmail(command.email)` | `memberRepository.findAll(MemberQuery(email = command.email)).isEmpty()` |
| `LoginService` | `authMemberRepository.findByEmail(command.email)` | `authMemberRepository.findAll(AuthMemberQuery(email = command.email)).firstOrNull()` |
| `RegisterService` | `authMemberRepository.findByEmail(command.email)` | `authMemberRepository.findAll(AuthMemberQuery(email = command.email)).isNotEmpty()` |
| `LogoutService` | `refreshTokenRepository.deleteByToken(command.refreshToken)` | `refreshTokenRepository.delete(RefreshTokenCommand.Delete(command.refreshToken))` |
| `RefreshService` | `refreshTokenRepository.findByToken(command.refreshToken)` | `refreshTokenRepository.findAll(RefreshTokenQuery(token = command.refreshToken)).firstOrNull()` |
| `RefreshService` | `refreshTokenRepository.deleteByToken(command.refreshToken)` | `refreshTokenRepository.delete(RefreshTokenCommand.Delete(command.refreshToken))` |

---

## 항목 2: delete/save의 Unit 반환 → 엔티티 반환

### 문제

`CategoryRepository.delete()`, `TaskRepository.delete()`, `RefreshTokenRepository.save()`가 `Unit`을 반환하여 Repository 계약(엔티티 반환) 위반.

### 수정 내역

- `CategoryRepository.delete(command)` → `Category` 반환 (삭제 전 엔티티 조회 후 반환)
- `TaskRepository.delete(command)` → `Task` 반환 (삭제 전 엔티티 조회 후 반환)
- `RefreshTokenRepository.save(command)` → `StoredRefreshToken` 반환 (`insertAndGetId` 활용)
- `RefreshTokenRepository.delete(command)` → `StoredRefreshToken` 반환
- `RefreshTokenRepository.deleteAllByMemberId(command)` → `List<StoredRefreshToken>` 반환

---

## 항목 3: findTaskIdsByCategoryId 제거 (VO 반환 → infrastructure 책임 이전)

### 문제

`CategoryRepository.findTaskIdsByCategoryId(categoryId): List<TaskId>`는 엔티티가 아닌 VO(`TaskId`) 리스트를 반환하므로 규칙 위반.
또한 `CategoryRepository`가 `TasksTable`을 직접 조회하는 BC 경계 문제도 존재.

### 결정

- `CategoryRepository` 인터페이스에서 완전 제거.
- 태스크 존재 여부 검사 로직을 `ExposedCategoryRepository.delete()` 구현체 내부로 이전.
- `DeleteCategoryService`에서 해당 검사 코드 제거.

```kotlin
// ExposedCategoryRepository.delete() — 구현체 내부로 이동
override fun delete(command: CategoryCommand.Delete): Category {
    val hasTasks = TasksTable.selectAll()
        .where { TasksTable.categoryId eq command.categoryId.value }
        .any()
    require(!hasTasks) { "Cannot delete category with existing tasks" }
    val category = findById(command.categoryId)
        ?: throw NoSuchElementException("Category not found: ${command.categoryId.value}")
    val entityId = EntityID(command.categoryId.value, CategoriesTable)
    CategoriesTable.deleteWhere { id eq entityId }
    return category
}
```

```kotlin
// DeleteCategoryService — 태스크 검사 제거 (infrastructure가 담당)
fun execute(command: CategoryCommand.Delete) {
    val category = categoryRepository.findAll(CategoryQuery(categoryId = command.categoryId)).firstOrNull()
        ?: throw NoSuchElementException(...)
    require(category.memberId == command.memberId) { "Not authorized to delete this category" }
    categoryRepository.delete(command)   // 내부에서 태스크 검사 수행
}
```

---

## 항목 4: TaskReadRepository 분리

### 문제

`findAllWithCategoryInfo(query): List<TaskWithCategoryInfo>`는 `Task` 엔티티가 아닌 `TaskWithCategoryInfo`(조회 전용 복합 객체)를 반환.
`TaskRepository`에 조회 전용 복합 메서드가 혼재하는 것이 부적절.

### 결정

별도 인터페이스 `TaskReadRepository`로 분리.

**신규: `task/domain/repository/TaskReadRepository.kt`**
```kotlin
interface TaskReadRepository {
    fun findAllWithCategoryInfo(query: TaskQuery): List<TaskWithCategoryInfo>
}
```

**신규: `task/infrastructure/persistence/ExposedTaskReadRepository.kt`**
- `findAllWithCategoryInfo` 구현 (`TasksTable` JOIN `CategoriesTable`)

**`GetTasksByDateService`, `GetTaskStatsService`**
```kotlin
// Before
class GetTasksByDateService(
    private val taskRepository: TaskRepository,
) { ... taskRepository.findAllWithCategoryInfo(query) ... }

// After
class GetTasksByDateService(
    private val taskReadRepository: TaskReadRepository,
) { ... taskReadRepository.findAllWithCategoryInfo(query) ... }
```

---

## 변경된 파일 전체 목록 (2차)

### 메인 코드

| 파일 | 변경 유형 |
|------|-----------|
| `task/domain/model/TaskQuery.kt` | 수정 (`taskId` 필드 추가) |
| `task/domain/repository/TaskRepository.kt` | 수정 (`findById`, `findAllWithCategoryInfo` 제거, `findAll`, `delete` 반환 변경) |
| `task/domain/repository/TaskReadRepository.kt` | **신규 생성** |
| `task/infrastructure/persistence/ExposedTaskRepository.kt` | 수정 (`findAll` 추가, `delete` 반환 변경, `findAllWithCategoryInfo` 제거) |
| `task/infrastructure/persistence/ExposedTaskReadRepository.kt` | **신규 생성** |
| `task/application/service/UpdateTaskService.kt` | 수정 |
| `task/application/service/DeleteTaskService.kt` | 수정 |
| `task/application/service/ToggleTaskCompleteService.kt` | 수정 |
| `task/application/service/GetTasksByDateService.kt` | 수정 (`TaskReadRepository` 주입) |
| `task/application/service/GetTaskStatsService.kt` | 수정 (`TaskReadRepository` 주입) |
| `category/domain/repository/CategoryRepository.kt` | 수정 (`findById`, `findTaskIdsByCategoryId` 제거, `delete` 반환 변경) |
| `category/infrastructure/persistence/ExposedCategoryRepository.kt` | 수정 (`delete` 반환 변경, 태스크 검사 이전) |
| `category/application/service/UpdateCategoryService.kt` | 수정 |
| `category/application/service/DeleteCategoryService.kt` | 수정 (태스크 검사 제거) |
| `member/domain/repository/MemberRepository.kt` | 수정 (`findById`, `findByEmail` 제거) |
| `member/infrastructure/persistence/ExposedMemberRepository.kt` | 수정 (`findById` private화) |
| `member/application/service/UpdateMemberProfileService.kt` | 수정 |
| `member/application/service/RegisterMemberService.kt` | 수정 |
| `auth/domain/model/RefreshTokenQuery.kt` | **신규 생성** |
| `auth/domain/model/RefreshTokenCommand.kt` | 수정 (`Delete`, `DeleteAllByMemberId` 추가) |
| `auth/domain/repository/AuthMemberRepository.kt` | 수정 (`findByEmail` 제거) |
| `auth/domain/repository/RefreshTokenRepository.kt` | 수정 (전면 교체) |
| `auth/infrastructure/persistence/AuthMemberRepositoryImpl.kt` | 수정 (`findByEmail` 제거) |
| `auth/infrastructure/persistence/RefreshTokenRepositoryImpl.kt` | 수정 (전면 교체) |
| `auth/application/service/LoginService.kt` | 수정 |
| `auth/application/service/RegisterService.kt` | 수정 |
| `auth/application/service/LogoutService.kt` | 수정 |
| `auth/application/service/RefreshService.kt` | 수정 |

### 테스트 코드

| 파일 | 변경 내용 |
|------|-----------|
| `task/application/service/UpdateTaskServiceTest.kt` | `findById` mock → `findAll(TaskQuery(taskId=...))` mock |
| `task/application/service/DeleteTaskServiceTest.kt` | `findById` mock → `findAll` mock, `delete` 반환 `just runs` → `returns existingTask` |
| `task/application/service/ToggleTaskCompleteServiceTest.kt` | `findById` mock → `findAll` mock |
| `task/application/service/GetTasksByDateServiceTest.kt` | `TaskRepository` → `TaskReadRepository` 주입 |
| `task/application/service/GetTaskStatsServiceTest.kt` | `TaskRepository` → `TaskReadRepository` 주입 |
| `category/application/service/UpdateCategoryServiceTest.kt` | `findById` mock → `findAll(CategoryQuery(categoryId=...))` mock |
| `category/application/service/DeleteCategoryServiceTest.kt` | `findById`/`findTaskIdsByCategoryId` mock 제거, `findAll` mock 추가, "할일 있는 카테고리" 케이스 제거 (infrastructure 책임) |
| `member/application/service/UpdateMemberProfileServiceTest.kt` | `findById` mock → `findAll` mock |
| `member/application/service/RegisterMemberServiceTest.kt` | `findByEmail` mock → `findAll` mock |
| `auth/application/service/LoginServiceTest.kt` | `findByEmail` mock → `findAll(AuthMemberQuery(...))` mock, `save` 반환 `Unit` → `StoredRefreshToken` |
| `auth/application/service/RegisterServiceTest.kt` | `findByEmail` mock → `findAll` mock, `save` 반환 `Unit` → `StoredRefreshToken` |
| `auth/application/service/LogoutServiceTest.kt` | `deleteByToken` mock → `delete(RefreshTokenCommand.Delete(...))` mock |
| `auth/application/service/RefreshServiceTest.kt` | `findByToken`/`deleteByToken` mock → `findAll`/`delete` mock, `save` 반환 `Unit` → `StoredRefreshToken` |
