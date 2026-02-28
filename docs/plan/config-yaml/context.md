# 설정 파일 YAML 전환 맥락

## 배경
application.properties를 YAML로 전환하고, 로컬(H2)과 테스트 환경 모두에서 Flyway가 동작하도록 설정한다.

## 목표
- application.yml로 전환 완료
- local 프로파일: H2 + Flyway 동작
- test 환경: H2 + Flyway 동작
- prod 프로파일: PostgreSQL + Flyway 동작

## 제약조건
- 설정 파일만 변경 (코드 변경 없음)
