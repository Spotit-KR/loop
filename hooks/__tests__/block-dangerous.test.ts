import { describe, expect, test } from "bun:test"

import { check } from "../core/block-dangerous"

function bash(command: string) {
  return check({ tool: "Bash", args: { command }, cwd: "/x" })
}

describe("block-dangerous: hard block", () => {
  test("rm -rf /", () => {
    expect(bash("rm -rf /").kind).toBe("block")
  })
  test("rm -rf / && echo done", () => {
    expect(bash("rm -rf / && echo done").kind).toBe("block")
  })
  test("rm -rf ~", () => {
    expect(bash("rm -rf ~").kind).toBe("block")
  })
  test("rm -rf *", () => {
    expect(bash("rm -rf *").kind).toBe("block")
  })
  test("rm -rf ../sibling", () => {
    expect(bash("rm -rf ../sibling").kind).toBe("block")
  })
  test("git push --force", () => {
    expect(bash("git push --force origin feature").kind).toBe("block")
  })
  test("git push -f", () => {
    expect(bash("git push -f origin main").kind).toBe("block")
  })
  test("git reset --hard", () => {
    expect(bash("git reset --hard HEAD~1").kind).toBe("block")
  })
  test("git clean -fd", () => {
    expect(bash("git clean -fd").kind).toBe("block")
  })
  test("git checkout .", () => {
    expect(bash("git checkout .").kind).toBe("block")
  })
  test("git restore .", () => {
    expect(bash("git restore .").kind).toBe("block")
  })
  test("git stash drop", () => {
    expect(bash("git stash drop").kind).toBe("block")
  })
  test("git stash clear", () => {
    expect(bash("git stash clear").kind).toBe("block")
  })
  test("DROP TABLE", () => {
    expect(bash("psql -c 'DROP TABLE users'").kind).toBe("block")
  })
  test("DROP DATABASE", () => {
    expect(bash("psql -c 'DROP DATABASE x'").kind).toBe("block")
  })
  test("TRUNCATE TABLE", () => {
    expect(bash("psql -c 'TRUNCATE TABLE x'").kind).toBe("block")
  })
})

describe("block-dangerous: allow", () => {
  test("ls -la", () => {
    expect(bash("ls -la").kind).toBe("allow")
  })
  test("git status", () => {
    expect(bash("git status").kind).toBe("allow")
  })
  test("git push origin main", () => {
    expect(bash("git push origin main").kind).toBe("allow")
  })
  test("rm file.txt", () => {
    expect(bash("rm file.txt").kind).toBe("allow")
  })
  test("./gradlew build", () => {
    expect(bash("./gradlew build").kind).toBe("allow")
  })
  test("Edit tool ignored", () => {
    expect(check({ tool: "Edit", args: { file_path: "/x" }, cwd: "/x" }).kind).toBe("allow")
  })
  test("empty command allowed", () => {
    expect(bash("").kind).toBe("allow")
  })
  test("lowercase bash tool name (opencode) is recognized", () => {
    expect(check({ tool: "bash", args: { command: "rm -rf /" }, cwd: "/x" }).kind).toBe("block")
  })
})
