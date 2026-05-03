# 훅 로직 단일화 계획

> Issue: #46

## 단계

- [x] 1. `hooks/` 패키지 골격 구성 (`package.json`, `tsconfig.json`, `__tests__/`)
- [x] 2. 공통 라이브러리 추출 — `lib/plan-scanner.ts`, `lib/path.ts`
- [x] 3. block-dangerous 코어 포팅 + 테스트
- [x] 4. layer-doc-reminder 코어 포팅 + 테스트
- [x] 5. plan-update-reminder 코어 포팅 + 테스트
- [x] 6. work-plan-enforcer 코어 포팅 + 테스트
- [x] 7. plan-completion-guard 코어 포팅 + 테스트
- [x] 8. Claude dispatcher (`adapters/claude/run.mjs`) 작성 + `.claude/settings.json` 갱신
- [x] 9. OpenCode 단일 플러그인 (`adapters/opencode/plugin.ts`) 작성
- [x] 10. 양 시스템 시나리오 검증 (checklist.md)
- [x] 11. 기존 Python 가드/리마인더 훅 + JS 플러그인 삭제

## 1. 배경

현재 동일한 가드/리마인더 로직이 두 언어로 중복 구현되어 있다.

| 훅 | Claude (`.claude/hooks/`) | OpenCode (`.opencode/plugins/`) |
|----|---------------------------|--------------------------------|
| 위험 명령 차단 | `block_dangerous.py` | `block-dangerous.js` |
| 작업 계획 강제 | `work_plan_enforcer.py` | `work-plan-enforcer.js` |
| 계획 완료 가드 (PR/push) | `plan_completion_guard.py` | **누락** |
| 계획 업데이트 리마인더 | `plan_update_reminder.py` | `plan-update-reminder.js` |
| 레이어 문서 리마인더 | `layer_doc_reminder.py` | `layer-doc-reminder.js` |

> 포매팅(`markdown_formatter`, `ktlint-format` 등)은 통합 대상에서 제외한다.
> OpenCode는 [내장 포매터 시스템](https://opencode.ai/docs/ko/formatters/)으로 ktlint·prettier 등을 `opencode.json`의 `formatter` 섹션에서 자동 처리한다. Claude 측 포매팅 훅은 그대로 두되, 동등한 동작은 OpenCode 내장 메커니즘에 위임한다.

이미 드리프트가 발생한 상태:
- `plan_completion_guard`는 OpenCode 측에 없음
- `BLOCKED_PATTERNS` 메시지가 미세하게 다름 (예: `git checkout .` 메시지 표현)

## 2. 두 훅 시스템 비교

| 항목 | Claude Code | OpenCode |
|------|-------------|----------|
| 진입점 | `settings.json` → `hooks.<EventName>[].hooks[].command` | `.opencode/plugins/*.{js,ts}` |
| 실행 방식 | 매 호출마다 외부 프로세스 spawn | Bun 인-프로세스 |
| 입력 | stdin JSON (`tool_name`, `tool_input`, `cwd`, …) | `(input, output)` 객체 |
| 차단 | exit code 2 + stderr | `throw new Error(...)` |
| 분기/주입 | stdout JSON (`hookSpecificOutput.additionalContext`, `permissionDecision`, `updatedInput`) | `output.args` 변형 |
| 외부 명령 호출 | 임의 가능 (이미 프로세스) | Bun `$` 쉘 헬퍼 |
| 의존성 공유 | 임의 (PATH 기준) | `package.json` + `import` |
| 환경 변수 | `CLAUDE_PROJECT_DIR` 등 | 핸들러 인자 `directory` |

핵심: 두 시스템 모두 **언어 무관**(read stdin / write stdout 또는 ESM export)하며 로직 입력은 사실상 `(toolName, args, cwd)`로 동치.

## 3. 접근 옵션 비교

| 옵션 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **A** | 현 상태 유지 | 변경 없음 | 드리프트 가속 |
| **B** | 두 언어로 포팅 + pure-logic 라이브러리만 추출 | 양쪽 인-프로세스 | 두 포팅 유지 비용 그대로 |
| **C** | **단일 JS/TS 코어 + 어댑터 2종** | 진실의 단일 소스, OpenCode는 import, Claude는 `bun run.mjs` 한 줄 호출 | Claude 측에 Bun 의존 강제 (이미 OpenCode가 사용 중) |
| **D** | 단일 Python 코어 + OpenCode가 spawn | Claude 측 변경 최소 | OpenCode 인-프로세스 이점 상실, Bun→Python 호출 부자연 |

**권장: 옵션 C.**
- Bun은 이미 `.opencode/bun.lock`으로 프로젝트 런타임에 포함.
- Bun 콜드스타트(~30ms)는 Python(~50ms)과 비슷하거나 빠름 → Claude 측 spawn 비용 악화 없음.
- Bun은 `.ts` 직접 실행 가능 → 트랜스파일 단계 불필요.

## 4. 목표 디렉토리 구조

```text
hooks/
├── package.json                       # bun, type: module, ../.opencode와 별개 패키지
├── tsconfig.json
├── core/                              # 단일 진실 소스
│   ├── block-dangerous.ts
│   ├── work-plan-enforcer.ts
│   ├── plan-completion-guard.ts
│   ├── plan-update-reminder.ts
│   ├── layer-doc-reminder.ts
│   └── lib/
│       ├── plan-scanner.ts            # find/has Active/Incomplete plan
│       └── path.ts                    # normalize, isExempt
├── adapters/
│   ├── claude/run.mjs                 # `bun .../run.mjs <hook> <phase>` 단일 dispatcher
│   └── opencode/plugin.ts             # 모든 코어를 OpenCode 이벤트로 매핑한 단일 플러그인
└── __tests__/                         # bun test
    ├── block-dangerous.test.ts
    ├── work-plan-enforcer.test.ts
    └── ...
```

각 코어 모듈의 시그니처를 통일:

```ts
export type HookCtx = {
  tool: string                // "Bash" | "Edit" | "Write" | ...
  args: Record<string, unknown>
  cwd: string
  phase: "before" | "after"
}

export type HookResult =
  | { kind: "allow" }
  | { kind: "block"; reason: string }
  | { kind: "context"; message: string }            // additionalContext / 콘솔 출력
  | { kind: "modify"; updatedInput: Record<string, unknown> }
  | { kind: "sideEffect"; run: () => Promise<void> } // 포매터처럼 파일 수정만 하는 경우

export function check(ctx: HookCtx): HookResult | Promise<HookResult>
```

## 5. Claude 어댑터 (단일 dispatcher)

`hooks/adapters/claude/run.mjs`:

```js
#!/usr/bin/env bun
import { argv, stdin, stderr, stdout, env, exit } from "process"

const [, , hookName, phase = "before"] = argv
const raw = await new Response(stdin).text()
const data = JSON.parse(raw || "{}")
const cwd = data.cwd ?? env.CLAUDE_PROJECT_DIR ?? process.cwd()

const mod = await import(`../../core/${hookName}.ts`)
const result = await mod.check({
  tool: data.tool_name,
  args: data.tool_input ?? {},
  cwd,
  phase,
})

switch (result.kind) {
  case "block":
    stderr.write(result.reason)
    exit(2)
  case "context":
    stdout.write(JSON.stringify({
      hookSpecificOutput: {
        hookEventName: data.hook_event_name,
        additionalContext: result.message,
      },
    }))
    exit(0)
  case "modify":
    stdout.write(JSON.stringify({
      hookSpecificOutput: {
        hookEventName: data.hook_event_name,
        updatedInput: result.updatedInput,
      },
    }))
    exit(0)
  case "sideEffect":
    await result.run()
    exit(0)
  case "allow":
  default:
    exit(0)
}
```

`.claude/settings.json` 항목은 다음 패턴으로 단순화:

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          { "type": "command", "command": "bun \"$CLAUDE_PROJECT_DIR/hooks/adapters/claude/run.mjs\" block-dangerous before" },
          { "type": "command", "command": "bun \"$CLAUDE_PROJECT_DIR/hooks/adapters/claude/run.mjs\" work-plan-enforcer before" },
          { "type": "command", "command": "bun \"$CLAUDE_PROJECT_DIR/hooks/adapters/claude/run.mjs\" plan-completion-guard before" }
        ]
      },
      {
        "matcher": "Edit|Write",
        "hooks": [
          { "type": "command", "command": "bun \"$CLAUDE_PROJECT_DIR/hooks/adapters/claude/run.mjs\" layer-doc-reminder before" },
          { "type": "command", "command": "bun \"$CLAUDE_PROJECT_DIR/hooks/adapters/claude/run.mjs\" work-plan-enforcer before" }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "TaskCreate|TaskUpdate",
        "hooks": [
          { "type": "command", "command": "bun \"$CLAUDE_PROJECT_DIR/hooks/adapters/claude/run.mjs\" plan-update-reminder after" }
        ]
      }
    ]
  }
}
```

## 6. OpenCode 어댑터 (단일 플러그인)

`hooks/adapters/opencode/plugin.ts`:

```ts
import * as BlockDangerous from "../../core/block-dangerous"
import * as WorkPlanEnforcer from "../../core/work-plan-enforcer"
import * as PlanCompletionGuard from "../../core/plan-completion-guard"
import * as LayerDocReminder from "../../core/layer-doc-reminder"
import * as PlanUpdateReminder from "../../core/plan-update-reminder"

const BEFORE = [BlockDangerous, WorkPlanEnforcer, PlanCompletionGuard, LayerDocReminder]
const AFTER = [PlanUpdateReminder]

const TOOL_MAP: Record<string, string> = {
  bash: "Bash", edit: "Edit", write: "Write", read: "Read",
  // 필요시 확장
}

export const LoopHooks = async ({ directory }) => ({
  "tool.execute.before": async (input, output) => {
    const ctx = {
      tool: TOOL_MAP[input.tool] ?? input.tool,
      args: output.args,
      cwd: directory,
      phase: "before" as const,
    }
    for (const mod of BEFORE) {
      const r = await mod.check(ctx)
      if (r.kind === "block") throw new Error(r.reason)
      if (r.kind === "modify") Object.assign(output.args, r.updatedInput)
      // context/sideEffect는 OpenCode에서 출력 채널이 없으므로 console.log 또는 무시
    }
  },
  "tool.execute.after": async (input, output) => {
    const ctx = { tool: TOOL_MAP[input.tool] ?? input.tool, args: output.args, cwd: directory, phase: "after" as const }
    for (const mod of AFTER) await mod.check(ctx)
  },
})
```

기존 `.opencode/plugins/{block-dangerous,work-plan-enforcer,layer-doc-reminder,plan-update-reminder}.js` 4개를 제거하고 위 단일 플러그인 한 개만 남긴다. `.opencode/plugins/markdown-formatter.js`는 OpenCode 내장 포매터로 대체 가능한지 검토 후 별도 결정. `.opencode/lib/work-plan-utils.js`는 `hooks/core/lib/plan-scanner.ts`로 흡수.

## 7. 단계별 작업

1. **이슈 생성** — `gh issue create --title "훅 로직 단일화" --label refactor`. 본문에 본 문서 링크.
2. **`hooks/` 패키지 골격 구성** — `package.json` (`@types/node`, dev: `bun-types`), `tsconfig.json`, `__tests__/` 디렉토리.
3. **공통 라이브러리 추출** — `lib/plan-scanner.ts`(현 `work-plan-utils.js` + `work_plan_enforcer.has_active_plan` + `plan_completion_guard.find_incomplete_plans` 통합), `lib/path.ts`(`normalizePath`, `isExempt`).
4. **코어 5개 포팅 (TDD)** — 모듈마다 (a) 기존 Python 테스트 케이스를 Bun test로 옮기고 (b) `check()` 시그니처로 구현. 순서:
   - block-dangerous (가장 단순, 의존성 없음)
   - layer-doc-reminder
   - plan-update-reminder
   - work-plan-enforcer (plan-scanner 의존)
   - plan-completion-guard (plan-scanner 의존)
5. **Claude dispatcher (`run.mjs`) 작성 + `.claude/settings.json` 갱신** (포매팅 훅 항목은 그대로 유지).
6. **OpenCode 단일 플러그인 작성** + 기존 가드/리마인더 플러그인 4개 + `.opencode/lib/` 제거.
7. **양 시스템 시나리오 검증** — 아래 체크리스트.
8. **기존 Python 가드/리마인더 훅 + JS 플러그인 삭제 커밋** (포매팅 관련 파일은 별도 작업).

## 8. 검증 체크리스트

상세 항목은 [`checklist.md`](checklist.md) 참고. 모두 통과.

## 9. 트레이드오프 및 리스크

- **Bun 의존성을 Claude 측에도 강제** — Claude Code만 쓰던 기여자도 Bun 설치 필요. 다만 이미 OpenCode 사용자에겐 필요. README/CLAUDE.md에 "Bun 1.x 필요" 명시.
- **`.ts` 직접 실행** — Bun이 처리하므로 빌드 단계 없음. 단, Node.js만으로 디버깅 시도하면 동작 안 함 → README 명시.
- **인-프로세스 vs 서브프로세스** — Claude 측은 어차피 spawn. Bun 콜드스타트는 ~30ms, 기존 Python과 동급.
- **기능 동등성 회귀 위험** — 단계 4의 TDD로 기존 Python 테스트 케이스를 모두 옮기면 큰 회귀는 없을 것. 다만 `markdown-formatter`는 Claude 측이 PostToolUse Edit·Write 시점에 도는 반면 OpenCode는 `file.edited`로 동기화 → 두 시스템에서 같은 트리거가 가능한지 확인 필요. (Claude의 `PostToolUse` Edit·Write ≈ OpenCode의 `file.edited`. 충분히 동등.)

## 10. 후속 과제 (Out of Scope)

- 글로벌(`~/.claude/`, `~/.config/opencode/`) 훅까지 공유할지 — 현재는 프로젝트 단위로 한정.
- 다른 에이전트 시스템(Codex, Cursor 등)이 추가될 때 어댑터 한 개 더 작성으로 끝나도록 코어 시그니처를 의식적으로 도구 명세에 종속시키지 않음 (`HookCtx`가 이미 일반화됨).
