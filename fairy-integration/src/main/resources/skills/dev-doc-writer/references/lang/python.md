# Python 语言规范

本文件定义 Python 项目文档的代码块规范与分层惯例。

---

## 代码块规范

### 保留内容
- 类声明行（含继承，如 `class AgentHandler(BaseHandler):`）
- 类变量和实例变量声明（`__init__` 中的 `self.xxx` 赋值行，带类型注解优先保留）
- 公共方法签名（含类型注解、默认参数，省略方法体，用 `...` 或 `pass` 占位）
- 关键装饰器：`@dataclass`、`@property`、`@classmethod`、`@staticmethod`、`@abstractmethod`、`@router.get/post/...`（FastAPI）、`@app.route`（Flask）、`@celery.task`、`@pytest.fixture`
- 类型注解（`TypeVar`、`Protocol`、`dataclass` 字段类型）
- `__init__` 方法签名（仅参数列表，不含方法体）

### 删除内容
- 方法体（用 `...` 替代）
- `import` 语句（除非需要说明关键依赖）
- 私有辅助方法（`_xxx` 开头），可用 `# 私有方法：xxx` 注释代替
- 类型忽略注释（`# type: ignore`）

### 字段注释风格
注释跟在行末，用 `#` 引出（Python 原生注释符）：
```python
self.model_cache: dict[str, ModelConfig] = {}  # 模型配置缓存，按名称索引
self.max_concurrent: int = 20                   # 最大并发请求数
```

### dataclass 写法
```python
@dataclass
class AgentChatDTO:
    user_id: str                          # 用户 ID
    message: str                          # 本轮用户输入
    session_id: str | None = None         # 会话 ID，首次请求可不传
    model_name: str | None = None         # 指定模型，空时使用默认值
    max_iterations: int | None = None     # 最大推理循环次数
```

### 普通类写法
```python
class AgentHandler:
    def __init__(
        self,
        agent: ReactAgent,
        stream_thinking: bool = True,
    ) -> None:
        self.agent = agent                # ReactAgent 实例
        self.stream_thinking = stream_thinking  # 是否推送思考过程

    async def run(self, message: str) -> AsyncIterator[AgentEvent]: ...
    async def _handle_node_output(self, output: NodeOutput) -> None: ...
```

---

## 分层惯例

Python 项目没有强制的分层规范，但常见框架有约定俗成的层次：

### FastAPI / Flask（Web 后端）

| 层名 | 典型文件/目录 | 职责说明 |
|---|---|---|
| 数据层 | `schemas/`、`models/`（Pydantic/SQLAlchemy） | 数据结构定义、ORM 模型 |
| 仓储层 | `repositories/`、`crud/` | 数据库操作封装 |
| 服务层 | `services/` | 业务逻辑编排 |
| 依赖层 | `dependencies/`、`deps.py` | FastAPI 依赖注入 |
| 路由层 | `routers/`、`api/`、`views/` | HTTP 路由与请求处理 |

### 通用模块（工具库、SDK、Agent 框架）

| 层名 | 典型目录/文件 | 职责说明 |
|---|---|---|
| 模型层 | `models.py`、`types.py` | 数据类型定义（dataclass/TypedDict） |
| 配置层 | `config.py`、`settings.py` | 配置加载（pydantic-settings/environs） |
| 核心层 | `core/`、`engine/` | 核心算法或调度逻辑 |
| 工厂层 | `factory.py`、`registry.py` | 对象创建与注册 |
| 接口层 | `abc.py`、`base.py`、`protocol.py` | 抽象基类或 Protocol 定义 |
| 工具层 | `utils/`、`helpers/` | 无副作用的纯工具函数 |

---

## 模块结构注释风格

Python 项目用文件路径展示，注释同样用 `#`：
```
src/
└── agent/
    ├── __init__.py
    ├── dto.py               # 请求/响应数据类（AgentChatDTO、AgentEventDTO）
    ├── config.py            # 全局配置（AgentSettings via pydantic-settings）
    ├── factory.py           # 模型工厂（按 provider 创建 LLM 客户端）
    ├── manager.py           # 模型管理（配置缓存与热更新）
    ├── handler.py           # Agent 推理驱动与 SSE 事件推送
    └── service.py           # 业务编排入口（chat() 方法）
```

---

## 特殊说明

- **异步方法**：`async def` 在签名中保留 `async` 关键字，注释中标注 `# 异步方法`
- **Protocol 类**：等同于 Java 接口，层名写"协议层"或"接口层"
- **`__all__`**：不需要在代码块中保留
- **类型别名**：若是公共 API 的一部分（如 `ModelName = str`），保留并注释
- **生成器 / 异步生成器**：返回类型写 `Iterator[X]` 或 `AsyncIterator[X]`，在说明中注明流式语义