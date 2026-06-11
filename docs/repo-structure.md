# 前后端仓库与 Git Submodule 维护说明

## 目标

本项目必须前后端分两个 Git 仓库维护：

```text
ai-gateway-backend      后端主仓库
ai-gateway-web-console  前端独立仓库
```

后端仓库引用前端仓库，但不直接保存前端源码。  
前端源码必须通过 Git Submodule 维护在后端仓库根目录下。

推荐路径：

```text
ai-gateway-backend/frontend/web-console
```

---

## 推荐目录结构

```text
ai-gateway-backend/
  AGENT.MD
  PHASE_1_CORE_PROMPT.md
  README.md
  .gitmodules

  ai-common/
  ai-platform-service/
  ai-relay-gateway/

  frontend/
    web-console/        # Git Submodule

  deploy/
  docs/
    FULL_REQUIREMENTS.md
    repo-structure.md
```

前端仓库结构：

```text
ai-gateway-web-console/
  package.json
  vite.config.ts
  src/
  tests/
  README.md
```

---

## 添加前端子模块

在后端仓库根目录执行：

```bash
git submodule add <FRONTEND_REPO_URL> frontend/web-console
git submodule update --init --recursive
```

提交后端仓库：

```bash
git add .gitmodules frontend/web-console
git commit -m "chore: add frontend as git submodule"
```

---

## 克隆项目

推荐：

```bash
git clone --recurse-submodules <BACKEND_REPO_URL>
```

如果已经普通克隆：

```bash
git submodule update --init --recursive
```

---

## 更新前端子模块引用

```bash
cd frontend/web-console
git pull origin main
cd ../..
git add frontend/web-console
git commit -m "chore: update frontend submodule"
```

---

## Codex 注意事项

Codex 不能把前端源码作为普通目录提交到后端仓库。

如果没有真实前端仓库地址：

- 不要编造 URL
- 使用 `<FRONTEND_REPO_URL>` 占位
- 生成 `.gitmodules` 模板
- 在 README 和本文档中提醒用户替换

`.gitmodules` 模板：

```ini
[submodule "frontend/web-console"]
    path = frontend/web-console
    url = <FRONTEND_REPO_URL>
```
