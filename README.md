# Fairy

## 项目简介

Fairy是一个基于Spring Boot 3.2和Spring AI的智能聊天应用框架，集成了多种大语言模型，提供流式聊天服务。项目采用微服务架构设计，支持动态模型配置和Nacos配置中心。

## ✨ 核心特性

- 🤖 **多模型支持**: 集成多种大语言模型，支持阿里云通义千问等主流AI模型
- 🔄 **流式响应**: 基于SSE(Server-Sent Events)实现实时流式聊天体验
- ⚙️ **动态配置**: 通过Nacos配置中心实现模型参数的动态更新，无需重启服务
- 🏗️ **微服务架构**: 采用多模块设计，包括web、service、repository、integration四个核心模块
- 💾 **数据持久化**: 支持MySQL和Redis，实现聊天记录的存储和缓存
- 🧠 **智能记忆**: 内置聊天记忆功能，支持上下文对话管理

## 🏛️ 技术架构

### 核心技术栈

- **Spring Boot 3.2.11**: 现代化的Java应用开发框架
- **Spring Cloud 2023.0.3**: 微服务架构支持
- **Spring AI**: AI应用开发框架
- **Spring Cloud Alibaba 2023.0.1.2**: 阿里云生态集成
- **MyBatis Plus 3.5.5**: 数据访问层框架
- **MySQL 8.3.0**: 关系型数据库
- **Redis**: 缓存和会话存储
- **Nacos**: 配置中心和服务发现
- **Java 21**: 最新Java特性支持

### 模块结构

```
fairy/
├── fairy-web/           # Web层 - REST API接口
├── fairy-service/       # 业务逻辑层 - 核心业务服务
├── fairy-repository/    # 数据访问层 - 数据存储操作
└── fairy-integration/   # 集成层 - 外部系统集成
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.0+

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/JensonXu-NUAA/Fairy.git
   cd Fairy
   ```

2. **配置数据库**
   ```sql
   -- 创建数据库
   CREATE DATABASE fairy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **配置Nacos**
   - 启动Nacos服务器
   - 在Nacos中创建配置文件 `llm_config.json`，分组为 `GENSOKYO_AI_GROUP`
   - 配置示例：
   ```json
   {
     "qwen-turbo": {
       "provider": "alibaba",
       "apiKey": "your-api-key",
       "model": "qwen-turbo",
       "temperature": 0.7,
       "maxTokens": 2000
     }
   }
   ```

4. **启动应用**
   ```bash
   mvn clean install
   mvn spring-boot:run -pl fairy-web
   ```

5. **访问应用**
   - 应用地址: http://localhost:8080
   - 聊天接口: POST /chat/stream

## 📖 API文档

### 流式聊天接口

**接口地址**: `POST /chat/stream`

**请求格式**:
```json
{
  "userId": "user123",
  "conversationId": "conv456",
  "message": "你好，请介绍一下你自己",
  "modelName": "qwen-turbo"
}
```

**响应格式**: Server-Sent Events (SSE) 流式响应

**请求示例**:
```bash
curl -X POST http://localhost:8080/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "conversationId": "conv456", 
    "message": "你好",
    "modelName": "qwen-turbo"
  }'
```

## 🔧 配置说明

### 应用配置

主要配置文件位于 `fairy-web/src/main/resources/application.yml`：

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        extension-configs:
          - data-id: llm_config.json
            group: GENSOKYO_AI_GROUP
```

### 模型配置

在Nacos中配置支持的AI模型参数：

```json
{
  "model-name": {
    "provider": "alibaba",
    "apiKey": "your-api-key",
    "model": "model-id",
    "temperature": 0.7,
    "maxTokens": 2000
  }
}
```

## 🎯 核心功能

### 1. 智能聊天
- 支持多轮对话，具备上下文记忆能力
- 实时流式响应，提供流畅的聊天体验
- 支持多种AI模型切换

### 2. 动态模型管理
- 通过Nacos实现模型配置的动态更新
- 支持热加载，无需重启服务
- 模型参数灵活配置

### 3. 数据持久化
- 聊天记录存储到MySQL数据库
- 支持用户会话管理
- Redis缓存提升性能

## 🛠️ 开发指南

### 项目结构说明

- **fairy-web**: 提供REST API接口，处理HTTP请求
- **fairy-service**: 核心业务逻辑，包括聊天服务、用户服务等
- **fairy-repository**: 数据访问层，封装数据库操作
- **fairy-integration**: 外部系统集成，包括AI模型集成、消息队列等

### 添加新的AI模型

1. 在Nacos配置中添加新模型配置
2. 在`ChatClientFactoryManager`中实现对应的工厂类
3. 重启应用或等待配置自动更新

### 扩展功能

项目采用模块化设计，便于功能扩展：
- 在对应模块添加新的服务类
- 在Controller层添加新的API接口
- 在Repository层添加数据访问逻辑

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 作者

**Jenson Xu** - *项目维护者* - [GitHub](https://github.com/JensonXu-NUAA)

## 🙏 致谢

- [Spring AI](https://spring.io/projects/spring-ai) - 提供AI应用开发框架
- [Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba) - 提供阿里云集成支持
- [Nacos](https://nacos.io/) - 提供配置中心和服务发现
- 所有为开源社区做出贡献的开发者们

---

如果这个项目对您有帮助，请给我们一个 ⭐️ Star！
