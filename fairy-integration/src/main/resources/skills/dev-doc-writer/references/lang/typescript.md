# TypeScript / JavaScript 语言规范

本文件定义 TypeScript 和 JavaScript 项目文档的代码块规范与分层惯例。
JavaScript 项目若无类型注解，遵循同样的结构规范，省略类型部分。

---

## 代码块规范

### 保留内容
- 类声明行（含 `implements`、`extends`，泛型参数）
- 属性声明（含访问修饰符 `public`/`private`/`protected`/`readonly`、类型注解）
- 构造函数签名（`constructor(...)` 含参数类型，省略函数体）
- 公共方法签名（`public` 方法或无修饰符方法，含参数类型和返回类型，用 `;` 或 `{}` 结束，不写方法体）
- 接口 `interface` 和类型别名 `type` 的完整定义（这是 TypeScript 文档的核心）
- 关键装饰器：`@Injectable()`、`@Controller()`、`@Get/Post/...`（NestJS）、`@Component`、`@Prop`（Vue）、`@Entity`（TypeORM）
- `export` 关键字（保留，说明公开性）
- 枚举 `enum` 的完整定义

### 删除内容
- 方法体（用 `{}` 空体替代，或在签名后直接加 `;`）
- `import` 语句（除非需要说明关键外部依赖）
- 私有辅助方法（`private` 方法），可用 `// 私有方法：xxx` 注释代替
- 类型断言（`as Type`）和非空断言（`!`）内部细节

### 字段注释风格
注释跟在字段行末，用 `//` 引出：
```typescript
private readonly modelCache = new Map<string, ModelConfig>(); // 模型配置缓存，按名称索引
private maxConcurrent: number = 20;                           // 最大并发请求数
```

### 接口/类型写法（TypeScript 核心）
接口和类型定义必须完整保留，这是 TypeScript 文档最重要的内容：
```typescript
export interface AgentChatRequest {
  userId: string;                    // 用户 ID
  message: string;                   // 本轮用户输入
  sessionId?: string;               // 会话 ID，首次请求可不传
  modelName?: string;               // 指定模型名称
  maxIterations?: number;           // 最大推理循环次数，不传则使用默认值
}

export type AgentEventType =
  | 'agent_start'
  | 'agent_thinking'
  | 'agent_tool_call'
  | 'agent_answer'
  | 'agent_end'
  | 'agent_error';
```

### 类写法
```typescript
@Injectable()
export class AgentHandler {
  constructor(
    private readonly agent: ReactAgent,
    private readonly config: AgentConfig,
  ) {}

  async run(message: string): Promise<void>;
  private async handleNodeOutput(output: NodeOutput): Promise<void>;
}
```

### 函数式组件（无类场景）
当模块以函数为主要组织单元时，展示函数签名而不是类：
```typescript
export async function createAgent(
  modelName: string,
  tools: ToolCallback[],
): Promise<ReactAgent>;

export function resolveMaxIterations(
  requested: number | undefined,
  globalMax: number,
): number;
```

---

## 分层惯例

### NestJS（后端框架）

| 层名 | 典型装饰器/目录 | 职责说明 |
|---|---|---|
| 类型/DTO 层 | `*.dto.ts`、`*.interface.ts` | 请求/响应数据结构（class-validator 装饰） |
| 模块层 | `*.module.ts`、`@Module()` | 依赖注入容器配置 |
| 配置层 | `*.config.ts`、`ConfigService` | 环境变量与配置项 |
| 仓储层 | `*.repository.ts`、`@InjectRepository` | 数据库操作封装 |
| 服务层 | `*.service.ts`、`@Injectable()` | 业务逻辑实现 |
| 控制器层 | `*.controller.ts`、`@Controller()` | HTTP 路由与请求处理 |
| 守卫/拦截器 | `*.guard.ts`、`*.interceptor.ts` | 横切关注点（认证、日志、转换） |

### React / Vue（前端框架）

| 层名 | 典型目录/文件 | 职责说明 |
|---|---|---|
| 类型层 | `types/`、`interfaces/` | 全局类型定义 |
| 状态层 | `store/`、`context/`、`composables/` | 全局状态管理（Zustand/Pinia/Redux） |
| 服务层 | `services/`、`api/` | HTTP 请求封装 |
| Hook 层 | `hooks/`（React）、`composables/`（Vue） | 可复用业务逻辑 |
| 组件层 | `components/` | UI 组件（无状态或轻状态） |
| 页面层 | `pages/`、`views/` | 路由级组件，组合多个组件 |

### 通用工具库 / Node.js 模块

| 层名 | 典型文件 | 职责说明 |
|---|---|---|
| 类型层 | `types.ts`、`interfaces.ts` | 公共类型和接口 |
| 配置层 | `config.ts` | 配置项定义与默认值 |
| 核心层 | `core/`、`engine/` | 核心算法逻辑 |
| 工厂层 | `factory.ts`、`registry.ts` | 对象创建与注册 |
| 工具层 | `utils/`、`helpers/` | 纯函数工具集 |
| 入口层 | `index.ts` | 公共 API 导出 |

---

## 模块结构注释风格

```
src/
└── agent/
    ├── index.ts               # 公共 API 导出入口
    ├── types.ts               # 接口与类型定义（AgentRequest、AgentEvent 等）
    ├── config.ts              # 全局配置与默认值
    ├── factory.ts             # 模型工厂（按 provider 创建 LLM 客户端）
    ├── manager.ts             # 模型管理（配置缓存）
    ├── handler.ts             # Agent 推理驱动与 SSE 事件推送
    └── service.ts             # 业务编排入口
```

---

## 特殊说明

- **`interface` 优先于 `type`**：在代码块中，interface 是公开契约，必须完整展示；type 别名若是公开 API 的一部分也需完整展示
- **泛型参数**：保留泛型参数（`<T extends BaseModel>`），这是接口契约的重要部分
- **`readonly`**：保留 `readonly` 修饰符，它表达了设计意图（不可变）
- **可选参数 `?`**：保留，说明字段的可选语义
- **JavaScript 项目**：若无 TypeScript，代码块展示函数签名（含 JSDoc 注释类型），层名和规范与 TypeScript 相同
- **`async`/`await`**：方法签名中保留 `async` 关键字，返回类型写 `Promise<X>`
- **箭头函数导出**：若模块用 `export const fn = () => {}` 风格，展示为 `export const fn: (params) => ReturnType`