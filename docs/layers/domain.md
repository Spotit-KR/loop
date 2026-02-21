# Domain Layer

외부 의존 없는 순수 Kotlin 코드. 비즈니스 규칙의 중심.

## model/

- **엔티티**: `data class`. 비즈니스 상태를 가진 핵심 객체.
- **Value Object**: `@JvmInline value class`. `init` 블록에서 유효성 검증.
- **Command**: `sealed interface`. CUD 입력 수단.
- **Query**: `data class`. nullable 필드. 조회 입력 수단.

### Value Object

```kotlin
@JvmInline
value class TaskTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskTitle must not be blank" }
        require(value.length <= 200) { "TaskTitle must not exceed 200 characters" }
    }
}
```

BC간 공유 VO(MemberId, TaskId 등)는 `common/domain/`에 위치합니다.

### Command

Domain 레이어의 model/에 sealed interface로 정의합니다. Presentation 이후 모든 레이어의 CUD 입력 수단입니다.

```kotlin
sealed interface TaskCommand {
    data class Create(
        val title: TaskTitle,
        val description: TaskDescription?,
        val memberId: MemberId,
    ) : TaskCommand

    data class Update(
        val taskId: TaskId,
        val title: TaskTitle,
    ) : TaskCommand

    data class Delete(
        val taskId: TaskId,
    ) : TaskCommand
}
```

### Query

nullable 필드를 가진 단일 data class. 조건이 추가되면 필드만 추가합니다.

```kotlin
data class TaskQuery(
    val memberId: MemberId? = null,
    val weekId: WeekId? = null,
    val status: TaskStatus? = null,
    val page: Int = 0,
    val size: Int = 20,
)
```

## repository/

Command/Query를 기본 입력으로 사용하되, **의도가 명확한 단건 메서드도 허용**합니다.

허용되는 메서드 시그니처:

```kotlin
interface TaskRepository {
    // Command 기반
    fun save(command: TaskCommand.Create): Task
    fun update(command: TaskCommand.Update): Task
    fun delete(command: TaskCommand.Delete)

    // Query 기반
    fun findAll(query: TaskQuery): List<Task>

    // 의도형 단건 메서드 — 허용
    fun findById(id: TaskId): Task?
    fun existsByTitle(title: TaskTitle): Boolean
    fun countByMemberId(memberId: MemberId): Long
}
```

**ReadRepository 분리 기준**: JOIN이 3개 이상 필요한 복합 조회가 있을 때만 분리합니다.

```kotlin
// JOIN 3개 이상 → ReadRepository 분리
interface TaskReadRepository {
    fun findDetailById(id: TaskId): TaskDetail?  // Task + Category + Member JOIN
}
```

## service/ (Domain Service)

다음 기준을 **모두** 충족할 때만 도입합니다:

1. BC 내 최상위 도메인 객체 여럿이 엮인 비즈니스 로직이 존재할 때
2. 코드가 100줄 이상이 필요하다고 판단될 때
3. 여러 repository 구현체의 조합이 필요한 로직이 아닐 때
