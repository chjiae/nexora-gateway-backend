# Claude Code 第一次对话提示词

请先阅读以下文件：

1. CLAUDE.md
2. PHASE_1_CORE_PROMPT.md
3. docs/FULL_REQUIREMENTS.md
4. docs/repo-structure.md
5. docs/BLOCKERS_AND_ALIGNMENT_POLICY.md

优先级：

CLAUDE.md > PHASE_1_CORE_PROMPT.md > docs/FULL_REQUIREMENTS.md

说明：

- CLAUDE.md 是长期项目护栏，任何设计不得违背。
- PHASE_1_CORE_PROMPT.md 是当前第一阶段核心闭环任务，不代表全部功能。
- docs/FULL_REQUIREMENTS.md 是完整产品蓝图，必须阅读，但不能一次性全部实现。
- docs/repo-structure.md 是前后端双仓库和 Git Submodule 维护说明。
- docs/BLOCKERS_AND_ALIGNMENT_POLICY.md 是阻塞停顿和需求对齐规则，遇到不明确或不能完成的地方必须按它执行。

开始开发前，请先检查当前环境可用的 skill、MCP、插件。

如果已安装，必须优先使用：

- superpowers skill：后端架构、编码、测试、自检
- impeccable skill：前端 UI、交互、信息架构
- taste skill：前端审美、视觉风格、文案体验
- Context7 MCP：查询最新技术文档
- Playwright MCP：打开页面验证 UI 和 E2E
- 数据库 MCP：验证 PostgreSQL 表结构和迁移脚本
- Redis MCP：验证缓存、Pub/Sub、事件发布消费
- Docker / Kubernetes MCP：验证容器、日志、服务连通性
- GitHub / GitLab MCP：检查双仓库、Git Submodule、PR、CI
- Figma MCP：如有设计稿则读取设计稿

如果某个工具未安装，不要编造已经使用了实际未使用的工具。

重要：你不是在任何情况下都要自主完成。遇到需求不明确、工具不可用、权限受限、真实外部资源缺失、命令失败无法修复、会影响架构或交付质量的地方，必须停下来问我，和我对齐需求，或者让我帮助你完成你不能完成的操作。不要为了继续推进而猜测、编造或牺牲交付质量。

额外硬约束：

1. 本项目产品名暂定为 Nexora Gateway。
2. 后端主仓库建议命名为 nexora-gateway-backend。
3. 前端仓库建议命名为 nexora-gateway-web-console。
4. 前后端必须分两个 Git 仓库维护。
5. 后端仓库是主仓库。
6. 前端仓库必须作为后端仓库根目录下的 Git Submodule 维护。
7. 推荐路径是 frontend/web-console。
8. 不允许把前端源码作为普通目录直接提交进后端仓库。
9. 如果没有前端远程仓库地址，不要编造 URL，使用 <FRONTEND_REPO_URL> 占位，并停下来让我提供真实仓库地址或确认使用占位方案。
10. 后端只有两个可运行核心服务：ai-relay-gateway 和 ai-platform-service。
11. ai-common 只是公共模块 / 公共依赖包，不是独立服务，不应单独启动、部署、暴露接口或配置独立端口。
12. 数据库固定使用 PostgreSQL。
13. relay-gateway 热路径禁止查 PostgreSQL，禁止同步落库。
14. relay-gateway 通过 Redis Pub/Sub 发布事件。
15. platform-service 通过 Redis Pub/Sub 订阅事件并写 PostgreSQL。
16. Provider / Endpoint / Protocol 必须解耦。
17. ModelRoute.upstreamProtocol 决定上游协议。
18. 必须实现统一 UrlBuilder，避免 /v1/v1、漏 /v1、多斜杠、pathPrefix 丢失。
19. 前端 UI 框架、组件库、样式方案、图表库、表格方案、Markdown 渲染、代码高亮、主题方案，不能由你擅自决定。
20. 前端技术选型必须结合 impeccable skill、taste skill、Context7 MCP、Playwright MCP 进行评估；如果无法使用这些工具完成有效评估，必须停下来说明受限点并问我是否继续。
21. 前端技术选型完成后，必须创建 docs/frontend-ui-tech-decision.md，记录候选方案、优缺点、最终选择、选择理由、未选择其他方案的原因，以及对 Chat / Playground / Endpoint 测试页面的适配说明。
22. 当前阶段只按 PHASE_1_CORE_PROMPT.md 完成第一阶段核心闭环，不要一次性展开 FULL_REQUIREMENTS.md 的全部功能。
23. 核心链路必须真实可运行，不要只写 TODO 或伪代码。
24. 完成后必须保证项目能编译、能启动、能运行最小测试。

现在请先确认你已经阅读这些文件，并列出当前环境可用的 skill / MCP / 插件。然后按 PHASE_1_CORE_PROMPT.md 的执行顺序开始第一阶段核心闭环开发；遇到阻塞必须停下来问我。
