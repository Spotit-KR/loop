# Loop Server Architecture v2

## 기술 스택

- **Spring Boot 4.0.3** (Spring Framework 7.x) / **Kotlin 2.3.10** / **Java 25**
- **Netflix DGS Framework** (GraphQL) + **DGS Codegen** — 모든 엔드포인트를 GraphQL로 제공, 스키마 타입은 Codegen으로 자동 생성
- **Exposed ORM** (Kotlin DSL ORM) + **Flyway** (마이그레이션)
- **PostgreSQL** (메인 DB) / **H2** (개발/테스트)
- **Spring Cloud OpenFeign** (외부 API)
- **Kotest** (BehaviorSpec) / **MockK** / **SpringMockK**

## 의존성 규칙

Domain이 중심이며, 나머지 레이어가 Domain에 의존합니다.

- **Domain** → 외부 의존 없음. 순수 비즈니스 로직.
- **Presentation** → Application (Service 호출) + Domain (Command/Query 생성, VO·Entity 사용)
- **Application** → Domain (Command/Query, Repository 인터페이스, Entity, VO 사용)
- **Infrastructure** → Domain (Repository 인터페이스 구현)

## 디렉토리 구조

```text
kr.io.team.loop/
├── ServerApplication.kt
├── common/                        # 공통 모듈
│   ├── domain/                   # 공유 VO (MemberId, TaskId 등)
│   └── config/                   # 공통 설정 (Security, GraphQL 등)
└── {bounded-context}/            # 도메인별 Bounded Context
    ├── presentation/
    │   ├── datafetcher/          # @DgsComponent (Query/Mutation 리졸버)
    │   └── dataloader/           # @DgsDataLoader (N+1 방지, 필요시에만)
    ├── application/
    │   ├── dto/                  # (선택) 변환 필요시에만 생성
    │   └── service/              # BC당 1개 Service 기본
    ├── domain/
    │   ├── model/                # 엔티티, VO, Command, Query
    │   ├── repository/           # Repository 인터페이스
    │   └── service/              # Domain Service (필요시에만)
    └── infrastructure/
        ├── persistence/          # Repository 구현 + Exposed Table 정의
        └── external/             # OpenFeign 클라이언트 (필요시에만)

src/main/resources/schema/         # GraphQL 스키마 정의
└── {bounded-context}.graphqls     # BC별 스키마 파일
```

## 레이어별 상세 규칙

각 레이어의 상세 규칙, 코드 예시, 도입 기준은 개별 문서를 참고합니다.
**해당 레이어 코드를 작성하기 전에 반드시 해당 문서(docs/layers/{layer}.md)를 읽고 패턴을 따를 것.**

- **[Domain Layer](layers/domain.md)** — 엔티티, VO, Command/Query, Repository 인터페이스, Domain Service
- **[Application Layer](layers/application.md)** — Service (BC당 1개 기본, 150줄 분리), Application DTO (선택적)
- **[Infrastructure Layer](layers/infrastructure.md)** — Exposed Table (BC 소유), Repository 구현체, BC 간 FK 참조
- **[Presentation Layer](layers/presentation.md)** — GraphQL 스키마, DGS DataFetcher, DataLoader
- **Common** — 공유 VO(`domain/`), 공통 설정(`config/`). 2개 이상 BC에서 사용하는 VO만 배치. 필요한 것만 생성.

## 데이터 흐름

### Mutation (CUD) — Application DTO 없는 경우 (기본)

```txt
GraphQL Request → DGS Framework 파싱
    ↓
DataFetcher: Codegen Input(Primitive) → TaskCommand.Create(VO) 변환
    ↓
Service: TaskCommand.Create 수신 → Repository에 전달, 트랜잭션 관리
    ↓
Repository: TaskCommand.Create 수신 → 영속화 → Task 엔티티 반환
    ↓
Service: Task 엔티티 직접 반환
    ↓
DataFetcher: Task 반환 → DGS가 스키마 기반으로 필드 직렬화
```

### Mutation (CUD) — Application DTO 있는 경우

```txt
GraphQL Request → DGS Framework 파싱
    ↓
DataFetcher: Codegen Input(Primitive) → AuthCommand.Register(VO) 변환
    ↓
Service: AuthCommand.Register 수신 → 비즈니스 로직 수행
    ↓
Service: Entity → AuthTokenDto(보안 필드 제외) 변환 후 반환
    ↓
DataFetcher: AuthTokenDto 반환 → DGS가 스키마 기반으로 필드 직렬화
```

### Query (R)

```txt
GraphQL Request → DGS Framework 파싱
    ↓
DataFetcher: Codegen 타입/GraphQL 인자(Primitive) → TaskQuery(VO) 변환
    ↓
Service: TaskQuery 수신 → Repository에 전달
    ↓
Repository: TaskQuery 수신 → 조회 → List<Task> 반환
    ↓
Service: List<Task> 직접 반환
    ↓
DataFetcher: List<Task> 반환 → DGS가 스키마 기반으로 필드 직렬화
```

## BC 간 통신 규칙

- **Domain 레이어**: `common/domain/`의 공유 VO만 사용 (예: `MemberId`, `TaskId`)
- **Infrastructure 레이어**: 다른 BC의 Table을 FK로 참조 허용
- **직접 참조 금지**: common을 제외한 다른 BC의 코드를 직접 import하지 않음
- **publicapi 불필요**: 현재 규모에서는 BC 간 public API 레이어를 만들지 않음

## 파일 명명 규칙

| 레이어 | 파일 | 명명 규칙 | 예시 |
|--------|------|-----------|------|
| Domain | 엔티티 | `{Entity}.kt` | `Task.kt` |
| Domain | VO | `{VOName}.kt` | `TaskTitle.kt` |
| Domain | Command | `{Entity}Command.kt` | `TaskCommand.kt` |
| Domain | Query | `{Entity}Query.kt` | `TaskQuery.kt` |
| Domain | Repository 인터페이스 | `{Entity}Repository.kt` | `TaskRepository.kt` |
| Domain | ReadRepository | `{Entity}ReadRepository.kt` | `TaskReadRepository.kt` |
| Domain | Domain Service | `{Concept}DomainService.kt` | `TaskScheduleDomainService.kt` |
| Application | Service | `{BC}Service.kt` | `TaskService.kt` |
| Application | Service (분리시) | `{BC}{Command\|Query}Service.kt` | `TaskCommandService.kt` |
| Application | DTO | `{Name}Dto.kt` | `AuthTokenDto.kt` |
| Infrastructure | Table | `{Entity}Table.kt` | `TaskTable.kt` |
| Infrastructure | Repository 구현체 | `Exposed{Entity}Repository.kt` | `ExposedTaskRepository.kt` |
| Presentation | DataFetcher | `{BC}DataFetcher.kt` | `TaskDataFetcher.kt` |
| Presentation | DataLoader | `{Entity}DataLoader.kt` | `TaskDataLoader.kt` |
| Schema | GraphQL 스키마 | `{bc}.graphqls` | `task.graphqls` |

## 선택적 레이어 도입 기준

기본은 최소 구조. 다음 정량적 기준을 충족할 때 선택적으로 도입합니다.

| 선택적 요소 | 도입 기준 |
|-------------|-----------|
| Application DTO | 집계 결과, 보안 필드 제외, 계산 필드 필요시 |
| ReadRepository 분리 | JOIN 3개 이상인 복합 조회가 있을 때 |
| Service 분리 | 단일 Service가 150줄 초과할 때 |
| Domain Service | 도메인 객체 여럿 엮임 + 100줄 이상 + 여러 repo 조합이 아닐 때 |
| `application/dto/` 디렉토리 | Application DTO가 1개 이상 필요할 때 |
| `infrastructure/external/` | 외부 API 호출이 필요할 때 |
| DataLoader | 부모 목록 조회 시 자식 필드가 N+1 쿼리를 유발할 때 |
| 새 파일 생성 | 기존 파일에 추가할 수 없는지 먼저 확인 |
| common 배치 | 2개 이상의 BC에서 사용하는 VO만 common/domain/에 배치 |

## 테스트 전략

| 레이어 | 유형 | 필수 여부 | Spring Context | 방식 |
|--------|------|----------|----------------|------|
| Domain | 단위 테스트 | 필수 | 없음 | 순수 Kotlin POJO 테스트 |
| Application | 단위 테스트 | 필수 | 없음 | MockK으로 의존성 Mocking |
| Infrastructure | 통합 테스트 | 선택적 (사람이 판단) | 있음 | H2 DB, @SpringBootTest |
| Presentation | 통합 테스트 | 선택적 (사람이 판단) | 있음 | `@EnableDgsTest` + `DgsQueryExecutor` |
| Architecture | ArchUnit | 필수 | 없음 | JUnit5 `@ArchTest` (Kotest 예외) |

테스트 클래스명: `대상클래스명Test` (예: `TaskServiceTest`)

> **참고**: `ArchitectureTest`는 ArchUnit의 `@AnalyzeClasses` + `@ArchTest`를 사용하므로 JUnit5 스타일로 작성합니다. 이 프로젝트에서 유일하게 Kotest BehaviorSpec이 아닌 JUnit5 스타일 테스트입니다.

## 완전 예시: task BC

```text
task/
├── presentation/
│   └── datafetcher/
│       └── TaskDataFetcher.kt      # @DgsComponent (Query/Mutation 리졸버)
├── application/
│   └── service/
│       └── TaskService.kt          # BC당 1개 (dto/ 없음)
├── domain/
│   ├── model/
│   │   ├── Task.kt                 # 엔티티
│   │   ├── TaskTitle.kt            # VO
│   │   ├── TaskDescription.kt      # VO
│   │   ├── TaskStatus.kt           # enum
│   │   ├── TaskCommand.kt          # sealed interface
│   │   └── TaskQuery.kt            # data class
│   └── repository/
│       └── TaskRepository.kt       # 인터페이스 (Read/Write 통합)
└── infrastructure/
    └── persistence/
        ├── TaskTable.kt            # Exposed Table (BC 소유)
        └── ExposedTaskRepository.kt

src/main/resources/schema/
└── task.graphqls                    # GraphQL 스키마
```

**파일 수: 11개** — Application DTO 없음, Service 1개, Repository 통합, 스키마 1개.

## 완전 예시: auth BC (task BC와의 차이점)

보안 필드 제외가 필요하므로 `application/dto/AuthTokenDto.kt`가 추가되고, Service가 Entity 대신 DTO를 반환합니다. 나머지 구조는 task BC와 동일합니다.
