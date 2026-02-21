# 프로젝트 평가 — 아키텍처, 제약, 그리고 에이전트 팀 경험

> 2시간 동안 에이전트 팀 16명을 거쳐 5개 BC(common, auth, member, category, task)를 구현한 경험에 대한 종합 평가.

---

## 1. 아키텍처에 대한 평가

### DDD + Clean Architecture: 이 규모에서 적절했는가?

**솔직히 말하면, 과했다.**

Loop Server는 "할일 기록 + 회고" 서비스다. 핵심 도메인은 CRUD에 가깝고, 복잡한 비즈니스 규칙이 거의 없다. "카테고리 최대 10개", "하위 할일 있으면 삭제 불가" 정도가 가장 복잡한 규칙인데, 이 수준의 복잡도에 4레이어 분리 + BC 격리 + Command/Query 패턴은 상당한 오버엔지니어링이다.

실제로 생성된 파일을 보면:
- **Category BC**: 비즈니스 로직은 Service 4개에 각 5~15줄. 나머지는 전부 변환·전달 코드.
- **Task BC**: GetTasksByDateService의 groupBy 로직이 가장 복잡한데, 이것도 10줄 수준.
- 전체 코드 중 실제 "비즈니스 로직"이 차지하는 비중은 체감상 20% 미만. 나머지 80%는 레이어 간 변환(Request→Command, Entity→DTO, DTO→Response)과 패턴 준수를 위한 보일러플레이트.

**하지만 학습/실험 목적으로는 가치가 있었다.** 에이전트 팀이 아키텍처 규칙을 따라 코드를 생성하고, 리뷰어가 위반을 잡아내고, 리팩토링하는 전체 사이클을 경험한 것 자체가 의미 있다.

### Command/Query 패턴: 엄격 모드의 현실

리팩토링 2차에서 `findById`, `findByEmail` 같은 편의 메서드를 모두 제거하고 `findAll(Query).firstOrNull()`로 통일했다. 아키텍처 규칙에는 맞지만, 결과 코드를 보면:

```kotlin
// Before (직관적)
val task = taskRepository.findById(command.taskId)
    ?: throw NoSuchElementException("Task not found")

// After (규칙 준수)
val task = taskRepository.findAll(TaskQuery(taskId = command.taskId)).firstOrNull()
    ?: throw NoSuchElementException("Task not found")
```

가독성이 떨어졌다. `findAll`이라는 이름은 "목록 조회"를 암시하는데, 실제로는 단건 조회 용도로 쓰이고 있다. 규칙의 일관성과 코드의 표현력 사이에서 트레이드오프가 발생한 것이다.

Repository 반환 타입도 마찬가지. `existsByMemberIdAndName`을 `findAll(query).isEmpty()`로 바꾸면 쿼리 효율이 떨어진다(전체 row를 로드한 뒤 empty 체크). 규칙 준수를 위해 성능을 희생한 셈이다. 물론 현 시점에서 데이터가 적으니 문제되지 않지만, 규칙이 실무적 합리성을 넘어서고 있다는 신호다.

**교훈**: 아키텍처 규칙은 "왜 이 규칙이 존재하는가"를 함께 정의해야 한다. "Repository 출력은 엔티티만"이라는 규칙이 `existsBy...`나 `count...` 같은 유틸리티 메서드까지 금지할 의도였는지는 의문이다.

### BC 격리: 실용적 타협이 필요했던 지점

Category BC의 "하위 할일 있으면 삭제 불가" 규칙이 대표적이다:
1. 처음엔 `CategoryRepository`에 `existsTasksByCategory()` → Boolean 반환 (규칙 위반)
2. 1차 리팩토링: `findTaskIdsByCategoryId()` → `List<TaskId>` 반환 (여전히 엔티티가 아님)
3. 2차 리팩토링: Repository 인터페이스에서 제거 → infrastructure 구현체 내부로 이전

최종 결과는 **DeleteCategoryService가 태스크 존재 여부를 모르는 채로 `categoryRepository.delete(command)`를 호출하고, 구현체 내부에서 `require(!hasTasks)`를 던지는 구조**가 되었다. 비즈니스 규칙이 Application Service에서 Infrastructure로 흘러내린 것이다. 이것이 과연 "더 나은 아키텍처"인지는 논란의 여지가 있다.

BC 격리는 대규모 시스템에서 팀 간 독립성을 보장하기 위한 것인데, 1인 또는 소규모 팀 프로젝트에서는 오히려 간단한 로직을 복잡하게 만든다.

---

## 2. 기술 스택에 대한 평가

### Spring Boot 4 + Kotlin 2.3 + Java 25 + Exposed 1.0.0

**최전선(bleeding edge) 스택이었다.** 코더들의 피드백에서 가장 큰 고통 지점은 **Exposed 1.0.0의 패키지 변경**(`org.jetbrains.exposed.sql.*` → `org.jetbrains.exposed.v1.*`)이었다. 인터넷에 레퍼런스가 적고, 에이전트의 학습 데이터에도 반영이 안 되어 있어서 member-coder가 가장 오래 헤맸다.

Spring Boot 4 + Spring Security 7도 마찬가지. `authorizeHttpRequests` API가 바뀌었고, H2 Console 보안 설정도 달라져서 auth-coder가 여러 차례 시행착오를 겪었다.

**에이전트 팀으로 bleeding edge 스택을 다루는 것은 비효율적이다.** 에이전트는 기본적으로 학습 데이터에 의존하는데, 최신 라이브러리는 학습 데이터에 없다. Planner가 공식 문서 URL을 전달받아 웹 조사를 하긴 했지만, 그 결과를 Coder에게 다시 전달하는 과정에서 정보 손실이 발생했다.

### Exposed ORM 자체에 대해

Exposed는 Kotlin DSL ORM이라는 점에서 매력적이지만, 1.0.0 버전은 아직 에코시스템이 약하다. 코더들이 공통적으로 힘들어한 것:
- `Column<Long>` vs `Column<EntityID<Long>>` 구분 → 쿼리 작성법이 달라짐
- `long().references()` vs `reference()` 패턴 혼재
- `kotlinx.datetime` ↔ `java.time` 변환 필요성

JPA/Hibernate였다면 에이전트들이 훨씬 수월하게 작업했을 것이다. 학습 데이터에 풍부하기 때문이다. 물론 JPA를 선택하지 않은 데에는 이유가 있겠지만, **에이전트 팀 활용도를 고려하면 주류 기술을 택하는 것이 효율적**이라는 현실적 결론이 있다.

### TDD: 에이전트가 TDD를 하면

CLAUDE.md에 TDD가 필수로 명시되어 있었다. 코더들이 Domain/Application 레이어에서 RED→GREEN→REFACTOR를 따랐는데:

**좋았던 점**: 테스트가 확실히 견고하게 나왔다. 모든 Service에 대해 정상 케이스 + 예외 케이스가 빠짐없이 작성되었고, 리팩토링 시에도 테스트가 안전망 역할을 했다.

**한계**: 에이전트의 TDD는 "진짜 TDD"인지 검증 불가능하다. 코드 스냅샷만 봐서는 테스트를 먼저 작성했는지, 구현을 먼저 하고 테스트를 뒤에 붙였는지 구별할 수 없다. 이것은 아키텍처 리뷰어도 지적한 부분이다.

---

## 3. 에이전트 팀 경험에 대한 평가

### 2시간, 16명, 결과물

**산출물**:
- 5개 BC (common, auth, member, category, task) 완전 구현
- REST API 14개 엔드포인트
- Domain/Application 단위 테스트 약 70개+
- Flyway 마이그레이션 4개
- JWT 인증 시스템
- 아키텍처 리뷰 2회 + 보안 리뷰 2회
- 리팩토링 2회
- 문서 12개+

**2시간이 빠른가?** 같은 규모를 혼자 코딩했다면 하루(8시간) 이상 걸렸을 것이다. 에이전트 팀이 4배 정도 빠른 셈이다. 하지만 "에이전트 1명에게 순차적으로 시켰다면?"이라는 질문도 가능하다. 조율 비용 없이 한 에이전트가 쭉 작업했으면 아마 1.5시간 정도에 끝났을 수 있다. **팀을 운영한 오버헤드가 꽤 컸다**.

### 가치가 있었던 부분

1. **리뷰어**: 아키텍처 리뷰어와 보안 리뷰어는 확실히 가치가 있었다. 사람이 직접 하면 놓치기 쉬운 IDOR 취약점(CreateTaskService 카테고리 소유권)을 보안 리뷰어가 잡아냈고, BC 격리 위반은 아키텍처 리뷰어가 잡았다.

2. **문서화 강제**: 각 코더에게 작업 문서와 피드백을 요구한 것은 매우 유용했다. Exposed 1.0.0 마이그레이션 가이드, `Column<Long>` vs `EntityID` 비교표 같은 내용은 다음 작업에서 재사용 가치가 높다.

3. **피드백 루프**: Sprint 1 코더 피드백을 Sprint 2 코더 프롬프트에 반영하면서, 같은 실수를 반복하지 않게 했다. 특히 Exposed 패키지 경로 문제는 Sprint 1에서 가장 큰 삽질이었는데, Sprint 2에서는 발생하지 않았다.

### 가치가 낮았던 부분

1. **Planner**: Plan 타입 에이전트는 파일을 쓸 수 없어서, 리더가 계획서를 대신 저장해야 했다. Planner의 출력을 리더가 읽고 → 사용자에게 보고하고 → 결정 받고 → Coder에게 전달하는 릴레이는 비효율적이었다. 차라리 general-purpose 에이전트 1명이 계획+구현을 순차적으로 하는 게 나았을 것.

2. **병렬 코더**: Category와 Task를 동시에 작업시켰는데, 공유 파일 충돌 + 빌드 타이밍 이슈가 발생. 순차 실행이 더 안전했을 것.

### 전체적으로, 좋은 경험이었나?

**실험으로서는 매우 가치 있었다.** 에이전트 팀의 가능성과 한계를 동시에 체감했다:

- **가능성**: 2시간에 운영 가능한 수준의 백엔드 API를 만들어냈다. 리뷰까지 포함해서.
- **한계**: 조율 비용이 실제 구현 비용에 맞먹는다. 리더의 컨텍스트 윈도우가 진짜 병목이다. 팀원이 많다고 빨라지지 않는다.

**다음에 같은 작업을 한다면**: Planner 없이, Coder 2~3명(BC당 1명을 순차가 아닌 독립 BC만 병렬)과 Reviewer 2명으로 진행하겠다. 팀 규모를 절반으로 줄이고, 리더가 사소한 수정은 직접 하는 것을 허용하겠다.

---

## 4. 아키텍처 규칙 자체에 대한 제안

이번 경험을 바탕으로 CLAUDE.md와 architecture.md에 반영할 만한 조정 사항:

| 현재 규칙 | 제안 | 이유 |
|-----------|------|------|
| Repository 출력은 엔티티만 | `Boolean`, `Int`, `Long` 반환하는 유틸리티 메서드 허용 | `findAll().size` 같은 우회가 오히려 비효율적 |
| Query 필드는 전부 nullable | 필수 조건은 non-nullable 허용 | TaskQuery의 memberId+taskDate는 항상 필요한데 nullable로 만들면 매번 requireNotNull 호출 |
| BC 간 직접 참조 금지 | Infrastructure 레벨에서 common 테이블 JOIN은 허용(현재도 사실상 허용 중) | CategoriesTable, TasksTable이 common에 있으니 이미 타협 |
| findById/findByEmail 편의 메서드 | Repository 인터페이스에 허용 (Query 기반 findAll과 공존) | 단건 조회의 의도를 명확히 표현 |
| 비즈니스 규칙은 Application/Domain에만 | Infrastructure에서의 불변식 검증 허용 (예: 삭제 전 참조 무결성) | DB 제약조건과 중복되는 검증은 가까운 곳에서 하는 게 자연스러움 |
