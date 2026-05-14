# AI Customer Service — Code Wiki

> 基于 Spring Boot 3 + Langchain4j + 智谱 GLM + RAG 的智能客服系统

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
   - 5.8 [前端页面 (static)](#58-前端页面-static)
   - 5.9 [知识库 (knowledge)](#59-知识库-knowledge)
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
| 向量数据库 | Chroma（已接入）/ InMemory（已注释） |
| 嵌入模型 | AllMiniLmL6V2（本地 ONNX，离线运行） |
| 服务端口 | 8081 |

本项目是一个面向"智慧商城"电商场景的 AI 智能客服系统。核心能力包括：

- **RAG 知识库问答**：基于本地文档（.txt / .md）构建向量索引，检索增强生成
- **Tool Calling**：支持计算器工具和订单查询工具（含链式调用）
- **流式对话**：SSE 实时推送 LLM 生成内容
- **用户鉴权**：Cookie-based Token 认证，拦截器守卫
- **Web 聊天界面**：内嵌单页聊天 UI，支持快捷问题、打字动画

---

## 2. 技术栈与依赖

### Maven 依赖清单

| 依赖 | 用途 |
|---|---|
| `spring-boot-starter-web` | Web 框架，提供 REST API 与 SSE 支持 |
| `langchain4j-spring-boot-starter` | Langchain4j 与 Spring Boot 自动装配 |
| `langchain4j-zhipu-ai` | 智谱 GLM 大模型集成（Chat + Streaming） |
| `langchain4j-document-parser-apache-tika` | 文档解析（支持 txt/md 等格式） |
| `langchain4j-embeddings-all-minilm-l6-v2` | 本地 ONNX 嵌入模型，无需外部 API |
| `langchain4j-chroma` | Chroma 向量数据库客户端 |
| `spring-boot-starter-test` | 测试框架 |

### 外部服务依赖

| 服务 | 地址 | 说明 |
|---|---|---|
| 智谱 AI API | `https://open.bigmodel.cn` | 大模型推理服务，需 API Key |
| Chroma | `http://localhost:8000` | 向量数据库服务，需本地部署 |

---

## 3. 项目架构

```
┌─────────────────────────────────────────────────────────────┐
│                        浏览器 (前端)                          │
│  login.html ──登录──> Cookie AUTH_TOKEN ──> index.html       │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP / SSE
┌───────────────────────────▼─────────────────────────────────┐
│                    Spring Boot (port 8081)                    │
│                                                              │
│  ┌──────────────┐   ┌────────────────┐   ┌───────────────┐  │
│  │ AuthInterceptor│  │  AuthController │   │ ChatController│  │
│  │  (鉴权守卫)    │  │  (登录/登出)    │   │  (对话接口)   │  │
│  └──────────────┘   └───────┬────────┘   └───────┬───────┘  │
│                             │                    │           │
│                     ┌───────▼───────┐   ┌────────▼────────┐  │
│                     │  AuthService   │   │ LangchainChat   │  │
│                     │ (Token 管理)   │   │ Service         │  │
│                     └───────────────┘   └────────┬────────┘  │
│                                                  │           │
│                                    ┌─────────────▼────────┐  │
│                                    │ CustomerServiceAgent  │  │
│                                    │ (Langchain4j AiServices│  │
│                                    │  声明式 AI 代理)       │  │
│                                    └──┬──────────┬────────┘  │
│                                       │          │           │
│                           ┌───────────▼──┐  ┌────▼─────────┐ │
│                           │ ContentRetriever│ │ Tool Services│ │
│                           │ (RAG 检索)     │ │ ├ Calculator │ │
│                           └───────┬──────┘ │ └ OrderQuery │ │
│                                   │        └──────────────┘ │
│                          ┌────────▼─────────┐               │
│                          │ EmbeddingStore    │               │
│                          │ (Chroma / InMem)  │               │
│                          └────────┬──────────┘               │
│                                   │                          │
│  ┌────────────────────────────────▼──────────────────────┐   │
│  │              KnowledgeBaseInitializer                  │   │
│  │   (应用启动时加载 knowledge/ 目录文档 → 向量化入库)      │   │
│  └───────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
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
| RAG 层 | `com.ai.customerservice.rag` | 文档处理与知识库初始化 |

---

## 4. 目录结构

```
ai-customer-service/
├── pom.xml                                    # Maven 构建配置
├── README.md                                  # 项目说明
├── src/
│   └── main/
│       ├── java/com/ai/customerservice/
│       │   ├── AiCustomerServiceApplication.java     # Spring Boot 启动类
│       │   ├── config/
│       │   │   ├── AuthProperties.java               # 鉴权配置属性
│       │   │   ├── LangchainConfig.java              # LLM/Embedding 模型配置
│       │   │   ├── RagConfig.java                    # RAG 检索 & Agent 组装
│       │   │   ├── VectorStoreConfig.java            # 向量存储配置
│       │   │   └── WebConfig.java                    # CORS & 拦截器注册
│       │   ├── controller/
│       │   │   ├── AuthController.java               # 登录/登出/用户信息
│       │   │   └── ChatController.java               # 对话接口
│       │   ├── interceptor/
│       │   │   └── AuthInterceptor.java              # 鉴权拦截器
│       │   ├── model/
│       │   │   ├── ChatRequest.java                  # 对话请求 DTO
│       │   │   └── ChatResponse.java                 # 对话响应 DTO
│       │   ├── rag/
│       │   │   ├── DocumentProcessor.java            # 文档加载/分块/向量化
│       │   │   └── KnowledgeBaseInitializer.java     # 知识库启动初始化
│       │   └── service/
│       │       ├── AuthService.java                  # Token 认证服务
│       │       ├── ChatService.java                  # 对话服务接口
│       │       ├── CustomerServiceAgent.java         # AI 代理接口
│       │       ├── impl/
│       │       │   └── LangchainChatService.java     # 对话服务实现
│       │       └── tool/
│       │           ├── CalculatorService.java        # 计算器工具
│       │           └── OrderQueryService.java        # 订单查询工具
│       └── resources/
│           ├── application.yml                       # 应用配置
│           ├── knowledge/
│           │   ├── company-info.md                   # 公司信息知识
│           │   └── product-faq.txt                   # 产品 FAQ 知识
│           └── static/
│               ├── index.html                        # 聊天主页面
│               └── login.html                        # 登录页面
└── target/                                           # 构建输出
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

- 使用 `@ConfigurationProperties(prefix = "app.auth")` 绑定 YAML 中的用户列表
- 内部类 `User` 持有 `username` / `password` 字段
- 配置示例见 `application.yml` 中的 `app.auth.users`

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
| `customerServiceAgent` | 通过 `AiServices.builder()` 组装 AI 代理，注入模型、检索器、工具和聊天记忆 |

Agent 组装细节：
- `chatMemoryProvider`：每个 `memoryId`（即 sessionId）创建独立的 `MessageWindowChatMemory`，窗口大小 20 条
- `tools`：注册 `CalculatorService` 和 `OrderQueryService`
- `contentRetriever`：RAG 检索器，在对话时自动检索相关知识片段

#### VectorStoreConfig

**[VectorStoreConfig.java](src/main/java/com/ai/customerservice/config/VectorStoreConfig.java)**

当前激活的向量存储配置，创建 `ChromaEmbeddingStore` Bean，连接本地 Chroma 服务。

> 注：`RagConfig` 中保留了 `InMemoryEmbeddingStore` 的注释代码，可切换回内存模式。

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
| `POST /api/chat` | `chat(ChatRequest)` | 同步对话，返回完整回答 |
| `GET /api/chat/stream` | `chatStream(message, sessionId, model)` | SSE 流式对话，逐 Token 推送 |
| `GET /api/health` | `health()` | 健康检查 |

#### AuthController

**[AuthController.java](src/main/java/com/ai/customerservice/controller/AuthController.java)**

| 端点 | 方法 | 说明 |
|---|---|---|
| `POST /api/auth/login` | `login(username, password)` | 登录，成功后设置 Cookie |
| `POST /api/auth/logout` | `logout()` | 登出，清除 Token 与 Cookie |
| `GET /api/auth/me` | `me()` | 获取当前登录用户信息 |

---

### 5.4 拦截器层 (interceptor)

#### AuthInterceptor

**[AuthInterceptor.java](src/main/java/com/ai/customerservice/interceptor/AuthInterceptor.java)**

实现 `HandlerInterceptor.preHandle()`，鉴权逻辑：

1. 从 Cookie 中提取 `AUTH_TOKEN`
2. 调用 `AuthService.resolveUsername(token)` 验证 Token
3. **API 请求**（`/api/` 开头）：返回 401 JSON
4. **页面请求**：重定向到 `/login.html`

---

### 5.5 模型层 (model)

#### ChatRequest

**[ChatRequest.java](src/main/java/com/ai/customerservice/model/ChatRequest.java)**

| 字段 | 类型 | 说明 |
|---|---|---|
| `message` | `String` | 用户消息内容 |
| `sessionId` | `String` | 会话 ID（为空时自动生成 UUID） |
| `model` | `String` | 模型选择（预留字段，当前未使用） |

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

定义两个方法：
- `ChatResponse chat(ChatRequest)` — 同步对话
- `SseEmitter chatStream(ChatRequest)` — 流式对话

#### LangchainChatService (实现)

**[LangchainChatService.java](src/main/java/com/ai/customerservice/service/impl/LangchainChatService.java)**

核心对话服务实现：

- **`chat()`**：调用 `CustomerServiceAgent.chat(sessionId, message)`，返回完整回答
- **`chatStream()`**：
  1. 创建 `SseEmitter`（超时 300s）
  2. 调用 `CustomerServiceAgent.chatStream()` 获取 `TokenStream`
  3. 逐 Token 通过 SSE 推送
  4. 完成时发送 `[DONE]` 标记
- **`resolveSessionId()`**：sessionId 为空时自动生成 UUID

#### CustomerServiceAgent (AI 代理接口)

**[CustomerServiceAgent.java](src/main/java/com/ai/customerservice/service/CustomerServiceAgent.java)**

Langchain4j 声明式 AI 代理，使用 `@SystemMessage` 定义系统提示词：

```
你是"智慧商城"的智能客服助手。请根据以下规则回答用户的问题：
1. 优先使用知识库中的信息来回答用户的问题。
2. 当用户需要数学计算时，使用计算器工具进行精确计算。
3. 回答要准确、简洁、有条理。
4. 如果知识库中没有相关信息，请如实告知用户，并建议联系人工客服。
5. 保持友好、专业的语气。
6. 如果用户的问题不清楚，请礼貌地要求用户提供更多信息。
7. 如果用户查询订单状态为已发货，调用queryOrderLogistics方法
8. 如果用户要求转人工，则提供电话号码666666请用户联系该电话
```

关键注解：
- `@MemoryId String sessionId` — 会话记忆隔离
- `@UserMessage String userMessage` — 用户输入

两个方法：
- `String chat(...)` — 同步调用
- `TokenStream chatStream(...)` — 流式调用

#### AuthService

**[AuthService.java](src/main/java/com/ai/customerservice/service/AuthService.java)**

基于内存的 Token 认证服务：

| 常量/字段 | 值 | 说明 |
|---|---|---|
| `COOKIE_NAME` | `"AUTH_TOKEN"` | Cookie 名称 |
| `COOKIE_MAX_AGE_SECONDS` | `7 * 24 * 60 * 60`（7天） | Cookie 有效期 |
| `tokenToUsername` | `ConcurrentHashMap` | Token → 用户名映射 |

| 方法 | 说明 |
|---|---|
| `login(username, password)` | 校验凭据，生成 32 字节随机 Token（Base64 编码） |
| `resolveUsername(token)` | 根据 Token 查找用户名 |
| `logout(token)` | 移除 Token |

> 注意：当前 Token 存储在内存中，应用重启后所有 Token 失效。

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

模拟订单查询工具（当前返回硬编码数据）：

| 方法 | 功能 | 返回值 |
|---|---|---|
| `queryOrderStatus(orderId)` | 查询订单状态 | `"已发货"` |
| `queryOrderLogistics(orderId)` | 查询物流信息 | `"您的包裹待揽收"` |
| `hiThere(orderId)` | 待发货安抚 | `"小二正在加紧处理..."` |

> 链式调用设计：当 `queryOrderStatus` 返回"已发货"时，Agent 的 SystemMessage 指示其自动调用 `queryOrderLogistics`，实现 Tool 的链式调用。

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

1. 读取 `app.knowledge.path` 配置（默认 `classpath:knowledge/`）
2. 调用 `DocumentProcessor.processDocuments()` 加载所有知识文档
3. 日志输出加载文档数量

---

### 5.8 前端页面 (static)

#### index.html — 聊天主页面

**[index.html](src/main/resources/static/index.html)**

功能特性：
- 聊天气泡 UI（用户/AI 区分样式）
- SSE 流式接收（当前使用同步 `/api/chat` 接口）
- 打字动画指示器
- 快捷问题按钮（退款政策、配送时间、会员权益、联系客服）
- 模型选择下拉框（GLM / Qwen 待接入）
- 退出登录按钮
- 自动生成 sessionId
- Enter 发送 / Shift+Enter 换行

#### login.html — 登录页面

**[login.html](src/main/resources/static/login.html)**

功能特性：
- 用户名/密码表单
- 调用 `/api/auth/login` 接口
- 登录成功跳转 `/index.html`
- 错误信息展示

---

### 5.9 知识库 (knowledge)

#### company-info.md

公司基本信息，包括：
- 公司简介（智慧商城，2020年成立）
- 主营业务（智能家居、穿戴设备、数码配件）
- 联系方式（客服电话、邮箱、地址）
- 工作时间（客服、物流、售后）
- 服务承诺（正品保证、急速发货、无忧售后、隐私保护）

#### product-faq.txt

产品常见问题，Q&A 格式：
- 退款政策
- 配送时间
- 会员权益（普通/银卡/金卡）
- 产品保修期
- 支付方式
- 联系人工客服
- 修改/取消订单
- 优惠券使用

> 扩展方式：将新的 `.txt` 或 `.md` 文件放入 `src/main/resources/knowledge/` 目录，重启应用即可自动加载。

---

## 6. 核心数据流

### 6.1 同步对话流程

```
用户输入 → ChatController.chat()
         → LangchainChatService.chat()
         → CustomerServiceAgent.chat(sessionId, message)
              │
              ├─ [RAG] ContentRetriever 检索知识库相关片段
              ├─ [Tool] 判断是否需要调用 Calculator / OrderQuery
              ├─ [Memory] MessageWindowChatMemory 维护会话上下文
              │
         ← 返回 AI 回答
         ← ChatResponse (answer + sessionId + timestamp)
```

### 6.2 流式对话流程

```
用户输入 → ChatController.chatStream()
         → LangchainChatService.chatStream()
         → SseEmitter 创建
         → CustomerServiceAgent.chatStream(sessionId, message)
              │
              ├─ TokenStream.onNext(token) → SSE 推送
              ├─ TokenStream.onComplete()  → SSE [DONE]
              ├─ TokenStream.onError()     → SseEmitter.completeWithError()
              │
         ← SSE 事件流
```

### 6.3 知识库初始化流程

```
Spring Boot 启动完成
  → KnowledgeBaseInitializer.run()
    → DocumentProcessor.processDocuments("classpath:knowledge/*")
      → 扫描 knowledge/ 目录
      → 解析 .txt / .md 文件
      → 递归分块 (300字/块, 30字重叠)
      → AllMiniLmL6V2 向量化
      → 存入 Chroma EmbeddingStore
```

### 6.4 鉴权流程

```
请求到达 → AuthInterceptor.preHandle()
         → 从 Cookie 提取 AUTH_TOKEN
         → AuthService.resolveUsername(token)
              │
         ├─ 有效 → 放行
         ├─ 无效 + API请求 → 401 JSON
         └─ 无效 + 页面请求 → 重定向 /login.html
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
| `AuthService` | `@Service` | `AuthController`, `AuthInterceptor` |
| `AuthProperties` | `@Configuration` | `AuthService` |
| `DocumentProcessor` | `@Component` | `KnowledgeBaseInitializer` |

### 关键函数签名

```java
// CustomerServiceAgent — AI 代理
String chat(@MemoryId String sessionId, @UserMessage String userMessage)
TokenStream chatStream(@MemoryId String sessionId, @UserMessage String userMessage)

// ChatService — 对话服务接口
ChatResponse chat(ChatRequest request)
SseEmitter chatStream(ChatRequest request)

// AuthService — 认证服务
Optional<String> login(String username, String password)
Optional<String> resolveUsername(String token)
void logout(String token)

// DocumentProcessor — 文档处理
int processDocuments(String resourcePattern)

// CalculatorService — 计算器工具
double add(double a, double b)
double subtract(double a, double b)
double multiply(double a, double b)
String divide(double a, double b)
double power(double base, double exponent)
String sqrt(double number)

// OrderQueryService — 订单查询工具
String queryOrderStatus(String orderId)
String queryOrderLogistics(String orderId)
String hiThere(String orderId)
```

---

## 8. 依赖关系图

### 组件依赖

```
ChatController ──────→ ChatService (interface)
                           │
                           ▼
                    LangchainChatService ──→ CustomerServiceAgent (interface)
                                                    │
                     ┌──────────────────────────────┤
                     │                              │
                     ▼                              ▼
            EmbeddingStoreContentRetriever    Tool Services
                     │                    ├─ CalculatorService
                     ▼                    └─ OrderQueryService
            ┌────────┴────────┐
            ▼                 ▼
    EmbeddingStore      EmbeddingModel
    (Chroma)           (AllMiniLmL6V2)

AuthController ──→ AuthService ──→ AuthProperties
AuthInterceptor ──→ AuthService

KnowledgeBaseInitializer ──→ DocumentProcessor ──→ EmbeddingStore + EmbeddingModel
```

### Maven 依赖树（核心）

```
spring-boot-starter-web
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
```

---

## 9. 配置说明

### application.yml 完整配置项

```yaml
server:
  port: 8081                          # 服务端口

langchain4j:
  zhipu-ai:
    api-key: ${ZHIPU_API_KEY:xxx}     # 智谱 API Key（建议用环境变量）
    chat-model:
      model-name: glm-4-flash         # 同步模型名称
      temperature: 0.7                # 生成温度
      max-tokens: 2048                # 最大 Token 数
    streaming-chat-model:
      model-name: glm-4-flash         # 流式模型名称
      temperature: 0.7
      max-tokens: 2048

app:
  auth:
    users:                            # 用户列表（用户名/密码）
      - username: admin
        password: admin123
      - username: user
        password: user123
  knowledge:
    path: classpath:knowledge/        # 知识库文档路径
  rag:
    max-results: 3                    # RAG 检索最大返回条数
    min-score: 0.5                    # RAG 检索最低相似度阈值
    chunk-size: 300                   # 文档分块大小（字符数）
    chunk-overlap: 30                 # 分块重叠字符数
    chroma:
      url: http://localhost:8000      # Chroma 服务地址
      collection-name: customer_service_db  # Chroma 集合名
  chat:
    memory-max-messages: 20           # 会话记忆窗口大小
    sse-timeout: 300000               # SSE 连接超时（毫秒）
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

### Docker 部署

项目已成功在本地 Docker 环境部署，也已在腾讯云（2C2G 香港节点）部署运行。

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

### 已规划的功能

| 优先级 | 功能 | 说明 |
|---|---|---|
| 高 | 自动化测试 | 扩充常见真实客服问题库，模拟真实流量 |
| 高 | 兜底策略 | 在特定条件下自动转人工客服 |
| 中 | 数据库持久化 | 保存历史对话数据，统计问题闭环率和转人工率 |
| 中 | 专业向量库 | 已完成 Chroma 接入，替代原有内存向量库 |
| 低 | 多模型支持 | Qwen、DeepSeek 等模型接入（前端已预留选项） |

### 扩展指南

**添加新的知识文档：**
1. 将 `.txt` 或 `.md` 文件放入 `src/main/resources/knowledge/`
2. 重启应用，`KnowledgeBaseInitializer` 会自动加载

**添加新的 Tool：**
1. 在 `service/tool/` 下创建新类，使用 `@Component` + `@Tool` 注解
2. 在 `RagConfig.customerServiceAgent()` 中将新 Tool 传入 `.tools(...)` 方法
3. 在 `CustomerServiceAgent` 的 `@SystemMessage` 中补充使用指引

**切换向量存储：**
- 内存模式：取消 `RagConfig` 中 `InMemoryEmbeddingStore` 的注释，注释掉 `VectorStoreConfig`
- Chroma 模式：当前默认，需确保 Chroma 服务运行

**切换大模型：**
- 修改 `LangchainConfig` 中的模型构建逻辑，替换为对应的 Langchain4j 模型实现
- 修改 `application.yml` 中的模型配置
