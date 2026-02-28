# GraphQL 예외 처리 표준 적용 맥락

## 배경
현재 auth, common BC의 코드가 `docs/graphql-error-handling.md`에 정의된 예외 처리 표준을 따르지 않음.
- VO에서 `require()` → `IllegalArgumentException` (클라이언트에 `INTERNAL`로 전달)
- Service에서 `NoSuchElementException`, `require()` 사용 (역시 `INTERNAL`로 전달)
- 예외 → GraphQL ErrorType 매핑 핸들러 부재

## 목표
- sealed `LoopException` 예외 계층 도입
- 기존 코드의 범용 예외를 도메인 예외로 교체
- `@ControllerAdvice` 핸들러로 예외 → GraphQL ErrorType 자동 매핑

## 제약조건
- Domain 순수성 유지 (프레임워크 의존 없음)
- 기존 테스트 모두 통과
- Architecture 규칙 준수

## 관련 문서
- `docs/graphql-error-handling.md`
- `docs/architecture.md`
