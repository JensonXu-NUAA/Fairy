<div align="center">

# ✨ Fairy

**基于 Spring AI 的智能 Agent 应用框架**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-blue.svg)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

</div>

---

## 📖 项目简介

Fairy 是一个基于 **Spring Boot 3.4** 和 **Spring AI 1.1.2** 构建的智能 AI 应用后端框架，围绕 **ReAct Agent** 为核心，集成了多模型聊天、智能记忆管理、RAG 文档处理、Skill 技能市场与文件管理等完整能力。项目采用分层多模块架构，通过 Nacos 实现模型配置的动态热更新，支持 DashScope（通义千问）、DeepSeek、智谱 AI 等多个大模型提供商，并内置 MCP 协议支持，可灵活扩展 Agent 工具集。

---

## ✨ 核心特性

### 🤖 多模型 LLM 集成
- 支持 **DashScope（通义千问）**、**DeepSeek**、**智谱 AI（GLM）**、**OpenAI 兼容接口** 等多个提供商
- Nacos 配置中心驱动的模型动态热更新，无需重启服务
- 独立的 Chat / Agent 双工厂体系，职责清晰、互不干扰
- 用户级自定义模型配置（API Key、Base URL、参数），支持多租户场景

### 🧠 ReAct Agent 框架
- 基于 Spring AI Alibaba `ReactAgent` 构建，支持 **Think → Act → Observe** 推理循环
- 内置工具调用（Tool Calling）与 **MCP 协议** 支持，可动态注册外部工具服务
- 流式推送 Agent 推理全过程：思考链（Thinking）、工具调用、最终答案
- 基于 Semaphore 的并发控制，防止资源耗尽；429 限流事件客户端可感知
- Java 21 虚拟线程驱动，高并发低开销

### 💾 智能双层记忆管理
- **短期记忆**：Redis 滑动窗口 + MySQL 持久化双写，支持服务重启后历史回填与 Redis 故障自动恢复
- **长期记忆**：LLM 驱动的自动摘要提炼，注入 System Prompt 实现跨会话用户画像
- **混合触发机制**：滑动窗口淘汰 + 会话结束双触发，确保记忆不丢失
- **RocketMQ 异步摘要**：基于消息队列的异步记忆提取，不阻塞主对话流程
- **上下文压缩**：当历史消息超出窗口时自动压缩，保持关键信息不丢失

### 📄 RAG 文档处理
- 支持 PDF、Office（Word/Excel/PPT）等多格式文档解析（Tika + PDFBox + POI）
- PDF 结构化解析：逐页提取文本与嵌入图片，支持图文混合提取
- 多种分块策略：纯文本分块（Spring AI TokenTextSplitter 适配）、图文混合分块
- 媒体上下文附加：基于位置距离排序为图片/表格块注入近邻文本上下文
- 位置 5 元组透传（page, top, bottom, left, right），支持检索溯源与前端高亮
- Token 计数、分块元数据管理与统计汇总

### 🛠️ Skill 技能市场
- 内置 Skill 技能体系：ArXiv 论文搜索、Commit Message 生成、开发文档生成等
- **Skill Hub** 平台：用户可发布、更新、删除、浏览、下载自定义 Skill
- 支持公开 / 私有可见性控制，分页浏览与标签筛选
- Skill 自动注册为 Agent 工具，Agent 可根据用户意图动态调用

### 📁 文件管理
- 基于 **MinIO** 的对象存储，支持上传、下载、删除
- 大文件**分片上传**：初始化 → 分片上传 → 状态查询 → 合并
- 基于 **RocketMQ** 的异步文件处理

### 🔄 全链路流式响应
- 全链路 SSE（Server-Sent Events）流式输出
- Chat 与 Agent 独立的 SSE 事件体系
- 丰富的 SSE 事件类型覆盖完整的推理生命周期

---

## 🏛️ 技术架构

### 核心技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| **基础框架** | Spring Boot | 3.4.1 |
| **AI 框架** | Spring AI | 1.1.2 |
| **AI 扩展** | Spring AI Alibaba | 1.1.2.0 |
| **微服务** | Spring Cloud | 2024.0.1 |
| **微服务扩展** | Spring Cloud Alibaba | 2025.0.0.0 |
| **ORM** | MyBatis Plus | 3.5.5 |
| **数据库** | MySQL | 8.3.0 |
| **缓存** | Redis + Redisson | 3.45.0 |
| **配置中心** | Nacos | 3.1.1 |
| **对象存储** | MinIO | 8.5.7 |
| **消息队列** | RocketMQ | 2.3.1 |
| **本地缓存** | Caffeine | 3.1.8 |
| **文档解析** | Apache Tika / PDFBox / POI | 2.9.1 / 3.0.1 / 5.2.5 |
| **JDK** | Java | 21 |

### 模块结构

```
fairy/
├── fairy-web/                  # Web 层 — REST API 接口（Controller）
├── fairy-service/              # 业务层 — 核心业务编排
│   ├── agent/                  #   Agent 服务（对话、会话查询、模型配置管理）
│   ├── chat/                   #   Chat 服务
│   ├── file/                   #   文件服务（上传下载、分片上传、MQ 消费）
│   ├── rag/                    #   RAG 服务（文档分块编排）
│   └── skill/                  #   Skill Hub 服务（技能市场 CRUD）
├── fairy-integration/          # 集成层 — 外部系统集成
│   ├── agent/                  #   Agent 模块
│   │   ├── handler/            #     推理执行与并发控制
│   │   ├── memory/             #     双层记忆（短期 Redis + 长期 MySQL + MQ 异步摘要）
│   │   │   ├── hook/           #       记忆拦截器（AfterHook、长期记忆注入）
│   │   │   └── mq/             #       RocketMQ 记忆消息生产者/消费者
│   │   └── model/              #     模型工厂与管理（Nacos 配置监听、工厂路由）
│   │       ├── factory/        #       按提供商创建 ReactAgent（DashScope/DeepSeek/ZhipuAI/OpenAI）
│   │       └── manager/        #       模型配置管理（Nacos / Native / Custom）
│   ├── chat/                   #   Chat 模块（模型工厂、聊天处理、记忆 Advisor）
│   └── service/
│       ├── mcp/                #   MCP 客户端管理（外部工具服务连接池）
│       ├── rag/                #   RAG 分块策略（纯文本 / 图文混合 / 上下文附加）
│       ├── skill/              #   Native Skill 注册（内置技能扫描与加载）
│       └── tools/              #   Agent 工具集（MCP 注册、论文搜索、时间服务、Skill 工具）
├── fairy-common/               # 公共层 — 数据模型、仓库、解析器、中间件
│   ├── data/                   #   DTO / VO / Request / Response 数据载体
│   │   ├── llm/                #     LLM 相关（ChatDTO、ModelConfig、Agent 请求/响应）
│   │   ├── rag/                #     RAG 相关（ChunkResult、EnhancedDocumentChunk、位置信息）
│   │   ├── skill/              #     Skill 相关（SkillHubFormDTO、SkillHubVO）
│   │   └── file/               #     文件相关（上传/下载请求与响应）
│   ├── repository/             #   数据仓库（MySQL、Redis、MinIO、Caffeine）
│   ├── parser/                 #   文档/图片解析器（PDF 结构化解析、Tika 通用解析）
│   ├── file/                   #   文件处理抽象（FileType、FileProcessor、FileProcessResult）
│   └── rocketmq/               #   RocketMQ 基础设施（AbstractProducer / AbstractConsumer）
└── docs/                       # 设计文档
```

### 架构总览

```
┌──────────────────────────────────────────────────────────────────────┐
│                         fairy-web (Controller)                       │
│  AgentController │ ChatController │ FileController │ SkillHubController │
└────────┬─────────┴───────┬────────┴───────┬────────┴───────┬────────┘
         │                 │                │                │
┌────────▼─────────────────▼────────────────▼────────────────▼────────┐
│                      fairy-service (业务编排)                        │
│  AgentService  │ ChatService │ FileService │ DocumentChunkService    │
│  SessionQuery  │ SkillHubService │ AgentModelConfigService           │
└────────┬────────────┬───────────┬────────────────────────────────────┘
         │            │           │
┌────────▼────────────▼───────────▼───────────────────────────────────┐
│                   fairy-integration (外部集成)                        │
│                                                                      │
│  ┌───────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  Agent 模块    │  │  Chat 模块    │  │  RAG / Tools / Skills    │  │
│  │ ┌───────────┐ │  │ ┌──────────┐ │  │ ┌──────────────────────┐ │  │
│  │ │ Model     │ │  │ │ Factory  │ │  │ │ DocumentParser       │ │  │
│  │ │ Factory   │ │  │ │ Manager  │ │  │ │ DocumentChunker      │ │  │
│  │ │ Manager   │ │  │ └──────────┘ │  │ │ MediaContextAttacher │ │  │
│  │ └───────────┘ │  │ ┌──────────┐ │  │ └──────────────────────┘ │  │
│  │ ┌───────────┐ │  │ │ Advisor  │ │  │ ┌──────────────────────┐ │  │
│  │ │ Handler   │ │  │ │ Memory   │ │  │ │ MCP Client Manager   │ │  │
│  │ │ Builder   │ │  │ └──────────┘ │  │ │ PaperSearchService   │ │  │
│  │ │ Memory    │ │  └──────────────┘  │ │ SkillToolService     │ │  │
│  │ └───────────┘ │                     │ │ TimeService          │ │  │
│  └───────────────┘                     └──────────────────────────┘  │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────────┐
│                      fairy-common (公共基础)                          │
│   DTO/VO  │  MySQL Repository  │  Redis  │  MinIO  │  RocketMQ      │
│   Parser  │  File Processor    │  Caffeine Cache                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 环境要求

- **JDK 21+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Nacos 2.0+**
- **MinIO**（可选，文件存储）
- **RocketMQ**（可选，异步文件处理与记忆提取）

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/jenson2525/fairy-backend.git
   cd fairy-backend
   ```

2. **配置数据库**
   ```sql
   CREATE DATABASE fairy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **配置 Nacos**
   - 启动 Nacos 服务器
   - 创建 Agent 模型配置 `fairy_agent_config`，分组 `FAIRY_LLM_GROUP`：
   ```json
   {
     "qwen3.5-plus": {
       "provider": "dashscope",
       "apiKey": "your-dashscope-api-key",
       "model": "qwen3.5-plus",
       "temperature": 0.7,
       "maxTokens": 4096
     },
     "deepseek-chat": {
       "provider": "deepseek",
       "apiKey": "your-deepseek-api-key",
       "model": "deepseek-chat",
       "temperature": 0.7
     },
     "glm-4-flash": {
       "provider": "zhipuai",
       "apiKey": "your-zhipuai-api-key",
       "model": "glm-4-flash",
       "temperature": 0.7
     }
   }
   ```

4. **修改应用配置**

   编辑 `fairy-web/src/main/resources/application.yml`，配置数据库、Redis、MinIO、Nacos、RocketMQ 等连接信息。

5. **构建并启动**
   ```bash
   ./mvnw clean install -DskipTests
   ./mvnw spring-boot:run -pl fairy-web
   ```

6. **验证服务**
   ```bash
   # 聊天接口测试
   curl -X POST http://localhost:12100/chat/stream \
     -H "Content-Type: application/json" \
     -d '{"userId": "test", "message": "你好", "modelName": "qwen-turbo"}'

   # Agent 接口测试
   curl -X POST http://localhost:12100/agent/chat \
     -H "Content-Type: application/json" \
     -d '{"userId": "test", "message": "帮我搜索最新的AI论文"}'
   ```

---

## 📖 API 文档

### 1. 聊天接口

#### `POST /chat/stream` — 流式聊天

**请求体**：
```json
{
  "userId": "user123",
  "conversationId": "conv456",
  "message": "你好，请介绍一下你自己",
  "modelName": "qwen-turbo"
}
```

**响应**：SSE 流式文本事件

---

### 2. Agent 接口

#### `POST /agent/chat` — Agent 流式对话

**请求体**：
```json
{
  "userId": "user123",
  "message": "帮我搜索关于大语言模型的最新论文",
  "modelName": "qwen3.5-plus",
  "maxIterations": 10
}
```

**SSE 事件序列**：

| 事件类型 | 说明 |
|---------|------|
| `agent_start` | Agent 任务启动（含 `agentSessionId`） |
| `agent_thinking` | LLM 思考过程（Chain-of-Thought） |
| `agent_tool_call` | Agent 决定调用工具（含工具名和参数） |
| `agent_tool_result` | 工具执行结果返回 |
| `agent_answer` | 最终答案（流式分块推送） |
| `agent_end` | Agent 任务完成 |
| `agent_error` | 执行异常（含 429 限流提示） |
| `[DONE]` | 流结束标记 |

#### `GET /agent/sessions?userId=xxx` — 查询用户会话列表

返回用户所有 Agent 会话，按最后活跃时间倒序。

#### `GET /agent/sessions/{sessionId}/messages` — 查询会话消息记录

返回指定会话的完整消息记录，按消息顺序升序。

---

### 3. Agent 模型配置接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/agent/model/config` | POST | 新增用户自定义模型配置 |
| `/agent/model/config/{id}` | PUT | 更新模型配置 |
| `/agent/model/config/{id}` | DELETE | 删除模型配置 |
| `/agent/model/config/models?userId=xxx` | GET | 查询用户可用模型名称列表 |
| `/agent/model/config/config?userId=xxx` | GET | 查询用户完整配置列表 |

---

### 4. Skill Hub 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/skill/publish` | POST | 发布 Skill（上传 SKILL.md + 附件） |
| `/skill/{skillName}` | PUT | 更新 Skill |
| `/skill/{skillName}/visibility` | PATCH | 切换公开 / 私有 |
| `/skill/{skillName}?userId=xxx` | DELETE | 删除 Skill |
| `/skill/mine?userId=xxx` | GET | 查询我的 Skill 列表 |
| `/skill/mine/{skillName}?userId=xxx` | GET | 查询我的 Skill 详情 |
| `/skill/explore?pageNum=1&pageSize=20` | GET | 浏览公开 Skill（支持标签筛选） |
| `/skill/{authorId}/{skillName}` | GET | 查看公开 Skill 详情 |
| `/skill/{authorId}/{skillName}/download` | GET | 下载 Skill 包 |

---

### 5. 文件接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/file/upload` | POST | 上传文件（MultipartFile） |
| `/api/file/download?fileId=xxx` | GET | 下载文件 |
| `/api/file/delete/{fileId}` | DELETE | 删除文件 |

### 6. 分片上传接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chunk/init` | POST | 初始化分片上传（MD5 秒传支持） |
| `/api/chunk/upload` | POST | 上传单个分片 |
| `/api/chunk/status` | GET | 查询分片上传状态 |
| `/api/chunk/merge` | POST | 合并所有分片 |

---

## 🧠 Agent 记忆系统

Fairy Agent 内置了完整的双层记忆架构，通过 `AgentMemoryManager` 统一编排：

```
                    ┌─────────────────────────────────┐
                    │       AgentMemoryManager         │
                    │      （统一编排入口）               │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    ▼                              ▼
       ┌────────────────────┐         ┌─────────────────────┐
       │   短期记忆          │         │   长期记忆            │
       │  AgentShortTerm    │         │  AgentLongTerm       │
       │  AgentMemory       │         │  AgentMemory         │
       ├────────────────────┤         ├─────────────────────┤
       │ • Redis 滑动窗口    │         │ • MySQL 持久化       │
       │ • MySQL 双写兜底    │         │ • LLM 摘要提炼       │
       │ • 服务重启历史回填   │         │ • System Prompt 注入 │
       │ • 可配置窗口/TTL    │         │ • 重要性评分去重      │
       └────────────────────┘         └─────────────────────┘
                    │                              ▲
                    │  RocketMQ 异步摘要提取         │
                    ├──── 滑动窗口淘汰消息 ──────────┤
                    └──── 会话结束剩余消息 ──────────┘
```

### 记忆处理链路

```
请求到达 → 加载短期记忆（Redis/MySQL fallback） → 回填 MemorySaver
         → 加载长期记忆（MySQL） → 注入 System Prompt
         → Agent 推理执行 → SSE 流式推送
         → 新消息写入短期记忆（Redis + MySQL 双写）
         → 窗口淘汰/会话结束 → RocketMQ 发送记忆提取消息
         → Consumer 异步调用摘要模型 → 结果写入长期记忆（MySQL upsert）
```

---

## 🛠️ Skill 体系与内置工具

### 内置 Skill

| Skill | 说明 | 触发场景 |
|-------|------|---------|
| `arxiv-watcher` | ArXiv 论文搜索与摘要 | 搜索论文、获取最新研究 |
| `commit-message-writer` | Conventional Commits 格式的提交信息生成 | 写 commit、生成提交说明 |
| `dev-doc-writer` | 多语言开发技术文档生成 | 写文档、模块说明、架构文档 |

### Agent 工具集

| 工具 | 说明 |
|------|------|
| **ArXiv 搜索** | 按关键词/作者/类别搜索论文，支持按日期或相关性排序 |
| **时间服务** | 获取当前时间、时区转换 |
| **Skill 工具** | 动态加载已注册 Skill 作为 Agent 可调用工具 |
| **MCP 工具** | 通过 MCP 协议连接外部工具服务，动态注册 |

---

## ⚙️ 配置说明

核心配置位于 `fairy-web/src/main/resources/application.yml`：

```yaml
fairy:
  agent:
    default-model: qwen3.5-plus       # Agent 默认模型
    max-iterations: 10                 # ReAct 最大迭代次数
    stream-thinking: true              # 是否推送思考过程
    max-history-size: 10               # 历史消息保留轮次
    memory:
      short-term:
        max-messages: 20               # 短期记忆滑动窗口大小
        ttl-hours: 24                  # Redis key 过期时间
      long-term:
        summarize-threshold: 20        # 摘要触发阈值
        max-facts-per-user: 50         # 每用户最大记忆条数
        model-name: qwen3.5-flash      # 摘要专用模型（轻量级）
    concurrency:
      max-concurrent: 20               # 最大并发 Agent 请求数
      acquire-timeout-ms: 5000         # 等待超时时间
      permit-ttl-ms: 60000             # 单个许可持有 TTL（防泄漏）
```

---

## 📁 设计文档

| 文档 | 说明 |
|------|------|
| [Agent 架构设计方案](docs/Agent-架构设计方案.md) | Agent 模块完整架构、数据流、各组件设计细节 |
| [Agent 记忆管理设计方案](docs/Agent-记忆管理设计方案.md) | 双层记忆系统设计、摘要机制、持久化策略 |
| [RAG 开发文档](docs/RAG-开发文档.md) | RAG 文档处理与分块策略开发指南 |
| [RAG 测试文档](docs/RAG-测试文档.md) | RAG 功能测试方案与结果 |
| [PDFBox 结构化解析接入说明](docs/PDFBox-结构化解析接入说明.md) | PDF 结构化解析、图文提取、位置透传实现细节 |

---

## 🛠️ 扩展指南

### 添加新的 AI 模型提供商

1. 在 Nacos 配置中添加新模型定义（指定 `provider` 字段）
2. 在 `fairy-integration` 的 `agent/model/factory/` 下新建工厂类：

```java
@Slf4j
@Component
public class NewProviderAgentModelFactory extends BaseAgentModelFactory {

    @Override
    public boolean supports(String provider) {
        return "new-provider".equals(provider);
    }

    @Override
    public String getProviderName() {
        return "new-provider";
    }

    @Override
    public ReactAgent createAgent(ModelConfig modelConfig,
                                   ToolCallback[] tools,
                                   BaseCheckpointSaver saver) {
        // 构建 ChatModel → ReactAgent
    }
}
```

3. 重启应用或等待 Nacos 配置自动刷新

### 添加新的 Agent 工具

实现 Spring AI 的 `ToolCallbackProvider` 或使用 `@Tool` 注解，工具将自动注册到 Agent 的工具集中。

### 添加新的 Skill

1. 在 `fairy-integration/src/main/resources/skills/` 下创建 Skill 目录
2. 编写 `SKILL.md`，包含 YAML front matter（name、description）和技能指令
3. 可附加 `references/` 目录存放参考文件（如语言规范、写作风格指南等）
4. Skill 会通过 `NativeSkillRegistry` 自动扫描并注册为 Agent 可用工具

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

## 👥 作者

**Jenson Xu** — *项目维护者* — [GitHub](https://github.com/jenson2525)

## 🙏 致谢

- [Spring AI](https://spring.io/projects/spring-ai) — AI 应用开发框架
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba) — 阿里云 AI 扩展及 Agent 框架
- [Nacos](https://nacos.io/) — 配置中心和服务发现
- 所有为开源社区做出贡献的开发者们

---

<div align="center">

如果这个项目对您有帮助，请给我们一个 ⭐️ Star！

</div>