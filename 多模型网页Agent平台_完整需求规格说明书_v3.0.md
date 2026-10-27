# 多模型网页 Agent 平台

## 完整需求规格说明书（PRD + SRS）

版本 v3.0 | 日期 2026-10-27 | 开发基线草案

Vue 实时工作台 · 多模型网页会话 · 结构化 Agent 执行引擎 · Tools / MCP / Skills · 上下文预算 · Spring Boot + Redis + MySQL


---


# 0. 文档说明与需求来源

| 标识 | 含义 |
| --- | --- |
| 【用户明确】 | 用户直接提出：多模型网页登录；千问、DeepSeek、豆包、ChatGPT、Codex 等；免费网页模型优先；Vue 实时交互；扫码登录入口；账户/Session 管理；Spring Boot Alibaba、Redis、MySQL；上下文大小管理；结构化输出驱动步骤执行；Tools、MCP、Skills 模块。 |
| 【补充设计】 | 为使项目可开发、可测试、可维护而补充的页面、状态机、API、数据表、权限、监控、验收标准、开发阶段和默认决策。 |
| 【待验证】 | 需结合具体站点实际网页、账户、平台条款和当前依赖版本验证的内容；本文不承诺所有网站均可自动化。 |

本文是统一需求基线，包含产品需求、功能规格、逻辑架构、数据/API 设计、上下文策略、安全边界和验收标准。需求中“浏览器登录”指用户在官方网页内自行登录或扫码；本系统不收集平台密码，也不伪造或转发平台登录二维码。


## 0.1 核心结论

- 结构化输出是 Agent 关键能力：网页模型输出自然语言或 JSON 文本，本地引擎解析、校验后执行；不能把模型回答直接当作命令执行。
- Agent 执行引擎掌握最终执行权：它验证工具是否在白名单、参数是否符合 Schema、当前用户是否有权限、是否需要人工审批。
- Tools、MCP、Skills 需要独立模块：Tools 是实际动作，MCP 是外部能力的标准接入协议，Skills 是可复用的方法/流程说明。三者协作但不互相替代。
- 网页端免费额度不等于免费 API：网页适配器只模拟用户可见的合法交互，不保证无限调用、稳定性或与 API 相同的上下文能力。
- 上下文使用量在网页模式下通常只能估算：系统应显示应用侧预算和估算，不伪装成第三方模型的准确 token 统计。

# 1. 产品概述


## 1.1 产品背景

用户希望开发一个本地优先的多模型 Agent 工作台。用户在同一 Vue 页面管理不同 AI 平台账户和浏览器会话，在官方网页内完成登录（包括平台提供的扫码方式），再创建 Agent 任务。Agent 通过对应网页输入任务和读取回答；本地 Java 运行时负责规划执行、工具调用、权限、步骤状态、上下文管理、数据持久化和结果校验。


## 1.2 产品目标

- 统一管理多个网页模型入口与可用状态。
- 支持用户实时观察 Agent 当前执行阶段、计划、工具调用、等待登录、等待审批和最终结果。
- 通过 JSON/结构化计划，把纯文本模型结果转成受控的任务步骤。
- 提供可扩展 Tools Registry、本地工具、MCP Client/Server 管理及 Skills Registry。
- 使用 MySQL 持久化业务记录、Redis 管理短期状态/锁/事件转发，支持任务恢复与追踪。
- 支持上下文预算、摘要、历史裁剪、固定信息、上下文预览和重置。
- 先实现单用户本地 MVP，验证后再考虑多用户或商业化。

## 1.3 目标用户

首期：希望使用多个网页模型完成本地自动化的开发者/个人用户。后续：需要管理团队工具、Skill、MCP 服务或可审计 Agent 工作流的小团队。


## 1.4 首期非目标

- 不自建大模型，不把网页端包装为未经授权的公共 API。
- 不绕过验证码、风控、访问频率、付费墙或平台使用限制。
- 不保证所有模型网页支持扫码登录、持久化会话或长期稳定自动化。
- 首版不做云端多租户、他人账户共享、云端保存 Cookie 或浏览器认证文件。
- 不允许模型任意执行 shell、访问全盘文件、修改生产数据或直接外发消息。

# 2. 用户明确需求与补充需求总表

| 编号 | 需求 | 实现范围/解释 | 来源 |
| --- | --- | --- | --- |
| REQ-001 | 多个模型网页 | 千问、DeepSeek、豆包、ChatGPT；其他模型可扩展；Codex 独立评估。 | 用户明确 |
| REQ-002 | 免费网页端交互 | 以用户拥有访问权限的网页为入口；不承诺无限免费，不假设有官方 API tool call。 | 用户明确 |
| REQ-003 | Vue 实时交互 | 任务创建、步骤流、运行状态、日志、结果、暂停/取消/审批。 | 用户明确 |
| REQ-004 | 登录状态可见 | 显示待登录/已连接/会话过期/验证挑战/限流/页面异常/最后检查时间。 | 用户明确 |
| REQ-005 | 扫码登录 | 连接时打开官方网页；如果官方页面提供扫码，用户在官方页面完成扫码。 | 用户明确 |
| REQ-006 | 账户管理 | 每个平台可配置一个或多个账户别名/Profile（多账户并行需验证），管理启用/停用/重连。 | 用户明确 |
| REQ-007 | 多种 Session | 区分浏览器认证 Session、Agent 任务 Session、对话/上下文 Session、MCP 连接 Session。 | 用户明确 + 补充 |
| REQ-008 | 技术栈 | Vue 3 + TypeScript，Spring Boot，Spring AI Alibaba（适用处），Redis、MySQL、Playwright Java。 | 用户明确 + 补充 |
| REQ-009 | 结构化计划 | 模型输出受限 JSON 任务计划；本地解析、校验、授权后执行。 | 用户明确 + 关键补充 |
| REQ-010 | Tools 模块 | 内置工具统一注册、参数 Schema、权限等级、超时、结果与审计。 | 用户明确 |
| REQ-011 | MCP 模块 | 管理 MCP Client 连接、服务配置、工具发现/筛选、调用日志；可预留对外 MCP Server。 | 用户明确 |
| REQ-012 | Skills 模块 | Skill 注册、描述、版本、文件内容、适用工具、输入输出约定；支持按需加载。 | 用户明确 |
| REQ-013 | 上下文管理 | 字符数/Token 估算、预算阈值、消息历史、摘要、固定事实、裁剪和重置。 | 用户明确 |
| REQ-014 | 实时可观测 | 任务步骤、模型回应、工具执行、审批等待、错误、耗时和连接状态事件。 | 补充设计 |
| REQ-015 | 安全与审计 | 工具白名单、路径限制、人工审批、日志脱敏、会话文件本地保护。 | 补充设计 |
| REQ-016 | 可靠性 | 超时、限次重试、暂停/恢复/取消、任务状态持久化与幂等控制。 | 补充设计 |
| REQ-017 | 测试和验收 | 端到端 MVP 验收、适配器独立测试、事件顺序、上下文超限和安全测试。 | 补充设计 |


# 3. 用户角色与操作权限

| 角色 | 权限 | 首期处理 |
| --- | --- | --- |
| 本机管理员 | 模型配置、Profile 管理、MCP 服务配置、Tools/Skills 管理、上下文和系统配置。 | 单用户模式下即本机用户。 |
| 任务操作者 | 创建任务、选择模型/Skill/工具、暂停/取消、查看结果、批准高风险动作。 | 首期与管理员可以是同一人。 |
| 只读观察者 | 查看允许范围内任务、事件、结果，不修改配置、不执行工具。 | P2 预留。 |
| Agent Runtime | 只能调用当前任务被授予的能力；不能自己提升权限或修改安全策略。 | 系统内部身份。 |


# 4. 页面与前端交互需求

| 页面 | 展示内容 | 主要操作 |
| --- | --- | --- |
| Dashboard 总览 | 模型在线数、待登录/异常会话、运行任务、等待审批任务、最近任务与失败原因。 | 新建任务、处理登录、跳转任务详情。 |
| 模型账户管理 | Provider、账户别名、Profile、状态、最近检查、当前窗口、错误/限流提示。 | 添加、打开网页、登录/扫码、检查、重连、断开、禁用。 |
| Agent 工作台 | 任务输入、模型/Skill/工具选择、计划预览、运行事件流、模型回复、最终结果。 | 运行、暂停、继续、取消、补充指令、审批、导出。 |
| 执行计划视图 | 结构化步骤、工具名、参数摘要、依赖、状态、风险等级、审批状态。 | 批准计划、拒绝计划、查看参数、单步执行（开发调试模式）。 |
| Tools 管理 | 名称、描述、参数 Schema、风险级别、权限、调用耗时、可用状态。 | 启用/禁用、编辑配置、测试、查看调用日志。 |
| MCP 管理 | Server 名称、传输类型、连接状态、工具列表、资源/Prompt、权限策略。 | 添加/编辑/连接/断开、工具筛选、测试、查看日志。 |
| Skills 管理 | 名称、描述、版本、来源目录、依赖工具/MCP、适用条件、启用状态。 | 导入、校验、预览、启用/禁用、版本回滚。 |
| 上下文面板 | 消息数量、字符数、Token 估算、上下文预算、系统提示词、历史/摘要占用、固定信息。 | 调整预算、生成摘要、预览最终输入、固定内容、裁剪、重置、导出。 |
| 任务历史 | 任务 ID、模型、Skill、状态、创建/结束时间、耗时、事件数量、失败摘要。 | 查看轨迹、复制结果、导出、重试。 |
| 系统设置 | 数据目录、工作目录、浏览器路径、并发、超时、重试、Redis/MySQL 连接。 | 诊断环境、保存配置、清理会话、查看版本。 |


## 4.1 前端关键交互规则

- 连接模型后，前端持续显示清晰状态；不要用“在线”代表模型网页一定能成功发消息，连接状态与任务可执行状态应分开。
- 执行任务时实时显示当前阶段。网页模式不保证模型 token 级实时流；可先显示“等待回答/正在生成/正在读取回答”，不得伪造 token 流。
- 计划执行前允许查看结构化计划摘要；低风险可按配置自动执行，高风险必须提示确认。
- 任务取消后立即阻止后续步骤；若当前浏览器动作不可立即终止，应显示“正在停止”，并记录最终停止状态。
- 连接/等待用户的任务需要显示持有原因，例如“请在打开的千问页面完成登录”或“请确认写入文件操作”。
- 上下文超限提醒应明确说明是“应用侧估算预算”，不声称为网页模型真实 token 余额。

# 5. 模型账户、浏览器与 Session 管理


## 5.1 模型连接状态机

| 状态码 | 含义 | 前端呈现及处理 |
| --- | --- | --- |
| NOT_CONFIGURED | 尚未配置模型 | 添加模型。 |
| DISCONNECTED | 配置存在，浏览器未启动/已关闭 | 连接/打开登录页面。 |
| CONNECTING | 正在启动浏览器并访问网页 | 显示进度和开始时间。 |
| LOGIN_REQUIRED | 页面要求登录 | 提示用户自行登录或扫码。 |
| WAITING_USER | 需要人工操作/确认 | 等待用户完成网页操作。 |
| READY | 可见页面特征表明已登录且当前可交互 | 允许提交任务，但仍需任务前检查。 |
| SESSION_EXPIRED | 登录失效/被退出 | 暂停相关任务，等待重新登录。 |
| CHALLENGE_REQUIRED | 验证码、设备确认或其他安全挑战 | 停止自动化，要求用户处理。 |
| RATE_LIMITED | 页面提示限流/额度限制 | 停止重复操作；按政策退避或让用户换模型。 |
| PAGE_CHANGED | 页面关键控件不匹配 | 记录诊断，禁止误操作。 |
| ERROR | 浏览器或适配器异常 | 展示错误码与安全重试入口。 |
| DISABLED | 用户主动停用 | 不调度任务。 |


## 5.2 登录/扫码流程

1. 用户点击连接，系统启动该账户独立的本地浏览器上下文并打开官方网页。
1. 如果官方页面提供扫码登录，二维码由官方页面直接展示；用户通过该平台官方客户端扫码。系统不自行生成、截取、转发或模拟平台认证二维码。
1. 应用检测页面可见状态；若无法可靠判断登录成功，则允许用户点击“我已完成登录”并由适配器进行再次可用性检查。
1. 登录后将浏览器 Profile 与模型账户绑定；Profile 不跨模型复用。单 Profile 同一时间只允许一个 Agent 控制。
1. 会话过期、验证码、风控或平台安全提示出现时，任务进入 WAITING_USER 或对应终止状态。不得尝试绕过。
1. 允许主动断开/清理本地会话；清理前再次确认，因为会导致需要重新登录。

## 5.3 Session 分类

| 类型 | 职责 | 持久化策略 | 注意事项 |
| --- | --- | --- | --- |
| Browser/Auth Session | 第三方网站登录态和页面状态。 | 由本地浏览器 Profile 管理；只在数据库保存 profileKey 和状态元数据。 | 禁止 Cookie/Token 写入 MySQL/Redis/日志。 |
| Agent Task Session | 一次任务的步骤、状态、执行计划、审批和错误。 | MySQL 持久化；Redis 缓存运行状态/短期锁。 | 支持中断和恢复时需验证浏览器及步骤可恢复性。 |
| Conversation/Context Session | 对话消息、摘要、固定事实、上下文预算。 | MySQL 持久化，可按策略归档/删除；Redis 仅缓存活动状态。 | 应用上下文不一定等于网页实际上下文。 |
| MCP Connection Session | 与 MCP Server 的连接与能力发现状态。 | 配置存 MySQL；连接对象在运行时管理，状态可缓存。 | 不在日志写入连接密钥；远程 MCP 需要鉴权与权限策略。 |
| Agent Run/Execution Context | 当前运行轮次的临时变量、工具结果引用、预算计数。 | 运行时内存/Redis + MySQL 检查点。 | 限制 TTL、大小和敏感信息。 |


# 6. 结构化输出与 Agent 执行引擎（核心）


## 6.1 为什么必须采用结构化输出

免费网页模型的可控接口通常是“输入一段文本—读取一段文本”。即便提示模型返回 JSON，也不保证每次格式正确。因此应用需要一个独立的解析与校验层，把原始文本转为受约束的 AgentPlan；校验失败时最多进行有限次数的修复请求，仍失败则停止并向用户展示错误，绝不猜测参数或执行模糊命令。


## 6.2 计划生成与执行闭环

1. 接收用户目标，结合选中的 Skill、当前会话摘要和当前可用工具清单生成提示词。
1. 请求网页模型输出严格 JSON；要求模型仅引用已提供的工具 ID，不能创建新工具或编造工具参数。
1. 解析 JSON：去除可识别 Markdown 围栏、检查版本字段和必填字段；禁止宽松地把自然语言解析成命令。
1. 结构校验：JSON Schema/DTO 校验、字段类型、长度、枚举、步骤数量、依赖关系、参数 Schema。
1. 权限校验：检查工具是否启用、用户是否授权、路径/资源范围、风险等级和审批状态。
1. 执行器按依赖顺序调度步骤；将执行事件通过 SSE 推送前端；记录输入输出摘要和耗时。
1. 将工具结果转换为简洁、结构化的结果返回模型，模型可以提出下一步计划或最终答复。
1. 循环在达到完成条件、人工停止、任务步骤上限、模型调用上限、预算上限或不可恢复错误时终止。

## 6.3 AgentPlan JSON 示例

```text
{
  "schemaVersion": "1.0",
  "taskSummary": "分析指定目录中的文章并生成结构化摘要",
  "steps": [
    {
      "stepId": "step_1",
      "actionType": "TOOL_CALL",
      "target": "workspace.list_files",
      "arguments": {"directory": "./input", "pattern": "*.md"},
      "dependsOn": [],
      "riskLevel": "LOW",
      "expectedOutput": "待处理文件列表"
    },
    {
      "stepId": "step_2",
      "actionType": "TOOL_CALL",
      "target": "workspace.read_text",
      "arguments": {"fileRef": "${step_1.files[0].ref}"},
      "dependsOn": ["step_1"],
      "riskLevel": "LOW",
      "expectedOutput": "文章正文"
    }
  ],
  "completionCriteria": ["全部目标文件有摘要", "结果通过 JSON Schema 校验"],
  "requiresApproval": false
}
```

说明：示例用于解释协议，实际实现应定义严格 JSON Schema，并确定引用变量的允许语法。模型不可直接生成任意路径、任意 SQL 或 shell 命令；目录路径、资源引用和参数都必须由应用端再次检查。


## 6.4 建议的 AgentPlan 字段

| 字段 | 类型/示例 | 约束 |
| --- | --- | --- |
| schemaVersion | 字符串，如 1.0 | 必须是支持的版本。 |
| taskSummary | 字符串 | 长度限制；用于 UI 展示，不作为执行指令。 |
| steps | Step[] | 步骤数有上限；Step ID 唯一。 |
| stepId | 字符串 | 应用端校验唯一性，避免重复 ID。 |
| actionType | TOOL_CALL / MCP_CALL / ASK_USER / FINALIZE | 枚举白名单；不能允许模型任意指定 Java 类。 |
| target | 已注册的 toolId / mcpServerId+toolId | 必须存在于当前任务可见能力集合。 |
| arguments | JSON Object | 必须通过目标工具参数 Schema。 |
| dependsOn | Step ID 数组 | 禁止未知依赖和循环依赖。 |
| riskLevel | LOW / MEDIUM / HIGH / CRITICAL | 仅作提示；最终风险由本地策略计算，不信任模型自报等级。 |
| expectedOutput | 字符串 | 用于展示与结果校验，不能代替代码校验。 |
| completionCriteria | 字符串数组/可验证条件 | 需由代码实现的条件应绑定验证器。 |
| requiresApproval | 布尔值 | 只能额外触发审批，不能覆盖服务端策略。 |


## 6.5 模型输出解析策略

- 优先解析完整 JSON；可移除单层 Markdown JSON 围栏，但不应从杂乱文本中“尽力猜测”执行计划。
- 计划必须通过 JSON Schema 和业务规则两层校验；Schema 通过并不代表业务操作安全。
- 格式错误最多重试 1–2 次，提示模型只修复格式，不增加新任务；达到上限立即失败/等待用户。
- 记录原始模型回复（按隐私策略裁剪或脱敏）、解析结果、验证错误、修复次数和最终选择。
- 如果模型仅能返回纯文本，Agent 可进入“建议模式”，显示可读的步骤草案，但执行前需要用户确认或人工转成有效计划。

## 6.6 执行器安全规则

- 模型只能引用注册过的 toolId 或 MCP 工具别名；未知目标立即拒绝。
- 所有输入参数需要反序列化为类型化 DTO 并再次校验；禁止执行任意表达式。
- 文件工具限制到用户批准的 workspace，解析规范化路径后阻止路径穿越。
- 数据库工具优先只读；写操作使用参数化 SQL、限定 schema/表/操作类型，并按风险要求审批。
- 网络工具限制协议、域名、端口和超时；防止本地网络/元数据地址等 SSRF 风险。
- 高风险和不可逆操作在执行前显示清晰的操作内容与影响；审批结果绑定具体参数和计划版本，不能复用到已修改的计划。
- 每个任务有最大步骤数、模型轮次、工具调用数、总时长、重试次数与上下文预算。

# 7. Tools、MCP、Skills 三大模块


## 7.1 三者边界

| 模块 | 本质 | 系统职责 | 例子 |
| --- | --- | --- | --- |
| Tools | 真正执行动作的能力单元。 | 定义、注册、参数校验、权限、超时、执行、结果转换、审计。 | read_text_file、write_json、query_mysql_readonly、http_get、parse_csv。 |
| MCP | 工具/资源/提示模板的标准化外部接入协议。 | 配置 MCP Server、发现工具/资源/Prompts、筛选权限、连接健康检查、调用转发。 | 连接本地文件 MCP、Git MCP、内部 HTTP 服务 MCP。 |
| Skills | 可复用的任务方法与上下文包。 | 管理 Markdown 指令、工作流程、示例、依赖工具和输入输出约束；按需注入上下文。 | article-analysis、financial-report、repo-review。 |

MCP 规格将服务器能力分为 Prompts（用户控制的提示模板）、Resources（由应用选择并注入的上下文资源）和 Tools（可执行操作）；本项目应把它们在 UI 和权限体系中分开管理。参考：Model Context Protocol 官方规范，链接见“参考资料”。


## 7.2 Tools 模块需求

- 建立 ToolRegistry：toolId、名称、描述、输入 Schema、输出 Schema、分类、风险等级、启用状态、超时、权限和实现类。
- 工具类型至少包含：文件、文本/JSON、数据查询、数据库只读、HTTP 请求、时间/计算、上下文管理。
- 工具执行统一返回 ToolResult：success、code、message、data、duration、traceId；错误使用稳定错误码。
- 工具必须具备单元测试和权限测试；工具清单提供给模型的描述应短而明确，参数必填项必须准确。
- 默认只暴露当前 Skill/任务需要的工具，避免将全部工具说明塞进上下文。
- 危险工具不允许全局默认暴露，采用按任务授权和人工审批。

## 7.3 MCP 模块需求

- MCP Client Registry：新增/编辑/启用/停用服务配置，支持当前 Spring AI MCP 组件版本实际支持的传输类型。
- 连接前展示服务器来源、运行位置、传输类型、需要的权限和可见能力；用户确认后连接。
- 连接成功后发现 tools/resources/prompts，显示名称、描述、参数 Schema、来源服务和风险等级。
- 允许对服务器或单个工具启用/禁用、设置前缀避免命名冲突、设定调用超时与访问策略。
- 工具结果作为不可信输入；不能把远程 MCP 工具描述当作系统指令。
- 提供“测试连接”“刷新能力”“查看最近调用”和“断开连接”。
- 首期以 MCP Client 为主；是否把本应用自身能力暴露为 MCP Server 作为 P2。

## 7.4 Skills 模块需求

- Skill 使用可审阅的目录/Markdown 格式，包含 metadata（id、name、description、version）、任务说明、流程、示例、输出约束与推荐工具。
- 加载时只把 Skill 的名称和描述放入候选清单；选中后才按需加载完整内容，降低上下文消耗。
- Skill 可以引用工具 ID、MCP 工具别名和输出 Schema，但不能通过文本自动获得额外权限。
- 支持 Skill 导入、校验、预览、启用/禁用、版本号、变更日志和回滚。
- Skill 内容视为不可信配置；不允许从任意 Skill 文本自动执行内嵌 shell/脚本。
- Skill 的依赖工具未启用或不存在时，启动任务前提示缺少依赖。
- 首期内置 Skill：文章分析、目录文件归纳、结构化数据提取、报告生成（低风险）。

## 7.5 MCP、Tools、Skills 与执行引擎的关系

```text
用户目标
  -> Skill Selector（选择任务方法）
  -> Planner（生成结构化 AgentPlan）
  -> Plan Validator（Schema + 业务规则 + 权限）
  -> Scheduler / State Machine
       -> Built-in Tools (Java 函数)
       -> MCP Client (已授权的外部工具)
       -> Ask User / Approval Gate
  -> ToolResult / MCPResult
  -> Model Adapter（网页模型）
  -> 判断继续、总结或结束
```


# 8. 上下文工程与大小管理


## 8.1 目标与限制

上下文模块维护应用侧对话历史和本轮组装后的输入内容。网页模型可能自行保留当前网页对话历史，且网页模式通常无法可靠读取模型真实 Token 使用量、隐藏系统提示词或准确上下文窗口。因此 UI 必须区分“应用侧上下文预算/估算”和“平台真实限制（未知或平台可见）”。


## 8.2 上下文面板指标

| 指标 | 说明 |
| --- | --- |
| 消息数量 | 用户、模型、工具、系统/摘要消息的数量与轮次。 |
| 字符数 | 即将发送的文本字符数，按类型分项。 |
| Token 估算 | 优先使用适用 tokenizer；否则用可配置估算规则，并标明“估算”。 |
| 预算上限 | 本应用允许输入上下文使用的预算。 |
| 提示词占用 | 系统指令、任务目标、Skill、工具描述等估算占用。 |
| 历史对话占用 | 最近消息、旧消息摘要和固定事实的估算占用。 |
| 输出预留 | 为模型最终回答预留的预算；网页模型模式下仅为应用策略值。 |
| 摘要/裁剪状态 | 哪些消息被压缩、排除、固定或清理。 |
| 当前网页上下文同步 | 展示应用消息与网页当前会话是否可确认同步；未知必须标记未知。 |


## 8.3 上下文管理策略

| 策略 | 行为 | 默认建议 |
| --- | --- | --- |
| FULL_HISTORY | 在预算内保留完整消息历史。 | 短任务使用。 |
| SUMMARIZE_OLD | 旧消息生成摘要，保留最近 N 轮原文。 | 长对话默认。 |
| PINNED_PLUS_RECENT | 保留固定事实和最近消息，其他旧记录摘要化。 | 持续性工作流。 |
| MANUAL_TRIM | 用户选择移除部分历史或不发送某些消息。 | 需要精细控制。 |
| NEW_CONTEXT | 新建对话，只选择性带入摘要/固定事实。 | 切换项目或避免上下文污染。 |


## 8.4 预算与阈值

```text
effectiveInputBudget = appContextBudget - systemPromptEstimate - skillEstimate - toolSchemaEstimate - outputReserveEstimate

```

- 预算占用达到 70%：提示上下文接近阈值。
- 达到 85%：建议生成摘要或裁剪旧消息。
- 超过 100%：按策略摘要/裁剪；若处理后仍超限则阻止请求并提示用户。
- 用户可配置最大预算、保留最近轮数、摘要模型/策略（网页模型条件不支持 API 调用时，可通过当前网页适配器请求摘要）。
- 固定信息也计入预算；不得在 UI 中隐藏固定上下文的开销。

## 8.5 上下文操作与审计

- 可预览“本次将提交给网页的最终文本”，包含系统/Skill/任务/工具说明和选中的历史摘要。
- 生成摘要先预览，再由用户确认应用；原始消息是否保留由隐私/存储设置决定。
- 支持固定重要事实、解除固定、复制摘要到新会话、重置会话、导出和删除。
- 网页端可能会把之前的网页对话继续带入模型上下文。若无法清空/新建网页会话，需提醒用户应用侧重置不等于网页侧重置。
- 摘要失败、模型返回非预期格式或摘要丢失关键信息时，允许撤销摘要并回到原消息。

# 9. 实时任务、状态机与事件


## 9.1 任务状态

| 状态 | 含义 |
| --- | --- |
| CREATED | 任务已创建。 |
| QUEUED | 等待执行资源/浏览器 Profile 锁。 |
| CHECKING_MODEL | 检查模型与登录状态。 |
| WAITING_LOGIN | 等待用户登录/扫码。 |
| PLANNING | 请求模型生成计划。 |
| VALIDATING_PLAN | 解析并校验计划。 |
| WAITING_APPROVAL | 等待用户批准计划/风险操作。 |
| RUNNING_STEP | 执行当前工具/MCP/浏览器操作。 |
| WAITING_MODEL | 正在等待网页模型回答。 |
| SUMMARIZING_CONTEXT | 生成或压缩上下文。 |
| PAUSED | 用户暂停。 |
| CANCEL_REQUESTED | 收到取消请求，处理中。 |
| SUCCEEDED | 任务完成且结果校验通过。 |
| FAILED | 发生不可恢复错误。 |
| CANCELLED | 任务已取消。 |


## 9.2 实时事件结构

```text
TaskEvent {
  eventId, taskId, sequence, timestamp, eventType, level,
  stepId, status, userMessage, detailSummary, durationMs,
  modelId, toolId, requiresApproval
}
```

事件消息应采用追加序列号；前端重连时可按 lastEventId 补取历史事件。事件中的 arguments、工具结果和网页回答按敏感策略只存摘要或受控内容。


## 9.3 实时通信选型

| 通道 | 职责 | 首期决策 |
| --- | --- | --- |
| REST API | 创建任务、管理配置、查询历史、提交审批/暂停/取消命令。 | 必须实现。 |
| SSE | 后端单向推送状态变化、步骤、工具日志与最终结果。 | MVP 首选，简单适合任务事件流。 |
| WebSocket | 双向高频交互或未来多通道实时控制。 | P2；当前非必须。 |
| Redis Pub/Sub/Stream | 多实例事件转发或任务事件缓冲。 | 单机先用本地事件总线 + MySQL；多实例时评估。 |


# 10. 建议技术架构与模块职责


## 10.1 技术栈

| 层级 | 推荐技术 | 职责/备注 |
| --- | --- | --- |
| 前端 | Vue 3 + TypeScript + Vite | 工作台、模型状态、执行计划、上下文可视化。 |
| UI/状态 | Element Plus/Naive UI + Pinia | 选择一种 UI 库；状态和事件流管理。 |
| 通信 | REST + SSE | REST 下命令，SSE 收事件。 |
| 后端 | Java 17/21 + Spring Boot 3.5.x（与选定 SAA 版本兼容） | 业务 API、任务状态机、工具与权限。 |
| Agent 框架 | Spring AI Alibaba Agent Framework / Graph（按需求选） | 可用于规划循环、状态工作流、人机协同、Skills/工具集成；首版不应过度复杂。 |
| 结构化输出 | DTO/JSON Schema/Jackson 校验 | 网页模型模式下手动提示 JSON 并本地校验；API 模式可用框架结构化输出能力。 |
| 浏览器自动化 | Playwright Java | 每个网页模型独立适配器和浏览器 Profile。 |
| 关系数据库 | MySQL 8.x | 任务、事件、消息、Skills、MCP 配置元数据、工具执行记录。 |
| 缓存 | Redis | 短期运行状态、Profile 锁、状态缓存、可选事件发布。 |
| 迁移工具 | Flyway（建议） | 版本化管理数据库结构。 |
| 构建/测试 | Maven、JUnit 5、Spring Boot Test、Vitest | 后端/前端单测和集成测试。 |


## 10.2 Spring AI Alibaba 使用边界

Spring AI Alibaba 可用于 Agent Framework、Graph 工作流、上下文工程、人机协同、Skills 和适用的模型/API 集成；但它不会自动获得对第三方网页 DOM 的控制能力。网页模式仍由 Playwright Java + ModelAdapter 完成。MCP 也应使用与选定依赖版本兼容的 MCP Client 组件。

版本建议：截至本文生成时，官方文档标注 Spring AI Alibaba 1.1.2.0 与 Spring AI 1.1.2、Spring Boot 3.5.x 的兼容组合；官方 GitHub release 列表可见稳定版 1.1.2.2 和 2.0.0-M1.1 预发布版本。新项目优先选稳定版组合，并在开始开发当天再次核对 BOM 与依赖兼容表，不混用不同主版本。


## 10.3 后端逻辑模块

| 模块 | 职责 | 关键约束 |
| --- | --- | --- |
| ModelRegistry | 登记模型和适配器，提供模型能力清单。 | 不能假定每个站点能自动化。 |
| BrowserSessionManager | 管理 Playwright 生命周期、Profile 路径、锁和清理。 | 认证文件属于敏感信息。 |
| ConnectionMonitor | 按需/有限频率检查登录和页面状态。 | 不要高频轮询或反复发消息探测。 |
| AgentOrchestrator | 组织规划、执行、观察、继续/结束循环。 | 有最大步骤/时间/轮次限制。 |
| PlanParserValidator | 文本转 JSON DTO，Schema/业务规则验证。 | 错误格式不执行。 |
| TaskScheduler | 依赖调度、步骤并发、超时、取消、重试。 | Profile 粒度串行。 |
| ToolRegistry/Executor | 内置工具注册、参数验证和执行。 | 白名单+风险策略。 |
| McpManager | MCP Client 连接、工具发现、能力筛选。 | 不可信服务器隔离、工具名命名空间。 |
| SkillRegistry/Loader | Skill 文件登记、校验、按需加载。 | Skill 不等于执行权限。 |
| ContextManager | 消息、摘要、预算、裁剪、预览。 | Token 估算需标注。 |
| EventPublisher | 任务事件、SSE 推送和重连。 | 持久事件与瞬态广播分开。 |
| AuditService | 记录审批、配置变更和工具执行。 | 敏感字段脱敏。 |
| Persistence/Cache | MySQL Repository 与 Redis 状态/锁。 | MySQL 为事实来源。 |


## 10.4 逻辑架构图

```text
Vue 3 SPA
  Dashboard / Accounts / Agent Workspace / Plan / Context / Tools / MCP / Skills
       | REST commands + SSE event stream
       v
Spring Boot API
  Auth & Config | Agent Orchestrator | Plan Parser/Validator | Task Scheduler
  Context Manager | Tools Registry | MCP Manager | Skills Registry | Audit Service
       |
       +--> MySQL: tasks, messages, plan, step, events, configs, skills metadata
       +--> Redis: locks, runtime state, optional pub/sub, cache (not auth cookies)
       +--> Playwright Java -> per-site ModelAdapter -> official webpage session
       +--> Built-in Tools / MCP Client -> approved local or remote capabilities
```


# 11. REST API 初始清单

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | /api/models | 查询模型/账户配置和状态。 |
| POST | /api/models | 添加模型配置。 |
| PATCH | /api/models/{id} | 修改名称、Profile、适配器和启用状态。 |
| POST | /api/models/{id}/connect | 启动/打开官方登录页面。 |
| POST | /api/models/{id}/check | 执行连接状态检查。 |
| POST | /api/models/{id}/disconnect | 断开/关闭浏览器会话。 |
| GET | /api/models/{id}/events | 订阅模型连接状态事件。 |
| POST | /api/tasks | 创建 Agent 任务。 |
| GET | /api/tasks | 分页查询任务。 |
| GET | /api/tasks/{id} | 查询任务、计划、步骤、结果。 |
| POST | /api/tasks/{id}/approve | 审批计划/风险步骤，绑定计划版本。 |
| POST | /api/tasks/{id}/pause | 请求暂停。 |
| POST | /api/tasks/{id}/resume | 恢复任务。 |
| POST | /api/tasks/{id}/cancel | 取消任务。 |
| GET | /api/tasks/{id}/events | 订阅单个任务事件。 |
| GET | /api/tools | 查询已注册工具。 |
| PATCH | /api/tools/{id} | 更新启用状态/策略配置。 |
| GET | /api/mcp/servers | 列出 MCP Server 配置。 |
| POST | /api/mcp/servers | 添加 MCP Server。 |
| POST | /api/mcp/servers/{id}/connect | 连接并发现能力。 |
| GET | /api/mcp/servers/{id}/capabilities | 查看 Tools/Resources/Prompts。 |
| POST | /api/mcp/servers/{id}/disconnect | 断开 MCP 连接。 |
| GET | /api/skills | 查询 Skill 列表。 |
| POST | /api/skills/import | 导入并校验 Skill。 |
| PATCH | /api/skills/{id} | 启用/禁用或编辑元数据。 |
| GET | /api/conversations/{id}/context | 获取上下文统计与消息。 |
| PATCH | /api/conversations/{id}/context/settings | 修改预算、策略、保留轮数。 |
| POST | /api/conversations/{id}/context/summarize | 生成/预览摘要。 |
| POST | /api/conversations/{id}/context/apply-summary | 确认应用摘要。 |
| POST | /api/conversations/{id}/reset | 重置/新建上下文。 |
| GET | /api/settings | 查询非敏感配置。 |
| PATCH | /api/settings/agent | 修改步数、超时、重试、并发等。 |

每个接口实现时需补充 DTO、错误码、鉴权/授权、幂等策略、分页参数、请求体大小限制和日志脱敏。


# 12. MySQL 数据模型建议

| 表 | 关键字段建议 | 说明 |
| --- | --- | --- |
| model_profile | id, provider, display_name, account_alias, adapter_type, base_url, profile_key, enabled, status, last_checked_at, last_error_code | 仅存 Profile 标识，不存 Cookie。 |
| agent_task | id, title, selected_model_id, conversation_id, skill_id, status, current_step_id, plan_version, started_at, finished_at, error_code, result_json | 任务主记录。 |
| agent_plan | id, task_id, version, schema_version, raw_response_ref, validated_json, validation_status, created_at | 存已验证计划及版本；原始回复按隐私策略保存。 |
| agent_task_step | id, task_id, plan_id, step_id, action_type, target_id, arguments_json, depends_on_json, status, retry_count, timing | 每个计划步骤执行记录。 |
| agent_task_event | id, task_id, seq, event_type, level, message, payload_json, created_at | 任务时间线，支持按序号补拉。 |
| conversation | id, title, selected_model_id, context_budget, context_strategy, summary_id, created_at, updated_at, archived_at | 对话主表。 |
| conversation_message | id, conversation_id, role, content, char_count, token_estimate, pinned, summarized, source_type, created_at | 消息历史与估算。 |
| context_summary | id, conversation_id, summary_text, covered_from, covered_to, version, approved_at, created_at | 摘要版本和覆盖范围。 |
| tool_definition | id, tool_id, name, description, input_schema_json, output_schema_json, risk_level, enabled, timeout_ms | 内置工具元数据。 |
| tool_execution | id, task_id, step_id, tool_id, args_summary, result_summary, status, requires_approval, approved_at, duration_ms | 工具执行审计。 |
| mcp_server_config | id, name, transport_type, endpoint_or_command_ref, enabled, connection_status, trust_level, last_checked_at | 不得存明文密钥；本地/远程配置分开校验。 |
| mcp_capability | id, server_id, capability_type, capability_name, description, schema_json, enabled, risk_level | 缓存发现的 Tools/Resources/Prompts 元数据。 |
| skill_definition | id, skill_id, name, version, description, source_path, content_hash, enabled, validation_status | Skill 元数据与本地路径。 |
| prompt_template | id, name, template_content, output_schema_json, created_at, updated_at | 提示模板。 |
| approval_record | id, task_id, step_id, plan_version, action_summary, decision, decision_by, decided_at | 审批记录与计划版本绑定。 |

建议统一时间以 UTC 存储并由前端本地化显示。对大文本结果应评估存数据库还是受控结果文件，并仅在 MySQL 保存引用/校验摘要。


# 13. Redis 设计边界

| 用途 | 示例 Key/策略 | 注意 |
| --- | --- | --- |
| 运行状态缓存 | agent:task:{taskId}:runtime；设置 TTL。 | MySQL 保存最终状态和检查点。 |
| 浏览器 Profile 锁 | lock:browser:{profileKey}；租约、超时和 ownerId。 | 防止两个任务同时操纵同一页面。 |
| 连接状态缓存 | model:status:{modelId}。 | 可重建；不要视为唯一事实来源。 |
| 事件转发 | task-events:{taskId} Stream/PubSub（规模需要时）。 | 单机 MVP 可先用本地事件总线。 |
| 短时限流/重试计数 | agent:budget:{taskId}。 | 防止任务无限请求。 |

绝不将第三方网页登录 Cookie、Local Storage 导出、认证 Token、密码或二维码凭据放入 Redis。


# 14. 安全、隐私与平台合规

- 默认仅监听 localhost；如需局域网访问，必须增加身份认证、CSRF/来源校验、访问控制和 HTTPS 配置。
- 浏览器 Profile 目录包含敏感登录态，必须放入 .gitignore、限制操作系统文件权限、提供断开与清理功能。
- 后端工具执行采用注册白名单；禁止把模型内容拼接成任意 shell/SQL/文件路径直接执行。
- 限制本地工作目录，规范化路径并阻断路径穿越；网络工具限制协议、主机和超时。
- 把网页回答、Skill、MCP Server 返回内容都视为不可信输入，不能覆盖系统安全策略。
- 文件覆盖/删除、数据库写操作、外发消息、代码执行等操作需分级审批；审批必须绑定明确参数和计划版本。
- 日志仅保留必要摘要；不得记录 Cookie、密码、Token、二维码认证信息或其他敏感凭据。
- 平台出现验证码、风控、限流或登录挑战时停止自动化并让用户处理，不尝试绕过。
- 面向多人或商业化之前，重新设计账号隔离、审计、数据删除、授权和每个平台规则的合规审查。

# 15. 非功能需求

| 类别 | 要求 |
| --- | --- |
| 实时性 | 任务创建后尽快在 UI 显示；服务端任务状态事件目标在 1 秒内推送。网页生成时间由第三方决定。 |
| 可恢复性 | 任务、计划版本与步骤状态持久化；异常后可明确标记失败/等待用户，不把不确定步骤自动重放。 |
| 并发控制 | MVP 同一 Browser Profile 同时只能被一个自动化任务持有；跨 Profile 并行数量可配置。 |
| 可维护性 | 每个站点 Adapter 独立；单站点 DOM 变化不应影响其他模型和执行引擎。 |
| 可观测性 | Task ID、Plan Version、Step ID、Model ID、Tool ID、时长、状态和错误码可追踪。 |
| 数据一致性 | MySQL 为持久数据事实源；Redis 仅运行态/缓存/锁，不作为唯一的任务结果存储。 |
| 上下文透明 | 显示 token 估算方法与时间；网页实际窗口未知时必须显示未知。 |
| 可扩展性 | 增加模型、工具、MCP 服务或 Skill 时，应尽量通过配置/插件注册，不改核心编排逻辑。 |
| 隐私 | 允许清理对话、结果和浏览器 Profile；有明确的删除/归档行为与提示。 |


# 16. MVP 验收标准

- [ ] 前端显示模型列表、账户别名、连接状态、最后检查时间、错误提示和重新登录操作。
- [ ] 连接模型时能打开官方网页；若该网页提供扫码登录，用户可在官方页面自行完成；应用不收集密码。
- [ ] 至少一个模型适配器完成手动登录后发送文字提示词、等待回答、读取回答并展示结果。
- [ ] 另一个模型适配器通过统一接口接入，而不改 AgentOrchestrator 核心流程。
- [ ] 模型输出结构化 JSON 计划时，应用能解析并进行 Schema/业务校验；非法 JSON 不执行，有限修复失败后明确停止。
- [ ] 执行引擎只调用白名单工具；未知 toolId、越权参数和未审批的高风险操作都会被拒绝。
- [ ] Tools 模块至少提供读取工作区文本、列出文件、解析 JSON、写入结果文件等能力。
- [ ] MCP 管理页可登记、连接、发现能力并启用/禁用单个工具；若当前阶段不接真实 MCP Server，至少有配置和接口测试。
- [ ] Skills 模块支持导入、校验、启用/禁用和按需加载 Skill 内容。
- [ ] 前端通过 SSE 实时显示规划、校验、等待模型、执行工具、等待审批、成功/失败等事件。
- [ ] 任务、步骤、事件、上下文消息和执行结果元数据存入 MySQL；Redis 锁能阻止同 Profile 并行操作。
- [ ] 上下文面板展示字符数、Token 估算、预算、提示词/历史/Skill/工具说明占用、摘要/裁剪记录。
- [ ] 达到 70% 预算提醒、85% 建议摘要、超过 100% 时按策略压缩/阻止发送，且估算标记清楚。
- [ ] 登录过期、验证码、限流、页面变化和浏览器退出都会暂停或失败，不会误报成功或无限重试。
- [ ] 浏览器会话文件、Cookie 和 Token 不进入 Git、不写入 Redis/MySQL 明文、不出现在普通日志。
- [ ] 任务可取消；重复点击不会导致同一任务被重复执行或结果串线；所有任务有最大步数、超时和重试上限。
- [ ] README 列出平台验证状态、已知限制、启动步骤、Profile 目录和会话清理方式。

# 17. 测试策略

| 测试类别 | 关键用例 |
| --- | --- |
| 单元测试 | Plan JSON 解析、Schema 校验、依赖环检测、工具参数校验、上下文预算、风险等级计算。 |
| 适配器测试 | 登录状态检测、页面控件存在性、发送、生成完成判断、读取回答、超时、页面改版。 |
| 执行器测试 | 工具白名单、审批拦截、取消、重试上限、计划版本变化、幂等与状态迁移。 |
| 上下文测试 | 中文/英文/代码估算、摘要/恢复、固定信息计入预算、超过预算的阻止逻辑。 |
| MCP 测试 | 连接失败、能力发现、工具禁用、命名冲突、权限和超时、远程结果不可信处理。 |
| Skill 测试 | 缺少依赖工具、无效 metadata、路径越界、版本切换、内容按需加载。 |
| 集成测试 | Vue SSE 重连、事件顺序、任务历史查询、MySQL/Redis 容错、浏览器意外关闭。 |
| 安全测试 | 路径穿越、恶意工具参数、任意命令注入、SSR F、日志脱敏、未授权 API 调用。 |


# 18. 开发里程碑

| 阶段 | 交付物 | 完成门槛 |
| --- | --- | --- |
| M0 技术验证 | 单网页模型 Playwright Demo、登录状态检测、提示词发送/回答读取、平台规则核验。 | 完成一次端到端流程，确认技术可行性和限制。 |
| M1 基础工程 | Vue + Spring Boot 工程、MySQL、Redis、模型配置 API、连接状态页。 | 前后端联调，状态可更新。 |
| M2 实时 Agent | 任务状态机、SSE、结构化计划、校验器、执行轨迹。 | 非法计划不会被执行；过程可实时观测。 |
| M3 Tools & Context | 工具注册表、低风险本地工具、上下文预算/摘要/裁剪 UI。 | 通过工具权限与上下文验收。 |
| M4 Skills & MCP | Skill Registry/Loader，MCP Client Registry、能力发现和工具筛选。 | Skill/MCP 可在执行前明确展示与授权。 |
| M5 第二模型适配 | 第二个已验证网页模型适配器、统一接口回归测试。 | 扩展适配器不改编排核心。 |
| M6 增强与评估 | 更多模型、Codex 独立评估、并行任务、工具沙箱、模型对比。 | 按风险和收益单独立项。 |


# 19. 风险、默认决策与待确认事项

| 事项/风险 | 本稿默认决策 | 后续确认/应对 |
| --- | --- | --- |
| 免费网页模型可靠性 | 不保证无限免费或稳定；应用支持暂停与人工切换。 | 逐个平台核验使用条款和当前限制。 |
| 扫码登录流程 | 由官方网页展示，用户直接扫码。 | 不同平台现场验证登录后状态识别。 |
| 结构化输出可靠性 | JSON 只是模型文本；必须本地解析/Schema/权限校验。 | 定义具体 AgentPlan JSON Schema。 |
| 框架使用边界 | SAA 负责合适的 Agent/Graph/Skills/MCP 集成；Playwright 负责网页。 | M0 后选定实际依赖版本和最小模块。 |
| MCP 安全 | 先启用少量可信服务，工具逐个批准。 | 设置 MCP 来源白名单、命名空间和调用风险。 |
| Skill 信任 | Skill 是指令与知识包，不是权限授权。 | 采用审查、版本、依赖检查和按需加载。 |
| 网页上下文精确度 | 网页模式 token 用量为估算；网页内部历史可能不同步。 | 尽量提供最终输入预览及网页会话重置提醒。 |
| Codex 接入 | 独立评估，不当作普通聊天网页。 | 明确目标产品形态（Web/桌面/CLI）及允许的官方接入方式。 |
| 目标 OS/运行形态 | 首期默认单人本地运行，浏览器与后端运行在同一设备。 | 确认 Windows/macOS/Linux；影响浏览器启动与部署脚本。 |
| 是否商业化 | 当前不做多租户设计。 | 如商业化需重新评审平台条款、账号隔离和隐私设计。 |


# 20. 推荐项目目录

```text
webmodel-agent/
├── frontend/                         # Vue 3 + TypeScript
│   └── src/
│       ├── api/                      # REST 与 SSE 客户端
│       ├── views/                    # Dashboard/Models/Workspace/History/Settings
│       ├── components/               # EventTimeline/PlanViewer/ContextPanel
│       ├── stores/                   # Pinia 状态
│       ├── composables/               # useTaskEvents/useModelStatus
│       └── types/                    # DTO 与事件类型
├── backend/
│   └── src/main/java/.../
│       ├── controller/               # REST Controllers
│       ├── application/              # AgentOrchestrator/TaskService
│       ├── agent/plan/                # PlanParser/SchemaValidator/PolicyValidator
│       ├── agent/runtime/             # StateMachine/Scheduler/RunContext
│       ├── model/adapter/              # ModelAdapter 与每个网页 Provider
│       ├── browser/                   # Playwright/BrowserProfile/Locks
│       ├── tools/                     # ToolRegistry/BuiltinTools/ToolPolicy
│       ├── mcp/                       # McpManager/CapabilityRegistry
│       ├── skills/                    # SkillRegistry/SkillLoader/Validator
│       ├── context/                   # Budget/Summary/Trimming/Preview
│       ├── events/                    # TaskEvent/SSE Publisher
│       ├── persistence/               # MySQL Entities/Repositories
│       ├── cache/                     # Redis locks and runtime state
│       ├── security/                  # Path policy/permissions/secret redaction
│       └── config/                    # Spring/Playwright/Redis/MySQL settings
├── workspace/                         # 受控输入/输出目录
├── browser-profiles/                  # 本地登录配置，必须 gitignore
├── skills/                            # 已审核的本地 Skill 目录
├── logs/
├── docker-compose.yml                 # 开发环境 MySQL + Redis（可选）
├── .gitignore
└── README.md
```


# 21. 参考资料与版本核验

以下官方资料用于确认技术边界和版本选型。依赖版本必须在开工时再次核验，不应机械复制文档中的示例版本。

| 资料 | 官方链接 |
| --- | --- |
| Spring AI Alibaba 版本说明（兼容矩阵、BOM、Agent Framework / Skills / Graph 组件） | https://java2ai.com/docs/versions/ |
| Spring AI Alibaba GitHub Releases（稳定版与预发布版信息） | https://github.com/alibaba/spring-ai-alibaba/releases |
| Spring AI Structured Output（结构化输出通常是提示模型遵循 Schema，再解析为强类型；格式仍可能失败） | https://docs.spring.io/spring-ai/reference/api/structured-output.html |
| Spring AI Tool Calling（应用层负责实际执行工具，工具执行权限属于应用） | https://docs.spring.io/spring-ai/reference/api/tools.html |
| Model Context Protocol Server Features（Prompts、Resources、Tools 的控制边界） | https://modelcontextprotocol.io/specification/draft/server/index |
| Playwright Java Authentication（复用登录态与认证状态文件的安全风险） | https://playwright.dev/java/docs/auth |
| Spring AI Alibaba 官方仓库（Agent Framework、Graph、MCP、Skills 等项目概览） | https://github.com/alibaba/spring-ai-alibaba |


# 22. 最终执行建议

先完成 M0，而不是立即实现所有平台。第一条竖向切片应包含：Vue 发起任务 → Spring Boot 检查模型登录态 → Playwright 发送文本 → 读取模型 JSON 计划 → 本地校验 → 执行一个低风险 Tool → SSE 实时展示 → MySQL 保存完整记录 → 上下文面板显示估算。完成后再增加 Skills 和 MCP；先接入一个可信 MCP Server，再扩展多个网页模型。这样能尽早验证产品最关键的技术风险，并避免在网页适配尚不可靠时过早构建庞大框架。
