---
name: add-bounded-context
description: 새로운 Bounded Context(도메인)를 추가합니다. 새로운 도메인이나 모듈을 추가할 때 사용하세요.
argument-hint: [bounded-context-name]
allowed-tools: Bash(mkdir:*)
---

# Add Bounded Context

인수로 전달된 `$ARGUMENTS`를 Bounded Context 이름으로 사용하여 디렉토리 구조를 생성합니다.

이름은 반드시 **소문자 단수형**이어야 합니다. (예: `task`, `member`, `notification`)

## 실행할 명령어

다음 두 명령어를 실행하세요:

```bash
mkdir -p src/main/kotlin/kr/io/team/loop/$ARGUMENTS/{presentation/{request,response,controller},application/{dto,service},domain/{model,repository,service},infrastructure/{persistence,external}}
```

```bash
mkdir -p src/test/kotlin/kr/io/team/loop/$ARGUMENTS/{domain/model,application/service}
```

## 완료 후

디렉토리 생성 결과를 사용자에게 보고하세요. 다음 내용을 포함합니다:

- 생성된 BC 이름
- main 디렉토리 구조
- test 디렉토리 구조
- CLAUDE.md의 레이어별 코딩 규칙을 따라 코드를 작성하라는 안내
