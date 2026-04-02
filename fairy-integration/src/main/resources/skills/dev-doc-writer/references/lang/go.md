# Go 语言规范

本文件定义 Go 项目文档的代码块规范与分层惯例。

---

## 代码块规范

Go 没有类，文档的核心展示单元是**结构体（struct）**、**接口（interface）**和**导出函数**。

### 保留内容
- 结构体声明（含字段名、类型、struct tag）
- 接口声明（完整保留，接口是 Go 文档最重要的契约）
- 导出函数签名（首字母大写的函数，含参数类型和返回类型）
- 导出方法的签名（接收者 + 方法名 + 参数 + 返回类型）
- 类型别名和类型定义（`type AgentState int`、`type HandlerFunc func(...)`）
- 常量和枚举块（`const (...)` 块）

### 删除内容
- 函数/方法体（用空体 `{}` 或注释 `// ...` 替代）
- `import` 块
- 未导出的函数（小写开头），可用注释说明存在私有辅助逻辑
- 错误检查的内部细节（`if err != nil { ... }`）

### 字段注释风格
Go 惯用行末注释，用 `//` 引出：
```go
type AgentConfig struct {
    DefaultModel    string        // 默认模型名称，未指定时使用
    MaxIterations   int           // ReAct 最大循环次数，默认 10
    StreamThinking  bool          // 是否向客户端推送推理过程
    MaxConcurrent   int           // 最大并发请求数（Semaphore 许可数）
    AcquireTimeout  time.Duration // 等待获取并发许可的超时时间
}
```

### 接口写法（Go 文档核心）
```go
// AgentModelFactory 定义模型工厂的统一契约，所有提供商工厂均需实现此接口。
type AgentModelFactory interface {
    Supports(provider string) bool
    ProviderName() string
    CreateAgent(cfg ModelConfig, tools []ToolCallback) (*ReactAgent, error)
}
```

### 结构体 + 方法写法
```go
type AgentModelManager struct {
    factories map[string]AgentModelFactory // 以 provider 名称为 key 的工厂注册表
    models    map[string]ModelConfig        // 模型配置缓存，支持并发读
    mu        sync.RWMutex                  // 保护 models 的读写锁
}

func NewAgentModelManager(factories []AgentModelFactory) *AgentModelManager
func (m *AgentModelManager) CreateAgent(modelName string, tools []ToolCallback) (*ReactAgent, error)
func (m *AgentModelManager) Reload(configJSON string) error
```

### 包级函数写法
```go
func NewAgentHandler(agent *ReactAgent, cfg *AgentConfig) *AgentHandler
func ResolveMaxIterations(requested, globalMax int) int
```

---

## 分层惯例

Go 项目通常按**包（package）**组织，而非按层次目录，但有约定俗成的结构：

### Web 服务（标准 Go HTTP / Gin / Echo / Fiber）

| 层名 | 典型包/目录 | 职责说明 |
|---|---|---|
| 类型层 | `types/`、`model/`、`dto/` | 数据结构定义（请求/响应/领域模型） |
| 配置层 | `config/` | 配置加载与验证 |
| 仓储层 | `repository/`、`store/` | 数据库操作封装 |
| 服务层 | `service/` | 业务逻辑实现 |
| 处理器层 | `handler/`、`controller/` | HTTP 请求处理，调用服务层 |
| 中间件层 | `middleware/` | 认证、日志、限流等横切逻辑 |

### 通用库 / SDK

| 层名 | 典型包 | 职责说明 |
|---|---|---|
| 接口层 | `interfaces.go`、`contract.go` 或包顶层 | 公共接口定义 |
| 核心层 | `core/`、`engine/`、`internal/` | 核心实现（`internal` 表示包内私有） |
| 工厂层 | `factory.go`、`registry.go` | 对象创建与注册 |
| 工具层 | `utils/`、`helper/` | 纯函数工具集 |

---

## 模块结构注释风格

Go 以包为单位，展示包路径结构：
```
agent/
├── agent.go          # 包入口，公共类型定义（AgentConfig、AgentState）
├── factory.go        # AgentModelFactory 接口 + 各 provider 实现
├── manager.go        # AgentModelManager，模型配置缓存与路由
├── handler.go        # AgentHandler，推理驱动与 SSE 事件推送
├── service.go        # AgentService，业务编排入口
└── internal/
    └── concurrency.go # 并发控制（Semaphore 实现，包内私有）
```

---

## 特殊说明

- **接口必须完整展示**：Go 的鸭子类型依赖接口，接口定义是文档中最重要的契约，一行都不能省略
- **error 返回值**：方法签名中保留 `error` 返回值（如 `func (m *X) Do() error`），它是 Go 错误处理的核心约定
- **指针接收者 vs 值接收者**：保留 `*X` 或 `X` 的区别，这表达了是否修改状态
- **Context 参数**：若方法有 `ctx context.Context` 参数，保留，说明它支持超时/取消
- **goroutine 并发**：在职责说明中明确标注"并发安全"或"非并发安全"
- **`internal` 包**：标注"包内私有，外部不可直接引用"
- **init 函数**：若包有 `init()`，在模块结构中注明 `# 包初始化逻辑`