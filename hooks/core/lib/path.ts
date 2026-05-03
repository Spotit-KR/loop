export const EXEMPT_PATTERNS = [
  "/docs/",
  "/.claude/",
  "/.opencode/",
  "README",
  "CLAUDE.md",
] as const

export const EXEMPT_EXTENSIONS = [
  ".gradle.kts",
  ".gradle",
  ".yml",
  ".yaml",
  ".properties",
  ".toml",
  ".xml",
] as const

export function normalizePath(filePath: string): string {
  return filePath.replace(/\\/g, "/")
}

export function isExempt(filePath: string): boolean {
  const normalized = normalizePath(filePath)
  for (const pattern of EXEMPT_PATTERNS) {
    if (normalized.includes(pattern)) return true
  }
  if (normalized.startsWith("docs/")) return true
  if (normalized.startsWith(".claude/")) return true
  if (normalized.startsWith(".opencode/")) return true
  for (const ext of EXEMPT_EXTENSIONS) {
    if (normalized.endsWith(ext)) return true
  }
  return false
}

export function isSrcPath(filePath: string): boolean {
  return /(^|\/)src\//.test(normalizePath(filePath))
}

export function shouldBlockSrcPath(filePath: string): boolean {
  const normalized = normalizePath(filePath)
  if (isExempt(normalized)) return false
  return isSrcPath(normalized)
}
