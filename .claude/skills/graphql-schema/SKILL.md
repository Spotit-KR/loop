---
name: graphql-schema
description: Write, create, edit, or review a GraphQL schema (.graphqls) file. Enforces mandatory descriptions on all types, fields, arguments, and enum values for API documentation. Triggers when working with any .graphqls file in src/main/resources/schema/.
---

# /graphql-schema

GraphQL 스키마(`.graphqls`) 파일을 작성·수정할 때 description 규칙을 강제합니다.
SpectaQL로 정적 API 문서를 자동 생성하므로, 스키마에 작성된 description이 곧 프론트엔드 개발자가 보는 API 문서입니다.

## Arguments

$ARGUMENTS

## Description 필수 규칙

**모든 공개 스키마 요소에 description을 반드시 작성합니다.** description이 없는 요소는 허용하지 않습니다.

### 적용 대상

| 요소 | 필수 여부 | 예시 |
|------|----------|------|
| type (Query, Mutation, 커스텀 타입) | 필수 | `"""할 일"""` |
| type 내 field | 필수 | `"고유 식별자"` |
| input | 필수 | `"""할 일 생성 입력"""` |
| input 내 field | 필수 | `"할 일 제목 (1~100자)"` |
| enum | 필수 | `"""할 일 상태"""` |
| enum value | 필수 | `"진행 중"` |
| argument | 필수 | `"할 일 상태 필터"` |

### Description 문법

- **한 줄**: `"설명"` (double quote)
- **여러 줄**: `"""설명"""` (triple quote, Markdown 지원)
- `# 주석`은 사용하지 않음 (graphql-java가 description으로 노출하지만 GraphQL 스펙에 부합하지 않으므로 `"""` / `""`를 사용)

### Description 작성 가이드

- 한국어로 작성
- 프론트엔드 개발자가 이해할 수 있는 수준으로 작성
- 필드의 제약조건이 있으면 명시 (예: 최대 길이, 허용 값 범위)
- nullable 필드는 "선택" 또는 미지정 시 동작을 설명

## 올바른 예시

```graphql
"""할 일을 조회하고 관리하는 쿼리"""
type Query {
  """
  조건에 맞는 할 일 목록을 조회합니다.
  조건이 없으면 전체를 반환합니다.
  """
  tasks(
    "할 일 상태 필터 (미지정 시 전체)"
    status: TaskStatus
    "담당자 ID 필터"
    assigneeId: ID
  ): [Task!]!

  "ID로 할 일을 단건 조회합니다. 존재하지 않으면 null 반환"
  task(
    "조회할 할 일 ID"
    id: ID!
  ): Task
}

"""할 일 데이터 변경"""
type Mutation {
  "새 할 일을 생성합니다"
  createTask(
    "생성할 할 일 정보"
    input: CreateTaskInput!
  ): Task!
}

"""할 일"""
type Task {
  "고유 식별자"
  id: ID!
  "할 일 제목 (1~100자)"
  title: String!
  "할 일 상세 설명 (선택)"
  description: String
  "현재 상태"
  status: TaskStatus!
}

"""할 일 생성 입력"""
input CreateTaskInput {
  "할 일 제목 (1~100자, 필수)"
  title: String!
  "할 일 상세 설명 (선택)"
  description: String
}

"""할 일 상태"""
enum TaskStatus {
  "할 일 (미시작)"
  TODO
  "진행 중"
  IN_PROGRESS
  "완료"
  DONE
}
```

## 잘못된 예시 (금지)

```graphql
# description 없음 — 금지
type Query {
  tasks(status: TaskStatus): [Task!]!
}

# '#' 주석을 description으로 사용 — 금지
type Task {
  # 고유 식별자
  id: ID!
}
```

## 기존 스키마 수정 시

기존 `.graphqls` 파일을 수정할 때, description이 없는 요소를 발견하면 해당 요소에도 description을 추가합니다.

## 제외 대상

`src/main/resources/schema/learning.graphqls`는 학습용 스키마이므로 이 규칙을 적용하지 않습니다 (수정 금지 코드).
