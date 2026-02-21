# Sprint 2 Coder 피드백 (검토 필요)

> Sprint 2의 category-coder, task-coder, refactor-coder가 작업 중 겪은 이슈와 개선 제안.
> 리더 검토 후 프로젝트 컨벤션/문서에 반영 여부 결정 필요.

---

## category-coder 피드백

### 1. `Column<Long>` vs `Column<EntityID<Long>>` 구분 (가장 고민한 부분)
- **문제**: `long().references()` vs `reference()` 패턴이 혼재. RefreshTokensTable은 `reference()`, TasksTable/CategoriesTable은 `long().references()` 사용.
- **영향**: 쿼리 시 EntityID 래핑 여부가 달라짐
- **제안**: 계획 문서에 "이 테이블은 `Column<Long>` 패턴이므로 `.value` 직접 비교" 등 명시

### 2. 테이블 파일 존재 여부 안내 미흡
- **문제**: 계획에 "CategoriesTable, TasksTable 생성 필요"로 되어 있었지만 실제로는 이미 존재
- **제안**: "이미 존재하는 파일" 목록에 테이블 파일도 포함하거나, "작업 전 존재 여부 확인" 지시 추가

### 3. `existsTasksByCategory` 설계 결정 미명시
- **문제**: DeleteCategoryService에서 하위 할일 검증 방법이 계획에 없음
- **제안**: "Repository에 existsTasksByCategory 추가" 등 미리 결정

### 4. `save()` sealed interface 전체 수신 문제
- **문제**: `save(command: CategoryCommand)`가 Delete도 받을 수 있는 타입. Create/Update만 처리하는데 불일치.
- **제안**: "sealed interface 전체 받는 이유" 또는 별도 타입 분리 여부 계획에 명시

### 5. Exposed `andWhere` vs `and` 가이드 부재
- **제안**: 다중 WHERE 조건 작성법 가이드 추가

---

## task-coder 피드백

### 1. 팀원 간 파일 소유권 경계 불명확 (가장 큰 이슈)
- **문제**: CategoriesTable/TasksTable이 "category-coder가 생성"이라고 명시되었지만 작업 시작 시 없어서 직접 생성
- **제안**: 공유 파일은 사전 생성하거나, 파일별 담당자와 생성 타이밍을 더 명확히

### 2. `deleteWhere` import 누락
- **문제**: Exposed Repository 예시에 delete 관련 import가 없음
- **제안**: `deleteWhere` import도 예시에 포함

### 3. BC 간 빌드 의존성 처리 기준 없음
- **문제**: Category BC 미완료 시 Task BC 빌드 검증 불가. 어떻게 해야 할지 지침 없음
- **제안**: "빌드 실패가 타 BC 원인이면 리더에게 보고하고 대기" 규칙 추가

### 4. `save(TaskCommand.Delete)` 처리 모호성
- **문제**: save()가 Delete 포함 sealed interface를 받지만 Delete는 별도 메서드. when 분기에서 throw 처리.
- **제안**: save() 시그니처를 Create/Update/ToggleComplete만 받도록 제한 검토

### 5. Kotest BehaviorSpec 선택적 테스트 실행 어려움
- **문제**: Gradle `--tests` 필터가 Kotest BehaviorSpec에서 동작하지 않음
- **대응**: clean build 후 XML 리포트로 검증

---

## refactor-coder 피드백

### 1. BC 경계 케이스 판단 비용 (가장 막혔던 부분)
- **문제**: `existsTasksByCategory`에서 "TaskRepository 이동" vs "반환 타입 조정" 두 선택지가 제시되었으나 어느 것이 적합한지 판단에 시간 소요. TaskRepository 이동은 BC 격리 위반.
- **해결**: `CategoryRepository`에 `findTaskIdsByCategoryId(categoryId): List<TaskId>` 형태로 타입 조정
- **제안**: BC 경계 케이스는 "선택지 A vs B 중 선택하시오" 형태로 명확히 가이드하거나, 프로젝트 채택 방식을 예시로 제시

### 2. 위반 파일 목록 누락
- **문제**: 프롬프트의 수정 대상에 `UpdateCategoryService`의 `existsByMemberIdAndName` 사용이 포함되지 않음. 빌드 실패 후에야 발견.
- **제안**: grep 등으로 전수 조사한 위반 파일 목록 제공하면 누락 없이 처리 가능

### 3. `findByEmail` 편의 메서드 유지 여부 불확실
- **문제**: `findByEmail(email: Email)` 자체도 위반으로 명시되었지만, "신중히 판단"이라는 안내와 최소 변경 원칙에 의해 유지. 판단의 확신 부족.
- **제안**: "신중히 판단 = 편의 메서드로 유지 가능" 처럼 결론을 명시
