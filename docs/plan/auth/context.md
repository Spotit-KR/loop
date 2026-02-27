# auth BC 맥락

## 배경
PRD의 모든 1순위 기능(목표/태스크/회고)은 회원 인증을 전제로 한다. 현재 프로덕션 코드가 전혀 없는 상태이므로, auth BC를 첫 번째 기능으로 구현한다.

## 목표
- 회원가입(register) + 로그인(login) GraphQL Mutation 제공
- JWT AccessToken 발급
- BCrypt 비밀번호 해싱
- DDD + Clean Architecture 준수

## 제약조건
- 아키텍처: DDD + Clean Architecture (docs/architecture.md)
- Spring Security 7 API 사용 (docs/spring-security-7.md)
- Domain/Application 레이어 TDD 필수
- DGS Codegen으로 GraphQL 타입 자동 생성
- Exposed 1.0.0 (v1 패키지) 사용
- RefreshToken 미구현 (MVP 범위 외)
- Password VO는 Domain에 두지 않음 (해싱은 인프라 관심사)

## 관련 문서
- PRD (Google Drive ID: 1Pb_Ma6mfLJZpD3gpUVaqPkLdBsyAfPFuwY6AIy6J19A)
- DB 스키마: docs/schema.sql
- 아키텍처: docs/architecture.md
- 레이어 가이드: docs/layers/*.md
- Spring Security 7: docs/spring-security-7.md
