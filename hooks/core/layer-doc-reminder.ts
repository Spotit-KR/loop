import { existsSync, mkdirSync, writeFileSync } from "node:fs"
import { join } from "node:path"
import { tmpdir } from "node:os"

import type { HookCtx, HookResult } from "./types"

const LAYER_DOCS: Record<string, string> = {
  domain: "docs/layers/domain.md",
  application: "docs/layers/application.md",
  infrastructure: "docs/layers/infrastructure.md",
  presentation: "docs/layers/presentation.md",
}

const SECURITY_DOC = "docs/spring-security-7.md"

let reminderDir = join(tmpdir(), "loop_layer_reminders")

const inProcessReminded = new Set<string>()

export function detectLayer(filePath: string): string | null {
  for (const layer of Object.keys(LAYER_DOCS)) {
    if (new RegExp(`(^|/)${layer}(/|$)`).test(filePath)) return layer
  }
  return null
}

export function isSecurityRelated(filePath: string): boolean {
  return (
    /(^|\/)common\/config(\/|$)/.test(filePath) &&
    filePath.toLowerCase().includes("security")
  )
}

function isDocsPath(filePath: string): boolean {
  return /(^|\/)docs\//.test(filePath)
}

function isTestPath(filePath: string): boolean {
  return /(^|\/)src\/test\//.test(filePath) || filePath.endsWith("Test.kt")
}

function markerPath(sessionId: string, key: string): string {
  return join(reminderDir, `${sessionId}_${key}`)
}

function shouldRemind(sessionId: string | undefined, key: string): boolean {
  if (sessionId) return !existsSync(markerPath(sessionId, key))
  return !inProcessReminded.has(key)
}

function markReminded(sessionId: string | undefined, key: string): void {
  if (sessionId) {
    mkdirSync(reminderDir, { recursive: true })
    writeFileSync(markerPath(sessionId, key), "")
    return
  }
  inProcessReminded.add(key)
}

function collectFilePaths(args: Record<string, unknown>): string[] {
  const direct = (args.file_path ?? args.filePath ?? args.path) as string | undefined
  return direct ? [direct] : []
}

export function check(ctx: HookCtx): HookResult {
  if (!["Edit", "Write", "edit", "write"].includes(ctx.tool)) return { kind: "allow" }

  const paths = collectFilePaths(ctx.args)
  if (paths.length === 0) return { kind: "allow" }

  const messages: string[] = []

  for (const filePath of paths) {
    if (!filePath) continue
    if (isDocsPath(filePath)) continue
    if (isTestPath(filePath)) continue

    const layer = detectLayer(filePath)
    if (layer && shouldRemind(ctx.sessionId, layer)) {
      markReminded(ctx.sessionId, layer)
      messages.push(`[${layer}] 이 레이어 첫 수정입니다. 먼저 ${LAYER_DOCS[layer]} 를 읽으세요.`)
    }

    if (isSecurityRelated(filePath) && shouldRemind(ctx.sessionId, "security")) {
      markReminded(ctx.sessionId, "security")
      messages.push(`[security] Security 설정 첫 수정입니다. 먼저 ${SECURITY_DOC} 를 읽으세요.`)
    }
  }

  if (messages.length > 0) return { kind: "block", reason: messages.join("\n") }
  return { kind: "allow" }
}

export const __testing = {
  setReminderDir(dir: string) {
    reminderDir = dir
  },
  resetInProcess() {
    inProcessReminded.clear()
  },
}
