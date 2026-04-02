# Java / Kotlin 语言规范

本文件定义 Java 和 Kotlin 项目文档的代码块规范与分层惯例。

---

## 代码块规范

### 保留内容
- 类声明行（含泛型、继承、接口实现）
- 字段定义（含类型和访问修饰符）
- 构造方法签名（含参数列表，无方法体）
- 公共方法签名（`public` 方法，含返回类型和参数）
- 关键注解：`@Data`、`@Builder`、`@Slf4j`、`@Component`、`@Service`、`@Repository`、`@RestController`、`@Configuration`、`@ConfigurationProperties`、`@RequiredArgsConstructor`、`@PostConstruct`、`@Transactional`
- 内部静态类的类声明和字段（≤5个字段时展开，否则只展示声明 + 代表性字段 + `// ...`）

### 删除内容
- 方法体（用空参数列表 `{}` 表示，或直接省略花括号只写签名加 `;`）
- 噪音注解：`@Override`、`@SuppressWarnings`、`@NonNull`（除非它是接口契约的一部分）
- `import` 语句
- 局部变量声明
- 私有辅助方法（可用 `// 私有方法：xxx` 注释代替）

### 字段注释风格
注释跟在字段行末，用 `//` 引出，与示例文档保持一致：
```java
private final Map<String, ModelConfig> agentModels = new ConcurrentHashMap<>(); // 模型配置缓存
private String defaultModel = "qwen3.5-plus";  // 未指定模型时的全局默认值
```

### 示例（完整类的代码块写法）
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentModelManager {

    private final NacosConfigManager nacosConfigManager;
    private final AgentModelFactoryManager factoryManager;

    private static final String AGENT_DATA_ID = "fairy_agent_config"; // Nacos DataId
    private final Map<String, ModelConfig> agentModels = new ConcurrentHashMap<>(); // 模型配置缓存

    @PostConstruct
    public void init();                                              // 初始化：拉取配置 + 注册监听器

    public ReactAgent createAgent(String modelName,
                                   ToolCallback[] toolCallbacks,
                                   BaseCheckpointSaver saver);
}
```

---

## 分层惯例

Java Spring Boot 项目的标准层次，**按从底到顶的顺序**展开：

| 层名 | 典型类后缀/注解 | 职责说明 |
|---|---|---|
| 数据层 | `DTO`、`VO`、`Entity`、`DO` | 请求/响应/持久化数据载体 |
| 常量层 | `Constant`、`Enum`、`Type` | 常量定义、枚举、类型标识 |
| 配置层 | `Properties`、`Config`、`@ConfigurationProperties` | 配置属性绑定与默认值 |
| 工厂层 | `Factory`、`Builder`（创建复杂对象） | 对象创建策略，封装构建细节 |
| 管理层 | `Manager`、`Registry`、`Cache` | 对象生命周期管理、路由分发 |
| 构建层 | `Builder`（组装多依赖） | 整合多个依赖，组装可用实例 |
| 实现层 | `Handler`、`Processor`、`Executor` | 核心业务逻辑执行 |
| 服务层 | `@Service` | 业务编排，串联多个下层组件 |
| Web 层 | `@RestController`、`@Controller` | HTTP 接入，参数解析，响应封装 |

若项目使用非标准分层，按实际调用链顺序从底到顶排列，层名沿用代码中的包名。

---

## 模块结构注释风格

目录树注释用 `#`，Java 风格为：
```
src/main/java/com/example/
└── agent/
    ├── AgentProperties.java     # 全局配置属性（@ConfigurationProperties）
    ├── AgentClientBuilder.java  # ReactAgent 构建入口（整合模型 + 工具）
    └── handler/
        ├── AgentHandler.java            # Agent 推理驱动核心
        └── AgentConcurrencyLimiter.java # 并发控制（Semaphore）
```

---

## Kotlin 特殊说明

- `data class` 等同于 Java 的 `@Data` 类，注释为 `# 数据载体`
- `object` 单例等同于工具类，注释为 `# 单例，负责…`
- 挂起函数（`suspend fun`）保留 `suspend` 关键字，注释标注 `# 协程挂起函数`
- 扩展函数单独列出，放在被扩展类说明的末尾