# PHASE_1_CORE_PROMPT.md

## 0. 必读文件

开始开发前，必须按顺序阅读：

1. `AGENT.MD`
2. `PHASE_1_CORE_PROMPT.md`
3. `docs/FULL_REQUIREMENTS.md`

三者用途：

```text
AGENT.MD              = 长期架构护栏，防止架构走偏
PHASE_1_CORE_PROMPT.md         = 当前阶段执行任务，限制本轮只做 第一阶段核心闭环
docs/FULL_REQUIREMENTS.md = 完整产品蓝图，作为长期需求和细节参考
```

重要：

- `FULL_REQUIREMENTS.md` 必须阅读，但不能把其中所有长期功能都塞进本轮。
- 本轮只实现 第一阶段核心闭环。
- FULL_REQUIREMENTS.md 中超出 MVP 的内容，只能作为扩展点、字段预留、文档说明或后续规划。
- 如果三者冲突：`AGENT.MD` 优先，其次 `PHASE_1_CORE_PROMPT.md`，最后 `FULL_REQUIREMENTS.md`。

---

## Codex 当前任务

你现在在一个全新项目中工作，请严格按照根目录 `AGENT.MD` 的长期架构原则执行。

本轮不是一次性完成完整商业化 SaaS 平台，而是完成 **第一阶段核心闭环 核心闭环**。

请不要只写方案，不要只写伪代码，不要只留下 TODO。  
核心链路必须真实可运行，非核心功能可以保留清晰扩展点。

---

## 必须优先使用的 Skill / MCP / 插件

本阶段开发必须遵守 `AGENT.MD` 中的 Skill / MCP / 插件要求。

开始开发前，请先检查当前环境可用能力，并在执行过程中主动使用：

### 后端

必须优先使用：

- superpowers skill

用于：

- 后端架构设计
- relay-gateway 热路径审查
- platform-service 业务边界审查
- PostgreSQL 迁移脚本审查
- Redis Pub/Sub 事件链路审查
- 权限和租户隔离审查
- 测试设计和自检

### 前端

必须优先使用：

- impeccable skill
- taste skill

用于：

- UI 视觉设计
- 交互体验
- 信息架构
- 菜单命名
- Chat / Playground / Endpoint 测试页面体验
- 表单、表格、弹窗、状态反馈优化
- 避免老旧后台模板和明显 AI 味

### MCP

必须根据任务主动使用：

- Context7 MCP：查询技术文档和库用法
- Playwright MCP：打开页面验证 UI、交互、E2E
- 数据库 MCP：验证 PostgreSQL 迁移脚本和表结构
- Redis MCP：验证 Redis 缓存、Pub/Sub 频道、事件发布消费
- Docker / Kubernetes MCP：验证 Docker Compose、容器、日志和连通性
- GitHub / GitLab MCP：检查两个仓库、Git Submodule、PR、CI
- Figma MCP：如有设计稿则读取设计稿；没有设计稿则自行完成产品级 UI

如果某个 skill / MCP / 插件未安装：

- 不要中断任务
- 不要等待用户确认
- 继续完成开发
- 最终交付说明里列出未使用原因和建议安装项

禁止编造已经使用了实际未使用的工具。
禁止因为工具缺失就停止完成第一阶段核心闭环。

---

## 前端 UI 技术选型要求

当前阶段前端虽然只做核心页面，但 UI 框架和相关前端技术选型不能由 Claude Code 擅自决定。

在选择以下方案前，必须结合相关 skill / MCP / 插件评估：

- UI 组件库
- 样式方案
- 图标库
- 图表库
- 表格 / DataGrid 方案
- Markdown 渲染
- 代码高亮
- 表单校验
- 主题系统 / 暗色模式

必须优先使用：

- impeccable skill：评估信息架构、交互体验、组件体系
- taste skill：评估审美、视觉调性、是否有 AI 味或老旧后台味
- Context7 MCP：查询候选库最新文档、Vue 3 / TypeScript 兼容性
- Playwright MCP：页面实现后验证真实视觉和交互

前端技术选型完成后，必须创建：

```text
docs/frontend-ui-tech-decision.md
```

该文档必须说明：候选方案、评估维度、使用了哪些 skill / MCP / 插件、最终选择、选择理由、未选择其他方案的原因，以及对 Chat / Playground / Endpoint 测试页面的适配说明。

如果相关 skill / MCP 未安装，不得编造已经使用。必须继续开发，并在最终交付说明中列出未安装项、未确认项和影响范围。

---

## 前后端仓库要求

本项目必须拆成两个独立 Git 仓库：

```text
ai-gateway-backend      后端主仓库
ai-gateway-web-console  前端独立仓库
```

后端仓库作为主仓库，前端仓库通过 Git Submodule 挂载到后端项目根目录。

推荐路径：

```text
ai-gateway-backend/
  ai-common/
  ai-platform-service/
  ai-relay-gateway/
  frontend/
    web-console/        # Git Submodule，独立前端仓库
  deploy/
  docs/
```

强制要求：

- 前端代码必须是独立仓库。
- 后端代码必须是独立仓库。
- 前端作为后端仓库的 Git Submodule 维护。
- 不允许把前端源码作为普通目录直接提交到后端仓库。
- 后端仓库必须提交 `.gitmodules`。
- 后端 README / docs 必须说明如何拉取、初始化、更新子模块。
- 如果当前没有前端远程仓库地址，不要编造 URL，使用 `<FRONTEND_REPO_URL>` 占位，并生成清晰的替换说明。
- 如果 Codex 在本地初始化项目，可以先创建前后端两个独立本地 Git 仓库，再将前端仓库作为本地 submodule 加入后端仓库；如果环境不允许执行 git submodule，则至少生成 `.gitmodules` 模板和 `docs/repo-structure.md` 操作说明。

推荐命令：

```bash
git submodule add <FRONTEND_REPO_URL> frontend/web-console
git submodule update --init --recursive
```

克隆后端仓库时推荐：

```bash
git clone --recurse-submodules <BACKEND_REPO_URL>
```

---

## 一、项目目标

从 0 创建一个多租户运营型 AI API Gateway 平台的 MVP。

MVP 需要打通：

```text
用户 / 租户 / API Key
  -> Provider / Endpoint / ModelRoute
  -> OpenAI / Anthropic 中继入口
  -> UrlBuilder
  -> 上游请求
  -> SSE 流式基础转发
  -> Redis Pub/Sub 事件发布
  -> platform-service 事件消费
  -> PostgreSQL 调用日志 / 计费记录基础落库
  -> 前端 Chat / Playground / Endpoint 测试最小可用
```

---

## 二、固定技术要求

### 后端

优先使用：

- JDK 25
- Spring Boot 4
- PostgreSQL
- Redis
- Flyway 或 Liquibase
- MyBatis / MyBatis-Plus
- OpenAPI / Swagger
- Docker Compose

如果 JDK 25 + Spring Boot 4 依赖不稳定，允许降级为：

- JDK 21
- Spring Boot 3.5.x

但必须在最终说明中写清楚原因。

### relay-gateway

优先使用：

- Spring WebFlux
- Reactor Netty
- WebClient
- SSE 流式转发

relay-gateway 禁止：

- 热路径查 PostgreSQL
- 热路径使用 MyBatis
- 热路径同步落库
- 热路径做复杂计费
- 热路径做复杂报表统计

### platform-service

使用：

- Spring MVC 或普通 Spring Boot Web
- MyBatis / MyBatis-Plus
- PostgreSQL
- Redis Pub/Sub 订阅消费

### 前端

使用：

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia 或同等状态管理
- Playwright E2E

UI 框架不要由用户强制指定。  
你需要根据项目定位自行选择，并在最终说明中解释原因。

---

## 三、数据库要求

数据库固定使用 PostgreSQL。

请创建 PostgreSQL 迁移脚本，优先使用 Flyway：

```text
backend/ai-platform-service/src/main/resources/db/migration/
```

MVP 至少包含这些表：

### 用户 / 租户 / 权限

- sys_user
- sys_role
- sys_permission
- sys_user_role
- tenant

### API Key

- ai_api_key

要求：

- 不存明文 key
- 存 key_hash
- 存 key_prefix
- 只在创建时返回完整 key

### Provider / Endpoint / Route / Price

- ai_provider_config
- ai_provider_endpoint
- ai_model_route
- ai_model_price
- ai_endpoint_health_state

### 日志 / 计费 / 事件

- ai_request_log
- ai_billing_record
- ai_event_consume_log

要求：

- 所有表有 id、create_time、update_time
- 租户相关表有 tenant_id 或 owner_tenant_id
- 用户私有资源有 owner_user_id
- 金额字段用 numeric
- JSON 配置字段用 jsonb
- 常用字段加索引

---

## 四、Redis Pub/Sub 要求

当前 MVP 使用 Redis Pub/Sub 实现 relay-gateway 到 platform-service 的事件通知。

### relay-gateway 负责发布

relay-gateway 在请求生命周期中发布事件：

- RequestStartedEvent
- UpstreamSelectedEvent
- FirstTokenEvent
- RequestCompletedEvent
- RequestFailedEvent
- UsageReportedEvent
- FallbackTriggeredEvent
- RateLimitRejectedEvent

### platform-service 负责消费

platform-service 订阅 Redis 频道并消费事件，完成：

- 调用日志落库
- usage 统计基础落库
- 计费记录基础落库
- 健康统计扩展点
- 事件消费日志落库

### 推荐频道

```text
ai-gateway:event:request
ai-gateway:event:usage
ai-gateway:event:billing
ai-gateway:event:health
ai-gateway:event:config
```

### 事件 payload 最少字段

```json
{
  "eventId": "uuid",
  "eventType": "RequestCompletedEvent",
  "requestId": "req_xxx",
  "tenantId": "xxx",
  "userId": "xxx",
  "apiKeyId": "xxx",
  "modelAlias": "gpt-4o",
  "providerId": "xxx",
  "endpointId": "xxx",
  "upstreamModel": "gpt-4o",
  "inputTokens": 0,
  "outputTokens": 0,
  "totalTokens": 0,
  "upstreamCost": 0,
  "platformCharge": 0,
  "tenantCharge": 0,
  "latencyMs": 0,
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "occurredAt": "2026-01-01T00:00:00Z"
}
```

### 重要要求

- relay 发布 Redis 事件不能阻塞流式响应
- Redis 发布失败不能导致中继完全不可用
- 计费相关事件必须通过 eventId / requestId 幂等
- platform-service 消费后写 PostgreSQL
- 当前可先 Pub/Sub，后续保留 Redis Stream / Kafka 扩展点
- 不要把 Redis Pub/Sub 写死到业务代码深处，要通过 EventPublisher / EventConsumer 接口抽象

---

## 五、必须实现的后端模块

### ai-common

必须包含：

- ProtocolType
- ProviderType
- UpstreamOperation
- InternalChatRequest
- InternalChatMessage
- InternalChatResponse
- InternalChatChunk
- InternalUsage
- InternalToolDefinition
- InternalToolCall
- InternalToolResult
- InternalModelInfo
- InternalError
- ProviderEndpointSnapshot
- ResolvedModelRoute
- GatewayEvent
- RequestCompletedEvent
- RequestFailedEvent
- UsageReportedEvent

### ai-platform-service

必须实现：

- 登录 / 当前用户基础接口
- 租户基础 CRUD
- API Key 创建 / 查询 / 禁用
- ProviderConfig CRUD
- ProviderEndpoint CRUD
- ModelRoute CRUD
- ModelPrice 基础 CRUD
- Endpoint 测试接口
- Redis Pub/Sub 事件订阅消费
- RequestLog 落库
- BillingRecord 基础落库
- 配置发布接口，把快照写入 Redis

Endpoint 测试接口至少包括：

```text
POST /api/provider-endpoints/{id}/test-connectivity
POST /api/provider-endpoints/{id}/test-models
POST /api/provider-endpoints/{id}/test-chat
POST /api/provider-endpoints/{id}/test-stream
POST /api/provider-endpoints/{id}/save-as-route
```

要求：

- 前端不能直接拿到 Provider API Key
- 后端读取加密后的 Provider API Key
- 请求上游时注入密钥
- 返回 headers 必须脱敏
- 测试失败要返回清晰错误
- 测试请求必须有超时

### ai-relay-gateway

必须实现：

```text
GET /v1/models
POST /v1/chat/completions
POST /v1/messages
```

必须支持：

- OpenAI 请求解析
- Anthropic 请求解析
- OpenAI 非流式基础响应
- OpenAI 流式 SSE 基础响应
- Anthropic 非流式基础响应
- Anthropic 流式 SSE 基础响应
- API Key 鉴权
- 用户状态快照校验
- 租户状态快照校验
- ModelRoute 解析
- Endpoint 健康状态过滤
- UrlBuilder
- Redis Pub/Sub 异步事件发布

---

## 六、Provider / Endpoint / Protocol 设计要求

严禁错误设计：

```text
providerType = OPENAI，所以这个 provider 永远只能走 OpenAI 协议
```

严禁错误设计：

```text
一个 provider 只能有一个 endpoint
```

严禁错误设计：

```text
一个 endpoint 只能支持一种协议
```

正确设计：

```text
客户端协议 clientProtocol
  -> ProtocolAdapter 转 InternalChatRequest
  -> ModelRoute 决定 upstreamProtocol
  -> ProviderAdapterRegistry 根据 upstreamProtocol + endpoint.supportedProtocols 选择适配器
  -> 请求上游
  -> 再转换回客户端协议
```

必须支持三类上游端点形态：

1. 一个 Provider，多个 Endpoint，不同 Endpoint 支持不同协议
2. 一个 Provider，一个 Endpoint，同一个 Endpoint 支持多种协议
3. 一个 Provider，一个 Endpoint，只支持一种协议

必须支持跨协议路由：

- OpenAI 客户端 -> OpenAI 上游
- OpenAI 客户端 -> Anthropic 上游
- Anthropic 客户端 -> Anthropic 上游
- Anthropic 客户端 -> OpenAI 上游

---

## 七、UrlBuilder 要求

必须实现统一 UrlBuilder，输入：

- baseUrl
- pathPrefix
- upstreamProtocol
- operation

operation 至少包括：

- MODELS
- CHAT_COMPLETIONS
- MESSAGES
- RESPONSES
- EMBEDDINGS

必须避免：

- /v1/v1
- 漏 /v1
- 多斜杠
- pathPrefix 丢失
- OpenAI path 发到 Anthropic endpoint
- Anthropic path 发到 OpenAI endpoint

必须写单元测试覆盖：

```text
https://api.openai.com/v1 + CHAT_COMPLETIONS
https://api.anthropic.com + MESSAGES
https://api.deepseek.com/anthropic + MESSAGES
https://gateway.example.com + CHAT_COMPLETIONS
https://gateway.example.com + MESSAGES
```

---

## 八、前端 MVP 页面

只实现这些页面：

- 登录
- 动态菜单布局
- 概览
- Provider 管理
- Endpoint 管理
- ModelRoute 管理
- API Key 管理
- Endpoint 测试
- Chat
- Playground

其他菜单不要做复杂空页面。

前端要求：

- API 请求统一封装
- Token / 登录态统一管理
- 动态菜单根据后端返回权限渲染
- 表格支持搜索、分页
- 表单有校验
- 危险操作二次确认
- API Key 只在创建时展示完整值
- Endpoint 测试页面展示 URL、脱敏 headers、原始响应、错误信息、耗时
- Chat 支持 stream
- Playground 支持原始 JSON 和 curl 示例

---

## 九、测试要求

必须至少实现：

### 后端测试

- UrlBuilder 单元测试
- API Key hash / prefix 测试
- ModelRoute 解析测试
- Endpoint supportedProtocols 校验测试
- OpenAI -> Anthropic 路由结构测试
- Anthropic -> OpenAI 路由结构测试
- Redis Pub/Sub EventPublisher 测试
- EventConsumer 幂等消费测试

### 前端测试

- 登录页面
- 动态菜单
- API Key 创建
- Provider 创建
- Endpoint 创建
- ModelRoute 创建
- Endpoint 测试页面基础交互
- Chat 页面基础交互
- Playground 页面基础交互

### 文档

必须提供：

- README.md
- docs/architecture.md
- docs/api.md
- docs/deployment.md
- docs/redis-pubsub-events.md
- docs/relay-benchmark.md

---

## 十、项目输出结构

建议结构：

```text
ai-gateway-backend/
  AGENT.MD
  PHASE_1_CORE_PROMPT.md
  README.md
  .gitmodules

  ai-common/
  ai-relay-gateway/
  ai-platform-service/

  frontend/
    web-console/      # Git Submodule -> ai-gateway-web-console

  deploy/
    docker-compose.yml
    postgres/
    redis/

  docs/
    FULL_REQUIREMENTS.md
    architecture.md
    api.md
    deployment.md
    redis-pubsub-events.md
    relay-benchmark.md
    repo-structure.md

ai-gateway-web-console/
  package.json
  vite.config.ts
  src/
  tests/
  README.md
```

如果你认为有更合理结构，可以调整，但必须说明原因。

---

## 十一、执行顺序

请按顺序执行：

1. 阅读 `AGENT.MD`
2. 初始化项目结构
3. 确定最终可运行技术栈
4. 创建 Docker Compose：PostgreSQL + Redis
5. 创建 PostgreSQL 迁移脚本
6. 实现 ai-common
7. 实现 ai-platform-service 基础接口
8. 实现 Redis Pub/Sub 消费
9. 实现 ai-relay-gateway
10. 实现 Redis Pub/Sub 发布
11. 实现 Provider / Endpoint / Protocol / ModelRoute
12. 实现 UrlBuilder 和测试
13. 实现 Endpoint 测试接口
14. 实现 Vue 前端 MVP 页面
15. 对接真实后端接口
16. 编写测试
17. 编写文档
18. 确认项目能启动、能编译、能跑最小测试
19. 输出最终交付说明

---

## 十二、最终交付说明必须包含

完成后输出：

- 项目结构说明
- 最终技术栈选择说明
- 如果降级 JDK / Spring Boot，说明原因
- 两个后端服务职责说明
- PostgreSQL 表说明
- Redis 用途说明
- Redis Pub/Sub 事件说明
- Provider / Endpoint / Protocol 适配说明
- 三类上游端点形态适配说明
- UrlBuilder 说明
- relay 热路径说明
- 事件流说明
- Chat 页面说明
- Playground 页面说明
- Endpoint 测试页面说明
- 本地启动说明
- 测试说明
- 1000 QPS 流式请求压测方案
- 已知风险
- 后续优化建议

---

## 十三、不要做的事

不要：

- 把 relay-gateway 做成后台管理服务
- 在 relay 热路径查 PostgreSQL
- 在 relay 热路径同步写日志 / 账单
- 把 Provider 和协议绑定死
- 把 Endpoint 和协议绑定死
- 写死 OpenAI / Anthropic 分支导致未来无法扩展
- 前端直接暴露上游 API Key
- 为了覆盖菜单生成大量空页面
- 只写方案不写代码
- 生成无法编译的项目
- 留下核心链路 TODO


---

## ai-common 模块边界硬约束

`ai-common` 只是公共代码模块 / 公共依赖包，不是后端服务。

后端可运行核心服务只有两个：

```text
ai-relay-gateway
ai-platform-service
```

`ai-common` 只能用于放置两个服务共享的：

- enum
- DTO
- VO
- protocol model
- InternalChatRequest / InternalChatResponse
- InternalUsage
- GatewayEvent
- ProviderEndpointSnapshot
- ResolvedModelRoute
- UrlBuilder 相关公共类型
- 通用异常和工具类
- 常量定义
- 通用序列化配置

`ai-common` 禁止包含：

- Spring Boot 启动类
- Controller
- Web 路由
- 定时任务入口
- 独立部署配置
- Dockerfile
- 独立端口
- 独立数据库连接配置
- 独立 Redis 连接配置
- 独立业务 Service
- 需要单独启动的任何代码

禁止把 `ai-common` 设计成第三个服务。

正确理解：

```text
ai-common             = 公共依赖模块，不部署，不启动
ai-relay-gateway      = 可运行服务 1，中继热路径
ai-platform-service   = 可运行服务 2，平台业务服务
```

如果构建工具使用 Maven / Gradle 多模块，`ai-common` 应作为 library module 被 `ai-relay-gateway` 和 `ai-platform-service` 依赖。

最终交付说明中必须明确：

- 后端只有两个可运行核心服务
- ai-common 不是服务
- ai-common 不需要独立启动
- ai-common 不需要独立部署
