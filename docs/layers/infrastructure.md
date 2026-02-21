# Infrastructure Layer

## persistence/

**Repository 구현체 + Exposed Table 정의**. Table은 해당 BC가 소유합니다.

```kotlin
// task/infrastructure/persistence/TaskTable.kt
object TaskTable : LongIdTable("tasks") {
    val title = varchar("title", 200)
    val description = text("description").nullable()
    val memberId = long("member_id")
    val status = enumerationByName<TaskStatus>("status", 20)
    val createdAt = timestamp("created_at")
}

// task/infrastructure/persistence/ExposedTaskRepository.kt
@Repository
class ExposedTaskRepository : TaskRepository {
    override fun save(command: TaskCommand.Create): Task { ... }
    override fun findById(id: TaskId): Task? { ... }
    override fun findAll(query: TaskQuery): List<Task> { ... }
    // ...
}
```

### BC 간 FK 참조

다른 BC의 Table을 FK로 참조하는 것은 Infrastructure 레이어에서 허용합니다. Domain 코드에서는 공유 VO(MemberId 등)만 사용합니다.

```kotlin
// task/infrastructure/persistence/TaskTable.kt
object TaskTable : LongIdTable("tasks") {
    // auth BC의 테이블을 FK로 참조 — Infrastructure에서만 허용
    val memberId = long("member_id").references(AuthMemberTable.id)
}
```

## Exposed 1.0.0 마이그레이션 주의사항

이 프로젝트는 Exposed 1.0.0+를 사용합니다. 0.x 대비 **패키지 경로와 API가 대폭 변경**되었으므로 반드시 아래 규칙을 따릅니다.

### 패키지 경로

모든 import는 `org.jetbrains.exposed.v1.*` 패턴을 사용합니다.

| 용도 | 패키지 |
|------|--------|
| Table, Column, VO 타입 등 | `org.jetbrains.exposed.v1.core.*` |
| Database, SchemaUtils, transaction | `org.jetbrains.exposed.v1.jdbc.*` |
| DAO (Entity, IntEntity 등) | `org.jetbrains.exposed.v1.dao.*` |
| Spring Boot 4 연동 | `exposed-spring-boot4-starter` |

**절대 사용하지 않을 것**: `org.jetbrains.exposed.sql.*` (0.x 구 패키지)

> 전체 마이그레이션 가이드: https://www.jetbrains.com/help/exposed/migration-guide-1-0-0.html

## Exposed DSL API 레퍼런스

### Table 정의

```kotlin
object TaskTable : LongIdTable("tasks") {           // id: Long auto-increment PK
    val title = varchar("title", 200)                // VARCHAR(200)
    val description = text("description").nullable()  // TEXT, nullable
    val memberId = long("member_id")                 // BIGINT
    val status = enumerationByName<TaskStatus>("status", 20) // VARCHAR enum
    val createdAt = timestamp("created_at")          // TIMESTAMP
    val isActive = bool("is_active").default(true)   // BOOLEAN with default
    val memberId = long("member_id").references(OtherTable.id) // FK
}
```

주요 Table 타입: `Table` (PK 직접 정의), `IntIdTable`, `LongIdTable`, `UUIDTable`

### Insert

```kotlin
// 단건 삽입 + ID 반환
val id = TaskTable.insertAndGetId {
    it[title] = "제목"
    it[memberId] = 1L
}

// 배치 삽입
TaskTable.batchInsert(items) { item ->
    this[TaskTable.title] = item.title
    this[TaskTable.memberId] = item.memberId
}
```

### Select

```kotlin
// 전체 조회 + 조건
TaskTable.selectAll()
    .where { TaskTable.memberId eq memberId }
    .orderBy(TaskTable.createdAt, SortOrder.DESC)
    .limit(20)
    .map { row -> toEntity(row) }

// 특정 컬럼만
TaskTable.select(TaskTable.title, TaskTable.status)
    .where { TaskTable.id eq taskId }

// 단건 조회
TaskTable.selectAll()
    .where { TaskTable.id eq taskId }
    .singleOrNull()

// 집계
TaskTable.select(TaskTable.status, TaskTable.id.count())
    .groupBy(TaskTable.status)
```

### Update

```kotlin
TaskTable.update({ TaskTable.id eq taskId }) {
    it[title] = "새 제목"
}
```

### Delete

```kotlin
TaskTable.deleteWhere { TaskTable.id eq taskId }
```

### Join

```kotlin
// FK 기반 innerJoin
(TaskTable innerJoin CategoryTable)
    .selectAll()
    .where { TaskTable.memberId eq memberId }
    .map { row -> /* TaskTable, CategoryTable 컬럼 모두 접근 가능 */ }

// 명시적 조인 조건
TaskTable.join(CategoryTable, JoinType.LEFT, TaskTable.categoryId, CategoryTable.id)
    .selectAll()

// 추가 조건
TaskTable.join(
    CategoryTable, JoinType.INNER,
    additionalConstraint = { CategoryTable.id eq TaskTable.categoryId }
)
```

### Where 연산자

```kotlin
eq, neq, less, greater, lessEq, greaterEq  // 비교
like, notLike                                // 패턴
inList, notInList                            // IN
isNull(), isNotNull()                        // NULL 체크
and, or                                      // 논리 조합
```

> DSL 상세: https://www.jetbrains.com/help/exposed/dsl-crud-operations.html

## external/

OpenFeign 클라이언트. 필요시에만 생성합니다.
