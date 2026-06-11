# Codex 开发提示词：多租户运营型 AI API Gateway 新项目

你现在扮演资深后端架构师、高并发网关工程师、大模型 API 协议适配专家、SaaS 多租户平台产品经理、前端架构师、UI/UX 设计师、全栈工程负责人。

这是一个**全新项目**，不是旧项目改造。请从 0 开始完成一个“多租户运营型 AI API Gateway 平台”的前后端设计和代码开发。不要只写方案，不要只写伪代码，不要只留 TODO。请直接创建项目结构、后端服务、前端应用、数据库迁移、接口、页面、权限、测试和启动文档。

除非涉及真实生产密钥、真实付费密钥、删除生产数据、访问生产环境等危险操作，否则不要反复让我确认，自己判断并继续完成。

---

## 1. 必须使用的能力和工具

后端设计、编码、架构拆分、测试、自检必须使用 **superpowers skill**，确保后端架构清晰、边界正确、代码可维护、测试可运行。

前端 UI、交互、信息架构、视觉质量必须使用 **impeccable skill** 和 **taste skill**。前端不要做老旧后台模板风格，要做现代化、专业、清晰、适合 AI API 平台的界面。

请主动使用适合当前任务的 MCP：

- **Context7 MCP**：查询 Spring Boot 4、JDK 25、WebFlux、Reactor Netty、MyBatis、Redis、MQ、Vue 3、Vite、状态管理、UI 库、图表库等最新文档。
- **Playwright MCP**：打开前端页面，验证 UI、权限菜单、表单、表格、Chat、Playground、Endpoint 测试、E2E 流程。
- **数据库 MCP**：设计表结构、验证 SQL、检查迁移脚本。
- **Redis MCP**：验证缓存、限流、健康状态、配置快照。
- **Docker / Kubernetes MCP**：生成和验证本地部署、容器日志、服务连通性。
- **GitHub / GitLab MCP**：如项目接入仓库，用于提交、PR、代码审查。
- **Figma MCP**：如果存在设计稿则使用；没有设计稿则自行完成产品级 UI 设计。

如果某个 MCP 或 skill 未安装，不要中断任务，在最终说明里列出建议安装项并继续完成。

---

## 2. 项目定位

本项目是**多租户运营型 AI API Gateway 平台**，不是普通大模型转发工具。

平台方是最高级运营方，可以创建、管理、停用租户，查看租户调用日志、运营数据、收入、成本、毛利、健康状态、风控等。

这里的“租户”不是普通企业客户，而是购买平台运营权的二级运营主体 / 子平台代理商 / SaaS Operator。租户可以通过平台赚钱，拥有自己的用户，可以配置自己的上游 Provider、ProviderEndpoint、模型路由、模型价格、API Key、限流、额度、品牌信息、接入文档等。

平台也可以直接运营自己的 C 端用户。

系统层级：

```text
平台方 Super Platform
  ├─ 平台 C 端用户 Platform Customer
  │    ├─ API Key
  │    ├─ Chat
  │    ├─ Playground
  │    ├─ 调用日志
  │    └─ 账单 / 充值
  │
  └─ 租户运营方 Tenant Operator
       ├─ 租户成员 Tenant Member
       └─ 租户 C 端用户 Tenant Customer
            ├─ API Key
            ├─ Chat
            ├─ Playground
            ├─ 调用日志
            └─ 账单 / 充值
```

注意：

- 平台管理员、租户管理员、普通用户，本质上都可以是同一个用户体系里的用户账号。
- 用户通过角色、权限、所属租户决定能看到什么菜单、能操作什么资源。
- 平台管理员和租户管理员也可以作为普通用户创建 API Key、充值、调用模型、使用 Chat、使用 Playground、查看自己的用量和账单。

---

## 3. 总体架构

后端只拆成两个核心服务。

### 3.1 ai-relay-gateway

专门负责高性能模型端点中继，是请求热路径服务。

职责：

- OpenAI Compatible API
- Anthropic Compatible API
- Claude Code 接入
- OpenAI SDK、Cursor、Cline、Continue、自研客户端接入
- API Key 快速鉴权
- 用户状态快速校验
- 租户状态快速校验
- 模型别名解析
- ProviderEndpoint 路由选择
- Endpoint 健康状态过滤
- 限流快速判断
- 流式 SSE 中继
- 协议转换
- tools / tool_calls / tool_use / tool_result 转换
- 上游 fallback
- 本地缓存 / Redis 缓存
- 异步发送调用事件给 ai-platform-service

ai-relay-gateway 禁止做：

- 后台管理接口
- 页面复杂查询
- 日志分页查询
- 账单分页查询
- 报表统计
- 复杂计费计算
- 指标聚合
- 健康巡检定时任务
- 大文本审计落库
- 同步写 request_log
- 同步写 billing_record
- 每次请求查数据库 / MyBatis

热路径数据必须来自：

- 本地缓存
- Redis
- 配置快照
- 启动加载的只读配置
- 配置变更事件刷新

包括：

- API Key 快照
- 用户状态
- 租户状态
- allowedModels
- ModelRoute
- ProviderEndpoint
- EndpointHealthState
- RateLimitConfig
- QuotaSnapshot
- PriceSnapshot
- RouteSnapshot

### 3.2 ai-platform-service

承接除中继热路径外的所有平台业务。

职责：

- 用户体系
- 登录认证
- 菜单权限
- 平台后台接口
- 租户后台接口
- 用户控制台接口
- 租户管理
- 租户成员管理
- 租户用户管理
- API Key 管理
- Provider 管理
- ProviderEndpoint 管理
- Endpoint 测试
- ModelRoute 管理
- 模型价格管理
- 余额和充值
- 双层计费
- 调用日志
- 指标聚合
- 健康检测
- 通道剔除
- 风控
- 配置发布
- 事件消费
- 审计日志
- 页面查询接口

---

## 4. 技术栈要求

后端基础：

- JDK 25
- Spring Boot 4
- MyBatis / MyBatis-Plus
- Redis
- 数据库优先 PostgreSQL 或 MySQL，选择一种并保持一致
- Flyway 或 Liquibase
- OpenAPI / Swagger
- Docker Compose 本地环境

ai-platform-service 可以使用 Spring Boot 4 + MyBatis + 传统 Web MVC。

relay-gateway 技术栈必须评估，目标是支撑至少 **1000 QPS 流式请求**。注意流式 1000 QPS 不是普通短请求，真实并发可能接近：

```text
并发连接数 ≈ QPS × 平均流式持续时间
```

必须评估并给出结论：

- Spring Boot 4 + 虚拟线程是否足够
- Servlet / MVC / 虚拟线程在大量 SSE 长连接下的风险
- WebFlux / Reactor Netty 对高并发流式中继的优势
- Vert.x / Netty 独立实现的收益和改造成本
- 是否优先实现 Spring WebFlux / Reactor Netty 版 relay-gateway
- 连接池、超时、背压、SSE、上游流式读取、客户端断开释放如何处理

推荐原则：

- ai-platform-service 使用 Spring Boot 4 + MyBatis
- ai-relay-gateway 优先考虑 Spring Boot 4 + WebFlux / Reactor Netty
- 如果判断虚拟线程足够，也必须保证 relay 热路径无数据库查询、无同步落库、无复杂业务逻辑
- 最终必须提供 1000 QPS 流式压测方案

前端必须使用：

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia 或同等状态管理
- 统一 API 请求封装
- Playwright E2E

不要强制指定具体 UI 框架。请结合项目定位、页面复杂度、审美要求、可维护性，以及 impeccable / taste / Context7 / Playwright，自行选择合适的 UI 组件库、图表库、表格方案和交互方案，并在最终交付说明中解释选择原因。

---

## 5. 用户、租户、权限模型

用户统一建模，通过角色、权限、租户归属决定能力。

用户来源：

- 平台管理员：`PLATFORM_ADMIN`
- 平台 C 端用户：`PLATFORM_CUSTOMER`
- 租户成员：`TENANT_MEMBER`
- 租户 C 端用户：`TENANT_CUSTOMER`

建议用户归属字段：

- user_id
- username
- email
- phone
- user_type：PLATFORM_ADMIN / PLATFORM_CUSTOMER / TENANT_MEMBER / TENANT_CUSTOMER
- tenant_id，可为空
- owner_type：PLATFORM / TENANT
- owner_tenant_id，可为空
- status
- create_time
- update_time

规则：

- 平台管理员：owner_type=PLATFORM，user_type=PLATFORM_ADMIN
- 平台 C 端用户：owner_type=PLATFORM，user_type=PLATFORM_CUSTOMER
- 租户成员：owner_type=TENANT，owner_tenant_id=租户ID，user_type=TENANT_MEMBER
- 租户 C 端用户：owner_type=TENANT，owner_tenant_id=租户ID，user_type=TENANT_CUSTOMER

平台侧角色：

- PLATFORM_SUPER_ADMIN
- PLATFORM_ADMIN
- PLATFORM_FINANCE
- PLATFORM_OPERATION
- PLATFORM_SUPPORT
- PLATFORM_VIEWER

租户侧角色：

- TENANT_OWNER
- TENANT_ADMIN
- TENANT_FINANCE
- TENANT_DEVELOPER
- TENANT_SUPPORT
- TENANT_VIEWER

普通用户角色：

- USER_OWNER
- USER_DEVELOPER
- USER_VIEWER

后端必须提供：

- 登录
- 退出
- 当前用户信息
- 当前用户角色
- 当前用户权限
- 当前用户菜单
- 当前用户所属租户
- 当前用户可操作资源范围

后端接口必须做权限校验，不能只靠前端隐藏菜单。

---

## 6. 前端：一个 Vue 应用，动态菜单

前端只做一个应用：

- 一个登录入口
- 一个统一布局
- 一个统一路由系统
- 根据用户角色、权限、租户身份动态显示菜单

平台管理员、租户管理员、平台 C 端用户、租户 C 端用户共用同一套前端，只是菜单和权限不同。

基础页面：

- 登录
- 注册，如需要
- 找回密码，如需要
- 当前用户中心
- 账户设置
- 403
- 404

普通用户能力菜单：

- 我的概览
- Chat
- Playground
- 我的 API Key
- 可用模型
- 接入指南
- 我的调用日志
- 我的用量统计
- 我的账单费用
- 我的充值
- 我的限额预算
- 我的告警通知
- 我的 Webhook
- 账户设置

平台管理菜单：

- 平台总览
- 租户管理
- 租户套餐
- 租户额度
- 租户结算价
- 平台用户管理
- 平台 C 端用户管理
- 全局 Provider 管理
- 全局 Endpoint 管理
- Endpoint 测试
- 全局模型路由
- 全局价格管理
- 全局 API Key
- 全局调用日志
- 全局账单财务
- 全局指标监控
- 健康检测
- 通道剔除
- 风控管理
- 配置发布
- 事件队列
- 审计日志
- 系统设置

租户管理菜单：

- 租户概览
- 租户用户管理
- 租户成员管理
- 租户 Provider
- 租户 Endpoint
- 租户 Endpoint 测试
- 租户模型路由
- 租户模型定价
- 租户 API Key
- 租户调用日志
- 租户账单
- 租户财务
- 租户用量统计
- 租户限流配置
- 租户额度配置
- 租户品牌配置
- 租户接入文档配置
- 租户告警通知
- 租户操作日志

前端交互要求：

- 所有表格有搜索、筛选、分页、排序
- 所有表单有校验
- 危险操作二次确认
- API Key 只在创建时展示完整值，后续只展示 prefix
- 接入指南代码块可复制
- 调用日志可查看详情
- 账单能区分上游成本、平台收费、租户收费、毛利
- 健康状态展示 HEALTHY、DEGRADED、UNHEALTHY、RECOVERING、DISABLED
- Endpoint 页面展示 supportedProtocols
- ModelRoute 页面展示 upstreamProtocol 和 clientProtocolSupport
- 使用 impeccable 和 taste 检查页面质量
- 使用 Playwright MCP 打开页面验证交互

---

## 7. Chat 页面

必须实现 Chat 页面，用于普通用户、平台管理员、租户管理员直接测试和使用模型，类似轻量版 ChatGPT。

功能：

- 选择模型别名
- 新建会话
- 会话列表
- 多轮对话
- stream 流式输出
- 停止生成
- 重新生成
- 清空会话
- 复制消息
- Markdown 渲染
- 代码高亮
- system prompt，可折叠
- temperature、max_tokens、top_p 等参数
- 展示 requestId
- 展示输入 tokens、输出 tokens、总费用、耗时、首 token 延迟
- 展示调用失败原因
- 支持选择 OpenAI / Anthropic 协议测试，默认可自动根据模型能力选择
- 支持使用当前登录用户自己的 API Key 或平台内部临时调试凭证，具体由后端权限控制

Chat 页面调用后端接口时不要绕过权限。后端必须校验当前用户是否有权限使用该模型。

---

## 8. Playground 页面

Playground 偏 API 调试，Chat 偏真实聊天体验。

Playground 必须支持：

- 原始 messages 编辑
- JSON 参数编辑
- stream 开关
- tools / tool_calls 测试
- OpenAI / Anthropic 协议切换
- 原始请求预览
- 原始响应查看
- curl 生成
- SDK 示例生成
- requestId、tokens、费用、耗时展示

---

## 9. Provider / Endpoint / Protocol 适配原则

必须支持不同上游厂商的多种端点形态，不能把 Provider、Endpoint、Protocol 绑定死。

Provider 只表示厂商或服务商，例如：openai、anthropic、deepseek、openrouter、qwen、gemini、litellm、one-api、internal-gateway。

Endpoint 表示具体访问地址和能力。

Protocol 表示请求上游时实际使用的协议。

系统必须同时适配以下三类上游。

### 9.1 一个 Provider，多个 Endpoint，不同 Endpoint 支持不同协议

某些厂商会分别提供 OpenAI Compatible Endpoint 和 Anthropic Compatible Endpoint。

示例：

```text
Provider: deepseek

Endpoint A:
name=deepseek-openai
baseUrl=https://api.deepseek.com
supportedProtocols=[OPENAI]
defaultProtocol=OPENAI

Endpoint B:
name=deepseek-anthropic
baseUrl=https://api.deepseek.com/anthropic
supportedProtocols=[ANTHROPIC]
defaultProtocol=ANTHROPIC
```

### 9.2 一个 Provider，一个 Endpoint，同一个 Endpoint 支持多种协议

例如 OpenRouter、LiteLLM、One API、企业内部统一 AI Gateway 等可能通过一个 baseUrl 同时支持多种协议。

示例：

```text
Provider: openrouter

Endpoint:
name=openrouter-main
baseUrl=https://openrouter.ai/api
supportedProtocols=[OPENAI, ANTHROPIC]
defaultProtocol=OPENAI
```

或：

```text
Provider: internal-gateway

Endpoint:
name=internal-gateway-main
baseUrl=https://gateway.example.com
supportedProtocols=[OPENAI, ANTHROPIC, RESPONSES, EMBEDDINGS]
defaultProtocol=OPENAI
```

### 9.3 一个 Provider，一个 Endpoint，只支持一种协议

例如 OpenAI 官方通常只支持 OpenAI 协议，Anthropic 官方通常只支持 Anthropic 协议。

示例：

```text
Provider: openai

Endpoint:
name=openai-main
baseUrl=https://api.openai.com/v1
supportedProtocols=[OPENAI]
defaultProtocol=OPENAI
```

```text
Provider: anthropic

Endpoint:
name=anthropic-main
baseUrl=https://api.anthropic.com
supportedProtocols=[ANTHROPIC]
defaultProtocol=ANTHROPIC
```

### 9.4 设计要求

Endpoint 必须包含：

- endpointId
- providerId
- endpointName
- baseUrl
- supportedProtocols
- defaultProtocol
- pathPrefix，可选
- authType
- apiKeyEncrypted
- extraHeaders
- timeoutMs
- connectTimeoutMs
- readTimeoutMs
- maxRetries
- priority
- weight
- enabled
- healthStatus

ModelRoute 决定某个模型别名实际请求上游时使用哪个协议。

ModelRoute 必须包含：

- modelAlias
- providerId
- endpointId
- upstreamModel
- upstreamProtocol
- clientProtocolSupport
- priority
- weight
- fallbackGroup
- enabled

含义：

```text
supportedProtocols = 这个 Endpoint 客观支持哪些协议
upstreamProtocol = 本次路由请求上游时实际使用哪个协议
clientProtocolSupport = 客户端允许用哪些协议访问这个平台模型别名
```

### 9.5 禁止错误设计

禁止：

```text
providerType = OPENAI
然后认为这个 provider 永远只能走 OpenAI 协议
```

禁止：

```text
一个 provider 只能有一个 endpoint
```

禁止：

```text
一个 endpoint 只能支持一种协议
```

禁止：

```text
客户端用 OpenAI 协议，就强制上游也只能用 OpenAI 协议
```

正确设计是：

```text
客户端协议 clientProtocol
  -> ProtocolAdapter 转 InternalChatRequest
  -> ModelRoute 决定 upstreamProtocol
  -> ProviderAdapterRegistry 根据 upstreamProtocol + endpoint capability 选择适配器
  -> 请求上游
  -> 再转换回客户端协议
```

### 9.6 跨协议路由必须支持

必须支持：

```text
OpenAI 客户端 -> OpenAI 上游
OpenAI 客户端 -> Anthropic 上游
Anthropic 客户端 -> Anthropic 上游
Anthropic 客户端 -> OpenAI 上游
OpenAI 客户端 -> 多协议 Endpoint 上游
Anthropic 客户端 -> 多协议 Endpoint 上游
```

例如：

```text
客户端请求：
POST /v1/chat/completions
model=claude-sonnet

ModelRoute：
endpoint=openrouter-main
upstreamProtocol=ANTHROPIC
upstreamModel=anthropic/claude-sonnet-4

处理：
OpenAI 请求 -> InternalChatRequest -> Anthropic 上游 -> OpenAI 响应
```

---

## 10. Endpoint 测试页面

必须实现上游厂商 API Endpoint 测试页面，给平台管理员和租户管理员使用。

功能：

- 选择 Provider
- 选择 ProviderEndpoint
- 显示 baseUrl
- 显示 supportedProtocols
- 选择测试协议：OPENAI / ANTHROPIC / GEMINI / RESPONSES / EMBEDDINGS
- 选择测试 operation：MODELS / CHAT_COMPLETIONS / MESSAGES / EMBEDDINGS / RESPONSES
- 输入 upstreamModel
- 输入测试 prompt
- 设置 stream=true/false
- 设置 timeoutMs
- 设置 maxTokens
- 测试连通性
- 拉取模型列表
- 测试非流式响应
- 测试流式响应
- 测试 tools 能力
- 查看原始请求 URL
- 查看请求 headers，密钥必须脱敏
- 查看原始响应
- 查看 HTTP 状态码
- 查看错误信息
- 查看耗时
- 查看首 token 延迟
- 查看 token usage
- 一键保存为 ModelRoute
- 一键标记 endpoint 可用 / 不可用，需权限控制
- 一键复制 curl

Endpoint 测试页面必须覆盖三类情况：

1. 单 Provider 多 Endpoint 多协议
2. 单 Endpoint 多协议
3. 单 Endpoint 单协议

测试时必须可以选择：Endpoint、Protocol、Operation、upstreamModel、stream=true/false。

并展示最终请求 URL、脱敏 headers、原始响应、错误信息、耗时、首 token 延迟、usage。

ai-platform-service 需要提供 Endpoint 测试接口，不要让前端直接暴露上游密钥：

- POST /api/provider-endpoints/{id}/test-connectivity
- POST /api/provider-endpoints/{id}/test-models
- POST /api/provider-endpoints/{id}/test-chat
- POST /api/provider-endpoints/{id}/test-stream
- POST /api/provider-endpoints/{id}/test-tools
- POST /api/provider-endpoints/{id}/save-as-route

要求：

- 后端读取加密存储的 Provider API Key
- 请求上游时注入密钥
- 返回给前端的 header 必须脱敏
- 测试结果记录操作日志
- 租户管理员只能测试自己租户的 Endpoint
- 平台管理员可以测试平台 Endpoint 和租户 Endpoint
- 测试失败要返回清晰错误
- 测试请求要有超时限制，避免拖垮服务

---

## 11. 双层计费

必须支持平台和租户双层计费。

租户 C 端用户场景：

```text
上游成本：0.8
平台给租户结算价：1.0
租户给用户售价：1.5

平台毛利 = 1.0 - 0.8
租户毛利 = 1.5 - 1.0
```

平台 C 端用户直接使用平台模型时：

```text
上游成本 -> 平台向平台 C 端用户收费
```

此时：

- tenantId 可以为空，或使用 platform tenant
- tenantCharge = platformCharge
- tenantProfit = 0 或不适用
- platformProfit = platformCharge - upstreamCost

账单记录必须包含：requestId、tenantId、userId、apiKeyId、modelAlias、providerId、endpointId、upstreamModel、inputTokens、outputTokens、totalTokens、upstreamCost、platformCharge、tenantCharge、platformProfit、tenantProfit、billingStatus。

平台管理员可以看全局收入、成本、毛利。

租户管理员可以看自己租户收入、成本、毛利。

普通用户只能看自己的消费、余额、账单。

---

## 12. Provider / Endpoint / ModelRoute 字段

租户可以使用平台提供的 Provider，也可以配置自己的 Provider。

ProviderConfig：ownerType、ownerTenantId、providerName、providerType、displayName、enabled。

ProviderEndpoint：ownerType、ownerTenantId、providerId、endpointName、baseUrl、supportedProtocols、defaultProtocol、authType、apiKeyEncrypted、extraHeaders、pathPrefix、timeoutMs、connectTimeoutMs、readTimeoutMs、maxRetries、priority、weight、enabled、healthStatus。

ModelRoute：ownerType、tenantId、modelAlias、providerId、endpointId、upstreamModel、upstreamProtocol、clientProtocolSupport、priority、weight、fallbackGroup、supportStream、supportTools、supportVision、supportReasoning、maxInputTokens、maxOutputTokens、priceConfigId、enabled。

路由优先级：

```text
用户专属路由 > 租户自定义路由 > 平台分配给租户的路由 > 平台默认路由
```

---

## 13. 中继协议

ai-relay-gateway 必须支持：

OpenAI Compatible：

- GET /v1/models
- POST /v1/chat/completions
- stream=false
- stream=true
- tools
- tool_calls
- usage
- OpenAI SSE
- OpenAI 错误格式

Anthropic Compatible：

- GET /v1/models
- POST /v1/messages
- stream=false
- stream=true
- tools
- tool_use
- tool_result
- Anthropic SSE
- Anthropic 错误格式

Claude Code 接入：

```bash
ANTHROPIC_BASE_URL=https://你的网关域名
ANTHROPIC_AUTH_TOKEN=用户自己的 sk-xxxx
claude
```

---

## 14. 内部统一协议模型

必须实现：InternalChatRequest、InternalChatMessage、InternalChatResponse、InternalChatChunk、InternalUsage、InternalToolDefinition、InternalToolCall、InternalToolResult、InternalModelInfo、InternalError、ProtocolType、ProviderType、ProviderEndpoint、ResolvedModelRoute、UpstreamOperation。

处理链路：

```text
OpenAI Client
-> OpenAIProtocolAdapter
-> InternalChatRequest
-> Auth / RateLimit / Route
-> ProviderAdapterRegistry
-> ProviderAdapter
-> 上游
-> InternalChatResponse / InternalChatChunk
-> OpenAIProtocolAdapter
-> OpenAI Response

Anthropic Client
-> AnthropicProtocolAdapter
-> InternalChatRequest
-> Auth / RateLimit / Route
-> ProviderAdapterRegistry
-> ProviderAdapter
-> 上游
-> InternalChatResponse / InternalChatChunk
-> AnthropicProtocolAdapter
-> Anthropic Response
```

ProviderAdapter 选择不能只看 providerName，必须根据 upstreamProtocol、providerType、endpoint.supportedProtocols、route config。

必须实现可靠 UrlBuilder。

UrlBuilder 输入：baseUrl、pathPrefix、upstreamProtocol、operation。

operation 至少包括：MODELS、CHAT_COMPLETIONS、MESSAGES、RESPONSES、EMBEDDINGS。

必须兼容：

```text
baseUrl=https://api.openai.com/v1
operation=CHAT_COMPLETIONS
=> https://api.openai.com/v1/chat/completions

baseUrl=https://api.anthropic.com
operation=MESSAGES
=> https://api.anthropic.com/v1/messages

baseUrl=https://api.deepseek.com/anthropic
operation=MESSAGES
=> https://api.deepseek.com/anthropic/v1/messages，或根据 endpoint.pathPrefix / overridePath 配置生成正确地址

baseUrl=https://gateway.example.com
operation=CHAT_COMPLETIONS
=> https://gateway.example.com/v1/chat/completions

baseUrl=https://gateway.example.com
operation=MESSAGES
=> https://gateway.example.com/v1/messages
```

必须避免：/v1/v1、漏 /v1、多斜杠、错误把 /anthropic 拼丢、错误把 OpenAI path 发到 Anthropic endpoint。

---

## 15. 健康检测和通道剔除

健康检测由 ai-platform-service 负责，中继服务只读取健康状态并过滤。

状态：HEALTHY、DEGRADED、UNHEALTHY、RECOVERING、DISABLED。

ai-platform-service 通过主动探测和被动事件判断状态。

主动探测：定时请求 endpoint、拉取模型列表、轻量 chat 测试、超时检测。

被动统计：消费 relay 产生的调用成功/失败事件，统计 timeout_rate、5xx_rate、429_rate、error_rate、p95_latency、consecutive_failures。

状态变化后：写 Redis / 配置中心，发布 EndpointHealthChangedEvent，relay-gateway 订阅并刷新本地缓存，relay-gateway 定时拉取健康状态版本兜底，relay-gateway 请求选路由前过滤 UNHEALTHY / DISABLED / cooldown endpoint。

必须避免坏通道每次请求都先失败再 fallback。

恢复机制：UNHEALTHY 后进入 cooldown，cooldown 后进入 RECOVERING，RECOVERING 只允许主动探测或极少量灰度流量，连续成功后恢复 HEALTHY，失败则重新进入 UNHEALTHY。

---

## 16. 事件机制

relay-gateway 不同步做日志、计费、指标，只发事件。

事件包括：RequestStartedEvent、UpstreamSelectedEvent、FirstTokenEvent、RequestCompletedEvent、RequestFailedEvent、UsageReportedEvent、FallbackTriggeredEvent、RateLimitRejectedEvent、EndpointHealthChangedEvent。

事件要求：不阻塞流式响应，支持失败降级，MQ / Redis Stream 故障不能导致中继完全不可用，计费事件必须幂等，requestId 防重复。

ai-platform-service 消费事件后完成：调用日志落库、usage 统计、双层计费、指标聚合、健康统计、风控判断。

---

## 17. 数据库表

请创建迁移脚本、实体、Mapper、Service、DTO、VO。

建议表：

用户和租户：sys_user、sys_role、sys_permission、sys_menu、sys_user_role、sys_role_permission、tenant、tenant_member、tenant_user_profile、tenant_branding、tenant_plan。

调用凭证：ai_api_key、ai_api_key_scope、ai_api_key_model_permission。

Provider 和路由：ai_provider_config、ai_provider_endpoint、ai_model_route、ai_model_price、ai_endpoint_health_state。

账务：ai_tenant_account、ai_user_account、ai_usage_record、ai_billing_record、ai_settlement_record、ai_recharge_record、ai_balance_change_record。

日志和指标：ai_request_log、ai_request_error_log、ai_audit_log、ai_operation_log、ai_metrics_snapshot。

限流和额度：ai_rate_limit_config、ai_quota_config、ai_tenant_quota、ai_user_quota。

事件和配置：ai_config_version、ai_config_publish_record、ai_event_publish_log、ai_event_consume_log。

要求：

- 所有表遵循统一命名规范
- 必须有 id、create_time、update_time
- 必须有 status 或 is_deleted
- 租户相关表必须有 tenant_id
- 资源表尽量有 owner_type、owner_tenant_id、owner_user_id
- 金额字段使用 decimal
- 常用查询字段加索引
- API Key 不明文存储
- Provider API Key 加密存储

---

## 18. 后端接口

relay-gateway：GET /v1/models、POST /v1/chat/completions、POST /v1/messages。

platform-service 至少提供：

认证和当前用户：登录、退出、当前用户信息、当前用户菜单、当前用户权限、当前用户角色、当前用户余额、当前用户账单、当前用户 API Key、当前用户调用日志。

平台管理：租户 CRUD、租户启用 / 停用 / 冻结、租户套餐、租户额度、租户结算价、平台 C 端用户管理、全局 Provider、全局 Endpoint、Endpoint 测试、全局 ModelRoute、全局 Price、全局日志、全局账单、全局指标、健康状态、手动剔除 / 恢复 endpoint、配置发布、审计日志。

租户管理：租户概览、租户成员、租户用户、租户 Provider、租户 Endpoint、租户 Endpoint 测试、租户 ModelRoute、租户 Price、租户 API Key、租户日志、租户账单、租户指标、租户品牌、租户接入文档、租户告警。

普通用户：我的概览、Chat、Playground、我的 API Key、可用模型、接入指南、我的调用日志、我的用量、我的账单、我的充值、我的预算、我的 Webhook。

---

## 19. 安全要求

- API Key 不明文存储
- 只存 key_hash 和 key_prefix
- Provider API Key 加密存储
- 日志不打印完整 API Key
- 不返回上游密钥
- 管理接口必须鉴权
- relay 模型接口只接受用户 API Key
- Prompt / Response 审计默认关闭
- 配置变更要有操作日志
- 所有租户查询强制 tenant_id 隔离
- 所有用户资源查询强制 owner_user_id 隔离
- 防止越权访问其他租户和其他用户数据
- Endpoint 测试接口不得把上游密钥暴露给前端

---

## 20. 测试要求

后端覆盖：登录、当前用户菜单、权限校验、API Key 创建、Provider 创建、Endpoint 创建、Endpoint 测试、ModelRoute 创建、Price 创建、Chat 调用、Playground 调用、OpenAI 非流式、OpenAI 流式、Anthropic 非流式、Anthropic 流式、Claude Code 接入、OpenRouter 接入、单 Provider 多 Endpoint 多协议、单 Endpoint 多协议、单 Endpoint 单协议、OpenAI 客户端转 Anthropic 上游、Anthropic 客户端转 OpenAI 上游、fallback、health 过滤 unhealthy endpoint、rate limit、租户停用后不能调用、用户停用后不能调用、usage 事件发送、billing 事件消费、log 事件消费、权限越权测试、1000 QPS 流式压测方案。

前端覆盖：登录、动态菜单、平台管理员菜单、租户管理员菜单、平台 C 端用户菜单、租户 C 端用户菜单、Chat 页面、Playground 页面、API Key 创建、Provider 创建、Endpoint 创建、Endpoint 测试、ModelRoute 创建、Price 设置、日志查询、账单查询、健康状态查看、危险操作二次确认、表单校验、接入指南复制、Playwright E2E。

---

## 21. 项目输出结构

请创建完整项目结构，例如：

```text
backend/
  ai-relay-gateway/
  ai-platform-service/
  ai-common/

frontend/
  web-console/

deploy/
  docker-compose.yml
  init.sql
  README.md

docs/
  architecture.md
  api.md
  deployment.md
  relay-benchmark.md
  frontend-guide.md
```

如果你认为更合理，可以调整结构，但必须说明原因。

---

## 22. 最终交付说明

完成后输出：项目结构说明、后端技术栈选择说明、relay-gateway 是否使用 WebFlux / Reactor Netty 的判断、两个后端服务职责说明、单 Vue 前端权限菜单说明、UI 框架 / 图表库 / 表格方案选择原因、数据库表说明、Provider / Endpoint / Protocol 适配说明、三类上游端点形态适配说明、UrlBuilder 说明、中继热路径说明、事件流说明、双层计费说明、健康检测和通道剔除说明、Chat 页面说明、Playground 页面说明、Endpoint 测试页面说明、OpenAI / Anthropic / Claude Code 接入说明、OpenRouter 配置示例、DeepSeek 多 Endpoint 配置示例、单地址多协议配置示例、本地启动说明、前端启动说明、测试说明、压测方案、已知风险和后续优化建议。

---

## 23. 执行顺序

1. 初始化项目结构
2. 选择并确认后端技术栈，尤其是 relay-gateway
3. 创建数据库迁移脚本
4. 实现 ai-common 公共模型
5. 实现 ai-platform-service
6. 实现 ai-relay-gateway
7. 实现 Provider / Endpoint / Protocol / ModelRoute 架构
8. 实现 UrlBuilder
9. 实现三类上游端点形态适配
10. 实现事件机制
11. 实现健康检测和配置缓存
12. 实现 Vue 前端 web-console
13. 实现动态菜单和权限路由
14. 实现 Chat、Playground、Endpoint 测试页面
15. 对接真实后端接口
16. 使用 Playwright MCP 验证页面
17. 编写测试
18. 编写启动和部署文档
19. 给出 1000 QPS 流式压测方案
20. 输出最终交付说明

---

## 24. 最终判断标准

最终项目必须满足：

- 这是一个新项目，不依赖旧项目改造
- 后端只有两个核心服务：ai-relay-gateway 和 ai-platform-service
- relay-gateway 只做中继热路径
- platform-service 承接后台、计费、日志、指标、健康检测、配置管理
- 前端使用 Vue
- 不强制指定 UI 框架，由 AI 结合 impeccable / taste / Context7 自行选择并说明理由
- 前端只有一个应用
- 前端通过权限动态显示平台、租户、普通用户菜单
- 平台也可以直接运营自己的 C 端用户
- 租户也可以运营自己的 C 端用户
- 平台管理员和租户管理员也可以作为普通用户创建 Key、充值、调用模型
- 租户可以作为二级运营方经营自己的用户
- 租户可以配置自己的 Provider / Endpoint / ModelRoute / Price
- 租户可以给自己的模型定价
- 用户可以创建 API Key 并调用模型
- Chat 页面可用
- Playground 页面可用
- Endpoint 测试页面可用
- Endpoint 测试页面支持测试上游厂商 API 端点
- Endpoint 测试页面支持单 Provider 多 Endpoint 多协议
- Endpoint 测试页面支持单 Endpoint 多协议
- Endpoint 测试页面支持单 Endpoint 单协议
- Provider 不等于协议
- Endpoint 不等于协议
- 协议由 ModelRoute.upstreamProtocol 决定
- OpenAI 客户端可以路由到 Anthropic 上游
- Anthropic 客户端可以路由到 OpenAI 上游
- UrlBuilder 不出现 /v1/v1、漏 /v1、多斜杠、丢 pathPrefix 等问题
- 平台可以停用租户
- 租户停用后该租户下所有调用不可用
- OpenAI Compatible 可用
- Anthropic Compatible / Claude Code 可用
- OpenRouter 可用
- DeepSeek 多端点形态可用
- 单地址多协议上游可用
- 流式 SSE 可用
- 工具调用转换可用
- fallback 可用
- 健康检测能自动剔除不健康通道
- usage、计费、日志、指标异步可用
- 前端页面可用且交互良好
- 项目可以编译、启动、测试
- 给出 1000 QPS 流式请求的压测和优化建议
