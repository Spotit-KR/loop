# Netflix DGS GraphQL 쿼리 실행 및 DataFetcher 컨텍스트 가이드

이 문서는 Netflix DGS(Directed Graph Server)의 GraphQL 쿼리 실행 내부 동작과 커스텀 DataFetcher 컨텍스트에 대해 정리한 내용입니다.

## 목차

1. [GraphQL 쿼리 실행 흐름](#graphql-쿼리-실행-흐름)
2. [배치 쿼리 처리 (Batching)](#배치-쿼리-처리-batching)
3. [DataLoader 패턴](#dataloader-패턴)
4. [DGS 실행 파이프라인](#dgs-실행-파이프라인)
5. [커스텀 DataFetcher 컨텍스트](#커스텀-datafetcher-컨텍스트)
6. [Kotlin 구현 예시](#kotlin-구현-예시)

---

## GraphQL 쿼리 실행 흐름

### 기본 개념

GraphQL에서 클라이언트가 여러 쿼리(또는 뮤테이션)를 한 번의 HTTP 요청에 묶어 전송할 수 있습니다. 이를 **배치 쿼리(Batch Query)** 또는 **멀티 오퍼레이션(Multi-Operation)**이라 합니다.

### 단일 쿼리 요청 예시

```graphql
query {
  user(id: "1") {
    id
    name
    posts {
      id
      title
    }
  }
}
```

### 배치 쿼리 요청 예시

```graphql
query GetUser {
  user(id: "1") {
    id
    name
  }
}

query GetPosts {
  posts(limit: 10) {
    id
    title
  }
}
```

HTTP 요청으로는 두 쿼리가 함께 전송되며, DGS는 이를 하나의 배치로 처리합니다.

---

## 배치 쿼리 처리 (Batching)

### 배치 쿼리의 역할

배치 쿼리는 **여러 독립적인 GraphQL 쿼리를 하나의 HTTP 요청**으로 전송하는 방식입니다. 이를 통해:

1. **네트워크 오버헤드 감소** — 여러 HTTP 요청 대신 1개의 요청으로 처리
2. **서버 성능 향상** — 쿼리 파싱, 유효성 검사, 실행의 고정 비용이 감소
3. **클라이언트 편의성 증대** — 관련된 여러 데이터를 한 번에 조회

### 배치 쿼리 HTTP 요청 형식

```json
{
  "operationName": "GetUserAndPosts",
  "query": "query GetUser { ... } query GetPosts { ... }",
  "variables": {}
}
```

또는 Query String:

```
GET /graphql?query=query GetUser { ... } query GetPosts { ... }&operationName=GetUserAndPosts
```

### DGS의 배치 쿼리 처리 프로세스

```
HTTP 요청 수신
    ↓
GraphQL 쿼리 문자열 파싱 (문법 검증)
    ↓
여러 Operation 분리 (GetUser, GetPosts)
    ↓
각 Operation에 대해 독립적으로:
  - 유효성 검사 (Validation)
  - 실행 엔진 초기화
  - DataFetcher 호출
    ↓
모든 Operation의 결과를 배열로 수집
    ↓
JSON 응답으로 반환
```

### 배치 응답 형식

```json
[
  {
    "data": {
      "user": {
        "id": "1",
        "name": "John Doe"
      }
    }
  },
  {
    "data": {
      "posts": [
        {
          "id": "p1",
          "title": "Post 1"
        }
      ]
    }
  }
]
```

---

## DataLoader 패턴

### N+1 문제

GraphQL에서 가장 흔히 발생하는 성능 문제는 **N+1 문제**입니다.

#### 예시: 사용자와 게시물 조회

```
User 1명을 가져오기: 1개 쿼리
User의 Posts 가져오기: N개 쿼리 (각 Post마다 1개)
총 1 + N개 쿼리 실행
```

그래프 구조에서:

```
Query.user(id: "1")
  └─ User
      └─ posts [Post1, Post2, Post3]  ← 각 Post을 하나씩 조회하면 3개 쿼리
```

**N+1 문제의 근본 원인**: 각 필드의 DataFetcher가 **독립적으로 데이터베이스를 조회**하기 때문입니다.

### DataLoader의 원리

DataLoader는 **배치 처리 + 캐싱** 메커니즘을 통해 N+1 문제를 해결합니다.

#### 동작 단계

```
1단계: 로드 요청 수집 (Batching Window)
  - User.posts를 요청하는 DataFetcher 여러 개가 호출됨
  - DataLoader는 요청을 즉시 실행하지 않고 **메모리에 수집**
  
2단계: 배치 처리 (Batch Execution)
  - 일정 시간(ms) 또는 크기 도달 시 **누적된 요청들을 한 번에 처리**
  - 예: getPostsByUserIds([userId1, userId2, userId3])
  - 데이터베이스에는 **1개의 쿼리**만 날아감
  
3단계: 결과 매핑
  - 반환된 결과를 **요청자별로 매핑**
  - 각 DataFetcher가 자신의 결과를 받음
  
4단계: 캐싱
  - 동일한 ID에 대한 재요청은 **캐시에서 즉시 반환**
```

### DataLoader 구조

```
DataLoader<Key, Value>
  - Key: 데이터를 식별하는 값 (예: userId)
  - Value: 로드할 데이터의 타입 (예: List<Post>)
  - BatchLoader: Key 목록을 받아 Value 목록을 반환하는 함수
  - CacheMap: Key → Value 매핑 저장소
```

### 시각적 비교: N+1 vs DataLoader

**N+1 문제 (비효율적)**

```
User 조회: SELECT * FROM users WHERE id = 1  (1개 쿼리)
Post 조회:
  SELECT * FROM posts WHERE user_id = 1     (1개 쿼리)
  SELECT * FROM posts WHERE user_id = 2     (1개 쿼리)
  SELECT * FROM posts WHERE user_id = 3     (1개 쿼리)
총 4개 쿼리
```

**DataLoader 활용 (효율적)**

```
User 조회: SELECT * FROM users WHERE id = 1  (1개 쿼리)
Post 배치 조회:
  SELECT * FROM posts WHERE user_id IN (1, 2, 3)  (1개 쿼리)
총 2개 쿼리
```

### DataLoader 실행 타이밍

DataLoader는 **GraphQL 쿼리 실행 사이클의 각 단계**에서 배치를 처리합니다:

```
1. 쿼리 파싱 (Parse)
2. 유효성 검사 (Validate)
3. 실행 (Execute)
   ├─ Root Field 해결 (Resolve)
   ├─ Child Field 해결
   │  └─ DataFetcher 호출 (동기 또는 비동기)
   │     ├─ DataLoader 요청 등록
   │     └─ CompletableFuture 반환 (즉시 완료되지 않음)
   │
   └─ DataLoader 배치 윈도우 종료 시점
      └─ 축적된 요청들을 한 번에 처리 (배치 함수 호출)
      └─ 결과 매핑 및 캐싱
      └─ 대기 중인 CompletableFuture 완료

4. 응답 생성 (Serialize)
```

---

## DGS 실행 파이프라인

### HTTP 요청에서 응답까지의 전체 흐름

```
┌─────────────────────────────────────────┐
│   1. HTTP 요청 수신                      │
│   POST /graphql                         │
│   { "query": "...", "variables": {...} }│
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│   2. 요청 파싱 (GraphQL 엔진)            │
│   - 쿼리 문자열 → AST (추상 구문 트리)  │
│   - 배치 쿼리 분리                      │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│   3. 유효성 검사 (Validation)            │
│   - 쿼리가 스키마와 일치하는지 확인     │
│   - 변수 타입 검사                      │
│   - 필드 존재 여부 확인                 │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│   4. 실행 컨텍스트 생성                  │
│   - DGSContext 인스턴스화                │
│   - DataLoader 초기화                   │
│   - HTTP 헤더/쿠키 저장                 │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│   5. 쿼리 실행 (Execution)               │
│   Root Query Field 처리:                │
│   ├─ QueryResolver 메서드 호출           │
│   └─ 반환값 (User 엔티티) 획득          │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│   6. 자식 필드 해결 (Field Resolution)   │
│   User.posts:                           │
│   ├─ PostsDataFetcher 호출              │
│   ├─ DataLoader.load(userId)등록        │
│   └─ CompletableFuture<List<Post>>반환  │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│   7. 배치 윈도우 종료 & 배치 처리        │
│   DataLoader.dispatch():                │
│   ├─ 축적된 로드 요청 수집               │
│   ├─ BatchLoader 함수 실행              │
│   │  (SELECT * FROM posts WHERE ...)    │
│   ├─ 결과 매핑                          │
│   └─ 캐시 저장                          │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│   8. 깊이 우선 탐색 계속 (DFS)            │
│   Post의 자식 필드들 (author, comments) │
│   DataLoader를 통해 배치 처리            │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│   9. 모든 CompletableFuture 완료 대기    │
│   비동기 작업 모두 완료될 때까지        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│  10. 응답 직렬화 (Serialization)         │
│  결과 트리 → JSON 변환                   │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│  11. HTTP 응답 반환                      │
│  { "data": {...} }                      │
└─────────────────────────────────────────┘
```

### 각 단계의 상세 설명

#### 1-2단계: 파싱

DGS는 GraphQL 쿼리 문자열을 **Abstract Syntax Tree(AST)**로 변환합니다:

```
입력:  query GetUser { user(id: "1") { name } }
       query GetPosts { posts { id } }

AST:   Operation(name="GetUser")
         SelectionSet
           Field(name="user", args=[id: "1"])
             SelectionSet
               Field(name="name")
       Operation(name="GetPosts")
         SelectionSet
           Field(name="posts")
             SelectionSet
               Field(name="id")
```

#### 3단계: 유효성 검사

GraphQL 스키마와 대조하여 쿼리의 유효성을 확인합니다:

```
스키마:
  type Query {
    user(id: ID!): User
    posts: [Post!]!
  }
  
  type User {
    id: ID!
    name: String!
    posts: [Post!]!
  }

쿼리 검증:
  ✓ Query 타입에 user 필드 존재 확인
  ✓ user(id: String) 인자 타입 매칭 확인
  ✓ User 타입에 name 필드 존재 확인
  ✓ posts 필드 존재 확인
```

#### 4단계: 실행 컨텍스트 생성

```kotlin
class DGSContext(
    val request: HttpServletRequest,
    val headers: HttpHeaders,
    val cookies: Map<String, String>,
    val dataLoaders: DataLoaderRegistry,  // 모든 DataLoader 저장소
    val customData: MutableMap<String, Any> = mutableMapOf()
)
```

#### 5-8단계: 깊이 우선 탐색 실행

GraphQL 엔진은 쿼리 AST를 **깊이 우선 탐색(DFS)**으로 순회하면서 각 필드를 해결합니다.

```
Query
  └─ user(id: "1")              ← Root 해결
      └─ name                   ← 스칼라 필드 해결
      └─ posts                  ← DataLoader 요청 등록
          └─ (배치 윈도우 종료)
              └─ id              ← 배치 결과 내 필드 해결
              └─ author
                  └─ (또 다른 DataLoader)
```

#### 9단계: 비동기 완료 대기

```kotlin
// 모든 CompletableFuture가 완료될 때까지 대기
CompletableFuture.allOf(*futures.toTypedArray()).get()
```

---

## 커스텀 DataFetcher 컨텍스트

### DGSContext 접근

DGS에서는 각 DataFetcher가 `DGSContext`에 접근할 수 있습니다. 이를 통해:

1. **HTTP 요청 정보** — 헤더, 쿠키, 경로 등
2. **사용자 정보** — 인증된 사용자, 권한
3. **DataLoader** — N+1 문제 해결
4. **커스텀 데이터** — 리소스 풀, 서비스 인스턴스

### DGSContext 구조

```kotlin
@Component
class MyDgsComponent {
    
    @DgsQuery
    fun user(
        @DgsArgument id: String,
        dgsContext: DGSContext
    ): User {
        // HTTP 요청 정보 접근
        val userAgent = dgsContext.request.getHeader("User-Agent")
        val userId = dgsContext.request.getAttribute("userId") as String?
        
        // 커스텀 데이터 저장/접근
        dgsContext.customData["userId"] = userId
        
        // DataLoader 접근
        val dataLoader = dgsContext.getDataLoader<Int, User>("userLoader")
        
        return User(id, "John")
    }
    
    @DgsData(parentType = "User", fieldName = "posts")
    fun userPosts(
        dgsDataFetchingEnvironment: DgsDataFetchingEnvironment
    ): List<Post> {
        val user = dgsDataFetchingEnvironment.getSource<User>()
        val dgsContext = dgsDataFetchingEnvironment.dgsContext
        
        // DataLoader를 사용한 배치 처리
        val dataLoader = dgsContext.getDataLoader<Int, List<Post>>("postsLoader")
        return dataLoader.load(user.id).get()
    }
}
```

### DGSDataFetchingEnvironment

```kotlin
interface DGSDataFetchingEnvironment {
    // 부모 객체 접근
    fun <T> getSource(): T
    
    // 현재 필드 정보
    fun getFieldName(): String
    fun getArguments(): Map<String, Any>
    
    // DataFetcher 컨텍스트
    fun getDgsContext(): DGSContext
    
    // 실행 컨텍스트
    fun getExecutionStepInfo(): ExecutionStepInfo
}
```

### 커스텀 컨텍스트 데이터 사용

```kotlin
// 1. 컨텍스트에 데이터 저장 (Root Query)
@DgsQuery
fun me(dgsContext: DGSContext): User {
    val authentication = SecurityContextHolder.getContext().authentication
    dgsContext.customData["currentUser"] = authentication.principal
    
    return User(authentication.name)
}

// 2. 다른 필드에서 접근
@DgsData(parentType = "User", fieldName = "friends")
fun userFriends(
    dgsDataFetchingEnvironment: DgsDataFetchingEnvironment
): List<User> {
    val user = dgsDataFetchingEnvironment.getSource<User>()
    val currentUser = dgsDataFetchingEnvironment.dgsContext
        .customData["currentUser"] as UserPrincipal?
    
    // currentUser 권한으로 friends 조회
    return if (currentUser?.id == user.id) {
        // 자신의 친구 목록
        userService.getAllFriends(user.id)
    } else {
        // 공개 친구 목록만
        userService.getPublicFriends(user.id)
    }
}
```

---

## Kotlin 구현 예시

### 1. DataLoader 정의

```kotlin
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsDataLoaderProvider
import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import kotlin.coroutines.coroutineContext

@DgsComponent
class PostDataLoaderProvider(
    private val postService: PostService
) {
    
    /**
     * Post 배치 로더
     * Key: User ID, Value: List<Post>
     */
    @DgsDataLoaderProvider(name = "postsLoader")
    fun postsDataLoader(): DataLoader<Int, List<Post>> {
        val batchLoader = BatchLoader<Int, List<Post>> { userIds ->
            // userIds는 이 배치에서 요청된 모든 User ID들
            println("Batch loading posts for user IDs: $userIds")
            
            // 1개의 배치 쿼리로 모든 데이터 조회
            postService.findPostsByUserIds(userIds)
                .groupBy { it.userId }
                .mapKeys { it.key }
                .let { postsByUserId ->
                    // 결과를 입력 순서대로 매핑하여 반환
                    userIds.map { userId ->
                        postsByUserId[userId] ?: emptyList()
                    }
                }
                .let { java.util.concurrent.CompletableFuture.completedFuture(it) }
        }
        
        return DataLoaderFactory.newDataLoader(batchLoader)
    }
    
    /**
     * User 배치 로더
     * Key: User ID, Value: User
     */
    @DgsDataLoaderProvider(name = "userLoader")
    fun userDataLoader(): DataLoader<Int, User> {
        val batchLoader = BatchLoader<Int, User> { userIds ->
            println("Batch loading users for IDs: $userIds")
            
            val users = userService.findUsersByIds(userIds)
            val usersById = users.associateBy { it.id }
            
            // 입력 순서대로 결과 반환 (없으면 null)
            java.util.concurrent.CompletableFuture.completedFuture(
                userIds.map { usersById[it] }
            )
        }
        
        return DataLoaderFactory.newDataLoader(batchLoader)
    }
}
```

### 2. DataFetcher에서 DataLoader 사용

```kotlin
@DgsComponent
class UserQueryResolver(
    private val userService: UserService
) {
    
    @DgsQuery
    fun user(
        @DgsArgument id: Int,
        dgsContext: DGSContext
    ): User {
        // Root 쿼리 — 직접 조회
        return userService.findUserById(id)
    }
    
    @DgsQuery
    fun users(
        @DgsArgument limit: Int = 10,
        dgsContext: DGSContext
    ): List<User> {
        return userService.findUsers(limit)
    }
}

@DgsComponent
class UserDataFetchers(
    private val userService: UserService,
    private val postService: PostService
) {
    
    /**
     * User.posts 필드 해결
     * 여러 User의 posts를 조회할 때:
     * - DataLoader 요청들이 축적됨
     * - 배치 윈도우 종료 시 한 번에 처리
     */
    @DgsData(parentType = "User", fieldName = "posts")
    fun userPosts(
        dgsDataFetchingEnvironment: DgsDataFetchingEnvironment
    ): java.util.concurrent.CompletableFuture<List<Post>> {
        val user = dgsDataFetchingEnvironment.getSource<User>()
        val dgsContext = dgsDataFetchingEnvironment.dgsContext
        
        // DataLoader 획득
        val dataLoader = dgsContext.getDataLoader<Int, List<Post>>("postsLoader")
        
        // 로드 요청 등록 (즉시 실행되지 않음)
        return dataLoader.load(user.id)
    }
    
    /**
     * Post.author 필드 해결
     * 여러 Post의 author를 조회할 때 DataLoader로 배치 처리
     */
    @DgsData(parentType = "Post", fieldName = "author")
    fun postAuthor(
        dgsDataFetchingEnvironment: DgsDataFetchingEnvironment
    ): java.util.concurrent.CompletableFuture<User> {
        val post = dgsDataFetchingEnvironment.getSource<Post>()
        val dgsContext = dgsDataFetchingEnvironment.dgsContext
        
        val dataLoader = dgsContext.getDataLoader<Int, User>("userLoader")
        return dataLoader.load(post.authorId)
    }
    
    /**
     * User.friends 필드 — 커스텀 컨텍스트 사용
     */
    @DgsData(parentType = "User", fieldName = "friends")
    fun userFriends(
        dgsDataFetchingEnvironment: DgsDataFetchingEnvironment
    ): List<User> {
        val user = dgsDataFetchingEnvironment.getSource<User>()
        val dgsContext = dgsDataFetchingEnvironment.dgsContext
        
        // 현재 로그인한 사용자 확인
        val currentUserId = dgsContext.customData["currentUserId"] as? Int
        
        return if (currentUserId == user.id) {
            // 자신이면 모든 친구 반환
            userService.getAllFriends(user.id)
        } else {
            // 다른 사용자이면 공개 친구만 반환
            userService.getPublicFriends(user.id)
        }
    }
}
```

### 3. 배치 쿼리 클라이언트 요청 예시

```kotlin
@Test
fun `배치 쿼리 - 여러 사용자 및 게시물 동시 조회`() {
    val batchQuery = """
        query GetUser1 {
            user(id: 1) {
                id
                name
                posts {
                    id
                    title
                    author {
                        id
                        name
                    }
                }
            }
        }
        
        query GetUser2 {
            user(id: 2) {
                id
                name
                posts {
                    id
                    title
                }
            }
        }
    """.trimIndent()
    
    // HTTP 요청 실행
    val results = graphQLClient.executeBatchQuery(batchQuery)
    
    // 결과: [GetUser1의 결과, GetUser2의 결과]
    // DataLoader 덕분에:
    // - User 조회: 2개 쿼리
    // - Post 조회: 1개 배치 쿼리 (WHERE user_id IN (1, 2))
    // - Author 조회: 1개 배치 쿼리 (모든 저자)
    // 총 4개 쿼리로 처리됨 (N+1이 없음)
    
    assertThat(results).hasSize(2)
    assertThat(results[0].data.user.posts).isNotEmpty()
    assertThat(results[1].data.user.posts).isNotEmpty()
}
```

### 4. 커스텀 DGSContext 구성

```kotlin
/**
 * 애플리케이션별 확장된 DGSContext
 */
class AppDgsContext(
    request: HttpServletRequest,
    headers: HttpHeaders,
    cookies: Map<String, String>,
    dataLoaders: DataLoaderRegistry,
    val authentication: UserPrincipal? = null,
    val tenantId: String? = null,
    val requestId: String = UUID.randomUUID().toString()
) : DGSContext(request, headers, cookies, dataLoaders) {
    
    fun getCurrentUser(): UserPrincipal? = authentication
    
    fun getTenantId(): String = tenantId 
        ?: throw IllegalStateException("Tenant ID not set")
}

@DgsComponent
class ContextProvider(
    private val authService: AuthService
) {
    
    @DgsComponent
    fun provideContext(
        request: HttpServletRequest
    ): AppDgsContext {
        val authentication = SecurityContextHolder.getContext().authentication
        val userPrincipal = authentication?.principal as? UserPrincipal
        
        val tenantId = request.getHeader("X-Tenant-ID")
        
        return AppDgsContext(
            request = request,
            headers = HttpHeaders(), // 실제로는 요청에서 추출
            cookies = emptyMap(),
            dataLoaders = DataLoaderRegistry(),
            authentication = userPrincipal,
            tenantId = tenantId
        )
    }
}

@DgsComponent
class SecureDataFetchers {
    
    @DgsQuery
    fun me(dgsContext: DGSContext): User? {
        val appContext = dgsContext as? AppDgsContext ?: return null
        return appContext.getCurrentUser()?.let {
            User(id = it.id, name = it.username)
        }
    }
    
    @DgsData(parentType = "User", fieldName = "profile")
    fun userProfile(
        dgsDataFetchingEnvironment: DgsDataFetchingEnvironment
    ): UserProfile {
        val user = dgsDataFetchingEnvironment.getSource<User>()
        val appContext = dgsDataFetchingEnvironment.dgsContext as AppDgsContext
        
        val currentUser = appContext.getCurrentUser()
        val tenantId = appContext.getTenantId()
        
        // Tenant별 데이터 격리
        return userProfileService.findProfileByUserIdAndTenant(
            user.id,
            tenantId,
            canViewPrivate = currentUser?.id == user.id
        )
    }
}
```

### 5. DataLoader와 배치 쿼리 통합 예시

```kotlin
@Test
fun `복잡한 쿼리 - N+1 문제 없음`() {
    val query = """
        query {
            users(limit: 10) {              # 1개 쿼리: 10명의 User 조회
                id
                name
                posts {                     # N+1이 아님!
                    id                      # 배치 쿼리 1개: 10명의 posts 일괄 조회
                    title
                    author {                # 배치 쿼리 1개: 모든 author 일괄 조회
                        id
                        name
                        friends {           # 배치 쿼리 1개: 모든 author의 friends 일괄 조회
                            id
                            name
                        }
                    }
                    comments {              # 배치 쿼리 1개: 모든 posts의 comments 일괄 조회
                        id
                        content
                    }
                }
            }
        }
    """.trimIndent()
    
    // 실행 흐름:
    // 1. users(limit: 10) 조회 → 10명의 User
    // 2. User.posts DataFetcher 호출 × 10번
    //    → postsLoader.load(userId) × 10번 요청 등록
    //    → 배치 윈도우 종료
    //    → SELECT * FROM posts WHERE user_id IN (1,2,...,10)
    //
    // 3. Post.author DataFetcher 호출 (모든 posts에 대해)
    //    → userLoader.load(authorId) 요청들 축적
    //    → 배치 처리: SELECT * FROM users WHERE id IN (...)
    //
    // 4. User.friends DataFetcher 호출 (모든 author에 대해)
    //    → friendsLoader.load(userId) 요청들 축적
    //    → 배치 처리: 1개 쿼리
    //
    // 5. Post.comments DataFetcher 호출 (모든 posts에 대해)
    //    → commentsLoader.load(postId) 요청들 축적
    //    → 배치 처리: 1개 쿼리
    
    val result = graphQLClient.executeQuery(query)
    assertThat(result.data.users).hasSize(10)
    assertThat(result.data.users[0].posts).isNotEmpty()
}
```

---

## 핵심 요약

| 개념 | 설명 |
|------|------|
| **배치 쿼리** | 여러 GraphQL 쿼리를 1개 HTTP 요청으로 전송 |
| **DataLoader** | 여러 DataFetcher의 요청을 모아서 1개의 배치 쿼리로 처리 |
| **배치 윈도우** | DataLoader가 요청을 수집하는 시간 구간 (보통 마이크로초 단위) |
| **N+1 문제** | 각 필드를 독립적으로 조회하면 Query 수가 기하급수적으로 증가 |
| **DGSContext** | HTTP 요청, 사용자 정보, DataLoader에 접근 가능한 컨텍스트 객체 |
| **DFS 실행** | GraphQL은 깊이 우선 탐색으로 필드를 순회하며 해결 |

---

## 참고 자료

- Netflix DGS 공식 문서: https://netflix.github.io/dgs/
- DataLoader 패턴: https://github.com/graphql-java/java-dataloader
- GraphQL 스펙: https://spec.graphql.org/

