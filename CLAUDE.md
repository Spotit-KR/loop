# AI 에이전트 규칙

작업 계획 문서 작성 시 별도 지시가 없으면 docs/ 디렉토리에 마크다운 파일로 작성.

## Git 브랜치 규칙

- 이슈 기반 작업은 `ticket/#<이슈번호>` 형식으로 브랜치를 생성합니다. (예: `ticket/#13`)

## 아키텍처

DDD + Clean Architecture 기반. 상세 구조와 코드 패턴은 @docs/architecture.md 참고.

- Domain Layer는 외부 의존성 없이 순수 Kotlin 코드로 작성
- Presentation, Application, Infrastructure 모두 Domain에 직접 의존
- Presentation은 Application(Service, DTO)과 Domain(Command, Query, VO)에 의존
- Infrastructure는 Domain(Repository 인터페이스)에만 의존
- common을 제외한 다른 BC의 코드를 직접 참조하지 않음
- BC 간 공유 VO는 `common/domain/`에 위치
- Domain의 Command/Query가 Presentation 이후 모든 레이어의 공용 입력

## 레이어 규칙 요약

- **Domain model/**: 엔티티, VO(`@JvmInline value class`), Command(sealed interface), Query data class(nullable 필드)
- **Domain repository/**: Command/Query를 입력으로, 엔티티를 출력으로 사용
- **Domain service/**: 도메인 객체 여럿이 엮이고, 100줄 이상이고, 여러 repo 조합이 아닐 때만 도입
- **Application service/**: UseCase 인터페이스 없이 Service가 직접 구현. `@Service` + `@Transactional`
- **Application dto/**: 서비스 반환 DTO
- **Infrastructure persistence/**: Repository 구현체 + ORM 테이블 정의
- **Infrastructure external/**: OpenFeign 클라이언트 (필요시에만)
- **Presentation request/**: 클라이언트 요청 DTO (Primitive Type). Controller에서 Domain Command/Query로 변환
- **Presentation response/**: 클라이언트 응답 DTO (Primitive Type). Controller에서 Application DTO로부터 변환
- **Presentation controller/**: Request→Command/Query 변환, Service 호출, DTO→Response 변환
- **Common**: 공유 VO(`domain/`), 공통 설정(`config/`), 공통 인프라(`infrastructure/`). 필요한 것만 생성

## 테스트

- Kotest (BehaviorSpec — Given/When/Then) + MockK + SpringMockK
- Domain/Application: 단위 테스트 **필수** (Spring Context 없음)
- Infrastructure/Presentation: 통합 테스트 **선택적** (사람이 판단)
- 테스트 클래스명: `대상클래스명Test`

## TDD

Domain, Application 레이어 코드 작성 시 반드시 TDD 사이클을 따릅니다. 구현 코드를 테스트 없이 먼저 작성하지 않습니다.

1. **RED** — 실패하는 테스트를 먼저 작성
2. **GREEN** — 테스트를 통과시키는 최소한의 구현
3. **REFACTOR** — 테스트 통과 상태를 유지하면서 정리

기능 구현 요청 시: 요구사항에서 동작 목록 도출 → 첫 동작의 테스트 작성·실패 확인 → 최소 구현 → 리팩토링 → 다음 동작 반복.
Infrastructure/Presentation은 선택적, 설정·빌드·마이그레이션은 미적용.
