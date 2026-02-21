# Application Layer

## service/

**BC당 1개 Service가 기본**. 150줄 초과 시 분리합니다.

- UseCase 인터페이스 없이 Service가 직접 구현
- `@Service` + `@Transactional`
- **기본적으로 Entity를 직접 반환** (Application DTO 불필요)
- Application DTO가 있는 경우에만 DTO로 변환하여 반환

```kotlin
@Service
class TaskService(
    private val taskRepository: TaskRepository,
) {
    @Transactional
    fun create(command: TaskCommand.Create): Task {
        return taskRepository.save(command)
    }

    @Transactional
    fun update(command: TaskCommand.Update): Task {
        return taskRepository.update(command)
    }

    @Transactional
    fun delete(command: TaskCommand.Delete) {
        taskRepository.delete(command)
    }

    @Transactional(readOnly = true)
    fun findAll(query: TaskQuery): List<Task> {
        return taskRepository.findAll(query)
    }

    @Transactional(readOnly = true)
    fun findById(id: TaskId): Task {
        return taskRepository.findById(id)
            ?: throw NoSuchElementException("Task not found: ${id.value}")
    }
}
```

**Service 분리 기준**: Service가 150줄을 초과하면 Command/Query 또는 기능 단위로 분리합니다.

```
# 150줄 이내 → 1개
task/application/service/TaskService.kt

# 150줄 초과 → 분리
task/application/service/TaskCommandService.kt
task/application/service/TaskQueryService.kt
```

## dto/ (선택적)

Application DTO는 **기본적으로 생성하지 않습니다**. 다음 경우에만 도입합니다:

| 도입 기준 | 예시 |
|-----------|------|
| 여러 Entity를 조합한 집계 결과 | `TaskSummaryDto(taskCount, completedCount, ...)` |
| 보안상 제외해야 할 필드가 있을 때 | `MemberDto` (password 제외) |
| Entity와 다른 계산 필드가 필요할 때 | `TaskWithProgressDto(task, progressPercent)` |

```kotlin
// 보안 필드 제외가 필요한 경우에만
data class AuthMemberDto(
    val id: MemberId,
    val email: Email,
    val nickname: Nickname,
    // password 제외
) {
    companion object {
        fun from(member: AuthMember) = AuthMemberDto(
            id = member.id,
            email = member.email,
            nickname = member.nickname,
        )
    }
}
```
