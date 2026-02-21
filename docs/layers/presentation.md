# Presentation Layer

Controller는 외부(클라이언트)와 내부(Application/Domain) 사이의 변환 경계입니다.

## request/

클라이언트 요청 DTO. **Primitive Type만 사용**합니다.

```kotlin
data class CreateTaskRequest(
    val title: String,
    val description: String?,
)
```

## response/

클라이언트 응답 DTO. **Primitive Type만 사용**합니다.

- **기본**: `from(Entity)`로 변환
- Application DTO가 있는 경우: `from(dto)`로 변환

```kotlin
data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: String,
) {
    companion object {
        // 기본: Entity에서 직접 변환
        fun from(task: Task) = TaskResponse(
            id = task.id.value,
            title = task.title.value,
            description = task.description?.value,
            status = task.status.name,
        )
    }
}
```

## controller/

Request → Domain Command/Query 변환, Service 호출, Entity/DTO → Response 변환.

```kotlin
@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService,
) {
    @PostMapping
    fun createTask(
        @RequestBody request: CreateTaskRequest,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<TaskResponse> {
        val command = TaskCommand.Create(
            title = TaskTitle(request.title),
            description = request.description?.let { TaskDescription(it) },
            memberId = MemberId(memberId),
        )
        val task = taskService.create(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task))
    }

    @GetMapping
    fun getTasks(
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<List<TaskResponse>> {
        val query = TaskQuery(memberId = MemberId(memberId))
        val tasks = taskService.findAll(query)
        return ResponseEntity.ok(tasks.map { TaskResponse.from(it) })
    }
}
```
