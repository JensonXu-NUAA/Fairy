---
name: commit-message-writer
description: 根据用户提供的代码变更内容、变更描述或 git diff，生成符合约定式提交规范（Conventional Commits）
  的 git commit message。当用户需要写提交信息、生成 commit、写 git 记录时使用此技能。
  触发关键词：写commit、生成提交信息、commit message、提交说明、git提交、写提交记录。
---

# Commit Message 生成器

根据变更内容生成符合 Conventional Commits 规范的 git commit message。

---

## 第一步：理解变更内容

读取用户提供的内容，判断变更类型：

- 若提供了 **git diff 或代码片段**，从中提取改动范围和改动目的
- 若提供了**自然语言描述**，直接提取变更意图
- 若两者均有，以自然语言描述的意图为准，用代码变更补充 scope

---

## 第二步：确定提交类型

从以下类型中选择**最贴合**的一个：

| type | 适用场景 |
|---|---|
| `feat` | 新增功能或能力 |
| `fix` | 修复 bug |
| `refactor` | 代码重构，不改变功能也不修 bug |
| `docs` | 仅文档变更 |
| `test` | 新增或修改测试 |
| `chore` | 构建配置、依赖更新、脚手架等非业务变更 |
| `perf` | 性能优化 |
| `style` | 格式调整，不影响逻辑（空格、缩进等） |
| `ci` | CI/CD 流程变更 |

若变更同时涉及多个类型，选择**主要目的**对应的类型。

---

## 第三步：生成 commit message

### 格式规范

```
type(scope): description

[可选 body]

[可选 footer]
```

**Header 规则（必须遵守）**：
- `scope` 为可选，使用变更所在的模块名、文件名或功能域，用小写字母和连字符
- `description` 使用**祈使句**，**首字母小写**，**结尾不加句号**
- Header 总长度不超过 **72 个字符**
- 中文描述同样遵守祈使句结构（如：`添加`、`修复`、`重构`，而非`添加了`、`修复了`）

**Body 规则（可选，用于复杂变更）**：
- 与 header 空一行
- 说明**为什么**这样改，而非**改了什么**
- 每行不超过 72 个字符

**Footer 规则（可选）**：
- 关联 issue：`Closes #123`
- 破坏性变更：`BREAKING CHANGE: 描述影响`

---

## 输出格式要求

- 直接输出 commit message，用代码块包裹
- 若对变更意图有歧义，先给出**一个最可能的版本**，再提供 1-2 个备选
- 不输出任何解释性前缀（如"以下是生成的 commit message："），直接给内容
- 若用户未提供足够信息，询问：变更的**目的**是什么

---

## 示例

用户输入：在 AgentClientBuilder 中加入了 SkillsAgentHook 的构建逻辑

输出：
```
feat(agent): integrate SkillsAgentHook into ReactAgent build pipeline
```

用户输入：修了一个 Redis 缓存 key 拼错的 bug

输出：
```
fix(cache): correct Redis key naming for agent short-term memory
```
