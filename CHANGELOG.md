# 变更日志

本项目遵循 [Keep a Changelog](https://keepachangelog.com/) 格式。

## [Unreleased]

### Added
- 工具调用 XML+CDATA 格式，避免 JSON 内容中双引号导致解析失败
- 路径安全检查：工具执行限制在工作目录内，超出需用户授权
- SSE 实时消息推送，替代轮询
- 打字机效果逐字展示模型回复
- 模型调用日志和工具执行日志

### Changed
- `docker-compose.yml` 密码改用 `.env` 环境变量，端口绑定 `127.0.0.1`
- `application.yml` 数据库密码改用环境变量
- 工作目录保存绝对路径，前端展示 basename
- Agent Loop 事件使用 `conversationId` 作为 SSE taskId

### Fixed
- 浏览器会话断开时自动跳转模型管理面板
- 回车键发送消息不生效的问题
- 模型回复后无法进行第二次对话的问题

## [1.0.0] - 2026-10-27

### Added
- 多模型网页 Agent 平台核心功能
- 支持 10 个平台：千问、豆包、DeepSeek、Kimi、智谱、元宝、ChatGPT、Claude、Grok、Gemini
- Playwright headed 模式登录检测
- 对话式 UI（类 ChatGPT）
- 工具执行：bash / file.read / file.write / file.list / file.search
- Agent Loop 工具调用循环（最多 10 轮）
- 用户体系：注册/登录/JWT/数据隔离
- 管理面板：模型/工具/MCP/Skills/设置
- 系统默认配置（admin 可见可配置）
- 雪花算法生成 taskId
- 上下文管理：Token 预算、裁剪策略
- 工作目录持久化
- headless 可配置开关