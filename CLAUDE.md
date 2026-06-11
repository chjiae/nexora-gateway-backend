# CLAUDE.md

## Claude Code 必读说明

你正在参与一个全新的多租户运营型 AI API Gateway 平台项目。

本项目不是普通大模型转发器，也不是简单 one-api / litellm 克隆，而是一个允许平台方和租户运营方通过 AI API 服务盈利的 SaaS 平台。

Claude Code 开始任何开发前，必须先阅读并遵守以下文件：

```text
CLAUDE.md
PHASE_1_CORE_PROMPT.md
docs/FULL_REQUIREMENTS.md
docs/repo-structure.md
```

优先级：

```text
CLAUDE.md > PHASE_1_CORE_PROMPT.md > docs/FULL_REQUIREMENTS.md
```

说明：

- `CLAUDE.md` 是 Claude Code 的长期项目记忆和硬约束。
- `PHASE_1_CORE_PROMPT.md` 是当前第一阶段核心闭环任务。
- `docs/FULL_REQUIREMENTS.md` 是完整产品蓝图，必须阅读，但不能一次性全部实现。
- `docs/repo-structure.md` 是前后端双仓库和 Git Submodule 维护说明。

---

## 项目定位

这是一个全新的多租户运营型 AI API Gateway 平台。

平台方是最高级运营方，可以：

- 创建、管理、停用租户
- 管理平台自己的 C 端用户
- 管理全局 Provider / Endpoint / ModelRoute / Price
- 查看调用日志、账单、收入、成本、毛利、健康状态、风控数据
- 进行配置发布和通道治理

租户不是普通企业客户，而是：

- SaaS Operator
- 子平台代理商
- 二级运营方

租户可以通过平台赚钱，拥有自己的：

- 用户
- 成员
- Provider
- Endpoint
- ModelRoute
- 模型价格
- API Key
- 品牌配置
- 接入文档
- 运营数据

平台也可以直接运营自己的 C 端用户。

---

## 第一阶段边界

当前不是一次性实现全部商业化功能。

当前阶段目标是完成第一阶段核心闭环：

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

完整功能以后按 `docs/FULL_REQUIREMENTS.md` 逐步补全。

---

## 必须使用的 Skill / MCP / 插件

开始开发前，请先检查当前 Claude Code 环境可用的 skills、MCP、插件和工具。

如果已安装，必须优先使用：

### 后端

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

- Context7 MCP：查询最新技术文档和库用法
- Playwright MCP：打开页面验证 UI、交互、E2E
- 数据库 MCP：验证 PostgreSQL 表结构和迁移脚本
- Redis MCP：验证缓存、Pub/Sub、事件发布消费
- Docker / Kubernetes MCP：验证 Docker Compose、容器、日志、连通性
- GitHub / GitLab MCP：检查双仓库、Git Submodule、PR、CI
- Figma MCP：如有设计稿则读取设计稿

如果某个 skill / MCP / 插件未安装：

1. 不要中断任务
2. 不要等待用户确认
3. 继续完成开发
4. 在最终交付说明中列出未使用原因和建议安装项

禁止编造已经使用了实际未使用的工具。

---

---

## 产品与仓库命名建议

产品名暂定：

```text
Nexora Gateway
```

推荐仓库名：

```text
nexora-gateway-backend      后端主仓库
nexora-gateway-web-console  前端独立仓库
```

推荐根目录名：

```text
nexora-gateway-backend
```

如果用户后续更改产品名或仓库名，以用户最新明确命名为准。

## 前后端仓库与 Git Submodule 约束

本项目必须采用前后端两个独立 Git 仓库。

推荐仓库：

```text
ai-gateway-backend      后端主仓库
ai-gateway-web-console  前端独立仓库
```

后端仓库是主工程仓库，前端仓库必须以 Git Submodule 的方式挂载到后端仓库根目录下。

推荐路径：

```text
ai-gateway-backend/frontend/web-console
```

强制规则：

- 前端代码必须维护在独立前端仓库中。
- 后端代码必须维护在独立后端仓库中。
- 后端仓库只通过 Git Submodule 引用前端仓库。
- 禁止把前端源码作为普通目录直接提交进后端仓库。
- 后端和前端必须分别提交、分别管理版本。
- 后端仓库需要提交 `.gitmodules` 文件。
- 后端 README 必须说明如何初始化和更新前端子模块。
- CI/CD、Docker Compose、部署脚本如果需要构建前端，必须支持 submodule checkout。
- 如果不知道前端远程仓库地址，不能编造假的仓库地址；应使用 `<FRONTEND_REPO_URL>` 占位并在文档中提示用户替换。

---

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

## 后端核心架构原则

后端只有两个核心服务：

```text
ai-common/
ai-relay-gateway/
ai-platform-service/
```

### ai-relay-gateway

只负责中继热路径：

- OpenAI Compatible API
- Anthropic Compatible API
- Claude Code 接入
- API Key 鉴权
- 用户状态快速校验
- 租户状态快速校验
- 模型路由
- ProviderEndpoint 选择
- SSE 流式中继
- 协议转换
- Tools / tool_calls / tool_use / tool_result 转换
- Fallback
- 限流
- 健康状态过滤
- Redis Pub/Sub 异步事件发送

禁止放入：

- 后台管理
- 数据统计
- 账单计算
- 日志查询
- 报表
- MyBatis 热路径查询
- PostgreSQL 热路径查询
- 同步落库
- 复杂业务逻辑

原则：

> 中继服务只干中继。

### ai-platform-service

负责所有非热路径能力：

- 用户体系
- 租户体系
- 权限体系
- API Key 管理
- Provider 管理
- Endpoint 管理
- Endpoint 测试
- ModelRoute 管理
- 模型价格
- 充值
- 双层计费
- 调用日志
- 指标统计
- 健康检测
- 通道剔除
- 配置发布
- 审计日志
- 风控
- 页面查询接口
- Redis Pub/Sub 事件订阅和消费
- PostgreSQL 落库

原则：

> 所有业务逻辑都放 platform-service。

---

## 数据库和消息机制

数据库固定使用 PostgreSQL。

要求：

- 使用 Flyway 或 Liquibase 管理迁移
- 金额字段使用 numeric
- JSON 配置字段优先使用 jsonb
- 常用查询字段必须加索引
- 所有表必须有 id、create_time、update_time
- 租户相关表必须有 tenant_id 或 owner_tenant_id
- 用户私有资源必须有 owner_user_id
- API Key 不允许明文存储
- Provider API Key 必须加密存储

Redis 用于：

- relay 热路径缓存
- API Key 快照
- 用户状态快照
- 租户状态快照
- ModelRoute 快照
- ProviderEndpoint 快照
- EndpointHealthState 快照
- RateLimit / Quota 快照
- 配置版本号
- Redis Pub/Sub 事件发布订阅

当前阶段消息机制使用 Redis Pub/Sub。

relay-gateway 负责发布事件。  
platform-service 负责订阅并消费事件。

要求：

- relay 发布事件不能阻塞流式响应
- Redis 发布失败不能导致中继完全不可用
- 事件 payload 必须包含 eventId、eventType、requestId、occurredAt
- 计费相关事件必须幂等
- platform-service 消费后写 PostgreSQL
- 当前阶段可以使用 Redis Pub/Sub，后续可扩展为 Redis Stream / Kafka / Pulsar

---

## Provider / Endpoint / Protocol 原则

Provider 不等于协议。

Endpoint 不等于协议。

协议由 ModelRoute.upstreamProtocol 决定，不由 Provider 决定，也不由客户端协议强制决定。

正确链路：

```text
客户端协议 clientProtocol
  -> ProtocolAdapter 转 InternalChatRequest
  -> ModelRoute 决定 upstreamProtocol
  -> ProviderAdapterRegistry 根据 upstreamProtocol + endpoint.supportedProtocols 选择适配器
  -> 请求上游
  -> 再转换回客户端协议
```

必须支持：

- 一个 Provider，多个 Endpoint，不同 Endpoint 支持不同协议
- 一个 Provider，一个 Endpoint，同一个 Endpoint 支持多种协议
- 一个 Provider，一个 Endpoint，只支持一种协议
- OpenAI 客户端 -> OpenAI 上游
- OpenAI 客户端 -> Anthropic 上游
- Anthropic 客户端 -> Anthropic 上游
- Anthropic 客户端 -> OpenAI 上游

---

## UrlBuilder 原则

必须实现统一 UrlBuilder。禁止简单字符串拼接。

必须避免：

- /v1/v1
- 漏 /v1
- 多斜杠
- pathPrefix 丢失
- 把 OpenAI path 发到 Anthropic endpoint
- 把 Anthropic path 发到 OpenAI endpoint

UrlBuilder 必须根据 baseUrl、pathPrefix、upstreamProtocol、operation 生成最终请求 URL。

operation 至少包括：

- MODELS
- CHAT_COMPLETIONS
- MESSAGES
- RESPONSES
- EMBEDDINGS

---

---

## 前端 UI 技术选型硬约束

前端 UI 设计框架、组件库、样式方案、图表库、表格方案、代码高亮方案、Markdown 渲染方案，不能由 Claude Code 擅自决定。

在选择前必须结合相关 skill / MCP / 插件进行评估。

必须优先使用：

- impeccable skill：评估 UI 信息架构、页面质感、交互体验、组件体系是否适合 AI API 平台
- taste skill：评估视觉审美、现代感、品牌调性、是否存在老旧后台模板味和明显 AI 味
- Context7 MCP：查询候选 UI 框架、图表库、表格库、Markdown / Code Highlight 库的最新文档和兼容性
- Playwright MCP：在页面实现后打开浏览器验证真实视觉效果和交互体验

如果这些 skill / MCP 未安装：

- 不得编造已经使用
- 不得中断任务
- 必须在最终交付说明中写明未能使用哪些工具
- 必须基于项目定位做保守、可维护的选择，并说明原因

### 选择范围

至少需要评估：

- UI 组件库
- 样式方案
- 图标方案
- 图表库
- 表格 / DataGrid 方案
- Markdown 渲染方案
- 代码高亮方案
- 表单校验方案
- 主题系统 / 暗色模式方案

### 选择标准

必须结合本项目定位评估：

- 是否适合多租户运营型 AI API Gateway 平台
- 是否适合 Chat / Playground / Endpoint 测试等 AI 产品页面
- 是否能做出现代化、专业、清晰、有产品感的界面
- 是否避免老旧后台模板风格
- TypeScript 支持是否好
- Vue 3 生态是否成熟
- 表格、表单、弹窗、抽屉、通知、标签、状态展示是否完善
- 主题定制能力是否强
- 暗色模式支持是否好
- 可维护性是否好
- 文档是否清晰
- 社区活跃度是否足够
- 是否方便 Playwright E2E 测试

### 必须输出选型说明

前端技术选型完成后，必须创建：

```text
docs/frontend-ui-tech-decision.md
```

该文档至少包含：

1. 候选方案列表
2. 每个候选方案的优缺点
3. 使用了哪些 skill / MCP / 插件辅助判断
4. 最终选择
5. 选择理由
6. 不选择其他方案的原因
7. 对 Chat / Playground / Endpoint 测试页面的适配说明
8. 后续如果审美或交互不达标，如何替换或优化

### 禁止行为

禁止：

- 没有评估就直接选 Element Plus / Ant Design Vue / Naive UI / Arco Design / Tailwind / Shadcn 风格方案
- 因为熟悉某个框架就直接选择
- 只根据默认模板搭页面
- 使用老旧后台管理系统模板
- 忽略 Chat / Playground / Endpoint 测试这类 AI 产品核心体验
- 声称使用了 impeccable / taste / Context7 / Playwright，但实际没有使用

## 前端原则

只有一个 Vue 前端应用。

通过：

- Role
- Permission
- Tenant
- UserType

动态生成菜单。

固定技术：

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia 或同等状态管理
- Playwright E2E

不要固定 UI 框架，由 Claude Code 结合 impeccable、taste、Context7、Playwright 自行选择。

禁止使用老旧后台模板风格。

当前阶段只实现：

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

其他菜单可以注册为后续规划，不要生成大量空壳业务代码。

---

## 禁止 Mock 的核心链路

以下链路必须真实实现，不允许只写 mock、TODO 或伪代码：

- API Key 创建、hash 存储、prefix 展示
- relay-gateway API Key 鉴权
- ProviderEndpoint 建模
- ModelRoute 路由解析
- ProtocolAdapter 基础结构
- UrlBuilder
- OpenAI / Anthropic 请求入口
- SSE 流式基础转发
- Redis Pub/Sub 事件发布
- Redis Pub/Sub 事件消费
- Endpoint 测试接口

---

## 开发原则

优先保证：

1. 架构正确
2. 边界清晰
3. 性能稳定
4. 可扩展
5. 可维护
6. 可运行
7. 可测试

任何时候如果发现当前实现与本文件冲突，必须优先重构对齐本文件。
