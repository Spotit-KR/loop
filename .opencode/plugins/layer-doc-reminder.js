import { existsSync } from "fs"
import { resolve } from "path"

const LAYER_DOCS = {
  domain: "docs/layers/domain.md",
  application: "docs/layers/application.md",
  infrastructure: "docs/layers/infrastructure.md",
  presentation: "docs/layers/presentation.md",
}

const SECURITY_DOC = "docs/spring-security-7.md"

function detectLayer(filePath) {
  for (const layer of Object.keys(LAYER_DOCS)) {
    if (new RegExp(`(^|/)${layer}(/|$)`).test(filePath)) return layer
  }
  return null
}

function isSecurityRelated(filePath) {
  return (
    /(^|\/)common\/config(\/|$)/.test(filePath) &&
    filePath.toLowerCase().includes("security")
  )
}

function isDocsPath(filePath) {
  return /(^|\/)docs\//.test(filePath)
}

function isTestPath(filePath) {
  return /(^|\/)src\/test\//.test(filePath) || filePath.endsWith("Test.kt")
}

function extractPathsFromPatchText(patchText) {
  const paths = []
  const regex = /^\*\*\* (?:Add|Update|Delete) File: (.+)$/gm
  let match
  while ((match = regex.exec(patchText)) !== null) {
    paths.push(match[1].trim())
  }
  return paths
}

function collectFilePaths(input, output) {
  const directPath =
    output.args?.file_path || output.args?.filePath || output.args?.path || ""
  const paths = []

  if (directPath) paths.push(directPath)

  if (input.tool === "apply_patch" && typeof output.args?.patchText === "string") {
    paths.push(...extractPathsFromPatchText(output.args.patchText))
  }

  return paths
}

export const LayerDocReminder = async ({ directory }) => {
  const reminded = new Set()

  return {
    "tool.execute.before": async (input, output) => {
      const filePaths = collectFilePaths(input, output)
      if (filePaths.length === 0) return

      const messages = []

      for (const filePath of filePaths) {
        if (isDocsPath(filePath)) continue
        if (isTestPath(filePath)) continue

        const layer = detectLayer(filePath)
        if (layer && !reminded.has(layer)) {
          reminded.add(layer)
          const doc = LAYER_DOCS[layer]
          if (existsSync(resolve(directory, doc))) {
            messages.push(
              `[${layer}] 이 레이어 첫 수정입니다. 먼저 ${doc} 를 읽으세요.`
            )
          }
        }

        if (isSecurityRelated(filePath) && !reminded.has("security")) {
          reminded.add("security")
          messages.push(
            `[security] Security 설정 첫 수정입니다. 먼저 ${SECURITY_DOC} 를 읽으세요.`
          )
        }
      }

      if (messages.length > 0) {
        throw new Error(messages.join("\n"))
      }
    },
  }
}
