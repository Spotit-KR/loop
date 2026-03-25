# BC 간 이벤트 통신

BC 간 직접 참조 없이 상태 변경을 전파하는 방법. Spring `ApplicationEventPublisher`를 사용합니다.

## 언제 사용하는가

한 BC의 상태 변경이 다른 BC의 후속 작업을 유발할 때 사용합니다.

| 조건 | 설명 |
|------|------|
| BC 간 직접 참조가 불가능할 때 | common을 제외한 다른 BC의 코드를 import할 수 없으므로 |
| FK CASCADE로 해결할 수 없을 때 | DB 레벨 제약은 Infrastructure 레이어 결합이므로 비즈니스 로직 전파에 부적합 |
| 발행 BC가 수신 BC의 존재를 몰라야 할 때 | 의존 역전: 발행자는 이벤트만 발행, 누가 수신하는지 모름 |

## 이벤트 정의

`common/domain/event/`에 위치합니다. 2개 이상의 BC가 관여하는 공유 계약이므로 common에 배치합니다.

```kotlin
// common/domain/event/GoalDeletedEvent.kt
data class GoalDeletedEvent(
    val goalId: GoalId,
)
```

**규칙**:
- 이벤트 클래스는 **불변 data class**
- 수신자가 후속 작업에 필요한 **최소한의 식별자만** 포함 (VO 사용)
- 명명: `{Entity}{과거분사}Event` (예: `GoalDeletedEvent`, `MemberRegisteredEvent`)
- 비즈니스 로직을 포함하지 않음

## 발행 (Publisher)

Application Service에서 `ApplicationEventPublisher`를 주입받아 발행합니다.

```kotlin
@Service
class GoalService(
    private val goalRepository: GoalRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun delete(command: GoalCommand.Delete) {
        val goal = goalRepository.findById(command.goalId)
            ?: throw NoSuchElementException("Goal not found: ${command.goalId.value}")

        goalRepository.delete(command)
        eventPublisher.publishEvent(GoalDeletedEvent(goal.id))
    }
}
```

**규칙**:
- 상태 변경(DB 반영) **이후**에 이벤트를 발행
- 발행 BC는 수신 BC의 존재를 모름 — import하지 않음
- 하나의 비즈니스 동작에서 하나의 이벤트 발행이 기본

## 수신 (Listener)

수신 BC의 Application Service(또는 전용 EventListener 클래스)에서 `@TransactionalEventListener`로 수신합니다.

```kotlin
@Service
class TaskService(
    private val taskRepository: TaskRepository,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleGoalDeleted(event: GoalDeletedEvent) {
        taskRepository.deleteByGoalId(event.goalId)
    }
}
```

**규칙**:
- 기본적으로 `BEFORE_COMMIT` 사용 — 발행자와 같은 트랜잭션에서 원자적 처리
- 수신 로직이 실패하면 발행자의 트랜잭션도 롤백됨 (데이터 일관성 보장)
- 수신 메서드명: `handle{EventName}` (예: `handleGoalDeleted`)

### TransactionPhase 선택 기준

| Phase | 트랜잭션 | 사용 시점 |
|-------|----------|-----------|
| `BEFORE_COMMIT` | 발행자와 동일 | **기본값**. 원자성이 필요할 때 (삭제 전파, 상태 동기화) |
| `AFTER_COMMIT` | 별도 트랜잭션 | 실패해도 발행자에 영향 없어야 할 때 (알림, 로그) |

## 수신 로직이 커질 때

이벤트 리스너 로직이 Service에 섞이기 어려울 만큼 커지면 전용 클래스로 분리합니다.

```kotlin
// task/application/listener/GoalEventListener.kt
@Component
class GoalEventListener(
    private val taskRepository: TaskRepository,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleGoalDeleted(event: GoalDeletedEvent) {
        taskRepository.deleteByGoalId(event.goalId)
    }
}
```

**분리 기준**: 이벤트 핸들러가 2개 이상이거나, 핸들러 로직이 단순 위임을 넘어설 때.

## 디렉토리 구조

```text
common/
└── domain/
    └── event/                        # 공유 이벤트 클래스
        └── GoalDeletedEvent.kt

{publisher-bc}/
└── application/
    └── service/
        └── GoalService.kt           # eventPublisher.publishEvent(...)

{subscriber-bc}/
└── application/
    ├── service/
    │   └── TaskService.kt           # @TransactionalEventListener (간단한 경우)
    └── listener/                     # (선택) 전용 리스너 클래스
        └── GoalEventListener.kt
```

## 파일 명명 규칙

| 위치 | 명명 규칙 | 예시 |
|------|-----------|------|
| 이벤트 클래스 | `{Entity}{과거분사}Event.kt` | `GoalDeletedEvent.kt` |
| 전용 리스너 | `{발행BC}EventListener.kt` | `GoalEventListener.kt` |

## 테스트

| 대상 | 유형 | 방식 |
|------|------|------|
| 발행 (Service) | 단위 테스트 | MockK으로 `ApplicationEventPublisher` mock, `verify { publishEvent(any<GoalDeletedEvent>()) }` |
| 수신 (Listener) | 단위 테스트 | 이벤트 객체를 직접 생성하여 핸들러 메서드 호출, Repository mock으로 삭제 검증 |
| 통합 (발행→수신) | 통합 테스트 | `@SpringBootTest`에서 발행 후 수신 BC의 상태 변경 확인 (선택적) |
