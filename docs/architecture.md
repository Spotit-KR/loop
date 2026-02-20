# Loop Server Architecture

## 기술 스택

- **Spring Boot 4.0.3** (Spring Framework 7.x) / **Kotlin 2.3.10** / **Java 25**
- **Exposed ORM** (Kotlin DSL ORM) + **Flyway** (마이그레이션)
- **PostgreSQL** (메인 DB) / **H2** (개발/테스트)
- **Spring Cloud OpenFeign** (외부 API)
- **Kotest** (BehaviorSpec) / **MockK** / **SpringMockK**

## 의존성 규칙

Domain이 중심이며, 나머지 레이어가 Domain에 의존합니다.

- **Domain** → 외부 의존 없음. 순수 비즈니스 로직.
- **Presentation** → Application (Service 호출, DTO 사용) + Domain (Command/Query 생성, VO 사용)
- **Application** → Domain (Command/Query, Repository 인터페이스, Entity, VO 사용)
- **Infrastructure** → Domain (Repository 인터페이스 구현)

## 디렉토리 구조

```
kr.io.team.loop/
├── ServerApplication.kt
├── common/                        # 공통 모듈
│   ├── domain/                   # 공유 VO (MemberId, TaskId 등)
│   ├── config/                   # 공통 설정 (Security, WebMvc 등)
│   └── infrastructure/           # 공통 인프라 구현체
└── {bounded-context}/            # 도메인별 Bounded Context
    ├── presentation/
    │   ├── request/              # 요청 DTO (Primitive Type)
    │   ├── response/             # 응답 DTO (Primitive Type)
    │   └── controller/           # 변환 + Service 호출
    ├── application/
    │   ├── dto/                  # 응답 DTO (Service 반환용)
    │   └── service/              # Service 구현체
    ├── domain/
    │   ├── model/                # 엔티티, VO, Command, Query
    │   ├── repository/           # Repository 인터페이스
    │   └── service/              # Domain Service (필요시에만)
    └── infrastructure/
        ├── persistence/          # Repository 구현, Exposed Table 정의
        └── external/             # OpenFeign 클라이언트 (필요시에만)
```

## 데이터 흐름

### Command (CUD)

```
Controller: CreateTaskRequest(Primitive) → TaskCommand.Create(VO) 변환
    ↓
Service: TaskCommand.Create 수신 → Repository에 전달, 트랜잭션 관리
    ↓
Repository: TaskCommand.Create 수신 → 영속화 → Task 엔티티 반환
    ↓
Service: Task → TaskDto 변환 후 반환
    ↓
Controller: TaskDto → TaskResponse(Primitive) 변환 후 반환
```

### Query (R)

```
Controller: query params(Primitive) → TaskQuery(VO) 변환
    ↓
Service: TaskQuery 수신 → Repository에 전달
    ↓
Repository: TaskQuery 수신 → 조회 → List<Task> 반환
    ↓
Service: List<Task> → List<TaskDto> 변환 후 반환
    ↓
Controller: List<TaskDto> → List<TaskResponse>(Primitive) 변환 후 반환
```

## 핵심 패턴

### Value Object

`@JvmInline value class`로 정의하고, `init` 블록에서 유효성을 검증합니다.

```kotlin
@JvmInline
value class TaskTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskTitle must not be blank" }
        require(value.length <= 100) { "TaskTitle must not exceed 100 characters" }
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

### Query data class

Domain 레이어에 nullable 필드를 가진 단일 data class로 정의합니다. Presentation 이후 모든 레이어의 조회 입력 수단입니다. 조건이 추가되면 필드만 추가합니다.

```kotlin
data class TaskQuery(
    val memberId: MemberId? = null,
    val weekId: WeekId? = null,
    val status: TaskStatus? = null,
    val page: Int = 0,
    val size: Int = 20,
)
```

### Repository

인터페이스는 `domain/repository/`에 위치합니다. Command/Query를 입력으로, 엔티티를 출력으로 사용합니다.

```kotlin
interface TaskRepository {
    fun save(command: TaskCommand): Task
    fun findById(id: TaskId): Task?
    fun findAll(query: TaskQuery): List<Task>
}
```

구현체는 `infrastructure/persistence/`에 위치합니다.

### Service

UseCase 인터페이스를 별도로 만들지 않습니다. Service 클래스가 직접 비즈니스 로직을 수행합니다.
로직이 복잡하면 command별로 service를 분리할 수 있습니다. (예: `CreateTaskService`, `UpdateTaskService`)

```kotlin
@Service
class CreateTaskService(
    private val taskRepository: TaskRepository,
) {
    @Transactional
    fun execute(command: TaskCommand.Create): TaskDto {
        val task = taskRepository.save(command)
        return TaskDto.from(task)
    }
}
```

### Presentation

Controller는 외부(클라이언트)와 내부(Application/Domain) 사이의 변환 경계입니다.

- **request/**: 클라이언트 요청 DTO. Primitive Type만 사용.
- **response/**: 클라이언트 응답 DTO. Primitive Type만 사용. Application DTO로부터 변환.
- **controller/**: Request → Domain Command/Query 변환, Service 호출, Application DTO → Response 변환.

```kotlin
// Request DTO
data class CreateTaskRequest(
    val title: String,
    val description: String?,
)

// Response DTO
data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
) {
    companion object {
        fun from(dto: TaskDto) = TaskResponse(
            id = dto.id.value,
            title = dto.title.value,
            description = dto.description?.value,
        )
    }
}

// Controller
@RestController
@RequestMapping("/tasks")
class TaskController(
    private val createTaskService: CreateTaskService,
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
        val dto = createTaskService.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(dto))
    }
}
```

### Domain Service 도입 기준

다음 기준을 **모두** 충족할 때만 도입합니다:

1. BC 내 최상위 도메인 객체 여럿이 엮인 비즈니스 로직이 존재할 때
2. 코드가 100줄 이상이 필요하다고 판단될 때
3. 여러 repository 구현체의 조합이 필요한 로직이 아닐 때

## 테스트 전략

| 레이어 | 유형 | 필수 여부 | Spring Context | 방식 |
|--------|------|----------|----------------|------|
| Domain | 단위 테스트 | 필수 | 없음 | 순수 Kotlin POJO 테스트 |
| Application | 단위 테스트 | 필수 | 없음 | MockK으로 의존성 Mocking |
| Infrastructure | 통합 테스트 | 선택적 (사람이 판단) | 있음 | H2 DB, @SpringBootTest |
| Presentation | 통합 테스트 | 선택적 (사람이 판단) | 있음 | @WebMvcTest + MockK |

테스트 클래스명: `대상클래스명Test` (예: `CreateTaskServiceTest`)
