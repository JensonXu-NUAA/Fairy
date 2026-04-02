---
name: arxiv-watcher
description: Search and summarize papers from ArXiv. Use when the user asks for the latest research, specific topics on ArXiv, or a daily summary of AI papers.
---

# ArXiv Watcher

This skill interacts with the ArXiv API to find and summarize the latest research papers.

## Capabilities

- **Search**: Find papers by keyword, author, or category.
- **Summarize**: Fetch the abstract and provide a concise summary.
- **Deep Dive**: Use `getPaperById` on the ArXiv ID to extract more details if requested.

## Language

Detect the language of the user's input and respond entirely in that language. If the user writes in Chinese, respond in Chinese. If the user writes in English or Spanish, respond in that language accordingly.

Note: tool call parameters (query, authorName, etc.) should always be passed in English regardless of the user's language, as ArXiv search works best with English keywords.

## Workflow

1. Choose the appropriate tool based on the user's intent:
   - For **latest papers** on a topic: call `searchLatestPapers(query, category, maxResults)` — results are sorted by submission date (newest first).
   - For **most relevant papers** on a topic: call `searchPapers(query, category, maxResults)` — results are sorted by relevance.
   - For **papers by a specific author**: call `searchPapersByAuthor(authorName, maxResults)`.
   - For **a specific paper by ID**: call `getPaperById(arxivId)`.
2. Present the findings to the user.

## Examples

- "Busca los últimos papers sobre LLM reasoning en ArXiv."
- "Dime de qué trata el paper con ID 2512.08769."
- "Hazme un resumen de las novedades de hoy en ArXiv sobre agentes."
- "帮我搜索一下最近关于 Agent 记忆管理的论文，要最新的。"

## Resources

- `searchLatestPapers(query, category, maxResults)`: Search papers sorted by submission date. Use for "latest" or "recent" queries.
- `searchPapers(query, category, maxResults)`: Search papers sorted by relevance. category options: `cs.AI`, `cs.CL`, `cs.LG`, `cs.DB`, or leave empty for all.
- `searchPapersByAuthor(authorName, maxResults)`: Search papers by a specific author.
- `getPaperById(arxivId)`: Get full details of a paper by its ArXiv ID.
