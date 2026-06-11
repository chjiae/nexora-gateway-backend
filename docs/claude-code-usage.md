# Claude Code 使用说明

## 文件用途

本压缩包适用于 Claude Code。

建议把这些文件放进后端主仓库根目录：

```text
CLAUDE.md
PHASE_1_CORE_PROMPT.md
README.md
docs/
  FULL_REQUIREMENTS.md
  repo-structure.md
  claude-code-usage.md
```

Claude Code 会优先读取项目根目录的 `CLAUDE.md`，所以本包将原来面向 Codex 的 `AGENT.MD` 整合成了 Claude Code 更容易识别的 `CLAUDE.md`。

---

## 推荐目录

```text
ai-gateway-backend/
  CLAUDE.md
  PHASE_1_CORE_PROMPT.md
  README.md
  .gitmodules

  ai-common/
  ai-platform-service/
  ai-relay-gateway/

  frontend/
    web-console/        # Git Submodule -> ai-gateway-web-console

  deploy/
  docs/
    FULL_REQUIREMENTS.md
    repo-structure.md
    claude-code-usage.md
```

---

## 第一次喂给 Claude Code

建议复制下面这段：

```text
请先阅读以下文件：

1. CLAUDE.md
2. PHASE_1_CORE_PROMPT.md
3. docs/FULL_REQUIREMENTS.md
4. docs/repo-structure.md

优先级：

CLAUDE.md > PHASE_1_CORE_PROMPT.md > docs/FULL_REQUIREMENTS.md

说明：
- CLAUDE.md 是长期项目护栏，任何设计不得违背。
- PHASE_1_CORE_PROMPT.md 是当前第一阶段核心闭环任务。
- docs/FULL_REQUIREMENTS.md 是完整产品蓝图，必须阅读，但不能一次性全部实现。
- docs/repo-structure.md 是前后端双仓库和 Git Submodule 维护说明。

开始开发前，请先检查当前环境可用的 skill、MCP、插件。

如果已安装，必须优先使用：
- superpowers skill：后端架构、编码、测试、自检
- impeccable skill：前端 UI / 交互 / 信息架构
- taste skill：前端审美、视觉风格、文案体验
- Context7 MCP：查询最新技术文档
- Playwright MCP：打开页面验证 UI 和 E2E
- 数据库 MCP：验证 PostgreSQL 表结构和迁移脚本
- Redis MCP：验证缓存、Pub/Sub、事件发布消费
- Docker / Kubernetes MCP：验证容器、日志、服务连通性
- GitHub / GitLab MCP：检查双仓库、Git Submodule、PR、CI
- Figma MCP：如有设计稿则读取设计稿

如果某个工具未安装，不要中断任务，继续开发，并在最终交付说明中列出未安装项和建议安装项。
不要编造已经使用了实际未使用的工具。

额外硬约束：
1. 前后端必须分两个 Git 仓库维护。
2. 后端仓库是主仓库。
3. 前端仓库必须作为后端仓库根目录下的 Git Submodule 维护。
4. 推荐路径是 frontend/web-console。
5. 不允许把前端源码作为普通目录直接提交进后端仓库。
6. 数据库固定使用 PostgreSQL。
7. relay-gateway 热路径禁止查 PostgreSQL，禁止同步落库。
8. relay-gateway 通过 Redis Pub/Sub 发布事件。
9. platform-service 通过 Redis Pub/Sub 订阅事件并写 PostgreSQL。
10. Provider / Endpoint / Protocol 必须解耦。
11. ModelRoute.upstreamProtocol 决定上游协议。
12. 必须实现统一 UrlBuilder。
13. 当前阶段只按 PHASE_1_CORE_PROMPT.md 完成第一阶段核心闭环，不要一次性展开 FULL_REQUIREMENTS.md 的全部功能。
14. 完成后必须保证项目能编译、能启动、能运行最小测试。

现在请先确认已经阅读这些文件，并列出当前环境可用的 skill / MCP / 插件，然后按 PHASE_1_CORE_PROMPT.md 的执行顺序开始开发。
```

---

## 如果 Claude Code 走偏

### 1. 没读完整需求

```text
你还没有读取 docs/FULL_REQUIREMENTS.md。

请立即阅读：
1. CLAUDE.md
2. PHASE_1_CORE_PROMPT.md
3. docs/FULL_REQUIREMENTS.md
4. docs/repo-structure.md

然后重新对齐：
- CLAUDE.md 是最高优先级架构护栏。
- PHASE_1_CORE_PROMPT.md 是本轮第一阶段核心闭环执行范围。
- FULL_REQUIREMENTS.md 是完整产品蓝图，只作为长期方向和细节参考。

不要把 FULL_REQUIREMENTS.md 的全部功能一次性实现。
请只按 PHASE_1_CORE_PROMPT.md 做当前阶段核心闭环。
```

### 2. 没用 skill / MCP / 插件

```text
当前执行没有体现 CLAUDE.md 中的 Skill / MCP / 插件要求。

请立即重新检查可用工具：
1. 后端架构、编码、测试、自检必须优先使用 superpowers skill。
2. 前端 UI、交互、信息架构必须优先使用 impeccable skill 和 taste skill。
3. 技术文档查询优先使用 Context7 MCP。
4. 前端页面验证和 E2E 优先使用 Playwright MCP。
5. PostgreSQL 迁移脚本和表结构验证优先使用数据库 Postgre MCP 或 Docker DESKTOP。
6. Redis Pub/Sub 发布消费验证优先使用 Redis MCP 或本地Docker Desktop。
7. Docker Compose 和服务连通性验证优先使用 Docker / Kubernetes MCP。
8. 双仓库和 Git Submodule 检查优先使用 GitHub / GitLab MCP。

如果某个工具未安装，不要停止任务，但必须在最终交付说明中列出。
不要声称使用了实际未使用的工具。
```

### 3. relay 热路径查数据库

```text
当前实现违反 CLAUDE.md。

relay-gateway 热路径禁止查询 PostgreSQL，禁止 MyBatis，禁止同步落库。

请立即重构：
1. relay-gateway 只读取本地缓存 / Redis 快照 / 配置快照。
2. API Key、用户状态、租户状态、ModelRoute、ProviderEndpoint、EndpointHealthState 都必须从快照读取。
3. 请求日志、usage、billing、health 统计必须通过 Redis Pub/Sub 发布事件。
4. platform-service 订阅 Redis Pub/Sub 后再写 PostgreSQL。
5. 保留接口抽象，未来可替换为 Redis Stream / Kafka。
```

### 4. 把 Provider 和协议绑死

```text
当前 Provider / Endpoint / Protocol 设计错误。

请按 CLAUDE.md 重构：

1. Provider 只表示厂商或服务商，不等于协议。
2. Endpoint 表示具体访问地址和能力，supportedProtocols 表示该地址客观支持的协议。
3. ModelRoute.upstreamProtocol 决定本次请求上游实际使用哪个协议。
4. 客户端协议不强制等于上游协议。
5. 必须支持：
   - OpenAI 客户端 -> Anthropic 上游
   - Anthropic 客户端 -> OpenAI 上游
   - 单 Provider 多 Endpoint
   - 单 Endpoint 多协议
   - 单 Endpoint 单协议
6. ProviderAdapterRegistry 必须根据 upstreamProtocol + endpoint.supportedProtocols 选择适配器。
```

### 5. 项目跑不起来

```text
当前优先级错误。

请暂停新增功能，先让项目可运行。

请完成：
1. 修复后端编译错误。
2. 修复前端编译错误。
3. 确保 Docker Compose 能启动 PostgreSQL 和 Redis。
4. 确保 Flyway / Liquibase 能初始化 PostgreSQL。
5. 确保 ai-platform-service 能启动。
6. 确保 ai-relay-gateway 能启动。
7. 确保前端 Vite 能启动。
8. 确保最小测试通过。
9. 输出当前可运行状态和剩余问题清单。
```


---

## 前端 UI 框架选型的额外要求

喂给 Claude Code 时请额外强调：

```text
前端 UI 设计框架、组件库、样式方案、图表库、表格方案、Markdown 渲染、代码高亮、主题方案，不能由 Claude Code 擅自决定。

选择前必须结合：
- impeccable skill
- taste skill
- Context7 MCP
- Playwright MCP

进行评估。

如果相关 skill / MCP 不可用，不要编造已经使用，也不要中断任务。必须继续完成开发，并在最终交付说明中说明未使用原因。

前端技术选型完成后，必须创建 docs/frontend-ui-tech-decision.md，记录候选方案、优缺点、最终选择、选择理由、未选择其他方案的原因，以及对 Chat / Playground / Endpoint 测试页面的适配说明。
```

如果 Claude Code 擅自选了 UI 框架，直接发：

```text
当前前端 UI 技术选型违反 CLAUDE.md。

UI 框架、组件库、样式方案、图表库、表格方案不能由你擅自决定。

请立即补做选型评估：
1. 使用 impeccable skill 评估信息架构、交互体验、组件体系。
2. 使用 taste skill 评估审美、视觉调性、是否有 AI 味或老旧后台味。
3. 使用 Context7 MCP 查询候选库最新文档、Vue 3 / TypeScript 兼容性。
4. 使用 Playwright MCP 在页面实现后验证真实视觉效果。
5. 如果某些工具不可用，明确写“未安装 / 无法使用”，不要编造。
6. 创建 docs/frontend-ui-tech-decision.md，记录候选方案、优缺点、最终选择和理由。

完成后再继续前端实现。
```


---

## ai-common 被误做成服务时的纠偏

如果 Claude Code 把 `ai-common` 做成第三个服务，直接发送：

```text
当前实现违反 CLAUDE.md。

ai-common 不是后端服务，只是公共依赖模块。

请立即重构：
1. 删除 ai-common 中的 Spring Boot 启动类。
2. 删除 ai-common 中的 Controller / Web 路由。
3. 删除 ai-common 的独立端口、Dockerfile、部署配置。
4. ai-common 只能放 enum、DTO、协议模型、GatewayEvent、UrlBuilder 相关公共类型、通用异常、工具类。
5. 后端可运行服务只能有两个：ai-relay-gateway 和 ai-platform-service。
6. ai-common 只能作为 Maven / Gradle library module 被两个服务依赖。
7. 最终交付说明必须明确 ai-common 不启动、不部署、不是服务。
```


---

## 阻塞时必须停下来问用户

第一次提示必须强调：

```text
你不是在任何情况下都要自主完成。遇到需求不明确、工具不可用、权限受限、真实外部资源缺失、命令失败无法修复、会影响架构或交付质量的地方，必须停下来问我，和我对齐需求，或者让我帮助你完成你不能完成的操作。
```

如果 Claude Code 遇到阻塞仍然继续猜测，直接发送：

```text
当前执行违反 docs/BLOCKERS_AND_ALIGNMENT_POLICY.md。

你不能在需求不明确或工具/权限受限时强行猜测继续。

请停止当前相关实现，按以下格式列出：
1. 阻塞点是什么
2. 为什么会影响交付质量
3. 你已经尝试了什么
4. 可选解决方案
5. 你建议哪个方案
6. 需要我提供什么信息或执行什么操作
7. 哪些不受影响的事项可以先继续

在我确认前，不要继续做会受该阻塞影响的实现。
```
