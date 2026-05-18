---
name: wiki
description: "Manage the project's LLM Wiki — ingest new knowledge, search, lint for consistency, and show stats. The wiki lives at .opencode/wiki/ and is a persistent markdown knowledge base that compounds over time."
license: MIT
compatibility: opencode
metadata:
  workflow: knowledge-management
---

# Wiki Skill

Manage the **LLM Wiki** at `.opencode/wiki/` — a persistent, interlinked markdown knowledge base that opencode maintains.

## How the wiki works

The wiki is a **growing knowledge base compiled from daily work**. opencode reads it to answer questions faster (instead of re-deriving from raw sources each time) and writes to it when it discovers non-trivial knowledge.

Three core operations:
- **Ingest** — Process new sources, compile/update wiki pages
- **Query** — Search wiki, synthesize answer with claim type annotations
- **Lint** — Check for contradictions, orphans, stale info

---

## Commands

### `/wiki ingest [topic]`

Processes new content into the wiki.

**What it does:**
1. If raw files exist in `.opencode/wiki/raw/`, processes them first: reads each, extracts key info, creates/updates wiki pages, **deletes** the processed raw files
2. If a topic is specified (`/wiki ingest architecture`), searches the codebase for relevant info and compiles it into wiki pages
3. Updates `.opencode/wiki/index.md` (table of contents) and `.opencode/wiki/log.md` (changelog)

**When to use:** After resolving non-trivial issues, after dropping files into `raw/`.

### `/wiki search <query>`

Searches all wiki pages and synthesizes an answer.

**What it does:**
1. Greps across all `.md` files in `.opencode/wiki/`
2. Reads matching pages
3. Synthesizes a concise answer with claim type annotations

### `/wiki lint`

Health-check the wiki for consistency.

**What it checks:**
- **Broken links** — `[[path]]` references to non-existent pages
- **Orphan pages** — Pages not linked from any other page
- **Empty stubs** — Pages with minimal/no content
- **Stale pages** — Pages with `Last verified:` older than 30 days
- **Unverified claims** — Pages with many `[unverified]` tags

**When to use:** Periodically (e.g., weekly) or before important decisions.

### `/wiki status`

Show wiki statistics.

**Output:**
- Page count per category
- Last 5 changelog entries
- Number of `[unverified]` and `[gap]` claims
- Stale pages
- Pending items in `raw/`

---

## Page format rules

Every wiki page follows this structure:

```markdown
# Page Title

> **Last verified:** YYYY-MM-DD | **Verified by:** [source|analysis|gap]

Content here...
```

### Claim types

Every factual statement should be tagged:

| Tag | Meaning |
|---|---|
| `[source]` | Verified from code or docs |
| `[analysis]` | Conclusion from evidence |
| `[unverified]` | Assumed, not checked |
| `[gap]` | Known unknown |

### Cross-links

Use `[[relative/path/without-extension]]` to link between wiki pages. Example: `[[architecture/app-structure]]`.

### Diagrams

Use Mermaid for architecture/flow diagrams when helpful.

---

## Allowed tools

- Read, Write, Edit, Glob, Grep, Bash, Agent