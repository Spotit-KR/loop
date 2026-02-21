# Claude Code 에이전트 팀 운영 지침서

> 이 문서는 Loop Server 프로젝트 Sprint 1 팀 운영 경험을 바탕으로 작성된, 에이전트 팀 구성 및 운영을 위한 일반화된 지침입니다.

---

## 1. 팀 구성 원칙

### 1-1. 역할 분리

에이전트 팀은 **Planner → Coder → Reviewer** 3단계 파이프라인으로 구성합니다.

| 역할 | 에이전트 타입 | 모델 | 주요 책임 |
|------|-------------|------|----------|
| **Planner** | `Plan` | opus | 아키텍처 조사, 구현 계획 수립, 파일 목록·설계 상세 작성 |
| **Coder** | `general-purpose` | sonnet | 승인된 계획에 따라 코드 작성, 테스트 실행, 빌드 확인 |
| **Reviewer** | `Plan` 또는 `general-purpose` | opus | 아키텍처 규칙 준수 여부 검토, 보안 취약점 점검 |
| **Leader** | 메인 에이전트 | opus | 전체 조율, 태스크 관리, 의사결정, 결과 보고 |

### 1-2. 모델 선택 기준

- **opus**: 계획 수립(Planner), 리뷰(Reviewer), 아키텍처 결정이 필요한 작업. 비용이 높지만 복잡한 판단력이 요구될 때 사용.
- **sonnet**: 코드 구현(Coder). 명확한 계획이 주어진 상태에서 코드를 작성하는 데 충분. 비용 효율적.
- **haiku**: 단순 반복 작업(포맷팅, 린트 수정 등). 현재 팀에서는 미사용.

### 1-3. 팀 규모 가이드

- Planner는 BC(Bounded Context) 또는 스프린트 단위별 1명씩 할당
- Coder는 독립적인 작업 단위별 1명씩 할당 (동일 파일 수정 충돌 방지)
- Reviewer는 전체 스프린트당 1~2명 (아키텍처 1, 보안 1)
- **동시 에이전트 수는 5~6개 이하**를 권장 (컨텍스트 관리, 비용, 충돌 방지)

---

## 2. 통신 규칙

### 2-1. 허브-스포크 모델 (Hub-and-Spoke)

```
          ┌──────────┐
          │  Leader   │
          └────┬─────┘
    ┌──────┬───┼───┬──────┐
    ▼      ▼   ▼   ▼      ▼
Planner Planner Coder Coder Reviewer
   A       B     A     B
```

- **모든 통신은 Leader를 경유**합니다.
- 팀원 간 직접 소통을 금지합니다.
- 이유: 메시지 누락 방지, 의사결정 일관성, 충돌 감지 용이.

### 2-2. 통신 수단

- **SendMessage (type: message)**: 개별 팀원에게 메시지 전달. 기본 수단.
- **SendMessage (type: broadcast)**: 전체 공지. 긴급 상황(작업 중단, 중대 변경)에만 사용.
- **TaskUpdate/TaskList**: 비동기 상태 공유. 태스크 상태 변경으로 간접 커뮤니케이션.

### 2-3. Planner 프롬프트에 반드시 포함할 통신 규칙 문구

```
## 통신 규칙 (필수)
- 다른 팀원과 절대 직접 소통하지 마세요
- 모든 커뮤니케이션은 반드시 리더를 통해서만 합니다
- 작업 완료 시 SendMessage로 리더에게 결과를 보내세요
```

### 2-4. Coder 프롬프트에 추가할 제한 사항

```
- 웹 자료 조사(WebSearch, WebFetch)를 사용하지 마세요
```

이유: Coder는 승인된 계획만 따라야 합니다. 자체적으로 조사하면 계획과 다른 방향으로 갈 수 있습니다.

---

## 3. 태스크 의존성 설계 방법

### 3-1. 의존성 그래프 설계 원칙

```
[Plan A] ──→ [Code A] ──→ [Review]
[Plan B] ──→ [Code B] ──↗
[Plan C] ──→ [Code C] ──↗
```

- **Plan 태스크들은 서로 독립** → 병렬 실행 가능
- **Code 태스크는 해당 Plan + 선행 Code에 의존** → `addBlockedBy`로 설정
- **Review 태스크는 모든 Code에 의존** → 마지막에 실행

### 3-2. 실제 적용 예시 (Sprint 1)

```
#1 Foundation Plan ──→ #2 Foundation Code ──→ #4 Member Code ──→ #6 Auth Code ──→ #7 Arch Review
#3 Member Plan ─────────────────────────────↗                                    ↗
#5 Auth Plan ──────────────────────────────────────────────────↗   #8 Security Review
```

### 3-3. 태스크 생성 시 팁

- 의존성은 태스크 생성 후 `TaskUpdate`의 `addBlockedBy`로 설정
- Coder 태스크는 반드시 해당 Plan 태스크에 의존하도록 설정
- Coder 태스크 간에도 순차 의존성 설정 (같은 코드베이스 수정 충돌 방지)

---

## 4. TDD 워크플로우와 에이전트 역할 매핑

### 4-1. TDD 책임 분배

| TDD 단계 | 담당 | 산출물 |
|----------|------|--------|
| **테스트 시나리오 설계** | Planner | Given/When/Then 목록, 테스트 클래스 구조 |
| **RED (실패 테스트 작성)** | Coder | 테스트 코드 작성, 실패 확인 |
| **GREEN (최소 구현)** | Coder | 구현 코드 작성, 테스트 통과 확인 |
| **REFACTOR** | Coder | 코드 정리, 테스트 통과 유지 확인 |
| **테스트 커버리지 검증** | Reviewer | 누락 시나리오 지적 |

### 4-2. TDD 미적용 영역

- 설정 파일 (application.yml, build.gradle.kts)
- Flyway 마이그레이션 SQL
- Infrastructure 레이어 (ORM Table 정의, Repository 구현체)
- Presentation 레이어 (Controller, Request/Response DTO)
- 공통 설정 클래스 (SecurityConfig, WebMvcConfig)

---

## 5. Plan 에이전트 vs General-purpose 에이전트

### 5-1. Plan 에이전트 (subagent_type: "Plan")

- **사용 가능**: Glob, Grep, Read, WebFetch, WebSearch 등
- **사용 불가**: Edit, Write, NotebookEdit (파일 수정 불가)
- **적합한 역할**: Planner, Reviewer
- **주의**: 파일 생성/수정 불가. 산출물은 SendMessage 텍스트로 전달.

### 5-2. General-purpose 에이전트

- **사용 가능**: 모든 도구
- **적합한 역할**: Coder
- **주의**: WebSearch/WebFetch를 프롬프트에서 금지할 것.

---

## 6. 코드 리뷰 프로세스

### 6-1. 리뷰어 종류

| 리뷰어 | 관점 | 모델 |
|--------|------|------|
| **아키텍처 리뷰어** | 레이어 의존성, Command/Query 패턴, VO, BC 격리 | opus |
| **보안 리뷰어** | OWASP Top 10, 인증/인가, 입력 검증, 비밀번호 처리 | sonnet |

### 6-2. 리뷰 타이밍

모든 Coder 완료 → 아키텍처 + 보안 리뷰 (병렬) → 리더가 피드백 종합 → 수정 필요시 Coder에게 전달

---

## 7. 스프린트 단위 작업 흐름

```
Phase 1: 계획 (Parallel)     Phase 2: 구현 (Sequential)     Phase 3: 리뷰 (Parallel)
┌─────────────────┐          ┌─────────────────┐           ┌──────────────────┐
│ Foundation Plan │──→       │ Foundation Code │           │ Architecture     │
│ Member Plan     │──┐       │      ↓          │           │ Review           │
│ Auth Plan       │──┤       │ Member Code     │──→        │                  │
└─────────────────┘  └─→     │      ↓          │           │ Security Review  │
                             │ Auth Code       │──→        └──────────────────┘
                             └─────────────────┘
```

---

## 8. 주의사항 및 교훈

1. **Plan 에이전트에게 코드 작성 지시 금지** — 파일 수정 권한 없음
2. **Coder에게 계획을 프롬프트에 인라인으로 제공** — Planner가 파일을 쓸 수 없으므로
3. **동일 파일 수정 충돌 방지** — Coder 간 순차 의존성 설정
4. **Coder의 WebSearch 금지** — 계획과 다른 방향 방지
5. **이미 존재하는 파일 목록 명시** — 중복 생성 방지
6. **태스크 의존성 누락 주의** — 빌드 실패 원인
7. **Idle 상태에 과민반응 금지** — 정상적인 대기 상태
8. **Planner 결정 사항 빠르게 처리** — 후속 작업 지연 방지
9. **스프린트 범위 명확히 제한** — 범위 초과 방지
10. **빌드/테스트 실행 결과 확인 필수** — Coder 프롬프트에 명시

---

## 부록: 에이전트 Spawn 템플릿

### Planner
```json
{
  "subagent_type": "Plan",
  "model": "opus",
  "name": "{bc-name}-planner",
  "team_name": "{team-name}"
}
```

### Coder
```json
{
  "subagent_type": "general-purpose",
  "model": "sonnet",
  "name": "{bc-name}-coder",
  "team_name": "{team-name}"
}
```

### Reviewer
```json
{
  "subagent_type": "Plan",
  "model": "opus",
  "name": "{review-type}-reviewer",
  "team_name": "{team-name}"
}
```
