import { readFileSync, writeFileSync, existsSync } from "fs"

function detectLanguage(code) {
  const s = code.trim()

  if (/^\s*[{\[]/.test(s)) {
    try {
      JSON.parse(s)
      return "json"
    } catch {}
  }

  if (
    /\b(fun\s+\w+|val\s+\w+|var\s+\w+|data\s+class|sealed\s+interface|object\s+\w+)/.test(
      s
    )
  ) {
    return "kotlin"
  }

  if (/^\s*def\s+\w+\s*\(/m.test(s) || /^\s*(import|from)\s+\w+/m.test(s)) {
    return "python"
  }

  if (
    /\b(function\s+\w+\s*\(|const\s+\w+\s*=)/.test(s) ||
    /=>|console\.(log|error)/.test(s)
  ) {
    return "javascript"
  }

  if (
    /^#!.*\b(bash|sh)\b/m.test(s) ||
    /\b(if|then|fi|for|in|do|done)\b/.test(s)
  ) {
    return "bash"
  }

  if (/\b(SELECT|INSERT|UPDATE|DELETE|CREATE)\s+/i.test(s)) {
    return "sql"
  }

  if (/^\w+:\s+/m.test(s) && !/[{};]/.test(s)) {
    return "yaml"
  }

  return "text"
}

function formatMarkdown(content) {
  content = content.replace(
    /^([ \t]{0,3})```([ \t]*)\n([\s\S]*?)\n(\1```)\s*$/gm,
    (match, indent, info, body, closing) => {
      if (!info.trim()) {
        const lang = detectLanguage(body)
        return `${indent}\`\`\`${lang}\n${body}\n${closing}\n`
      }
      return match
    }
  )

  content = content.replace(/\n{3,}/g, "\n\n")

  return content.trimEnd() + "\n"
}

export const MarkdownFormatter = async () => {
  return {
    event: async (input) => {
      if (input.event.type !== "file.edited") return

      const filePath = input.event.properties.file
      if (!filePath.endsWith(".md") && !filePath.endsWith(".mdx")) return
      if (!existsSync(filePath)) return

      const content = readFileSync(filePath, "utf-8")
      const formatted = formatMarkdown(content)

      if (formatted !== content) {
        writeFileSync(filePath, formatted, "utf-8")
      }
    },
  }
}
