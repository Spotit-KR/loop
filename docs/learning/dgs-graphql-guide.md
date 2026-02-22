# Netflix DGS + GraphQL 학습 가이드

이 프로젝트(Loop Server)에 설정된 DGS 11.0.0 기반으로 GraphQL API를 제공하는 방법을 학습한다.

---

## 목차

1. [GraphQL 핵심 개념 정리](#1-graphql-핵심-개념-정리)
2. [DGS 프레임워크란](#2-dgs-프레임워크란)
3. [프로젝트 설정 현황](#3-프로젝트-설정-현황)
4. [스키마 작성법](#4-스키마-작성법)
5. [DataFetcher 작성법](#5-datafetcher-작성법)
6. [Mutation 작성법](#6-mutation-작성법)
7. [DataLoader와 N+1 문제](#7-dataloader와-n1-문제)
8. [한 요청에 여러 쿼리 보내기](#8-한-요청에-여러-쿼리-보내기)
9. [테스트 작성법](#9-테스트-작성법)
10. [Code Generation](#10-code-generation)
11. [실행 흐름 요약](#11-실행-흐름-요약)
12. [학습용 코드 위치](#12-학습용-코드-위치)

---

## 1. GraphQL 핵심 개념 정리

### REST vs GraphQL

| | REST | GraphQL |
|---|---|---|
| **엔드포인트** | 리소스마다 1개 (GET /shows, GET /shows/1) | 1개 (/graphql) |
| **응답 구조** | 서버가 결정 (고정) | 클라이언트가 결정 (필요한 필드만) |
| **Over-fetching** | 불필요한 필드도 포함 | 없음 |
| **Under-fetching** | 여러 API 호출 필요할 수 있음 | 한 쿼리로 해결 |
| **스키마** | OpenAPI/Swagger (선택적) | 필수 (계약서 역할) |

### 핵심 용어

- **Query**: 데이터 조회 (REST의 GET)
- **Mutation**: 데이터 변경 (REST의 POST/PUT/DELETE)
- **Subscription**: 실시간 데이터 스트림 (WebSocket)
- **Schema**: API 계약서. 타입, 쿼리, 뮤테이션을 정의
- **Resolver/DataFetcher**: 스키마의 각 필드를 실제 데이터로 채우는 함수
- **DataLoader**: N+1 문제를 해결하는 배치 로딩 메커니즘

---

## 2. DGS 프레임워크란

Netflix가 만든 Spring Boot 기반 GraphQL 프레임워크.

**DGS가 제공하는 것:**
- 스키마 파일(`.graphqls`)을 자동으로 읽어 GraphQL API 구성
- `@DgsComponent`, `@DgsQuery`, `@DgsMutation` 등 어노테이션 기반 프로그래밍
- `@DgsDataLoader`로 N+1 문제 해결
- `DgsQueryExecutor`로 쉬운 테스트
- Code Generation (스키마 → Kotlin/Java 타입 자동 생성)
- GraphiQL 쿼리 편집기 내장 (http://localhost:8080/graphiql)

**Spring for GraphQL과의 관계:**
DGS 11.x는 Spring for GraphQL 위에서 동작한다 (통합됨). DGS 고유 어노테이션을 사용하되, 내부적으로는 Spring GraphQL 인프라를 활용한다.

---

## 3. 프로젝트 설정 현황

`build.gradle.kts`에 이미 설정된 내용:

```kotlin
// 플러그인
id("com.netflix.dgs.codegen") version "8.3.0"    // 스키마 → 코드 생성

// 의존성
implementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter")  // DGS 코어
testImplementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter-test")  // 테스트

// BOM (버전 관리)
mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:11.0.0")

// Code Generation 설정
tasks.generateJava {
    schemaPaths.add("$projectDir/src/main/resources/graphql-client")
    packageName = "kr.io.team.loop.codegen"
    generateClient = true   // 클라이언트 쿼리 빌더 생성
}
```

**주요 경로:**
- 서버 스키마: `src/main/resources/schema/*.graphqls` (DGS 기본 경로)
- 클라이언트 스키마: `src/main/resources/graphql-client/` (Codegen용)
- 생성 코드: `build/generated/sources/dgs-codegen/`

---

## 4. 스키마 작성법

`src/main/resources/schema/` 디렉토리에 `.graphqls` 파일을 생성하면 DGS가 자동으로 읽는다.

### 기본 구조

```graphql
# Query — 조회 API 정의
type Query {
    show(id: ID!): Show           # 단건 조회. !는 필수값(non-null)
    shows(titleFilter: String): [Show!]!  # 리스트 조회. [Type!]! = non-null 리스트
}

# Mutation — 변경 API 정의
type Mutation {
    addShow(input: AddShowInput!): Show!
}

# Type — 반환 데이터 구조 (Response DTO에 해당)
type Show {
    id: ID!
    title: String!
    releaseYear: Int       # !가 없으므로 nullable
    actors: [Actor!]!      # 별도 DataFetcher로 lazy loading 가능
}

type Actor {
    id: ID!
    name: String!
}

# Input — 입력 데이터 구조 (Request DTO에 해당)
input AddShowInput {
    title: String!
    releaseYear: Int
}
```

### 타입 시스템

| GraphQL 타입 | Kotlin 매핑 | 설명 |
|---|---|---|
| `String` | `String` | 문자열 |
| `Int` | `Int` | 정수 |
| `Float` | `Double` | 부동소수점 |
| `Boolean` | `Boolean` | 불리언 |
| `ID` | `String` | 고유 식별자 (내부적으로 String) |
| `String!` | `String` (non-null) | 필수값 |
| `String` | `String?` (nullable) | 선택값 |
| `[String!]!` | `List<String>` | non-null 리스트 of non-null 항목 |

---

## 5. DataFetcher 작성법

### 기본 패턴

```kotlin
@DgsComponent  // Spring의 @Component + DGS 인식
class ShowDataFetcher {

    // 메서드명 = 스키마의 Query 필드명
    @DgsQuery
    fun shows(@InputArgument titleFilter: String?): List<Show> {
        return if (titleFilter != null) {
            showRepository.findByTitleContaining(titleFilter)
        } else {
            showRepository.findAll()
        }
    }

    @DgsQuery
    fun show(@InputArgument id: String): Show? {
        return showRepository.findById(id)
    }
}
```

### 자식 필드 DataFetcher

특정 타입의 특정 필드를 별도로 resolve할 때 사용한다. 비용이 큰 필드를 lazy loading할 때 유용하다.

```kotlin
@DgsComponent
class ShowDataFetcher {

    // Show 타입의 actors 필드를 resolve
    // 클라이언트가 actors를 요청할 때만 실행됨!
    @DgsData(parentType = "Show", field = "actors")
    fun actorsForShow(dfe: DataFetchingEnvironment): List<Actor> {
        val show: Show = dfe.getSource()  // 부모 객체 (Show) 접근
        return actorService.findByShowId(show.id)
    }
}
```

**`DataFetchingEnvironment` (dfe)가 제공하는 것:**
- `getSource<T>()` — 부모 객체 접근
- `getArgument("name")` — 쿼리 인자 접근
- `getDataLoader("name")` — DataLoader 접근
- `getSelectionSet()` — 클라이언트가 요청한 필드 목록

---

## 6. Mutation 작성법

```kotlin
@DgsComponent
class ShowMutations {

    // Input 타입을 Map으로 받기
    @DgsMutation
    fun addShow(@InputArgument input: Map<String, Any?>): Show {
        val title = input["title"] as String
        val releaseYear = input["releaseYear"] as Int?
        return showRepository.save(Show(title = title, releaseYear = releaseYear))
    }

    // 개별 스칼라 인자
    @DgsMutation
    fun addRating(
        @InputArgument showId: String,
        @InputArgument stars: Int,
    ): Rating {
        return ratingService.addRating(showId, stars)
    }
}
```

### Query vs Mutation 실행 차이

| | Query | Mutation |
|---|---|---|
| **목적** | 조회 | 변경 |
| **실행 방식** | 루트 필드들이 **병렬** 실행 가능 | 루트 필드들이 **순차** 실행 (순서 보장) |
| **부수효과** | 없어야 함 | 있을 수 있음 |

Mutation의 순차 실행이 중요한 이유:
```graphql
mutation {
    createUser(name: "John")    # 1번째 실행
    addRole(user: "John", role: "ADMIN")  # 2번째 실행 (1번 이후)
}
```

---

## 7. DataLoader와 N+1 문제

### N+1 문제 시나리오

```graphql
query {
    shows {         # 1번 쿼리: Show 5개 조회
        title
        actors {    # 5번 쿼리: 각 Show의 actors 개별 조회
            name
        }
    }
}
# 총 6번 DB 쿼리 = 1(shows) + 5(actors) = N+1 문제
```

### DataLoader로 해결

```kotlin
// 1. BatchLoader 정의
@DgsDataLoader(name = "actorsForShow")
class ActorsBatchLoader(
    private val actorService: ActorService
) : BatchLoader<String, List<Actor>> {

    override fun load(keys: List<String>): CompletionStage<List<List<Actor>>> {
        // keys = ["1", "2", "3", "4", "5"] — 모든 showId가 한 번에 들어옴
        // DB 쿼리 1번: SELECT * FROM actors WHERE show_id IN (keys)
        val actorsByShowId = actorService.findByShowIds(keys)
            .groupBy { it.showId }

        // 반환: keys 순서와 동일하게 매핑
        val results = keys.map { actorsByShowId[it] ?: emptyList() }
        return CompletableFuture.completedFuture(results)
    }
}

// 2. DataFetcher에서 DataLoader 사용
@DgsData(parentType = "Show", field = "actors")
fun actorsForShow(dfe: DataFetchingEnvironment): CompletableFuture<List<Actor>> {
    val show: Show = dfe.getSource()
    val dataLoader = dfe.getDataLoader<String, List<Actor>>("actorsForShow")
    return dataLoader.load(show.id)  // 즉시 실행 안 됨! 등록만.
}

# 총 2번 DB 쿼리 = 1(shows) + 1(actors 배치) = 해결!
```

### BatchLoader vs MappedBatchLoader

| | BatchLoader | MappedBatchLoader |
|---|---|---|
| **반환 타입** | `List<V>` | `Map<K, V>` |
| **순서 보장** | 필수 (keys 순서와 동일해야) | 불필요 (키로 매핑) |
| **적합한 경우** | 모든 키에 결과가 있을 때 | 일부 키에 결과가 없을 수 있을 때 |

### DataLoader 동작 타이밍

```text
1. shows() DataFetcher 실행 → Show 5개 반환
2. Show[0].actors → dataLoader.load("1")  ← 등록만
3. Show[1].actors → dataLoader.load("2")  ← 등록만
4. Show[2].actors → dataLoader.load("3")  ← 등록만
5. Show[3].actors → dataLoader.load("4")  ← 등록만
6. Show[4].actors → dataLoader.load("5")  ← 등록만
7. === 배치 윈도우 종료 ===
8. BatchLoader.load(["1","2","3","4","5"]) 실행  ← 한 번에!
9. 결과가 각 DataFetcher에 매핑되어 반환
```

---

## 8. 한 요청에 여러 쿼리 보내기

GraphQL에서 클라이언트가 한 번의 HTTP 요청에 여러 데이터를 요청하는 방법은 크게 3가지다.

### 방법 1: 한 Query 안에 여러 루트 필드 (가장 일반적)

```graphql
query {
    shows { title }              # 루트 필드 1
    show(id: "1") { title }      # 루트 필드 2
    showsPaged(page: 0) { totalElements }  # 루트 필드 3
}
```

**서버 동작:**
- DGS가 하나의 쿼리를 파싱
- 각 루트 필드의 DataFetcher를 호출
- Query 타입의 루트 필드들은 **독립적으로 (잠재적으로 병렬) 실행**
- 모든 결과를 하나의 응답에 합쳐서 반환

```json
{
  "data": {
    "shows": [{ "title": "..." }],
    "show": { "title": "..." },
    "showsPaged": { "totalElements": 5 }
  }
}
```

### 방법 2: Alias로 같은 필드 다중 호출

같은 필드를 다른 인자로 여러 번 호출하려면 alias가 필요하다:

```graphql
query {
    first: show(id: "1") { title }    # show DataFetcher 1번째 호출
    second: show(id: "2") { title }   # show DataFetcher 2번째 호출
}
```

```json
{
  "data": {
    "first": { "title": "Stranger Things" },
    "second": { "title": "Ozark" }
  }
}
```

### 방법 3: HTTP 배치 요청 (Query Batching)

여러 개의 완전히 독립적인 GraphQL 요청을 하나의 HTTP 요청에 묶어 보내는 것.

```json
// HTTP POST /graphql — body가 배열
[
  {
    "query": "query { shows { title } }"
  },
  {
    "query": "mutation { addShow(input: { title: \"New\" }) { id } }"
  }
]
```

**응답도 배열:**
```json
[
  { "data": { "shows": [...] } },
  { "data": { "addShow": { "id": "6" } } }
]
```

**특징:**
- 각 요청이 완전히 독립적 (별도의 실행 컨텍스트)
- DGS/Spring GraphQL에서 지원 여부는 설정에 따라 다름
- 일반적으로 **방법 1이 가장 많이 사용**됨

### 방법 비교

| 방법 | 사용 사례 | DataLoader 공유 |
|---|---|---|
| 여러 루트 필드 | 관련 데이터를 한 번에 조회 | O (같은 실행 컨텍스트) |
| Alias | 같은 필드를 다른 인자로 | O (같은 실행 컨텍스트) |
| HTTP 배치 | 완전 독립적인 요청 묶음 | X (별도 실행 컨텍스트) |

**DataLoader 공유 여부가 중요한 이유:**
- 방법 1, 2: 같은 DataLoader 인스턴스를 공유하므로 배치 처리 효율이 높음
- 방법 3: 각 요청이 별도 DataLoader를 사용하므로 배치 효과 없음

---

## 9. 테스트 작성법

### DgsQueryExecutor 기본

```kotlin
@SpringBootTest(classes = [ShowDataFetcher::class])
@EnableDgsTest  // DGS 테스트 슬라이스 — GraphQL 실행에 필요한 최소 컴포넌트만 로드
class ShowDataFetcherTest {

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Test
    fun `shows 전체 조회`() {
        // executeAndExtractJsonPath: 쿼리 실행 후 JsonPath로 결과 추출
        val titles: List<String> = dgsQueryExecutor.executeAndExtractJsonPath(
            """
            {
                shows { title }
            }
            """.trimIndent(),
            "data.shows[*].title"  // JsonPath 표현식
        )
        assertThat(titles).contains("Ozark")
    }
}
```

### 주요 메서드

```kotlin
// 1. JsonPath로 특정 값 추출
val title: String = executor.executeAndExtractJsonPath(query, "data.show.title")

// 2. DocumentContext로 여러 값 추출
val ctx = executor.executeAndGetDocumentContext(query)
val title: String = ctx.read("data.show.title")
val actors: List<Map<String,Any>> = ctx.read("data.show.actors")

// 3. ExecutionResult로 에러 확인
val result = executor.execute(query)
assertThat(result.errors).isEmpty()
assertThat(result.isDataPresent).isTrue()

// 4. 변수 전달
val title: String = executor.executeAndExtractJsonPath(
    "query(\$id: ID!) { show(id: \$id) { title } }",
    "data.show.title",
    mapOf("id" to "1")  // variables
)
```

### 외부 서비스 Mocking

```kotlin
@SpringBootTest(classes = [ShowDataFetcher::class])
@EnableDgsTest
class ShowDataFetcherTest {

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockkBean  // SpringMockK로 외부 의존성 Mock
    lateinit var showService: ShowService

    @Test
    fun `mocked 서비스로 테스트`() {
        every { showService.findAll() } returns listOf(Show("1", "Mock Show"))

        val titles: List<String> = dgsQueryExecutor.executeAndExtractJsonPath(
            "{ shows { title } }",
            "data.shows[*].title"
        )
        assertThat(titles).containsExactly("Mock Show")
    }
}
```

---

## 10. Code Generation

스키마에서 Kotlin/Java 타입을 자동 생성한다.

### 설정 (build.gradle.kts)

```kotlin
plugins {
    id("com.netflix.dgs.codegen") version "8.3.0"
}

tasks.generateJava {
    schemaPaths.add("$projectDir/src/main/resources/schema")
    packageName = "kr.io.team.loop.codegen"
    generateClient = true    // 클라이언트 쿼리 빌더 생성
    language = "kotlin"      // Kotlin 코드 생성 (기본값)
}
```

### 생성되는 것

스키마:
```graphql
type Show {
    id: ID!
    title: String!
    releaseYear: Int
}

input AddShowInput {
    title: String!
    releaseYear: Int
}
```

생성 코드:
```kotlin
// 타입
data class Show(val id: String, val title: String, val releaseYear: Int?)

// Input 타입
data class AddShowInput(val title: String, val releaseYear: Int?)

// DgsConstants — 문자열 상수
object DgsConstants {
    object SHOW {
        const val TYPE_NAME = "Show"
        const val Id = "id"
        const val Title = "title"
    }
}

// 클라이언트 쿼리 빌더 (generateClient = true일 때)
// 서버-to-서버 통신이나 테스트에서 사용
```

### 사용 판단

- **Codegen 사용 권장**: 타입이 많을 때, 스키마 변경이 잦을 때
- **수동 작성 가능**: 타입이 적을 때, 기존 도메인 모델을 재사용할 때
- **typeMapping**: 기존 타입을 Codegen에서 재사용하도록 매핑 가능

---

## 11. 실행 흐름 요약

```text
클라이언트 HTTP 요청
  POST /graphql
  { "query": "{ shows { title actors { name } } }" }
    │
    ▼
[1] 쿼리 파싱 — 문자열 → AST(추상 구문 트리)
    │
    ▼
[2] 유효성 검사 — 스키마와 대조 (필드 존재, 타입 매칭)
    │
    ▼
[3] 실행 계획 수립 — 어떤 DataFetcher를 호출할지 결정
    │
    ▼
[4] 루트 필드 실행 — @DgsQuery shows() 호출 → List<Show> 반환
    │
    ▼
[5] 자식 필드 실행 — 각 Show에 대해:
    │  ├─ title → Show 객체에서 바로 반환 (추가 DataFetcher 불필요)
    │  └─ actors → @DgsData actorsForShow() 호출
    │     └─ DataLoader 사용 시 → load() 등록만 (실행 안 함)
    │
    ▼
[6] DataLoader 배치 실행 — 축적된 요청을 한 번에 처리
    │
    ▼
[7] 응답 조립 — 결과 트리를 JSON으로 직렬화
    │
    ▼
HTTP 응답
  { "data": { "shows": [{ "title": "...", "actors": [...] }] } }
```

---

## 12. 학습용 코드 위치

| 파일 | 설명 |
|---|---|
| `src/main/resources/schema/learning.graphqls` | GraphQL 스키마 정의 (주석 포함) |
| `src/main/kotlin/.../learning/LearningModels.kt` | 도메인 모델 (data class) |
| `src/main/kotlin/.../learning/ShowDataFetcher.kt` | DataFetcher 구현 (Query, Mutation, 자식 필드) |
| `src/main/kotlin/.../learning/ShowDataLoader.kt` | DataLoader 구현 (BatchLoader, MappedBatchLoader) |
| `src/test/kotlin/.../learning/DgsGraphQLLearningTest.kt` | 학습용 테스트 (모든 개념 포함) |
| `docs/learning/dgs-graphql-guide.md` | 이 가이드 문서 |
| `docs/dgs-graphql-execution.md` | 실행 흐름 상세 문서 (파이프라인, DataLoader 내부 동작) |

### 실행 방법

1. **테스트 실행**: `./gradlew test --tests "kr.io.team.loop.learning.*"`
2. **서버 시작 후 GraphiQL**: `./gradlew bootRun` → http://localhost:8080/graphiql
3. **Codegen 실행**: `./gradlew generateJava`

### 다음 단계

1. 테스트 코드를 실행하면서 각 쿼리의 동작을 확인
2. `ShowDataLoader.kt`의 주석 처리된 DataLoader 버전을 활성화해서 N+1 해결 확인
3. GraphiQL에서 직접 쿼리를 날려보며 응답 형태 확인
4. 프로덕션 코드 작성 시 DDD 아키텍처와 조합하는 방법 검토
