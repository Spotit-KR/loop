# ArchitectureTest Infrastructure FK 예외 추가 맥락

## 배경

architecture.md의 BC 간 통신 규칙에서 Infrastructure 레이어 간 FK 참조를 허용하고 있지만, 현재 `ArchitectureTest`의 BC 격리 규칙은 `common`만 예외 처리하고 있어 Infrastructure 간 FK 참조도 위반으로 감지된다.

## 목표

`ArchitectureTest`의 BC 격리 규칙에 Infrastructure 간 의존을 허용하는 `ignoreDependency`를 추가하여, 문서와 테스트 코드를 일치시킨다.

## 제약조건

- ArchitectureTest는 JUnit5 스타일 (`@AnalyzeClasses` + `@ArchTest`)
- ArchitectureTest는 TDD 대상이 아님
- 기존 테스트 규칙에 영향 없어야 함

## 관련 문서

- `docs/architecture.md` — BC 간 통신 규칙 섹션
- `docs/layers/infrastructure.md` — BC 간 FK 참조 예시
- `src/test/kotlin/kr/io/team/loop/ArchitectureTest.kt` — 수정 대상
