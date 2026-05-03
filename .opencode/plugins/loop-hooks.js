import { dirname, resolve } from "node:path"
import { fileURLToPath } from "node:url"

const HERE = dirname(fileURLToPath(import.meta.url))
const CORE_DIR = resolve(HERE, "..", "..", "hooks", "core")

const TOOL_MAP = {
  bash: "Bash",
  edit: "Edit",
  write: "Write",
  read: "Read",
  apply_patch: "apply_patch",
  task_create: "TaskCreate",
  task_update: "TaskUpdate",
}

function mapTool(name) {
  return TOOL_MAP[name] ?? name
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

function adaptArgs(argsSource = {}) {
  const args = { ...argsSource }
  if (args.filePath && !args.file_path) args.file_path = args.filePath
  return args
}

async function loadCore(name) {
  return await import(resolve(CORE_DIR, `${name}.ts`))
}

async function runHooks(coreNames, ctx) {
  for (const name of coreNames) {
    const mod = await loadCore(name)
    const result = await mod.check(ctx)
    if (result.kind === "block") throw new Error(result.reason)
    if (result.kind === "modify") {
      Object.assign(ctx.args, result.updatedInput)
    }
  }
}

export const LoopHooks = async ({ directory }) => {
  return {
    "tool.execute.before": async (input, output) => {
      const tool = mapTool(input.tool)
      const args = adaptArgs(output.args)
      const ctx = { tool, args, cwd: directory }

      if (tool === "Bash") {
        await runHooks(["block-dangerous", "work-plan-enforcer", "plan-completion-guard"], ctx)
        return
      }

      if (tool === "Edit" || tool === "Write" || tool === "apply_patch") {
        if (input.tool === "apply_patch" && typeof output.args?.patchText === "string") {
          ctx.args.patchText = output.args.patchText
          for (const path of extractPathsFromPatchText(output.args.patchText)) {
            const sub = { ...ctx, args: { ...ctx.args, file_path: path } }
            await runHooks(["layer-doc-reminder", "work-plan-enforcer"], sub)
          }
          return
        }
        await runHooks(["layer-doc-reminder", "work-plan-enforcer"], ctx)
        return
      }
    },

    "tool.execute.after": async (input, output) => {
      const tool = mapTool(input.tool)
      if (tool !== "TaskCreate" && tool !== "TaskUpdate") return
      const args = adaptArgs(input.args)
      const ctx = { tool, args, cwd: directory }
      await runHooks(["plan-update-reminder"], ctx)
    },

    event: async ({ event }) => {
      if (event.type !== "todo.updated") return
      const todo = event.properties ?? {}
      const status = todo.status
      if (status !== "pending" && status !== "completed") return
      const ctx = {
        tool: status === "completed" ? "TaskUpdate" : "TaskCreate",
        args: {
          status,
          subject: todo.content,
          content: todo.content,
        },
        cwd: directory,
      }
      await runHooks(["plan-update-reminder"], ctx)
    },
  }
}
