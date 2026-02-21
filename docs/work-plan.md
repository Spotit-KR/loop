# Loop Server 백엔드 작업 계획

## 개요

Loop는 "할일 기록 + 회고를 통한 성장 순환" 서비스입니다.
이 문서는 와이어프레임 기반으로 도출된 백엔드 기능들의 구현 계획을 정리합니다.

## 프로젝트 현황

- Spring Boot 4.0.3 / Kotlin 2.3.10 / Java 25
- Exposed ORM + Flyway + PostgreSQL (H2 개발/테스트)
- Netflix DGS (GraphQL)
- 현재 코드: ServerApplication.kt만 존재 (초기 상태)

## Bounded Context 구성

| BC | 설명 |
|----|------|
| `common` | 공유 VO (MemberId 등), 공통 설정, 인프라 |
| `auth` | 이메일/비밀번호 인증, 토큰 관리 |
| `member` | 회원 정보 관리 |
| `category` | 카테고리 CRUD |
| `task` | 할일 CRUD, 완료 토글, 달성률 |

---

## 스프린트 1: 프로젝트 기반 + 인증

### 1-0. 프로젝트 기반 설정

- [ ] application.yml 구성 (DB, Flyway, 서버 설정)
- [ ] common/config 구성 (Security, WebMvc, CORS 등)
- [ ] Flyway baseline 마이그레이션 작성
- [ ] 공유 VO 정의 (`common/domain/`)
  - `MemberId`, `CategoryId`, `TaskId`

### 1-1. 회원 (Member BC)

> 카카오 로그인은 추후 도입 예정. MVP에서는 이메일/비밀번호 인증.

**Domain**

- Entity: `Member` (id, email, password, nickname, profileImageUrl, createdAt, updatedAt)
- VO: `Email`, `Password`, `Nickname`
- Command: `MemberCommand.Register`, `MemberCommand.UpdateProfile`
- Query: `MemberQuery` (memberId)
- Repository: `MemberRepository`

**DB 마이그레이션**

```sql
CREATE TABLE members (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(50)  NOT NULL,
    profile_image_url VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

**API**

| Method | Path | 설명 |
|--------|------|------|
| GET | `/members/me` | 내 정보 조회 |
| PUT | `/members/me` | 프로필 수정 |

### 1-2. 인증 (Auth BC)

**Domain**

- VO: `AccessToken`, `RefreshToken`
- Command: `AuthCommand.Login`, `AuthCommand.Register`, `AuthCommand.Refresh`

**Infrastructure**

- JWT 토큰 발급/검증 (Access + Refresh)
- 비밀번호 암호화 (BCrypt)
- Spring Security 필터 체인 구성

**API**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/auth/register` | 회원가입 (이메일, 비밀번호, 닉네임) |
| POST | `/auth/login` | 로그인 → JWT 발급 |
| POST | `/auth/refresh` | Access Token 갱신 |
| POST | `/auth/logout` | Refresh Token 무효화 |

> 추후 카카오 OAuth 도입 시 `POST /auth/kakao` 엔드포인트 추가 및
> Member 엔티티에 `oauthProvider`, `oauthId` 필드 확장 예정.

---

## 스프린트 2: 카테고리 + 할일 (핵심 기능)

### 2-1. 카테고리 (Category BC)

**Domain**

- Entity: `Category` (id, memberId, name, color, sortOrder, createdAt, updatedAt)
- VO: `CategoryName`, `CategoryColor`, `SortOrder`
- Command: `CategoryCommand.Create`, `CategoryCommand.Update`, `CategoryCommand.Delete`
- Query: `CategoryQuery` (memberId)
- Repository: `CategoryRepository`

**DB 마이그레이션**

```sql
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT       NOT NULL REFERENCES members(id),
    name        VARCHAR(50)  NOT NULL,
    color       VARCHAR(7)   NOT NULL,  -- #RRGGBB
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

**API**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/categories` | 카테고리 생성 |
| GET | `/categories` | 내 카테고리 목록 조회 |
| PUT | `/categories/{id}` | 카테고리 수정 (이름, 컬러) |
| DELETE | `/categories/{id}` | 카테고리 삭제 |

**비즈니스 규칙**

- 회원당 카테고리 최대 개수 제한 (예: 10개)
- 카테고리 삭제 시 하위 할일이 있으면 삭제 불가 (or 확인 필요)
- 카테고리명 중복 불가 (동일 회원 내)

### 2-2. 할일 (Task BC)

**Domain**

- Entity: `Task` (id, memberId, categoryId, title, completed, taskDate, createdAt, updatedAt)
- VO: `TaskTitle`, `TaskDate`
- Command: `TaskCommand.Create`, `TaskCommand.Update`, `TaskCommand.ToggleComplete`, `TaskCommand.Delete`
- Query: `TaskQuery` (memberId, categoryId?, taskDate?, completed?)
- Repository: `TaskRepository`

**DB 마이그레이션**

```sql
CREATE TABLE tasks (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT       NOT NULL REFERENCES members(id),
    category_id BIGINT       NOT NULL REFERENCES categories(id),
    title       VARCHAR(200) NOT NULL,
    completed   BOOLEAN      NOT NULL DEFAULT FALSE,
    task_date   DATE         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tasks_member_date ON tasks(member_id, task_date);
```

**API**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/tasks` | 할일 생성 |
| GET | `/tasks?date={yyyy-MM-dd}` | 날짜별 할일 조회 (카테고리별 그룹핑) |
| PUT | `/tasks/{id}` | 할일 수정 (제목, 카테고리 변경) |
| PATCH | `/tasks/{id}/status` | 완료/미완료 토글 |
| DELETE | `/tasks/{id}` | 할일 삭제 |
| GET | `/tasks/stats?date={yyyy-MM-dd}` | 달성률 조회 (완료/전체 개수) |

**비즈니스 규칙**

- 할일은 반드시 카테고리에 소속
- 날짜 기준 조회 시 카테고리별 그룹핑하여 반환
- 본인의 할일만 CRUD 가능 (소유권 검증)

---

## 후순위 기능 (추후 스프린트)

- 데일리 회고 (KPT: Keep/Problem/Try)
- 캘린더 월별 요약 조회
- 카카오 OAuth 로그인 도입
- 알림 (Notification) 시스템
- 설정 (Settings) 관리
- 카테고리 순서 변경 API

---

## 구현 순서 요약

```
스프린트 1: 기반 설정 → Member → Auth (이메일/비밀번호)
스프린트 2: Category → Task (핵심 기능)
후순위:     회고, 캘린더, 카카오 OAuth, 알림, 설정
```

## TDD 적용 범위

CLAUDE.md 규칙에 따라:

- **Domain, Application 레이어**: TDD 필수 (RED → GREEN → REFACTOR)
- **Infrastructure, Presentation**: 통합 테스트 선택적
- 각 BC 구현 시 테스트 먼저 작성 → 최소 구현 → 리팩토링 사이클 반복
