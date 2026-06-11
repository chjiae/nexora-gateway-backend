# AI Gateway Claude Code Pack v2

这是适用于 Claude Code 的项目提示词压缩包。

## v2 新增条件

前端 UI 技术选型不能由 Claude Code 擅自决定。

UI 框架、组件库、样式方案、图表库、表格方案、Markdown 渲染、代码高亮、主题方案，必须结合相关 skill / MCP / 插件进行评估：

- impeccable skill
- taste skill
- Context7 MCP
- Playwright MCP

并且必须输出：

```text
docs/frontend-ui-tech-decision.md
```

## 文件列表

```text
CLAUDE.md
PHASE_1_CORE_PROMPT.md
README.md
docs/FULL_REQUIREMENTS.md
docs/repo-structure.md
docs/claude-code-usage.md
docs/first-message-for-claude-code.md
.claude/settings.example.json
```

## 推荐根目录名

```text
nexora-gateway-backend
```

## 推荐读取顺序

```text
1. CLAUDE.md
2. PHASE_1_CORE_PROMPT.md
3. docs/FULL_REQUIREMENTS.md
4. docs/repo-structure.md
```

## 优先级

```text
CLAUDE.md > PHASE_1_CORE_PROMPT.md > docs/FULL_REQUIREMENTS.md
```

## ai-common 说明

`ai-common` 只是公共依赖模块，不是第三个后端服务。

后端只有两个可运行核心服务：

```text
ai-relay-gateway
ai-platform-service
```


## 最终版新增约束

本版新增 `docs/BLOCKERS_AND_ALIGNMENT_POLICY.md`。

Claude Code 不是在任何情况下都要自主完成。遇到需求不明确、工具不可用、权限受限、真实外部资源缺失、命令失败无法修复、会影响架构或交付质量的地方，必须停下来问用户，和用户对齐需求，或者请求用户帮助完成外部操作。
