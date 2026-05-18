export const WikiNudgePlugin = async ({ project, client, $, directory, worktree }) => {
  return {
    event: async ({ event }) => {
      if (event.type !== "session.idle") {
        return
      }
      const shouldNudge = Math.random() < 0.33
      if (!shouldNudge) {
        return
      }
      await client.app.log({
        body: {
          service: "wiki-nudge",
          level: "info",
          message: "Evaluar si esta sesión generó conocimiento que deba compilarse al wiki",
          extra: {
            prompt: `[wiki-nudge] Evaluate if this conversation generated knowledge worth compiling to the wiki (.opencode/wiki/). Processes, architecture, reusable patterns, gotchas, or tooling discoveries belong in the wiki. Check the 'LLM Wiki' section in AGENTS.md for rules.`,
          },
        },
      })
    },
  }
}