# Presentation Layer

DataFetcher는 GraphQL 요청과 내부(Application/Domain) 사이의 변환 경계입니다.
모든 엔드포인트는 **GraphQL** (Netflix DGS Framework)로 제공합니다.

## DGS Codegen (필수)

GraphQL 스키마의 Type, Input, Enum에 대응하는 Kotlin 클래스는 **반드시 DGS Codegen이 자동 생성**합니다.

**절대 금지**: GraphQL 스키마 타입에 대응하는 data class, enum class, input class를 수동으로 작성하는 것.

- `.graphqls` 스키마만 작성하면 빌드 시 Codegen이 Kotlin 클래스를 자동 생성
- DataFetcher에서는 Codegen이 생성한 타입을 `@InputArgument`로 바인딩하여 사용
- 생성된 코드는 `build/generated/` 하위에 위치하며 직접 수정하지 않음

## GraphQL 스키마 (`src/main/resources/schema/{bc}.graphqls`)

BC별 `.graphqls` 파일에 Query, Mutation, 타입, Input을 정의합니다.

```graphql
# src/main/resources/schema/task.graphqls

type Query {
    task(id: ID!): Task
    tasks(memberId: ID!): [Task!]!
}

type Mutation {
    createTask(input: CreateTaskInput!): Task!
    updateTask(id: ID!, input: UpdateTaskInput!): Task
    deleteTask(id: ID!): Boolean!
}

type Task {
    id: ID!
    title: String!
    description: String
    status: TaskStatus!
}

input CreateTaskInput {
    title: String!
    description: String
}

input UpdateTaskInput {
    title: String!
}

enum TaskStatus {
    TODO
    IN_PROGRESS
    DONE
}
```

## datafetcher/

`@DgsComponent`로 GraphQL Query/Mutation을 리졸브합니다.
역할: **GraphQL 인자 → Domain Command/Query 변환, Service 호출, Entity/DTO 반환**.

```kotlin
@DgsComponent
class TaskDataFetcher(
    private val taskService: TaskService,
) {
    @DgsMutation
    fun createTask(
        @InputArgument input: CreateTaskInput,  // Codegen 자동 생성 타입
        dfe: DataFetchingEnvironment,
    ): Task {
        val memberId = dfe.graphQlContext.get<Long>("memberId")
        val command = TaskCommand.Create(
            title = TaskTitle(input.title),
            description = input.description?.let { TaskDescription(it) },
            memberId = MemberId(memberId),
        )
        return taskService.create(command)
    }

    @DgsQuery
    fun tasks(
        @InputArgument memberId: String,
    ): List<Task> {
        val query = TaskQuery(memberId = MemberId(memberId.toLong()))
        return taskService.findAll(query)
    }
}
```

> `CreateTaskInput`은 DGS Codegen이 `task.graphqls`의 `input CreateTaskInput`으로부터 자동 생성한 클래스입니다.
> GraphQL 스키마 타입에 대응하는 클래스를 수동으로 작성하지 않습니다.

## dataloader/ (선택적)

N+1 문제가 발생하는 관계형 필드에서만 도입합니다.

```kotlin
@DgsDataLoader(name = "tasksForMember")
class TaskDataLoader(
    private val taskService: TaskService,
) : MappedBatchLoader<Long, List<Task>> {
    override fun load(keys: Set<Long>): CompletionStage<Map<Long, List<Task>>> {
        return CompletableFuture.supplyAsync {
            taskService.findByMemberIds(keys)
                .groupBy { it.memberId.value }
        }
    }
}
```

**DataLoader 도입 기준**: 부모 목록 조회 시 자식 필드가 N+1 쿼리를 유발할 때만 도입합니다.

## Presentation 테스트

`@EnableDgsTest` + `DgsQueryExecutor`로 GraphQL 쿼리 수준에서 테스트합니다.

```kotlin
@EnableDgsTest
@SpringBootTest(classes = [TaskDataFetcher::class])
class TaskDataFetcherTest {
    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockkBean
    lateinit var taskService: TaskService

    @Test
    fun `tasks 쿼리가 목록을 반환한다`() {
        every { taskService.findAll(any()) } returns listOf(task)

        val titles: List<String> = dgsQueryExecutor.executeAndExtractJsonPath(
            "{ tasks(memberId: \"1\") { title } }",
            "data.tasks[*].title",
        )

        assertThat(titles).containsExactly("할 일 1")
    }
}
```
