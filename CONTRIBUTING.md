# 贡献指南

感谢您对 free-agent 项目的关注！本文档说明了如何参与贡献。

## 开发环境搭建

1. Fork 并克隆仓库
2. 参照 [README.md](./README.md) 完成开发环境搭建
3. 确认前后端均可正常启动

## 分支策略

- `main` 分支为受保护分支，必须通过 Pull Request 合并
- 开发请从 `main` 创建功能分支：`git checkout -b feature/your-feature`
- 修复分支命名：`fix/issue-description`

## 提交规范

提交信息使用以下格式：

```
<type>: <description>

[optional body]
```

type 取值：
- `feat`: 新功能
- `fix`: 修复 Bug
- `refactor`: 重构
- `docs`: 文档
- `style`: 格式调整
- `test`: 测试
- `chore`: 构建/工具

示例：`feat: 添加 Gemini 平台适配器`

## Pull Request 流程

1. 确保代码通过构建：`cd frontend && npm run build` 且 `cd backend && mvn compile`
2. 更新相关文档（如涉及 API 变更）
3. 创建 PR，描述变更内容和动机
4. 等待 Review，根据反馈修改

## 代码风格

- **后端**：遵循已有 Java 命名约定（camelCase），无冗余注释
- **前端**：遵循已有 TypeScript / Vue 3 风格，使用 Composition API
- 所有交互文案使用简体中文

## 报告问题

- 使用 GitHub Issues 报告 Bug 或提出功能建议
- 安全漏洞请勿公开 Issue，参照 [SECURITY.md](./SECURITY.md) 私下报告