# TaskId 위치 수정 계획

> Issue: #27

## 단계

- [x] 1단계: TaskId를 task/domain/model/로 이동
- [x] 2단계: common/domain/TaskId.kt 삭제
- [x] 3단계: 모든 import 경로를 task.domain.model.TaskId로 변경
- [x] 4단계: domain.md 문서에서 TaskId 공유 VO 언급 제거
- [x] 5단계: 테스트 통과 확인
