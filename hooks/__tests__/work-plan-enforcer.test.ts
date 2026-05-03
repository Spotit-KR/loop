import { afterEach, beforeEach, describe, expect, test } from "bun:test"
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs"
import { tmpdir } from "node:os"
import { join } from "node:path"

import { check } from "../core/work-plan-enforcer"

let tmp: string

function makeActivePlan() {
  const dir = join(tmp, "docs", "plan", "active")
  mkdirSync(dir, { recursive: true })
  writeFileSync(join(dir, "plan.md"), "- [ ] step")
  writeFileSync(join(dir, "checklist.md"), "# c")
}

function edit(filePath: string) {
  return check({ tool: "Edit", args: { file_path: filePath }, cwd: tmp })
}

function bash(command: string) {
  return check({ tool: "Bash", args: { command }, cwd: tmp })
}

beforeEach(() => {
  tmp = mkdtempSync(join(tmpdir(), "wpe-"))
})

afterEach(() => {
  rmSync(tmp, { recursive: true, force: true })
})

describe("Edit/Write blocking", () => {
  test("src kotlin without plan blocks", () => {
    expect(edit(`${tmp}/src/main/kotlin/Task.kt`).kind).toBe("block")
  })
  test("src kotlin Write tool blocks", () => {
    expect(check({ tool: "Write", args: { file_path: `${tmp}/src/main/kotlin/X.kt` }, cwd: tmp }).kind).toBe(
      "block",
    )
  })
  test("docs path allowed", () => {
    expect(edit(`${tmp}/docs/architecture.md`).kind).toBe("allow")
  })
  test(".claude path allowed", () => {
    expect(edit(`${tmp}/.claude/hooks/x.py`).kind).toBe("allow")
  })
  test(".opencode path allowed", () => {
    expect(edit(`${tmp}/.opencode/plugins/x.js`).kind).toBe("allow")
  })
  test("build.gradle.kts allowed", () => {
    expect(edit(`${tmp}/build.gradle.kts`).kind).toBe("allow")
  })
  test("application.yml allowed", () => {
    expect(edit(`${tmp}/src/main/resources/application.yml`).kind).toBe("allow")
  })
  test("CLAUDE.md allowed", () => {
    expect(edit(`${tmp}/CLAUDE.md`).kind).toBe("allow")
  })
  test("README.md allowed", () => {
    expect(edit(`${tmp}/README.md`).kind).toBe("allow")
  })
  test("backslash src path blocked", () => {
    expect(edit(`${tmp}\\src\\main\\kotlin\\Task.kt`).kind).toBe("block")
  })
  test("backslash docs path allowed", () => {
    expect(edit(`${tmp}\\docs\\architecture.md`).kind).toBe("allow")
  })

  test("active plan unblocks src", () => {
    makeActivePlan()
    expect(edit(`${tmp}/src/main/kotlin/Task.kt`).kind).toBe("allow")
  })

  test("plan.md only (no checklist) still blocks", () => {
    const dir = join(tmp, "docs", "plan", "incomplete")
    mkdirSync(dir, { recursive: true })
    writeFileSync(join(dir, "plan.md"), "- [ ] step")
    expect(edit(`${tmp}/src/main/kotlin/Task.kt`).kind).toBe("block")
  })

  test("all plans done blocks", () => {
    const dir = join(tmp, "docs", "plan", "done")
    mkdirSync(dir, { recursive: true })
    writeFileSync(join(dir, "plan.md"), "- [x] done")
    writeFileSync(join(dir, "checklist.md"), "# c")
    expect(edit(`${tmp}/src/main/kotlin/Task.kt`).kind).toBe("block")
  })
})

describe("Bash mutation blocking", () => {
  test("echo > src blocks", () => {
    expect(bash("echo hello > src/main/kotlin/Task.kt").kind).toBe("block")
  })
  test("cat >> src blocks", () => {
    expect(bash("cat data.txt >> src/main/kotlin/Task.kt").kind).toBe("block")
  })
  test("sed -i src blocks", () => {
    expect(bash("sed -i 's/old/new/g' src/main/kotlin/Task.kt").kind).toBe("block")
  })
  test("tee src blocks", () => {
    expect(bash("tee src/main/kotlin/Task.kt").kind).toBe("block")
  })
  test("cp -> src blocks", () => {
    expect(bash("cp template.kt src/main/kotlin/Task.kt").kind).toBe("block")
  })
  test("mv -> src blocks", () => {
    expect(bash("mv old.kt src/main/kotlin/Task.kt").kind).toBe("block")
  })
  test("rm src blocks", () => {
    expect(bash("rm src/main/kotlin/Task.kt").kind).toBe("block")
  })

  test("cat src (read-only) allows", () => {
    expect(bash("cat src/main/kotlin/Task.kt").kind).toBe("allow")
  })
  test("grep src allows", () => {
    expect(bash("grep -r 'class' src/").kind).toBe("allow")
  })
  test("ls src allows", () => {
    expect(bash("ls src/main/kotlin/").kind).toBe("allow")
  })
  test("gradlew build allows", () => {
    expect(bash("./gradlew build").kind).toBe("allow")
  })
  test("echo > docs allows", () => {
    expect(bash("echo hello > docs/output.md").kind).toBe("allow")
  })
  test("echo > application.yml allows", () => {
    expect(bash("echo hello > src/main/resources/application.yml").kind).toBe("allow")
  })

  test("active plan unblocks src writes", () => {
    makeActivePlan()
    expect(bash("echo hello > src/main/kotlin/Task.kt").kind).toBe("allow")
  })
})

describe("edge cases", () => {
  test("empty file_path passes", () => {
    expect(edit("").kind).toBe("allow")
  })
  test("missing file_path passes", () => {
    expect(check({ tool: "Edit", args: {}, cwd: tmp }).kind).toBe("allow")
  })
  test("empty command passes", () => {
    expect(bash("").kind).toBe("allow")
  })
  test("Read tool always allows", () => {
    expect(check({ tool: "Read", args: { file_path: "src/main/kotlin/Task.kt" }, cwd: tmp }).kind).toBe(
      "allow",
    )
  })
})
