# free-agent — 多模型网页 Agent 平台

> Vue 实时工作台 · 多模型网页会话 · 结构化 Agent 执行引擎 · Tools / MCP / Skills · 上下文预算 · Spring Boot 3.5 + Redis + MySQL

通过 Playwright 驱动各大模型网页端（千问、豆包、DeepSeek、Kimi、智谱、腾讯元宝、ChatGPT、Claude、Grok、Gemini），以对话式界面与模型交互，支持工具执行（bash / 文件读写）与工作目录隔离。

## 目录结构

```
free-agent/
├── frontend/          # Vue 3 + TypeScript + Vite + Element Plus
├── backend/           # Spring Boot 3.5.x + JDK17 + Maven
├── schema.sql         # MySQL 建表脚本（17 张表，根目录，手动执行）
├── docker-compose.yml # 开发环境 MySQL 8 + Redis 7
├── 设计文档.md         # 工程设计文档 v4.0
├── 需求变更说明_v4.0.md # 需求变更说明
├── workspace/         # 受控输入/输出目录（gitignore）
├── browser-profiles/  # 本地登录配置（gitignore，敏感）
├── skills/            # 已审核的本地 Skill 目录
└── logs/              # 日志（gitignore）
```

## 环境要求

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 后端编译与运行 |
| Maven | 3.8+ | 后端构建（或使用 IDE 内置 Maven） |
| Node.js | 20+ | 前端构建 |
| npm | 10+ | 前端包管理 |
| Docker | 24+ | 一键启动 MySQL + Redis（可选，也可本地安装） |
| Playwright 浏览器 | Chromium | 后端首次运行前需安装 |

## 快速启动

### 1. 启动 MySQL + Redis

```bash
docker compose up -d
```

`schema.sql` 会自动挂载到 MySQL 初始化目录执行，创建 `webmodel_agent` 数据库及 17 张表。

也可手动执行：

```bash
mysql -h127.0.0.1 -uroot -proot123 < schema.sql
```

> Docker 启动的 MySQL root 密码为 `root123`，已在 `docker-compose.yml` 和 `application.yml` 中保持一致。

### 2. 安装 Playwright 浏览器

后端依赖 Playwright 驱动浏览器，首次使用前必须安装 Chromium：

```bash
cd backend
mvn exec:java -e -Dexec.mainClass="com.microsoft.playwright.CLI" -Dexec.args="install chromium"
```

或手动下载（若上方命令不可用）：

```bash
npx playwright install chromium
```

安装后浏览器二进制会缓存到 `~/.cache/ms-playwright/`，后续无需重复安装。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认监听 `http://127.0.0.1:8080`。

首次启动会自动创建 `workspace/` 和 `browser-profiles/` 目录。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`，已配置代理转发 `/api` 到后端 `http://localhost:8080`。

### 5. 访问应用

浏览器打开 `http://localhost:5173`，注册账号 → 登录 → 开始使用。

## 使用流程

```
注册/登录 → 新建对话 → 打开用户菜单 → 管理面板 → 添加模型账户
    ↓
选择平台（千问/豆包/...） → 点击「登录」→ 弹出浏览器窗口 → 用户手动登录
    ↓
后端检测登录态 → 状态变为 READY → 回到对话页 → 选择模型 → 设置工作目录 → 开始对话
    ↓
模型回复中包含 ```bash``` / ```file:read``` / ```file:write``` → 后端自动执行工具 → 结果回传模型 → 继续回复
```

## 配置说明

所有配置在 `backend/src/main/resources/application.yml`：

### 数据库与 Redis

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/webmodel_agent?...
    username: root
    password: root123        # 与 docker-compose.yml 一致
  data:
    redis:
      host: 127.0.0.1
      port: 6379
```

### Playwright

```yaml
webmodel:
  playwright:
    headless-after-login: false  # 登录后是否切换为无头模式（true=后台静默运行，false=保持窗口可见）

```

- `headless-after-login: false` — 登录成功后浏览器窗口保持可见，方便调试和观察模型操作
- `headless-after-login: true` — 登录成功后切换为 headless，后台静默运行，节省资源

### JWT

```yaml
webmodel:
  jwt:
    secret: free-agent-secret-key-change-in-production  # 生产环境务必修改
```

### Agent 参数

```yaml
webmodel:
  agent:
    max-steps: 50          # Agent 最大执行步数
    max-model-rounds: 20   # 模型最大调用轮数
    max-tool-calls: 100    # 工具最大调用次数
    timeout-ms: 600000     # 超时时间（10 分钟）
```

## 支持的平台

| 平台 | Provider 标识 | 登录检测方式 |
| --- | --- | --- |
| 通义千问 | `qianwen` | URL + 登录按钮 + 输入框 |
| 豆包 | `doubao` | URL + 登录按钮 + 输入框 |
| DeepSeek | `deepseek` | URL + 登录按钮 + 输入框 |
| Kimi | `kimi` | URL + 登录按钮 + 输入框 |
| 智谱清言 | `zhipu` | URL + 登录按钮 + 输入框 |
| 腾讯元宝 | `yuanbao` | URL + 登录按钮 + 输入框 |
| ChatGPT | `chatgpt` | URL + 登录按钮 + 输入框 |
| Claude | `claude` | URL + 登录按钮 + 输入框 |
| Grok | `grok` | URL + 登录按钮 + 输入框 |
| Gemini | `gemini` | URL + 登录按钮 + 输入框 |

## 工具调用格式

模型回复中可嵌入以下工具调用，后端 `ToolExecutor` 会自动解析并执行：

```
执行 Shell 命令：
```bash
ls -la
```

读取文件：
```file:read /path/to/file.txt
```

写入文件：
```file:write /path/to/file.txt
文件内容...
```
```

所有文件操作均限制在当前对话的工作目录下执行。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia |
| 通信 | REST + 轮询 |
| 后端 | Java 17 + Spring Boot 3.5.0 + Spring AI Alibaba 1.1.2.0 |
| 浏览器自动化 | Playwright Java 1.49.0 |
| 认证 | JWT (jjwt 0.12.6) |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 7 |
| 构建 | Maven / npm |

## API 概览

| 模块 | 路径前缀 | 功能 |
| --- | --- | --- |
| 认证 | `/api/auth` | 注册、登录、刷新 Token |
| 对话 | `/api/chat` | 对话 CRUD、发送消息、轮询消息、重命名、删除 |
| 模型 | `/api/models` | 模型账户管理、连接、登录检测、状态查询 |
| 工具 | `/api/tools` | 工具配置管理 |
| MCP | `/api/mcp` | MCP 服务管理 |
| Skills | `/api/skills` | Skill 管理 |
| 设置 | `/api/settings` | 用户设置 |

所有接口（除 `/api/auth/**`）需在请求头携带 `Authorization: Bearer <token>`。

## 安全须知

- 浏览器 Profile 目录含登录态，已加入 `.gitignore`，禁止提交。
- 后端默认仅监听 `127.0.0.1`，不对外暴露。
- JWT Secret 默认值仅用于开发，生产环境必须修改。
- 不收集平台密码、不转发登录二维码、不绕过验证码/风控。
- Cookie/Token 不写入 MySQL/Redis/日志明文。
- 用户数据按 `userId` 隔离，互不可见。

## 已知限制

- 网页端免费额度不等于免费 API，不保证无限调用或稳定性。
- 网页模式上下文 token 用量为应用侧估算，非第三方模型真实统计。
- 需结合各平台实际网页与使用条款验证自动化可行性。
- 后端重启后浏览器会话丢失，需重新登录模型。
- Playwright headed 模式需要图形界面环境，纯服务器（无 X11）需使用 headless 模式。

## 构建产物

```bash
# 前端构建
cd frontend && npm run build    # 输出到 frontend/dist/

# 后端打包
cd backend && mvn package       # 输出 backend/target/*.jar
```