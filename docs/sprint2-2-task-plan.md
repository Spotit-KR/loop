# Sprint 2-2: Task BC 구현 계획

## 개요
Task BC의 구현 계획서. task-planner(opus)가 수립, 리더가 승인.

## 파일 구조 (32파일)

### Domain Layer
- `model/TaskTitle.kt`, `TaskDate.kt` — VO
- `model/Task.kt` — 엔티티
- `model/TaskWithCategoryInfo.kt` — 조회 전용 read model
- `model/TaskCommand.kt` — Command sealed interface (Create, Update, ToggleComplete, Delete)
- `model/TaskQuery.kt` — Query data class
- `repository/TaskRepository.kt` — Repository 인터페이스

### Application Layer
- `dto/TaskDto.kt`, `TasksByDateDto.kt`, `TaskStatsDto.kt`
- `service/CreateTaskService.kt`, `GetTasksByDateService.kt`, `UpdateTaskService.kt`, `ToggleTaskCompleteService.kt`, `DeleteTaskService.kt`, `GetTaskStatsService.kt`

### Infrastructure Layer
- `persistence/TasksTable.kt` — Exposed Table (또는 common에 위치)
- `persistence/ExposedTaskRepository.kt`

### Presentation Layer
- `request/CreateTaskRequest.kt`, `UpdateTaskRequest.kt`
- `response/TaskResponse.kt`, `TasksByDateResponse.kt`, `TaskStatsResponse.kt`
- `controller/TaskController.kt`

## 카테고리별 그룹핑 응답 구조
```json
{
  "date": "2026-02-21",
  "categories": [
    { "categoryId": 1, "categoryName": "업무", "categoryColor": "#FF5733", "tasks": [...] }
  ]
}
```

## 달성률 조회
- total, completed, rate(%) 반환
- 할일 없으면 rate=0.0

## 주요 설계 결정
| 결정 | 내용 |
|------|------|
| 카테고리 존재 검증 | DB FK 제약조건에 위임 |
| 소유권 예외 | IllegalArgumentException (추후 공통 예외 도입 시 변경) |
| countByQuery | 두 번 쿼리로 시작, 필요시 최적화 |
| 그룹핑 | Repository에서 JOIN 조회 → Service에서 groupBy |
