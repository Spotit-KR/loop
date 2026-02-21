# Sprint 2 리뷰 — 잔여 이슈 (미수정)

> Sprint 2 아키텍처·보안 리뷰에서 발견된 이슈. 전부 미수정 상태.

---

## 즉시 수정 필요 (High / Major)

### 1. CreateTaskService 카테고리 소유권 미검증 (IDOR)
- **위치**: `task/application/service/CreateTaskService.kt`
- **문제**: `categoryId`가 현재 로그인 사용자 소유인지 검증하지 않음. 다른 사용자의 categoryId를 알면 해당 카테고리에 태스크 생성 가능.
- **아키텍처 제약**: BC 간 직접 참조 금지 규칙 때문에 Task BC에서 CategoryRepository 직접 사용 불가
- **해결 방안 후보**:
  - (A) TaskRepository에 카테고리 소유권 확인 메서드 추가 (ExposedTaskRepository가 이미 CategoriesTable import)
  - (B) common에 공유 인터페이스 정의
  - (C) Application Service 레벨에서 해결
- **판정**: 아키텍처·보안 양쪽 동시 지적. 아키텍처 설계 결정 필요.

---

## 배포 전 수정 권장 (Medium)

### 2. DateTimeParseException 미처리 — 상세 오류 노출
- **위치**: `task/presentation/controller/TaskController.kt:56, 71, 87, 129`
- **문제**: `LocalDate.parse(date)` 직접 호출 시 `DateTimeParseException` 발생 가능. GlobalExceptionHandler에 핸들러 없어 스택 트레이스 등 내부 정보 노출 위험.
- **수정 방법**: GlobalExceptionHandler에 `DateTimeParseException` 핸들러 추가 → 400 Bad Request + 안전한 메시지

### 3. 에러 메시지에 memberId 값 노출
- **위치**: `UpdateTaskService:19`, `DeleteTaskService:18`, `ToggleTaskCompleteService:18`
- **문제**: `"Task does not belong to member: ${command.memberId.value}"` 메시지가 응답에 그대로 노출
- **수정 방법**: ID 값 제거. 예: `"Not authorized to access this task"`

---

## 추후 개선 (Low / Minor)

### 4. TaskQuery 필드가 non-nullable
- **위치**: `task/domain/model/TaskQuery.kt`
- **문제**: 아키텍처 규칙은 Query 필드를 nullable로 정의하도록 명시하나, memberId와 taskDate가 non-nullable
- **비고**: 현재 사용 사례에서는 기능적 문제 없음. 향후 유연한 쿼리 확장 시 변경 필요.

### 5. TasksByDateDto categoryId 타입 불일치
- **위치**: `task/application/dto/TasksByDateDto.kt`
- **문제**: `categoryId: Long` (Primitive) 사용. CategoryId는 common에 있으므로 VO 타입 사용이 바람직.
- **비고**: categoryName/categoryColor는 Category BC VO이므로 BC 격리상 String 사용 합리적.

### 6. GetCategoriesService가 Query 대신 MemberId 직접 수신
- **위치**: `category/application/service/GetCategoriesService.kt`
- **문제**: `execute(memberId: MemberId)` 대신 `execute(query: CategoryQuery)` 형태가 아키텍처 패턴에 부합
- **비고**: 기능적 문제 없음. 패턴 일관성 차원.

### 7. TaskWithCategoryInfo에서 categoryName/categoryColor가 String
- **위치**: `task/domain/model/TaskWithCategoryInfo.kt`
- **문제**: 도메인 모델에 Primitive 노출 (Primitive Obsession)
- **비고**: Category BC VO 직접 참조 시 BC 격리 위반이므로 합리적 타협.

### 8. 에러 메시지에 리소스 ID 노출
- **위치**: `DeleteCategoryService:16`, `UpdateCategoryService:17`, `UpdateTaskService:17`, `DeleteTaskService:16`
- **문제**: `"Category not found: ${command.categoryId.value}"` 등 DB ID가 응답에 포함. 열거 공격 힌트 제공 가능.

### 9. SortOrder 상한선 없음
- **위치**: `category/domain/model/SortOrder.kt`
- **문제**: `>= 0` 검증만 있고 상한선 없음. 카테고리 최대 10개 제한 고려 시 합리적 상한선 추가 권장.
