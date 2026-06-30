# AI Customer Service — Code Wiki

> 基于 Spring Boot 3 + Langchain4j + 智谱 GLM + RAG + SQLite 的智能客服系统

---

## 目录

1. [项目概览](#1-项目概览)
2. [技术栈与依赖](#2-技术栈与依赖)
3. [项目架构](#3-项目架构)
4. [目录结构](#4-目录结构)
5. [模块详解](#5-模块详解)
   - 5.1 [启动入口](#51-启动入口)
   - 5.2 [配置层 (config)](#52-配置层-config)
   - 5.3 [控制器层 (controller)](#53-控制器层-controller)
   - 5.4 [拦截器层 (interceptor)](#54-拦截器层-interceptor)
   - 5.5 [模型层 (model)](#55-模型层-model)
   - 5.6 [服务层 (service)](#56-服务层-service)
   - 5.7 [RAG 层 (rag)](#57-rag-层-rag)
   - 5.8 [数据访问层 (dal)](#58-数据访问层-dal)
   - 5.9 [前端页面 (static)](#59-前端页面-static)
   - 5.10 [知识库与数据文件](#510-知识库与数据文件)
6. [核心数据流](#6-核心数据流)
7. [关键类与函数索引](#7-关键类与函数索引)
8. [依赖关系图](#8-依赖关系图)
9. [配置说明](#9-配置说明)
10. [项目运行方式](#10-项目运行方式)
11. [API 接口文档](#11-api-接口文档)
12. [扩展与 TODO](#12-扩展与-todo)

---

## 1. 项目概览

| 属性 | 值 |
|---|---|
| 项目名称 | ai-customer-service |
| 版本 | 1.1.2-SNAPSHOT |
| Java 版本 | 17 |
| Spring Boot 版本 | 3.3.5 |
| Langchain4j 版本 | 0.36.2 |
| 默认大模型 | 智谱 GLM-4-Flash |
| 向量数据库 | Chroma |
| 嵌入模型 | AllMiniLmL6V2（本地 ONNX，离线运行） |
| 关系型数据库 | SQLite（持久化对话记录与客诉数据） |
| Token 缓存 | Guava Cache（7 天 TTL 自动过期） |
| 服务端口 | 8081 |

本项目是一个面向"智慧商城"电商场景的 AI 智能客服系统。核心能力包括：

- **RAG 知识库问答**：基于本地文档（.txt / .md）构建向量索引，检索增强生成
- **Tool Calling**：支持计算器工具和订单查询工具（从 orders.json 加载真实数据，含链式调用）
- **流式对话**：SSE 实时推送 LLM 生成内容
- **用户鉴权**：Cookie-based Token 认证，Guava Cache 管理 Token 自动过期
- **对话记忆持久化**：基于 SQLite 的 ChatMemoryStore，重启不丢失对话上下文
- **客诉闭环管理**：自动记录客诉数据，用户主动确认闭环，自动检测转人工
- **Web 聊天界面**：内嵌单页聊天 UI，支持快捷问题、打字动画、问题解决确认按钮

---

## 2. 技术栈与依赖

### Maven 依赖清单

| 依赖 | 用途 |
|---|---|
| `spring-boot-starter-web` | Web 框架，提供 REST API 与 SSE 支持 |
| `spring-boot-starter-jdbc` | JDBC 数据访问，JdbcTemplate 支持 |
| `langchain4j-spring-boot-starter` | Langchain4j 与 Spring Boot 自动装配 |
| `langchain4j-zhipu-ai` | 智谱 GLM 大模型集成（Chat + Streaming） |
| `langchain4j-document-parser-apache-tika` | 文档解析（支持 txt/md 等格式） |
| `langchain4j-embeddings-all-minilm-l6-v2` | 本地 ONNX 嵌入模型，无需外部 API |
| `langchain4j-chroma` | Chroma 向量数据库客户端 |
| `sqlite-jdbc` | SQLite JDBC 驱动 |
| `guava` | Guava Cache，Token 自动过期管理 |
| `spring-boot-starter-test` | 测试框架 |

### 外部服务依赖

| 服务 | 地址 | 说明 |
|---|---|---|
| 智谱 AI API | `https://open.bigmodel.cn` | 大模型推理服务，需 API Key |
| Chroma | `http://localhost:8000` | 向量数据库服务，需本地部署 |

---

## 3. 项目架构

```
┌──────────────────────────────────────────────────────────────────┐
│                         浏览器 (前端)                              │
│  login.html ──登录──> Cookie AUTH_TOKEN ──> index.html            │
│                                          + 问题解决确认按钮         │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTP / SSE
┌────────────────────────────▼─────────────────────────────────────┐
│                    Spring Boot (port 8081)                        │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │AuthInterceptor│  │AuthController│  │ChatController          │  │
│  │ (鉴权守卫)    │  │(登录/登出)   │  │ChatHistoryController   │  │
│  └──────────────┘  └──────┬───────┘  │(闭环/状态查询)          │  │
│                           │           └───────────┬────────────┘  │
│                  ┌────────▼────────┐              │               │
│                  │  AuthService     │   ┌──────────▼────────────┐  │
│                  │ (Guava Cache TTL)│   │LangchainChatService   │  │
│                  └─────────────────┘   │  - 创建客诉记录         │  │
│                                        │  - 检测转人工标记       │  │
│                                        │  - 调用 Agent           │  │
│                                        └──────────┬────────────┘  │
│                                                   │               │
│                                    ┌──────────────▼────────────┐  │
│                                    │   CustomerServiceAgent     │  │
│                                    │   (Langchain4j AiServices)  │  │
│                                    └──┬────────┬──────────┬─────┘  │
│                                       │        │          │        │
│                          ┌────────────▼──┐  ┌──▼──────┐  ┌▼──────┐│
│                          │ContentRetriever│  │Tools    │  │Memory ││
│                          │(RAG 检索)      │  │Calc     │  │Store  ││
│                          └───────┬───────┘  │OrderQuery│  │(SQLite)│
│                                  │          └─────────┘  └───────┘│
│                         ┌────────▼────────┐                      │
│                         │ EmbeddingStore   │                      │
│                         │ (Chroma)         │                      │
│                         └────────┬─────────┘                      │
│                                  │                                 │
│  ┌───────────────────────────────▼──────────────────────────────┐ │
│  │              KnowledgeBaseInitializer                        │ │
│  │   (启动时加载 knowledge/ 目录文档 → 向量化入库)               │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    SQLite (data/customer_service.db)         │ │
│  │  ├─ customer_service_chat_history (客诉记录)                 │ │
│  │  └─ chat_message (对话记忆持久化)                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────┘
```

### 架构分层

| 层次 | 包路径 | 职责 |
|---|---|---|
| 配置层 | `com.ai.customerservice.config` | Bean 装配、外部属性绑定 |
| 控制器层 | `com.ai.customerservice.controller` | HTTP 入口，请求路由与响应 |
| 拦截器层 | `com.ai.customerservice.interceptor` | 鉴权拦截 |
| 模型层 | `com.ai.customerservice.model` | 请求/响应 DTO |
| 服务层 | `com.ai.customerservice.service` | 业务逻辑与 AI 代理 |
| 工具层 | `com.ai.customerservice.service.tool` | LLM 可调用的 Tool |
| RAG 层 | `com.ai.customerservice.rag` | 文档处理、知识库初始化、对话记忆持久化 |
| 数据访问层 | `com.ai.customerservice.dal` | SQLite 数据源配置、DAO |

---

## 4. 目录结构

```
ai-customer-service/
├── pom.xml                                        # Maven 构建配置
├── README.md                                      # 项目说明
├── 项目WIKI.md                                    # 本文档
├── knowledge/                                     # 外部知识库目录（运行时挂载）
│   ├── company-info.md
│   └── product-faq.txt
├── data/                                          # SQLite 数据目录（运行时生成）
│   └── customer_service.db
├── src/
│   └── main/
│       ├── java/com/ai/customerservice/
│       │   ├── AiCustomerServiceApplication.java         # Spring Boot 启动类
│       │   ├── config/
│       │   │   ├── AuthProperties.java                   # 鉴权配置属性
│       │   │   ├── LangchainConfig.java                  # LLM/Embedding 模型配置
│       │   │   ├── RagConfig.java                        # RAG 检索 & Agent 组装
│       │   │   ├── VectorStoreConfig.java                # 向量存储配置
│       │   │   └── WebConfig.java                        # CORS & 拦截器注册
│       │   ├── controller/
│       │   │   ├── AuthController.java                   # 登录/登出/用户信息
│       │   │   ├── ChatController.java                   # 对话/知识库接口
│       │   │   └── ChatHistoryController.java            # 客诉闭环接口
│       │   ├── interceptor/
│       │   │   └── AuthInterceptor.java                  # 鉴权拦截器
│       │   ├── model/
│       │   │   ├── ChatRequest.java                      # 对话请求 DTO
│       │   │   └── ChatResponse.java                    # 对话响应 DTO
│       │   ├── rag/
│       │   │   ├── DocumentProcessor.java                # 文档加载/分块/向量化
│       │   │   ├── KnowledgeBaseInitializer.java         # 知识库启动初始化
│       │   │   └── SqliteChatMemoryStore.java            # 对话记忆 SQLite 持久化
│       │   ├── dal/
│       │   │   ├── DataSourceConfig.java                 # SQLite 数据源配置
│       │   │   ├── ChatHistory.java                      # 客诉记录实体类
│       │   │   ├── ChatHistoryDao.java                   # 客诉记录 DAO
│       │   │   └── ChatMessageDao.java                   # 对话消息 DAO
│       │   └── service/
│       │       ├── AuthService.java                      # Token 认证服务 (Guava Cache)
│       │       ├── ChatService.java                      # 对话服务接口
│       │       ├── CustomerServiceAgent.java             # AI 代理接口
│       │       ├── impl/
│       │       │   └── LangchainChatService.java         # 对话服务实现
│       │       └── tool/
│       │           ├── CalculatorService.java             # 计算器工具
│       │           └── OrderQueryService.java            # 订单查询工具 (orders.json)
│       └── resources/
│           ├── application.yml                           # 应用配置
│           ├── db/
│           │   └── customer_service.db                   # SQLite 模板（含表结构）
│           ├── orders.json                               # 示例订单数据
│           ├── knowledge/
│           │   ├── company-info.md                       # 公司信息知识
│           │   └── product-faq.txt                       # 产品 FAQ 知识
│           └── static/
│               ├── index.html                            # 聊天主页面（含闭环按钮）
│               └── login.html                            # 登录页面
└── target/                                               # 构建输出
```

---

## 5. 模块详解

### 5.1 启动入口

**[AiCustomerServiceApplication.java](src/main/java/com/ai/customerservice/AiCustomerServiceApplication.java)**

标准 Spring Boot 启动类，使用 `@SpringBootApplication` 注解，触发组件扫描与自动装配。

---

### 5.2 配置层 (config)

#### AuthProperties

**[AuthProperties.java](src/main/java/com/ai/customerservice/config/AuthProperties.java)**

- 使用 `@ConfigurationProperties(prefix = "app.auth")` 绑定 YAML 配置
- 字段：`enabled`（是否启用鉴权）、`defaultUsername`（默认用户名）、`users`（用户列表）
- 内部类 `User` 持有 `username` / `password` 字段

#### LangchainConfig

**[LangchainConfig.java](src/main/java/com/ai/customerservice/config/LangchainConfig.java)**

注册三个核心 Bean：

| Bean | 类型 | 说明 |
|---|---|---|
| `chatLanguageModel` | `ZhipuAiChatModel` | 同步对话模型，用于 `/api/chat` |
| `streamingChatLanguageModel` | `ZhipuAiStreamingChatModel` | 流式对话模型，用于 `/api/chat/stream` |
| `embeddingModel` | `AllMiniLmL6V2EmbeddingModel` | 本地 ONNX 嵌入模型，用于文档向量化与检索 |

关键参数：`callTimeout=60s`, `connectTimeout=15s`, `readTimeout=60s`, `writeTimeout=15s`

#### RagConfig

**[RagConfig.java](src/main/java/com/ai/customerservice/config/RagConfig.java)**

核心组装类，构建完整的 RAG + Agent 管道：

| Bean | 说明 |
|---|---|
| `contentRetriever` | 基于 `EmbeddingStore` + `EmbeddingModel` 构建检索器，配置 `maxResults` 和 `minScore` |
| `customerServiceAgent` | 通过 `AiServices.builder()` 组装 AI 代理 |

Agent 组装细节：
- `chatMemoryProvider`：每个 `memoryId`（即 sessionId）创建独立的 `MessageWindowChatMemory`，窗口大小 20 条
- `chatMemoryStore`：注入 `SqliteChatMemoryStore`，对话记忆持久化到 SQLite
- `tools`：注册 `CalculatorService` 和 `OrderQueryService`
- `contentRetriever`：RAG 检索器，在对话时自动检索相关知识片段

#### VectorStoreConfig

**[VectorStoreConfig.java](src/main/java/com/ai/customerservice/config/VectorStoreConfig.java)**

创建 `ChromaEmbeddingStore` Bean，连接本地 Chroma 服务，配置 `logRequests` 和 `logResponses` 用于调试。

#### WebConfig

**[WebConfig.java](src/main/java/com/ai/customerservice/config/WebConfig.java)**

- **CORS**：允许 `/api/**` 路径跨域访问（GET/POST/OPTIONS）
- **拦截器注册**：`AuthInterceptor` 拦截 `/`, `/index.html`, `/api/**`
- **排除路径**：`/login.html`, `/api/auth/login`, `/api/auth/logout`, `/api/health`

---

### 5.3 控制器层 (controller)

#### ChatController

**[ChatController.java](src/main/java/com/ai/customerservice/controller/ChatController.java)**

| 端点 | 方法 | 说明 |
|---|---|---|
| `GET /api/hello` | `hello()` | 读取 Hello.txt 返回问候语 |
| `GET /api/knowledge` | `knowledge()` | 列出知识库目录中的文件 |
| `GET /api/knowledge/{filename}` | `knowledgeFile(filename)` | 读取知识库文件内容 |
| `POST /api/knowledge/reload` | `reloadKnowledge()` | 重新加载知识库到向量数据库 |
| `POST /api/chat` | `chat(ChatRequest)` | 同步对话，返回完整回答 |
| `GET /api/chat/stream` | `chatStream(message, sessionId, model)` | SSE 流式对话 |
| `GET /api/health` | `health()` | 健康检查 |

#### AuthController

**[AuthController.java](src/main/java/com/ai/customerservice/controller/AuthController.java)**

| 端点 | 方法 | 说明 |
|---|---|---|
| `POST /api/auth/login` | `login(username, password)` | 登录，成功后设置 Cookie |
| `POST /api/auth/logout` | `logout()` | 登出，清除 Token 与 Cookie |
| `GET /api/auth/me` | `me()` | 获取当前登录用户信息 |

#### ChatHistoryController

**[ChatHistoryController.java](src/main/java/com/ai/customerservice/controller/ChatHistoryController.java)**

| 端点 | 方法 | 说明 |
|---|---|---|
| `POST /api/chat-history/resolve` | `resolve(sessionId)` | 用户确认问题已解决，更新状态为"已闭环" |
| `GET /api/chat-history/status` | `getStatus(sessionId)` | 查询当前会话的客诉状态和转人工标记 |

---

### 5.4 拦截器层 (interceptor)

#### AuthInterceptor

**[AuthInterceptor.java](src/main/java/com/ai/customerservice/interceptor/AuthInterceptor.java)**

实现 `HandlerInterceptor.preHandle()`，鉴权逻辑：

1. 检查 `authProperties.isEnabled()`，未启用则直接放行
2. 从 Cookie 中提取 `AUTH_TOKEN`
3. 调用 `AuthService.resolveUsername(token)` 验证 Token
4. **API 请求**（`/api/` 开头）：返回 401 JSON
5. **页面请求**：重定向到 `/login.html`

---

### 5.5 模型层 (model)

#### ChatRequest

**[ChatRequest.java](src/main/java/com/ai/customerservice/model/ChatRequest.java)**

| 字段 | 类型 | 说明 |
|---|---|---|
| `message` | `String` | 用户消息内容 |
| `sessionId` | `String` | 会话 ID（为空时自动生成 UUID） |
| `model` | `String` | 模型选择（预留字段） |

#### ChatResponse

**[ChatResponse.java](src/main/java/com/ai/customerservice/model/ChatResponse.java)**

| 字段 | 类型 | 说明 |
|---|---|---|
| `answer` | `String` | AI 回答内容 |
| `sessionId` | `String` | 会话 ID |
| `timestamp` | `LocalDateTime` | 响应时间戳 |

---

### 5.6 服务层 (service)

#### ChatService (接口)

**[ChatService.java](src/main/java/com/ai/customerservice/service/ChatService.java)**

- `ChatResponse chat(ChatRequest)` — 同步对话
- `SseEmitter chatStream(ChatRequest)` — 流式对话

#### LangchainChatService (实现)

**[LangchainChatService.java](src/main/java/com/ai/customerservice/service/impl/LangchainChatService.java)**

核心对话服务实现，注入 `CustomerServiceAgent`、`ChatHistoryDao`、`AuthService`：

- **`chat()`**：
  1. `ensureChatHistory(sessionId)` — 首次对话时自动创建客诉记录（从 Cookie 解析用户名）
  2. 调用 `CustomerServiceAgent.chat(sessionId, message)`
  3. `checkTransferredToHuman(sessionId, answer)` — 检测回答中是否包含"666666"（转人工电话），自动更新 `transferred_to_human`
- **`chatStream()`**：
  1. 创建 `SseEmitter`（超时 300s）
  2. `ensureChatHistory(sessionId)` — 同上
  3. 调用 `CustomerServiceAgent.chatStream()` 获取 `TokenStream`
  4. 逐 Token 通过 SSE 推送
  5. 完成时发送 `[DONE]` 标记，并执行 `checkTransferredToHuman()`
- **`ensureChatHistory(sessionId)`**：如果该 sessionId 无客诉记录，则插入一条（状态=未闭环）
- **`resolveCurrentUsername()`**：从 `HttpServletRequest` Cookie 中提取 Token → `AuthService.resolveUsername()`

#### CustomerServiceAgent (AI 代理接口)

**[CustomerServiceAgent.java](src/main/java/com/ai/customerservice/service/CustomerServiceAgent.java)**

Langchain4j 声明式 AI 代理，使用 `@SystemMessage` 定义系统提示词（9 条规则）：

```
1. 优先使用知识库中的信息来回答用户的问题
2. 当用户需要数学计算时，使用计算器工具进行精确计算
3. 回答要准确、简洁、有条理
4. 如果知识库中没有相关信息，请如实告知用户，并建议联系人工客服
5. 保持友好、专业的语气
6. 如果用户的问题不清楚，请礼貌地要求用户提供更多信息
7. 如果用户查询订单状态为已发货，调用queryOrderLogistics方法
8. 如果用户要求转人工，则提供电话号码666666请用户联系该电话
9. 如果用户只是打招呼，要主动询问用户的需求
```

两个方法：
- `String chat(@MemoryId String sessionId, @UserMessage String userMessage)` — 同步调用
- `TokenStream chatStream(@MemoryId String sessionId, @UserMessage String userMessage)` — 流式调用

#### AuthService

**[AuthService.java](src/main/java/com/ai/customerservice/service/AuthService.java)**

基于 **Guava Cache** 的 Token 认证服务，支持自动过期：

| 常量/字段 | 值 | 说明 |
|---|---|---|
| `COOKIE_NAME` | `"AUTH_TOKEN"` | Cookie 名称 |
| `COOKIE_MAX_AGE_SECONDS` | `7 * 24 * 60 * 60`（7天） | Cookie 有效期 + Cache TTL |
| `tokenCache` | `Cache<String, String>` | Guava Cache，`expireAfterWrite(7天)` |

| 方法 | 说明 |
|---|---|
| `login(username, password)` | 校验凭据，生成 32 字节随机 Token（Base64 编码），存入 Cache |
| `resolveUsername(token)` | 从 Cache 查找用户名（过期 Token 自动淘汰） |
| `logout(token)` | 从 Cache 中移除 Token |

> Token 写入 7 天后由 Guava Cache 自动过期淘汰，无需手动清理。Cookie 过期（7天）与服务端 Token 过期完全对齐。

#### CalculatorService (工具)

**[CalculatorService.java](src/main/java/com/ai/customerservice/service/tool/CalculatorService.java)**

使用 `@Tool` 和 `@P` 注解声明 LLM 可调用工具：

| 方法 | 功能 | 异常处理 |
|---|---|---|
| `add(a, b)` | 加法 | — |
| `subtract(a, b)` | 减法 | — |
| `multiply(a, b)` | 乘法 | — |
| `divide(a, b)` | 除法 | 除数为零返回错误信息 |
| `power(base, exponent)` | 幂运算 | — |
| `sqrt(number)` | 平方根 | 负数返回错误信息 |

#### OrderQueryService (工具)

**[OrderQueryService.java](src/main/java/com/ai/customerservice/service/tool/OrderQueryService.java)**

从 **orders.json** 文件加载订单数据，启动时通过 `@PostConstruct` 初始化：

| 方法 | 功能 | 返回值 |
|---|---|---|
| `queryOrderStatus(orderId)` | 查询订单状态 | 返回订单号、状态、下单时间、商品明细、总金额 |
| `queryOrderLogistics(orderId)` | 查询物流信息 | 返回物流公司、运单号、当前状态 |
| `hiThere(orderId)` | 待发货安抚 | 返回安抚文案 |

> 链式调用：当 `queryOrderStatus` 返回"已发货"时，Agent 的 SystemMessage 规则 7 指示其自动调用 `queryOrderLogistics`。

---

### 5.7 RAG 层 (rag)

#### DocumentProcessor

**[DocumentProcessor.java](src/main/java/com/ai/customerservice/rag/DocumentProcessor.java)**

文档处理核心组件：

**`processDocuments(resourcePattern)`** 流程：
1. 使用 `PathMatchingResourcePatternResolver` 扫描匹配的资源文件
2. 过滤 `.txt` 和 `.md` 文件
3. 使用 `TextDocumentParser` 解析文档
4. 为每个文档添加 `source` 元数据
5. 构建 `EmbeddingStoreIngestor`：
   - 分块策略：递归分块（`chunkSize=300`, `chunkOverlap=30`）
   - 嵌入模型：`AllMiniLmL6V2EmbeddingModel`
   - 存储：`EmbeddingStore`（Chroma）
6. 执行 `ingest()` 将文档向量化并入库

#### KnowledgeBaseInitializer

**[KnowledgeBaseInitializer.java](src/main/java/com/ai/customerservice/rag/KnowledgeBaseInitializer.java)**

实现 `ApplicationRunner`，在 Spring Boot 启动完成后自动执行：

1. 读取 `app.knowledge.path` 配置（默认 `file:knowledge/`）
2. 调用 `DocumentProcessor.processDocuments()` 加载所有知识文档
3. 日志输出加载文档数量

#### SqliteChatMemoryStore

**[SqliteChatMemoryStore.java](src/main/java/com/ai/customerservice/rag/SqliteChatMemoryStore.java)**

实现 Langchain4j 的 `ChatMemoryStore` 接口，将对话记忆持久化到 SQLite：

| 方法 | 说明 |
|---|---|
| `getMessages(memoryId)` | 从 SQLite 读取对话历史，反序列化为 `ChatMessage` 列表 |
| `updateMessages(memoryId, messages)` | 将对话消息序列化后写入 SQLite（全量替换） |
| `deleteMessages(memoryId)` | 删除指定会话的所有消息 |

消息序列化策略：
- `UserMessage` → role="user", content=文本
- `AiMessage`（纯文本）→ role="ai", content=文本
- `AiMessage`（含工具调用）→ role="ai", content="[TOOL_CALLS]" + JSON
- `ToolExecutionResultMessage` → role="tool_result", content=JSON(toolName, id, result)
- `SystemMessage` → role="system", content=文本

---

### 5.8 数据访问层 (dal)

#### DataSourceConfig

**[DataSourceConfig.java](src/main/java/com/ai/customerservice/dal/DataSourceConfig.java)**

SQLite 数据源配置：

- 读取 `app.sqlite.db-path`（默认 `./data/customer_service.db`）
- 如果文件不存在，自动创建目录和文件
- 创建 `SQLiteDataSource`，设置 JDBC URL
- 启动时自动执行 `CREATE TABLE IF NOT EXISTS` 建表：
  - `customer_service_chat_history` — 客诉记录表
  - `chat_message` — 对话消息表

#### ChatHistory (实体类)

**[ChatHistory.java](src/main/java/com/ai/customerservice/dal/ChatHistory.java)**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 自增主键 |
| `username` | `String` | 用户名 |
| `sessionId` | `String` | 会话 ID |
| `complaintTime` | `LocalDateTime` | 客诉时间 |
| `complaintStatus` | `String` | 客诉状态（未闭环/已闭环） |
| `transferredToHuman` | `boolean` | 是否转人工 |

#### ChatHistoryDao

**[ChatHistoryDao.java](src/main/java/com/ai/customerservice/dal/ChatHistoryDao.java)**

客诉记录 DAO，基于 `JdbcTemplate`：

| 方法 | 类型 | 说明 |
|---|---|---|
| `insert(ChatHistory)` | 增 | 插入客诉记录，返回带 id 的对象 |
| `findById(Long)` | 查 | 按 id 查询 |
| `findByUsername(String)` | 查 | 按用户名查询（时间倒序） |
| `findBySessionId(String)` | 查 | 按会话 ID 查询 |
| `findByStatus(String)` | 查 | 按状态查询 |
| `findAll()` | 查 | 查询全部（时间倒序） |
| `updateStatus(Long, String)` | 改 | 更新客诉状态 |
| `updateTransferredToHuman(Long, boolean)` | 改 | 更新转人工标记 |
| `deleteById(Long)` | 删 | 按 id 删除 |
| `countByStatus(String)` | 统计 | 按状态计数 |
| `countTransferredToHuman()` | 统计 | 转人工计数 |

#### ChatMessageDao

**[ChatMessageDao.java](src/main/java/com/ai/customerservice/dal/ChatMessageDao.java)**

对话消息 DAO，供 `SqliteChatMemoryStore` 使用：

| 方法 | 类型 | 说明 |
|---|---|---|
| `findBySessionId(String)` | 查 | 按 sessionId 查询所有消息（按 id 正序） |
| `insert(String, String, String)` | 增 | 插入单条消息 |
| `deleteBySessionId(String)` | 删 | 删除指定会话的所有消息 |
| `replaceMessages(String, List)` | 改 | 全量替换会话消息（先删后插） |
| `countToolCallsBySessionId(String)` | 统计 | 统计工具调用次数 |

---

### 5.9 前端页面 (static)

#### index.html — 聊天主页面

**[index.html](src/main/resources/static/index.html)**

功能特性：
- 聊天气泡 UI（用户/AI 区分样式）
- 打字动画指示器
- 快捷问题按钮（退款政策、配送时间、会员权益、联系客服）
- 模型选择下拉框（GLM / Qwen 待接入）
- 退出登录按钮
- 自动生成 sessionId（页面级复用）
- Enter 发送 / Shift+Enter 换行
- **问题解决确认按钮**：AI 回答后展示「✓ 已解决」「✗ 未解决」按钮
  - 点击「已解决」→ 调用 `/api/chat-history/resolve` → 显示感谢反馈
  - 点击「未解决」→ 按钮消失，继续对话
  - 发送新消息 → 旧按钮自动消失

#### login.html — 登录页面

**[login.html](src/main/resources/static/login.html)**

功能特性：
- 用户名/密码表单
- 调用 `/api/auth/login` 接口
- 登录成功跳转 `/index.html`
- 错误信息展示

---

### 5.10 知识库与数据文件

#### knowledge/ 目录

| 文件 | 内容 |
|---|---|
| `company-info.md` | 公司简介、主营业务、联系方式、工作时间、服务承诺 |
| `product-faq.txt` | 退款政策、配送时间、会员权益、保修期、支付方式、人工客服、订单修改、优惠券 |

> 扩展方式：将新的 `.txt` 或 `.md` 文件放入 `knowledge/` 目录，调用 `POST /api/knowledge/reload` 热加载或重启应用。

#### orders.json

**[orders.json](src/main/resources/orders.json)**

10 条示例订单数据，覆盖所有状态场景：

| 订单号 | 状态 | 下单人 | 说明 |
|---|---|---|---|
| ORD20260501001 | 已发货 | 张三 | 多商品，顺丰运输中 |
| ORD20260502002 | 已完成 | 李四 | 单商品，已签收 |
| ORD20260503003 | 待发货 | 王五 | 多商品，无物流 |
| ORD20260504004 | 已取消 | 赵六 | 未支付 |
| ORD20260505005 | 已发货 | 孙七 | 多商品，待揽收 |
| ORD20260506006 | 已完成 | 周八 | 同款多件，已签收 |
| ORD20260507007 | 待发货 | 吴九 | 三件商品 |
| ORD20260508008 | 已发货 | 郑十 | 多商品，圆通运输中 |
| ORD20260509009 | 退款中 | 陈一一 | 签收后申请退款 |
| ORD20260510010 | 已完成 | 林二二 | 多商品，已签收 |

#### data/customer_service.db

SQLite 数据库文件（运行时自动创建），包含两张表：

**customer_service_chat_history 表结构：**

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 自增主键 |
| `username` | TEXT | NOT NULL | 用户名 |
| `session_id` | TEXT | — | 会话 ID |
| `complaint_time` | DATETIME | NOT NULL DEFAULT 当前时间 | 客诉时间 |
| `complaint_status` | TEXT | NOT NULL DEFAULT '未闭环', CHECK IN ('未闭环','已闭环') | 客诉状态 |
| `transferred_to_human` | INTEGER | NOT NULL DEFAULT 0, CHECK IN (0, 1) | 是否转人工 |

**chat_message 表结构：**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER | 自增主键 |
| `session_id` | TEXT | 会话 ID |
| `role` | TEXT | 消息角色（user/ai/tool_result/system） |
| `content` | TEXT | 消息内容 |

---

## 6. 核心数据流

### 6.1 同步对话流程（含客诉记录）

```
用户输入 → ChatController.chat()
         → LangchainChatService.chat()
              │
              ├─ ensureChatHistory(sessionId)
              │    → ChatHistoryDao.findBySessionId()
              │    → 不存在则插入新记录（状态=未闭环，username 从 Cookie 解析）
              │
              ├─ CustomerServiceAgent.chat(sessionId, message)
              │    │
              │    ├─ [Memory] SqliteChatMemoryStore 读取历史对话
              │    ├─ [RAG] ContentRetriever 检索知识库相关片段
              │    ├─ [Tool] 判断是否需要调用 Calculator / OrderQuery
              │    ├─ [Memory] SqliteChatMemoryStore 保存更新后的对话
              │    │
              │    ← 返回 AI 回答
              │
              ├─ checkTransferredToHuman(sessionId, answer)
              │    → 回答含"666666" → 更新 transferred_to_human = 1
              │
         ← ChatResponse (answer + sessionId + timestamp)
```

### 6.2 流式对话流程

```
用户输入 → ChatController.chatStream()
         → LangchainChatService.chatStream()
              │
              ├─ ensureChatHistory(sessionId)
              ├─ SseEmitter 创建（超时 300s）
              ├─ CustomerServiceAgent.chatStream(sessionId, message)
              │    │
              │    ├─ TokenStream.onNext(token) → SSE 推送
              │    ├─ TokenStream.onComplete()  → checkTransferredToHuman() + SSE [DONE]
              │    ├─ TokenStream.onError()     → SseEmitter.completeWithError()
              │    │
         ← SSE 事件流
```

### 6.3 知识库初始化流程

```
Spring Boot 启动完成
  → KnowledgeBaseInitializer.run()
    → DocumentProcessor.processDocuments("file:knowledge/*")
      → 扫描 knowledge/ 目录
      → 解析 .txt / .md 文件
      → 递归分块 (300字/块, 30字重叠)
      → AllMiniLmL6V2 向量化
      → 存入 Chroma EmbeddingStore
```

### 6.4 鉴权流程

```
请求到达 → AuthInterceptor.preHandle()
         → 检查 authProperties.isEnabled()
         → 从 Cookie 提取 AUTH_TOKEN
         → AuthService.resolveUsername(token)
              │
         ├─ 有效 → 放行
         ├─ 无效 + API请求 → 401 JSON
         └─ 无效 + 页面请求 → 重定向 /login.html
```

### 6.5 客诉闭环流程

```
AI 回答 → 前端展示回答 + [✓ 已解决] [✗ 未解决] 按钮
              │
              ├─ 点击「已解决」→ POST /api/chat-history/resolve
              │    → ChatHistoryDao.updateStatus(id, "已闭环")
              │    → 显示"感谢您的反馈"
              │
              ├─ 点击「未解决」→ 按钮消失，继续对话
              │
              └─ 继续发新消息 → 旧按钮消失，新回答后重新出现
```

### 6.6 对话记忆持久化流程

```
Agent.chat() 调用前:
  → SqliteChatMemoryStore.getMessages(sessionId)
    → ChatMessageDao.findBySessionId()
    → 反序列化为 ChatMessage 列表（user/ai/tool_result/system）

Agent.chat() 调用后:
  → SqliteChatMemoryStore.updateMessages(sessionId, messages)
    → 序列化所有消息
    → ChatMessageDao.replaceMessages()（先删后插全量替换）
```

---

## 7. 关键类与函数索引

### Bean 装配关系

| Bean 名称 | 定义位置 | 注入位置 |
|---|---|---|
| `ChatLanguageModel` | `LangchainConfig` | `RagConfig` → `CustomerServiceAgent` |
| `StreamingChatLanguageModel` | `LangchainConfig` | `RagConfig` → `CustomerServiceAgent` |
| `EmbeddingModel` | `LangchainConfig` | `RagConfig` → `ContentRetriever`; `DocumentProcessor` |
| `EmbeddingStore<TextSegment>` | `VectorStoreConfig` | `RagConfig` → `ContentRetriever`; `DocumentProcessor` |
| `EmbeddingStoreContentRetriever` | `RagConfig` | `RagConfig` → `CustomerServiceAgent` |
| `CustomerServiceAgent` | `RagConfig` | `LangchainChatService` |
| `CalculatorService` | `@Component` | `RagConfig` → `CustomerServiceAgent.tools` |
| `OrderQueryService` | `@Component` | `RagConfig` → `CustomerServiceAgent.tools` |
| `SqliteChatMemoryStore` | `@Component` | `RagConfig` → `CustomerServiceAgent.chatMemoryStore` |
| `AuthService` | `@Service` | `AuthController`, `AuthInterceptor`, `LangchainChatService` |
| `AuthProperties` | `@Configuration` | `AuthService`, `AuthInterceptor` |
| `DataSource` | `DataSourceConfig` | `JdbcTemplate` |
| `JdbcTemplate` | `DataSourceConfig` | `ChatHistoryDao`, `ChatMessageDao` |
| `ChatHistoryDao` | `@Repository` | `LangchainChatService`, `ChatHistoryController` |
| `ChatMessageDao` | `@Repository` | `SqliteChatMemoryStore` |
| `DocumentProcessor` | `@Component` | `KnowledgeBaseInitializer`, `ChatController` |

### 关键函数签名

```java
// CustomerServiceAgent — AI 代理
String chat(@MemoryId String sessionId, @UserMessage String userMessage)
TokenStream chatStream(@MemoryId String sessionId, @UserMessage String userMessage)

// ChatService — 对话服务接口
ChatResponse chat(ChatRequest request)
SseEmitter chatStream(ChatRequest request)

// LangchainChatService — 对话服务实现
void ensureChatHistory(String sessionId)           // 首次对话创建客诉记录
void checkTransferredToHuman(String sessionId, String answer)  // 检测转人工
String resolveCurrentUsername()                     // 从 Cookie 解析用户名

// AuthService — 认证服务 (Guava Cache)
Optional<String> login(String username, String password)
Optional<String> resolveUsername(String token)
void logout(String token)

// ChatHistoryDao — 客诉记录 DAO
ChatHistory insert(ChatHistory chatHistory)
Optional<ChatHistory> findById(Long id)
Optional<ChatHistory> findBySessionId(String sessionId)
List<ChatHistory> findByUsername(String username)
List<ChatHistory> findByStatus(String complaintStatus)
int updateStatus(Long id, String complaintStatus)
int updateTransferredToHuman(Long id, boolean transferredToHuman)
int deleteById(Long id)
int countByStatus(String complaintStatus)
int countTransferredToHuman()

// ChatMessageDao — 对话消息 DAO
List<Map<String, String>> findBySessionId(String sessionId)
void insert(String sessionId, String role, String content)
void deleteBySessionId(String sessionId)
void replaceMessages(String sessionId, List<Map<String, String>> messages)

// SqliteChatMemoryStore — 对话记忆持久化
List<ChatMessage> getMessages(Object memoryId)
void updateMessages(Object memoryId, List<ChatMessage> messages)
void deleteMessages(Object memoryId)

// DocumentProcessor — 文档处理
int processDocuments(String resourcePattern)

// OrderQueryService — 订单查询工具 (从 orders.json 加载)
String queryOrderStatus(String orderId)
String queryOrderLogistics(String orderId)
String hiThere(String orderId)

// CalculatorService — 计算器工具
double add(double a, double b)
double subtract(double a, double b)
double multiply(double a, double b)
String divide(double a, double b)
double power(double base, double exponent)
String sqrt(double number)
```

---

## 8. 依赖关系图

### 组件依赖

```
ChatController ──────→ ChatService (interface)
                     ─→ DocumentProcessor
                           │
                           ▼
                    LangchainChatService ──→ CustomerServiceAgent (interface)
                     ─→ ChatHistoryDao          │
                     ─→ AuthService       ┌──────┼──────────────┐
                                          │      │              │
                              ┌───────────▼──┐  ┌▼──────────┐  ┌▼──────────────┐
                              │ContentRetriever│ │Tools      │  │ChatMemoryStore│
                              │(RAG 检索)      │ │Calculator │  │(SQLite)       │
                              └───────┬──────┘ │OrderQuery │  └───────┬───────┘
                                      │        └───────────┘          │
                              ┌───────▼────────┐               ┌──────▼──────┐
                              │EmbeddingStore   │               │ChatMessageDao│
                              │(Chroma)         │               └─────────────┘
                              └───────┬─────────┘
                                      │
                              ┌───────▼─────────┐
                              │EmbeddingModel    │
                              │(AllMiniLmL6V2)   │
                              └──────────────────┘

ChatHistoryController ──→ ChatHistoryDao ──→ JdbcTemplate ──→ DataSource (SQLite)

AuthController ──→ AuthService ──→ AuthProperties
AuthInterceptor ──→ AuthService, AuthProperties
LangchainChatService ──→ AuthService (解析用户名)

KnowledgeBaseInitializer ──→ DocumentProcessor ──→ EmbeddingStore + EmbeddingModel
```

### Maven 依赖树（核心）

```
spring-boot-starter-web
spring-boot-starter-jdbc
langchain4j-spring-boot-starter
  └─ langchain4j-core
langchain4j-zhipu-ai
  └─ langchain4j-core
langchain4j-document-parser-apache-tika
  └─ tika-core
langchain4j-embeddings-all-minilm-l6-v2
  └─ onnxruntime
langchain4j-chroma
  └─ langchain4j-core
sqlite-jdbc
guava
```

---

## 9. 配置说明

### application.yml 完整配置项

```yaml
server:
  port: 8081                              # 服务端口

langchain4j:
  zhipu-ai:
    api-key: ${ZHIPU_API_KEY:xxx}         # 智谱 API Key（建议用环境变量）
    chat-model:
      model-name: glm-4-flash             # 同步模型名称
      temperature: 0.7                    # 生成温度
      max-tokens: 2048                    # 最大 Token 数
    streaming-chat-model:
      model-name: glm-4-flash             # 流式模型名称
      temperature: 0.7
      max-tokens: 2048

app:
  sqlite:
    db-path: ${SQLITE_DB_PATH:./data/customer_service.db}  # SQLite 数据库路径
  auth:
    enabled: true                         # 是否启用鉴权
    default-username: anonymous           # 默认用户名
    users:                                # 用户列表
      - username: admin
        password: admin123
      - username: user
        password: user123
  knowledge:
    path: file:knowledge/                 # 知识库文档路径
  rag:
    max-results: 3                        # RAG 检索最大返回条数
    min-score: 0.5                        # RAG 检索最低相似度阈值
    chunk-size: 300                       # 文档分块大小（字符数）
    chunk-overlap: 30                     # 分块重叠字符数
    chroma:
      url: http://localhost:8000           # Chroma 服务地址
      collection-name: customer_service_db # Chroma 集合名
  chat:
    memory-max-messages: 20               # 会话记忆窗口大小
    sse-timeout: 300000                   # SSE 连接超时（毫秒）
```

---

## 10. 项目运行方式

### 前置条件

1. **JDK 17+**
2. **Maven 3.6+**
3. **Chroma 向量数据库**（Docker 部署）：

```bash
docker run -d -p 8000:8000 chromadb/chroma
```

4. **智谱 API Key**：在 [智谱开放平台](https://open.bigmodel.cn) 申请

### 本地运行

```bash
# 1. 设置环境变量（可选，application.yml 中有默认值）
export ZHIPU_API_KEY=your_api_key

# 2. 确保 Chroma 服务运行
# Docker 方式见上方

# 3. 构建并运行
cd ai-customer-service
mvn clean package -DskipTests
java -jar target/ai-customer-service-1.1.2-SNAPSHOT.jar

# 或直接使用 Spring Boot Maven 插件
mvn spring-boot:run
```

### 访问地址

| 地址 | 说明 |
|---|---|
| `http://localhost:8081/login.html` | 登录页面 |
| `http://localhost:8081/index.html` | 聊天主页面（需登录） |
| `http://localhost:8081/api/health` | 健康检查（无需登录） |

### SQLite 数据库查看

```bash
# 命令行查看
sqlite3 ./data/customer_service.db "SELECT * FROM customer_service_chat_history"
sqlite3 ./data/customer_service.db "SELECT * FROM chat_message"

# 或用 Navicat / DB Browser for SQLite 打开
# 文件路径：项目根目录/data/customer_service.db
```

---

## 11. API 接口文档

### 对话接口

#### POST /api/chat

同步对话，返回完整回答。

**请求体：**
```json
{
  "message": "你们的退款政策是什么？",
  "sessionId": "可选，为空时自动生成",
  "model": "可选，预留字段"
}
```

**响应体：**
```json
{
  "answer": "我们提供7天无理由退款服务...",
  "sessionId": "uuid-string",
  "timestamp": "2026-05-14T10:30:00"
}
```

#### GET /api/chat/stream

SSE 流式对话，逐 Token 推送。

**参数：**
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| message | String | 是 | 用户消息 |
| sessionId | String | 否 | 会话 ID |
| model | String | 否 | 模型选择 |

**SSE 事件流：**
```
data: 我们
data: 提供7天
data: 无理由退款
data: [DONE]
```

### 认证接口

#### POST /api/auth/login

**请求体：**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**成功响应：**
```json
{
  "success": true,
  "username": "admin"
}
```
同时设置 `AUTH_TOKEN` Cookie（HttpOnly, 7天有效期）

**失败响应（401）：**
```json
{
  "success": false,
  "error": "用户名或密码错误"
}
```

#### POST /api/auth/logout

清除 Token 和 Cookie。

#### GET /api/auth/me

**成功响应：**
```json
{
  "username": "admin"
}
```

### 客诉闭环接口

#### POST /api/chat-history/resolve

用户确认问题已解决。

**请求体：**
```json
{
  "sessionId": "uuid-string"
}
```

**成功响应：**
```json
{
  "success": true
}
```

#### GET /api/chat-history/status

查询当前会话的客诉状态。

**参数：** `?sessionId=uuid-string`

**响应：**
```json
{
  "success": true,
  "complaintStatus": "未闭环",
  "transferredToHuman": false
}
```

### 知识库接口

#### GET /api/knowledge

列出知识库目录中的文件。

**响应：**
```json
[
  { "name": "company-info.md", "size": 1024, "lastModified": 1715000000000 },
  { "name": "product-faq.txt", "size": 2048, "lastModified": 1715000000000 }
]
```

#### GET /api/knowledge/{filename}

读取知识库文件内容。

#### POST /api/knowledge/reload

重新加载知识库到向量数据库。

**响应：**
```json
{
  "success": true,
  "count": 2
}
```

#### GET /api/health

**响应：**
```json
{
  "status": "ok",
  "service": "ai-customer-service"
}
```

---

## 12. 扩展与 TODO

### 已完成的功能

| 功能 | 说明 |
|---|---|
| RAG 知识库问答 | Chroma 向量数据库 + AllMiniLmL6V2 嵌入模型 |
| Tool Calling | 计算器 + 订单查询（从 orders.json 加载），含链式调用 |
| 流式对话 | SSE 实时推送 |
| 用户鉴权 | Guava Cache 管理 Token，7 天自动过期 |
| 对话记忆持久化 | SqliteChatMemoryStore，重启不丢失上下文 |
| 客诉闭环管理 | 自动记录、用户主动确认、转人工自动检测 |
| 知识库热加载 | `/api/knowledge/reload` 接口 |

### 后续规划

| 优先级 | 功能 | 说明 |
|---|---|---|
| 高 | 自动化测试 | 扩充常见真实客服问题库，模拟真实流量 |
| 中 | 兜底策略 | 在特定条件下自动转人工客服 |
| 中 | 数据统计 | 闭环率、转人工率、平均对话轮次统计 |
| 低 | 多模型支持 | Qwen、DeepSeek 等模型接入（前端已预留选项） |

### 扩展指南

**添加新的知识文档：**
1. 将 `.txt` 或 `.md` 文件放入 `knowledge/` 目录
2. 调用 `POST /api/knowledge/reload` 热加载，或重启应用

**添加新的 Tool：**
1. 在 `service/tool/` 下创建新类，使用 `@Component` + `@Tool` 注解
2. 在 `RagConfig.customerServiceAgent()` 中将新 Tool 传入 `.tools(...)` 方法
3. 在 `CustomerServiceAgent` 的 `@SystemMessage` 中补充使用指引

**切换向量存储：**
- 修改 `VectorStoreConfig` 中的 `EmbeddingStore` 实现
- `RagConfig` 中保留了 `InMemoryEmbeddingStore` 的注释代码可参考

**切换大模型：**
- 修改 `LangchainConfig` 中的模型构建逻辑，替换为对应的 Langchain4j 模型实现
- 修改 `application.yml` 中的模型配置
