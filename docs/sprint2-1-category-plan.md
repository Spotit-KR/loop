# Sprint 2-1: Category BC 구현 계획

## 개요
Category BC의 구현 계획서. category-planner(opus)가 수립, 리더가 승인.

## 파일 구조 (26파일)

### Domain Layer
- `model/CategoryName.kt`, `CategoryColor.kt`, `SortOrder.kt` — VO
- `model/Category.kt` — 엔티티
- `model/CategoryCommand.kt` — Command sealed interface (Create, Update, Delete)
- `model/CategoryQuery.kt` — Query data class
- `repository/CategoryRepository.kt` — Repository 인터페이스

### Application Layer
- `dto/CategoryDto.kt`
- `service/CreateCategoryService.kt`, `GetCategoriesService.kt`, `UpdateCategoryService.kt`, `DeleteCategoryService.kt`

### Infrastructure Layer
- `common/infrastructure/persistence/CategoriesTable.kt` — 공유 Table
- `persistence/ExposedCategoryRepository.kt`

### Presentation Layer
- `request/CreateCategoryRequest.kt`, `UpdateCategoryRequest.kt`
- `response/CategoryResponse.kt`
- `controller/CategoryController.kt`

## 비즈니스 규칙
- 회원당 카테고리 최대 10개
- 동일 회원 내 카테고리명 중복 불가
- 하위 할일 있으면 삭제 불가

## 주요 설계 결정
| 결정 | 내용 |
|------|------|
| CategoriesTable 위치 | common/infrastructure/persistence/ |
| TasksTable | common/infrastructure/persistence/에 미리 정의 (삭제 시 할일 확인용) |
| save()와 delete() | save()는 Create/Update만, delete()는 별도 메서드 |
| 소유권 검증 | Command에 memberId 포함, Service에서 검증 |
