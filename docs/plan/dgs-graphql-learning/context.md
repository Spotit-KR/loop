# DGS GraphQL 학습 자료 작성 맥락

## 배경
회사에서 GraphQL을 사용하고 있으나 담당 업무가 아니라 상세 구현 방법을 모름.
프로젝트에 DGS 11.0.0이 이미 의존성으로 설정되어 있어 이를 기반으로 학습 필요.

## 목표
- DGS 프레임워크를 사용한 GraphQL API 제공 방법 이해
- Query, Mutation, DataLoader 등 핵심 개념을 실행 가능한 코드로 학습
- 배치 처리(N+1 해결)와 멀티 쿼리 동작 이해
- learning/dgs-graphql 브랜치에 학습용 산출물 생성

## 제약조건
- 학습 목적이므로 프로덕션 아키텍처 규칙(DDD 레이어)을 엄격히 따르지 않음
- 테스트 코드는 실제 테스트 형식을 꼭 갖출 필요 없음 (학습용)
- Spring Boot 4.0.3 / DGS 11.0.0 / Kotlin 2.3.10 기반

## 관련 문서
- Netflix DGS 공식 문서: https://netflix.github.io/dgs/
- 프로젝트 build.gradle.kts: DGS 의존성 확인
