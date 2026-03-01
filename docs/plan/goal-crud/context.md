# Goal CRUD API 맥락

## 배경
PRD 2순위 기능인 "목표 관리" 구현. 피그마 디자인 기준으로 목표 CRUD API 제공.

## 목표
- 인증된 사용자가 목표를 생성/조회/수정/삭제할 수 있는 GraphQL API
- task BC 미구현 상태이므로 task 진행률 필드는 이후 DataLoader로 추가

## 제약조건
- DDD + Clean Architecture 준수
- TDD (Domain, Application 필수)
- DGS Codegen으로 GraphQL 타입 자동 생성
- GoalId는 common/domain에 배치 (task BC에서도 사용 예정)
- DB 스키마는 docs/schema.sql의 goal 테이블 그대로 사용

## 관련 문서
- docs/schema.sql — DB 스키마 정의
- docs/architecture.md — 아키텍처 가이드
- auth BC — 참조 구현 패턴
