# Sprint 2-1: Category BC 구현

## 개요

Category Bounded Context 전체 구현. Domain/Application은 TDD, Infrastructure/Presentation은 직접 구현.

## 생성된 파일 목록

### Domain Layer (`category/domain/`)

| 파일 | 유형 | 비고 |
|------|------|------|
| `model/CategoryName.kt` | VO | 공백 불가, 50자 이내 |
| `model/CategoryColor.kt` | VO | `#RRGGBB` 정규식 검증 |
| `model/SortOrder.kt` | VO | 0 이상 정수 |
| `model/Category.kt` | Entity | |
| `model/CategoryCommand.kt` | Command | Create / Update / Delete sealed interface |
| `model/CategoryQuery.kt` | Query | memberId, categoryId nullable 필드 |
| `repository/CategoryRepository.kt` | Interface | save / findById / findAll / delete / countByMemberId / existsByMemberIdAndName / existsTasksByCategory |

### Application Layer (`category/application/`)

| 파일 | 유형 | 비고 |
|------|------|------|
| `dto/CategoryDto.kt` | DTO | `from(category)` 팩토리 |
| `service/CreateCategoryService.kt` | Service | 최대 10개, 이름 중복 검증 |
| `service/GetCategoriesService.kt` | Service | 회원별 전체 조회 |
| `service/UpdateCategoryService.kt` | Service | 소유권 + 이름 중복 검증 |
| `service/DeleteCategoryService.kt` | Service | 소유권 + 하위 할일 존재 검증 |

### Infrastructure Layer

| 파일 | 위치 | 비고 |
|------|------|------|
| `CategoriesTable.kt` | `common/infrastructure/persistence/` | 이미 존재 — 수정 불필요 |
| `TasksTable.kt` | `common/infrastructure/persistence/` | 이미 존재 — 수정 불필요 |
| `ExposedCategoryRepository.kt` | `category/infrastructure/persistence/` | Exposed v1.0.0 구현 |

### Presentation Layer (`category/presentation/`)

| 파일 | 유형 | 비고 |
|------|------|------|
| `request/CreateCategoryRequest.kt` | Request DTO | name, color, sortOrder |
| `request/UpdateCategoryRequest.kt` | Request DTO | name, color, sortOrder |
| `response/CategoryResponse.kt` | Response DTO | Primitive 타입 |
| `controller/CategoryController.kt` | Controller | POST/GET/PUT/DELETE /categories |

### 테스트 파일 (`category/`)

| 파일 | 테스트 수 | 결과 |
|------|-----------|------|
| `domain/model/CategoryNameTest.kt` | 5 | ✅ 전부 통과 |
| `domain/model/CategoryColorTest.kt` | 5 | ✅ 전부 통과 |
| `domain/model/SortOrderTest.kt` | 3 | ✅ 전부 통과 |
| `application/service/CreateCategoryServiceTest.kt` | 3 | ✅ 전부 통과 |
| `application/service/GetCategoriesServiceTest.kt` | 2 | ✅ 전부 통과 |
| `application/service/UpdateCategoryServiceTest.kt` | 4 | ✅ 전부 통과 |
| `application/service/DeleteCategoryServiceTest.kt` | 4 | ✅ 전부 통과 |
| **합계** | **26** | **✅ 전부 통과** |

## API 엔드포인트

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/categories` | 카테고리 생성 | 필요 |
| GET | `/categories` | 카테고리 목록 조회 (본인 것만) | 필요 |
| PUT | `/categories/{id}` | 카테고리 수정 | 필요 |
| DELETE | `/categories/{id}` | 카테고리 삭제 | 필요 |

## 비즈니스 규칙 구현

| 규칙 | 구현 위치 | 처리 방식 |
|------|-----------|-----------|
| 회원당 최대 10개 | `CreateCategoryService` | `countByMemberId < 10` |
| 동일 회원 내 이름 중복 불가 | `CreateCategoryService`, `UpdateCategoryService` | `existsByMemberIdAndName` |
| 수정 시 소유권 검증 | `UpdateCategoryService` | `category.memberId == command.memberId` |
| 삭제 시 소유권 검증 | `DeleteCategoryService` | `category.memberId == command.memberId` |
| 하위 할일 있으면 삭제 불가 | `DeleteCategoryService` | `existsTasksByCategory` |

## 주요 설계 결정

### 1. CategoryRepository에 `existsTasksByCategory` 추가

하위 할일 존재 여부를 Service에서 검증하기 위해 Repository 인터페이스에 추가.
구현체에서 `TasksTable`을 직접 쿼리 (cross-BC는 common 테이블 통해서만).

### 2. `save()`에서 Delete 커맨드 처리

`CategoryCommand`가 Create/Update/Delete를 포함하는 sealed interface이므로,
`save(command: CategoryCommand)`의 `when` 블록에서 Delete는 예외 throw.
실제 삭제는 `delete(command: CategoryCommand.Delete)` 별도 메서드 사용.

### 3. Exposed ORM 쿼리 패턴

`CategoriesTable.memberId`와 `TasksTable.categoryId`가 `long().references()` 패턴으로 `Column<Long>` 타입.
→ EntityID 래핑 없이 `.value` 직접 비교:
```kotlin
CategoriesTable.memberId eq memberId.value  // Column<Long> eq Long
TasksTable.categoryId eq categoryId.value   // Column<Long> eq Long
```
단, `LongIdTable.id`는 `Column<EntityID<Long>>`이므로 EntityID 래핑 필요:
```kotlin
CategoriesTable.id eq EntityID(id.value, CategoriesTable)
```

### 4. SecurityConfig 수정 불필요

기존 `anyRequest().authenticated()` 설정이 `/categories/**`를 이미 커버.

### 5. 이름 중복 검증 (Update 시)

Update 시 이름이 변경된 경우에만 중복 체크:
```kotlin
if (category.name != command.name) {
    require(!categoryRepository.existsByMemberIdAndName(...))
}
```
DB에서 현재 카테고리가 아직 OLD 이름을 갖고 있으므로, 이름이 바뀐 경우에만 체크해도 올바르게 동작.

## 빌드 결과

```
BUILD SUCCESSFUL
테스트 26개 통과 (failures=0, errors=0)
```

## 의존성 영향

- `TasksTable`을 `ExposedCategoryRepository`에서 참조 → task-coder와 충돌 없음 (읽기 전용 count 쿼리만 사용)
- `CategoriesTable`을 task-coder가 참조할 예정 → 변경 없음
