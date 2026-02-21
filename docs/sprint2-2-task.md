# Sprint 2-2: Task BC 구현

## 개요

Task Bounded Context를 DDD + Clean Architecture 기반으로 구현.
Domain/Application 레이어는 TDD(RED→GREEN→REFACTOR), Infrastructure/Presentation은 TDD 미적용.

---

## 생성 파일 목록

### Domain Layer

| 파일 | 설명 |
|------|------|
| `task/domain/model/TaskTitle.kt` | VO — 공백 불가, 200자 이내 |
| `task/domain/model/TaskDate.kt` | VO — `java.time.LocalDate` 래핑 |
| `task/domain/model/Task.kt` | 엔티티 (id, memberId, categoryId, title, completed, taskDate, createdAt, updatedAt) |
| `task/domain/model/TaskWithCategoryInfo.kt` | 조회 전용 read model (Task + categoryName, categoryColor) |
| `task/domain/model/TaskCommand.kt` | Command sealed interface (Create, Update, ToggleComplete, Delete) |
| `task/domain/model/TaskQuery.kt` | Query data class (memberId, taskDate 필수) |
| `task/domain/repository/TaskRepository.kt` | Repository 인터페이스 |

### Application Layer

| 파일 | 설명 |
|------|------|
| `task/application/dto/TaskDto.kt` | Service 반환 DTO |
| `task/application/dto/TasksByDateDto.kt` | 날짜별 카테고리 그룹핑 DTO |
| `task/application/dto/TaskStatsDto.kt` | 달성률 DTO (total, completed, rate) |
| `task/application/service/CreateTaskService.kt` | 태스크 생성 |
| `task/application/service/GetTasksByDateService.kt` | 날짜별 조회 + 카테고리 그룹핑 |
| `task/application/service/UpdateTaskService.kt` | 제목/날짜 수정 (소유권 검증) |
| `task/application/service/ToggleTaskCompleteService.kt` | 완료 토글 (소유권 검증) |
| `task/application/service/DeleteTaskService.kt` | 삭제 (소유권 검증) |
| `task/application/service/GetTaskStatsService.kt` | 달성률 조회 |

### Infrastructure Layer

| 파일 | 설명 |
|------|------|
| `common/infrastructure/persistence/CategoriesTable.kt` | Exposed Table 정의 (category-coder 미생성으로 직접 생성) |
| `common/infrastructure/persistence/TasksTable.kt` | Exposed Table 정의 |
| `task/infrastructure/persistence/ExposedTaskRepository.kt` | TaskRepository 구현체 |

### Presentation Layer

| 파일 | 설명 |
|------|------|
| `task/presentation/request/CreateTaskRequest.kt` | 태스크 생성 요청 DTO |
| `task/presentation/request/UpdateTaskRequest.kt` | 태스크 수정 요청 DTO |
| `task/presentation/response/TaskResponse.kt` | 태스크 단건 응답 DTO |
| `task/presentation/response/TasksByDateResponse.kt` | 날짜별 카테고리 그룹핑 응답 DTO |
| `task/presentation/response/TaskStatsResponse.kt` | 달성률 응답 DTO |
| `task/presentation/controller/TaskController.kt` | REST API 컨트롤러 |

### 테스트 파일

| 파일 | 검증 내용 |
|------|----------|
| `task/domain/model/TaskTitleTest.kt` | 빈값·공백·200자 초과 예외, 정상 생성 |
| `task/domain/model/TaskTest.kt` | 엔티티 필드 정상, completed 상태 |
| `task/application/service/CreateTaskServiceTest.kt` | 생성 성공 → TaskDto 반환 |
| `task/application/service/GetTasksByDateServiceTest.kt` | 카테고리 그룹핑, 동일 카테고리 묶음, 빈 날짜 |
| `task/application/service/UpdateTaskServiceTest.kt` | 본인 수정, 타인 수정 IllegalArgumentException, 없는 태스크 NoSuchElementException |
| `task/application/service/ToggleTaskCompleteServiceTest.kt` | 토글 성공, 소유권 예외, 미존재 예외 |
| `task/application/service/DeleteTaskServiceTest.kt` | 삭제 성공 (verify), 소유권 예외, 미존재 예외 |
| `task/application/service/GetTaskStatsServiceTest.kt` | 부분 완료 rate 계산, 태스크 없을 때 rate=0.0, 전체 완료 100.0 |

---

## 테스트 결과

```
Task BC 단위 테스트: 8개 클래스, 모두 통과
전체 빌드: BUILD SUCCESSFUL (category-coder 완료 후)
```

---

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/tasks` | 태스크 생성 |
| GET | `/tasks?date=2026-02-21` | 날짜별 조회 (카테고리 그룹핑) |
| PUT | `/tasks/{id}` | 제목·날짜 수정 |
| PATCH | `/tasks/{id}/toggle` | 완료 토글 |
| DELETE | `/tasks/{id}` | 삭제 |
| GET | `/tasks/stats?date=2026-02-21` | 달성률 조회 |

---

## 주요 설계 결정

| 결정 | 내용 | 이유 |
|------|------|------|
| 카테고리 존재 검증 | DB FK 제약조건에 위임 | 코드 중복 없이 DB가 보장 |
| 소유권 예외 타입 | `IllegalArgumentException` | GlobalExceptionHandler에 이미 핸들링 존재 |
| `TaskRepository.save()` 시그니처 | `TaskCommand` (sealed) 전체 수용 | 기존 MemberRepository 패턴 통일 |
| `TaskCommand.Delete`를 `save()`에서 전달 시 | `IllegalArgumentException` throw | `delete()` 메서드 분리로 명확한 의도 표현 |
| `countByQuery` 구현 | `findAll` 후 메모리 count (두 번 쿼리 없이) | 단순하게 시작, 필요 시 최적화 |
| 날짜별 그룹핑 | Repository에서 JOIN 조회 → Service에서 `groupBy` | DB JOIN으로 N+1 방지, 그룹핑 로직은 Service에 |
| `rate` 계산 | total=0이면 0.0, 아니면 `completed/total * 100` | 0 나누기 방어 |

---

## 이슈 및 메모

- **CategoriesTable/TasksTable**: 계획상 category-coder 담당이었으나 미생성 상태여서 task-coder가 직접 생성 (`common/infrastructure/persistence/`).
- **빌드 블로킹**: category-coder의 `ExposedCategoryRepository` 미구현으로 `ServerApplicationTests` contextLoads 실패. category-coder 완료 후 전체 빌드 통과.
- **Exposed v1 `deleteWhere`**: `org.jetbrains.exposed.v1.jdbc.deleteWhere` import 필요 (기존 예제 코드에 없어 직접 추가).
