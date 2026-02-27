# GraphQL API 문서화 전략

프론트엔드 개발자에게 GraphQL API 문서를 정적 HTML로 제공하기 위한 도구 선정과 CI 통합 방안을 정리합니다.

## 도구 선정: SpectaQL

> **GitHub**: https://github.com/anvilco/spectaql
> **유형**: 정적 사이트 생성기 (Node.js)

### 선정 이유

| 기준 | SpectaQL 평가 |
|------|---------------|
| **CI/CD 통합** | CLI 기반으로 Gradle task, GitHub Actions 등에 즉시 통합 가능 |
| **정적 HTML 출력** | 빌드 결과물이 순수 HTML/CSS/JS — 별도 서버 없이 Nginx, S3 등으로 바로 배포 |
| **SDL 파일 직접 지원** | `src/main/resources/schema/*.graphqls` 파일을 그대로 입력으로 사용 |
| **프론트 개발자 대상** | 3-column 레이아웃(네비게이션/설명/예시)으로 API 레퍼런스 탐색에 최적화 |
| **자동화** | 스키마 변경 → CI 빌드 → 문서 자동 갱신 파이프라인 구성 가능 |

### 검토한 대안

- **Magidoc** — Svelte 기반 모던 UI, Fuzzy 검색 등 기능이 풍부하나 Svelte 의존성이 추가되고 커뮤니티가 SpectaQL 대비 작음
- **GraphiQL** — DGS 기본 내장 인터랙티브 IDE. API 테스트 용도로는 적합하나 정적 문서 생성이 불가하여 CI 파이프라인에 맞지 않음

## SpectaQL 개요

SpectaQL은 GraphQL 스키마로부터 **3-column 레이아웃의 정적 HTML 문서**를 자동 생성하는 Node.js 도구입니다.

### 주요 기능

- 모든 Type, Field, Query, Mutation, Argument, Subscription 자동 문서화
- SDL 파일(`.graphqls`), 라이브 엔드포인트, introspection JSON 모두 지원
- CSS/JS/HTML 테마 커스터마이징 (Handlebars 템플릿)
- 스키마 description의 Markdown 지원
- 예시 코드 자동/수동 생성

## 프로젝트 구성

### 디렉토리 구조

```text
project-root/
├── spectaql/
│   └── config.yml              # SpectaQL 설정 파일
├── Dockerfile.api-docs         # Nginx 기반 문서 서빙 이미지
├── .github/workflows/
│   └── deploy-api-docs.yml     # 문서 빌드·배포 워크플로우
└── src/main/resources/schema/
    ├── task.graphqls            # BC별 스키마 파일 (문서 소스)
    └── ...
```

### SpectaQL 설정 (`spectaql/config.yml`)

BC별 스키마가 추가되면 `schemaFile` 목록에 경로를 추가합니다. `learning.graphqls`는 학습용이므로 제외합니다.

### 로컬 실행

```bash
npx spectaql spectaql/config.yml
open build/spectaql/index.html
```

## CI/CD 파이프라인

### 빌드·배포 흐름

```text
main push (스키마/spectaql 변경) → deploy-api-docs.yml 트리거
    ↓
Docker 멀티스테이지 빌드 (Node.js → SpectaQL 생성 → Nginx 이미지)
    ↓
Harbor push (harbor.homelab.robinjoon.xyz/loop/server-docs)
    ↓
Helm repo 업데이트 (Spotit-KR/loop-helm → server-docs/values/values-prod.yaml)
    ↓
k3s 자동 배포 → Nginx가 정적 HTML 서빙
```

### 워크플로우 (`deploy-api-docs.yml`)

- **트리거**: `main` push 시 `src/main/resources/schema/**`, `spectaql/**`, `Dockerfile.api-docs` 변경 + 수동 트리거
- **이미지**: `loop/server-docs` (기존 `loop/server`와 분리)
- **태그 포맷**: `YYYYMMDD-<run-number>` + `latest`
- 기존 `deploy-prod.yml`과 동일한 패턴 (Harbor push → Helm repo 업데이트)

### Docker 이미지 (`Dockerfile.api-docs`)

멀티스테이지 빌드로 최종 이미지에는 Nginx + 정적 HTML만 포함됩니다.

```text
Stage 1 (node:20-alpine)  → npx spectaql로 HTML 생성
Stage 2 (nginx:alpine)    → 생성된 HTML을 /usr/share/nginx/html/에 복사
```

> 문서 접근 인증은 배포 서버의 인프라 레이어(Ingress, Basic Auth 등)에서 별도 처리합니다.

## 참고 링크

- [SpectaQL GitHub](https://github.com/anvilco/spectaql)
- [SpectaQL 설정 옵션](https://github.com/anvilco/spectaql#options)
- [Magidoc 공식 문서](https://magidoc.js.org) — 대안 참고
- [GraphiQL GitHub](https://github.com/graphql/graphiql) — 대안 참고
