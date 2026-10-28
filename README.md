# free-agent — 多模型网页 Agent 平台

> 通过 Playwright 驱动各大模型网页端，以对话式界面与模型交互，支持工具执行与工作目录隔离。

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

## 项目简介

free-agent 解决了一个问题：**不依赖官方 API，通过浏览器自动化直接使用各大模型网页端的能力**。

传统方案需要为每个模型申请 API Key、支付费用、对接不同 SDK。free-agent 通过 Playwright 控制浏览器，在用户登录后直接与模型网页交互，支持 10 个主流平台，零 API 成本。

### 核心功能

- **多模型网页连接**：支持千问、豆包、DeepSeek、Kimi、智谱、元宝、ChatGPT、Claude、Grok、Gemini
- **Session 管理**：Playwright headed 模式登录，headless 可选切换
- **实时 SSE 推送**：模型回复通过 SSE 实时推送，打字机效果逐字展示
- **结构化 Agent 执行**：Agent Loop 工具调用循环（最多 10 轮）
- **工具执行**：bash / file.read / file.write / file.list / file.search，XML+CDATA 格式
- **路径安全**：工具执行限制在工作目录内，超出需用户授权
- **上下文管理**：Token 预算、裁剪策略、历史摘要
- **用户隔离**：JWT 认证，数据按 userId 严格隔离
- **管理面板**：模型/工具/MCP/Skills/设置，隐藏在用户菜单抽屉中

## 架构

```
┌─────────────┐     ┌──────────────────────────────────────────┐
│  Vue3 前端   │────▶│            Spring Boot 后端               │
│  Element Plus│ SSE │                                          │
│  Pinia       │◀────│  ┌──────────┐  ┌───────────────────────┐ │
└─────────────┘     │  │ ChatSvc   │─▶│ Agent Loop (≤10轮)    │ │
                    │  │           │  │  ├─ ModelAdapter       │ │
                    │  │ AuthSvc   │  │  │   (Playwright)       │ │
                    │  │ ModelSvc  │  │  ├─ ToolExecutor       │ │
                    │  │ ToolSvc   │  │  │   (bash/file/auth)  │ │
                    │  └──────────┘  │  └─ EventPublisher      │ │
                    │  │ JPA/MySQL  │  │     (SSE → 前端)      │ │
                    │  │ Redis      │  └───────────────────────┘ │
                    │  └──────────┘                             │
                    └──────────────────────────────────────────┘
```

## 目录结构

```
free-agent/
├── frontend/           # Vue 3 + TypeScript + Vite + Element Plus
├── backend/            # Spring Boot 3.5.x + JDK17 + Maven
├── schema.sql          # MySQL 建表脚本（17 张表，手动执行）
├── docker-compose.yml  # 开发环境 MySQL 8 + Redis 7
├── .env.example        # 环境变量模板（复制为 .env 使用）
├── LICENSE             # Apache-2.0
├── CONTRIBUTING.md     # 贡献指南
├── SECURITY.md         # 安全策略
├── CHANGELOG.md        # 变更日志
├── 设计文档.md          # 工程设计文档
└── 需求变更说明_v4.0.md  # 需求变更说明
```

## 环境要求

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 后端编译与运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 20+ | 前端构建 |
| Docker | 24+ | 一键启动 MySQL + Redis |
| Playwright | Chromium | 后端首次运行前需安装 |

## 快速启动

### 1. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，设置你的 MySQL 密码
```

### 2. 启动 MySQL + Redis

```bash
docker compose up -d
```

`schema.sql` 会自动挂载到 MySQL 初始化目录执行。也可手动执行：

```bash
source .env
mysql -h127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" < schema.sql
```

### 3. 安装 Playwright 浏览器

```bash
cd backend
mvn exec:java -e -Dexec.mainClass="com.microsoft.playwright.CLI" -Dexec.args="install chromium"
```

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认监听 `http://127.0.0.1:8080`。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`。

### 6. 访问应用

浏览器打开 `http://localhost:5173`。

**管理员账号**：`admin` / `admin123`（由 `schema.sql` 初始化）。

## 使用流程

```
注册/登录 → 新建对话 → 用户菜单 → 管理面板 → 添加模型账户
    ↓
选择平台 → 点击「登录」→ 弹出浏览器窗口 → 手动登录
    ↓
后端检测登录态 → READY → 回到对话页 → 选择模型 → 设置工作目录 → 开始对话
    ↓
模型回复包含工具调用 → 后端自动执行 → 结果回传模型 → 继续回复（Agent Loop）
```

## 工具调用格式

模型使用 XML+CDATA 格式调用工具（CDATA 内可包含任意字符，无需转义）：

```
<tool_call>
<tool>bash</tool>
<command><![CDATA[
ls -la
]]></command>
</tool_call>

<tool_call>
<tool>file.read</tool>
<path>src/main.java</path>
</tool_call>

<tool_call>
<tool>file.write</tool>
<path>output.txt</path>
<content><![CDATA[
文件内容，可以包含 " 双引号 " 和 < 尖括号 > 无需转义
]]></content>
</tool_call>
```

所有文件操作限制在工作目录内，超出需用户授权。

## 支持的平台

| 平台 | Provider | 平台 | Provider |
| --- | --- | --- | --- |
| 通义千问 | `qianwen` | ChatGPT | `chatgpt` |
| 豆包 | `doubao` | Claude | `claude` |
| DeepSeek | `deepseek` | Grok | `grok` |
| Kimi | `kimi` | Gemini | `gemini` |
| 智谱清言 | `zhipu` | 腾讯元宝 | `yuanbao` |

## 配置说明

`backend/src/main/resources/application.yml`：

```yaml
webmodel:
  playwright:
    headless-after-login: false  # 登录后是否切换为无头模式
  agent:
    max-steps: 50
    timeout-ms: 600000
```

数据库密码通过环境变量注入，不硬编码：

```yaml
spring:
  datasource:
    password: ${MYSQL_ROOT_PASSWORD:?Please set MYSQL_ROOT_PASSWORD}
```

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia |
| 通信 | REST + SSE (EventSource) |
| 后端 | Java 17 + Spring Boot 3.5.0 |
| 浏览器自动化 | Playwright Java 1.49.0 |
| 认证 | JWT (jjwt 0.12.6) |
| 数据库 | MySQL 8.x |
| 缓存 | Redis 7 |

## 项目路线图

### 已完成
- [x] 多模型网页连接（10 个平台）
- [x] Playwright headed 登录检测
- [x] 对话式 UI + SSE 实时推送 + 打字机效果
- [x] Agent Loop 工具调用循环
- [x] 工具执行：bash / file.read / file.write
- [x] 路径安全检查 + 用户授权
- [x] 用户体系 + JWT + 数据隔离
- [x] 管理面板（模型/工具/MCP/Skills/设置）
- [x] 上下文管理（Token 预算/裁剪）
- [x] 工作目录持久化

### 开发中
- [ ] MCP 协议接入
- [ ] 上下文摘要压缩
- [ ] GitHub Actions CI/CD

### 规划中
- [ ] 多模型协作（A 调用 B）
- [ ] 对话导出/分享
- [ ] 移动端适配

## 安全须知

- 数据库密码通过 `.env` 环境变量注入，`.env` 已加入 `.gitignore`
- MySQL/Redis 端口仅绑定 `127.0.0.1`，不对外暴露
- 浏览器 Profile 目录含登录态，已加入 `.gitignore`
- JWT Secret 默认值仅用于开发，生产环境必须修改
- 不收集平台密码、不绕过验证码/风控
- 用户数据按 `userId` 隔离

详见 [SECURITY.md](./SECURITY.md)。

## 许可证

[Apache License 2.0](./LICENSE)

## 贡献

欢迎提交 Issue 和 Pull Request。详见 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## 变更日志

详见 [CHANGELOG.md](./CHANGELOG.md)。

## 已知限制

- 网页端免费额度不等于免费 API，不保证无限调用或稳定性
- 后端重启后浏览器会话丢失，需重新登录模型
- Playwright headed 模式需要图形界面环境
- 需结合各平台实际网页与使用条款验证自动化可行性
