# AI 에이전트 규칙

작업 계획 문서 작성 시 별도 지시가 없으면 docs/ 디렉토리에 마크다운 파일로 작성.

## 작업 계획 프로세스

모든 작업은 계획 → 실행 → 검증 순서로 진행합니다. 상세 규칙은 @docs/work-planning-rules.md 참고.

- 작업 시작 전 `docs/plan/{작업명}/`에 plan.md, context.md, checklist.md 생성
- TaskCreate/TaskUpdate로 진행 상황 추적
- 매 단계 완료 시 plan.md 체크 표시 업데이트
- 완료 전 checklist.md 필수 항목 (아키텍처 준수, 테스트 통과) 확인

## 구글 드라이브 (기획/PRD/할일)

프로젝트 기획, PRD, 할일 등 비코드 문서는 구글 드라이브에서 관리합니다.

- **루트 폴더**: `1WGdRszch7AuMrrnntwM-5kdv7Tjf7Z0Q`
- 기능 구현 시 PRD나 회의록을 참고해야 하면 구글 드라이브에서 먼저 조회
- 새 기획/PRD/할일 문서 작성이 필요하면 해당 폴더에 Google Docs로 생성
- 참고 문서:
  - `PRD` (ID: `1Pb_Ma6mfLJZpD3gpUVaqPkLdBsyAfPFuwY6AIy6J19A`) — 제품 요구사항 정의서
  - `회의록` (ID: `1gPNIO7JsdH1Iq8FFI_SAIm5SOWBJtLE2p8OzPSSkHfg`) — 회의 내용 기록

## Git 브랜치 규칙

- 이슈 기반 작업은 `ticket/#<이슈번호>` 형식으로 브랜치를 생성합니다. (예: `ticket/#13`)

## 아키텍처

DDD + Clean Architecture 기반. 상세 구조와 코드 패턴은 @docs/architecture.md 참고.
Security 설정 코드 작성 전 @docs/spring-security-7.md 참고.

- Domain Layer는 외부 의존성 없이 순수 Kotlin 코드로 작성
- Presentation, Application, Infrastructure 모두 Domain에 직접 의존
- Presentation은 Application(Service 호출)과 Domain(Command, Query, VO, Entity)에 의존
- Infrastructure는 Domain(Repository 인터페이스)에만 의존
- common을 제외한 다른 BC의 코드를 직접 참조하지 않음
- BC 간 공유 VO는 `common/domain/`에 위치
- Domain의 Command/Query가 Presentation 이후 모든 레이어의 공용 입력
- Service는 기본적으로 Entity를 직접 반환 (Application DTO는 필요시에만)

## 레이어 규칙 요약

- **Domain model/**: 엔티티, VO(`@JvmInline value class`), Command(sealed interface), Query data class(nullable 필드)
- **Domain repository/**: Command/Query를 기본 입력으로 사용. `findById`, `existsBy`, `countBy` 등 의도형 단건 메서드도 허용
- **Domain service/**: 도메인 객체 여럿이 엮이고, 100줄 이상이고, 여러 repo 조합이 아닐 때만 도입
- **Application service/**: BC당 1개 Service 기본, 150줄 초과시 분리. `@Service` + `@Transactional`. Entity 직접 반환이 기본
- **Application dto/**: 집계/보안 필드 제외 등 변환이 필요할 때만 생성. 기본은 Service가 Entity 직접 반환
- **Infrastructure persistence/**: Repository 구현체 + Exposed Table 정의. Table은 각 BC가 소유
- **Infrastructure external/**: OpenFeign 클라이언트 (필요시에만)
- **Presentation request/**: 클라이언트 요청 DTO (Primitive Type). Controller에서 Domain Command/Query로 변환
- **Presentation response/**: 클라이언트 응답 DTO (Primitive Type). Entity 또는 Application DTO로부터 변환
- **Presentation controller/**: Request→Command/Query 변환, Service 호출, Entity/DTO→Response 변환
- **Common**: 공유 VO(`domain/`), 공통 설정(`config/`). 필요한 것만 생성

## TDD

Domain, Application 레이어 코드 작성 시 반드시 TDD 사이클을 따릅니다. 구현 코드를 테스트 없이 먼저 작성하지 않습니다.

1. **RED** — 실패하는 테스트를 먼저 작성
2. **GREEN** — 테스트를 통과시키는 최소한의 구현
3. **REFACTOR** — 테스트 통과 상태를 유지하면서 정리

기능 구현 요청 시: 요구사항에서 동작 목록 도출 → 첫 동작의 테스트 작성·실패 확인 → 최소 구현 → 리팩토링 → 다음 동작 반복.
Infrastructure/Presentation은 선택적, 설정·빌드·마이그레이션은 미적용.

**예외**: `ArchitectureTest`는 ArchUnit(`@AnalyzeClasses` + `@ArchTest`)을 사용하며, TDD 대상이 아닙니다.
