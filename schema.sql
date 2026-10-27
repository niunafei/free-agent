-- ============================================================
-- free-agent v4.0 - 数据库初始化脚本（用户隔离 + 对话式）
-- MySQL 8.x  UTF8MB4
-- ============================================================

CREATE DATABASE IF NOT EXISTS webmodel_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE webmodel_agent;

-- ---------------------------- 用户表 ----------------------------
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username      VARCHAR(64)  NOT NULL COMMENT '登录用户名(唯一)',
    password_hash VARCHAR(128) NOT NULL COMMENT '密码哈希值(BCrypt)',
    display_name  VARCHAR(128) NULL COMMENT '显示名称',
    email         VARCHAR(128) NULL COMMENT '邮箱地址',
    phone         VARCHAR(32)  NULL COMMENT '手机号码',
    role          VARCHAR(32)  NOT NULL DEFAULT 'USER' COMMENT '角色(ADMIN/USER)',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='用户账号';

-- ---------------------------- 模型账户配置 ----------------------------
DROP TABLE IF EXISTS model_profile;
CREATE TABLE model_profile (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT       NOT NULL COMMENT '所属用户ID',
    provider        VARCHAR(32)  NOT NULL COMMENT '模型平台标识(qianwen/doubao/deepseek/kimi/zhipu/yuanbao/chatgpt/claude/grok/gemini)',
    display_name    VARCHAR(128) NOT NULL COMMENT '模型显示名称',
    account_alias   VARCHAR(128) NOT NULL COMMENT '账户别名(同一平台多账户区分)',
    adapter_type    VARCHAR(64)  NOT NULL DEFAULT 'WEB_PAGE' COMMENT '适配器类型(WEB_PAGE/API)',
    base_url        VARCHAR(512) NULL COMMENT 'API基础地址(API模式使用)',
    profile_key     VARCHAR(128) NOT NULL COMMENT '账户唯一标识',
    enabled         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用(0=禁用 1=启用)',
    status          VARCHAR(32)  NOT NULL DEFAULT 'NOT_CONFIGURED' COMMENT '连接状态(NOT_CONFIGURED/CONNECTING/WAITING_USER/READY/ERROR)',
    login_session_id VARCHAR(64) NULL COMMENT '网页登录会话ID(Playwright浏览器实例标识)',
    last_checked_at DATETIME     NULL COMMENT '最后检测登录状态时间',
    last_error_code VARCHAR(64)  NULL COMMENT '最后错误码',
    last_error_msg  VARCHAR(512) NULL COMMENT '最后错误信息',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_provider_alias (user_id, provider, account_alias),
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB COMMENT='模型账户配置(按用户隔离)';

-- ---------------------------- 对话/上下文 ----------------------------
DROP TABLE IF EXISTS conversation;
CREATE TABLE conversation (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id           BIGINT       NOT NULL COMMENT '所属用户ID',
    title             VARCHAR(256) NULL COMMENT '对话标题',
    selected_model_id BIGINT       NULL COMMENT '当前选中的模型ID(关联model_profile.id)',
    working_dir       VARCHAR(512) NULL COMMENT '工作目录路径(Agent文件操作根目录)',
    context_budget    INT          NOT NULL DEFAULT 32000 COMMENT '上下文Token预算上限',
    context_strategy  VARCHAR(32)  NOT NULL DEFAULT 'SUMMARIZE_OLD' COMMENT '上下文裁剪策略(SUMMARIZE_OLD/TRUNCATE_OLD)',
    keep_recent_turn  INT          NOT NULL DEFAULT 6 COMMENT '保留最近对话轮数',
    summary_id        BIGINT       NULL COMMENT '当前生效的摘要ID(关联context_summary.id)',
    archived_at       DATETIME     NULL COMMENT '归档时间(NULL=未归档)',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='对话会话(按用户隔离)';

DROP TABLE IF EXISTS conversation_message;
CREATE TABLE conversation_message (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    conversation_id BIGINT       NOT NULL COMMENT '所属对话ID(关联conversation.id)',
    role            VARCHAR(16)  NOT NULL COMMENT '消息角色(USER/MODEL/TOOL/SYSTEM/SUMMARY)',
    content         MEDIUMTEXT   NULL COMMENT '消息文本内容',
    char_count      INT          NOT NULL DEFAULT 0 COMMENT '字符数统计',
    token_estimate  INT          NOT NULL DEFAULT 0 COMMENT 'Token估算数(字符数/3)',
    pinned          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否置顶(0=否 1=是)',
    summarized      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已被摘要(0=否 1=是)',
    source_type     VARCHAR(32)  NULL COMMENT '来源类型(MODEL_REPLY/TOOL_RESULT等)',
    status          VARCHAR(32)  NULL COMMENT '发送状态(SENDING/SUCCESS/ERROR)',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_conv (conversation_id)
) ENGINE=InnoDB COMMENT='对话消息';

DROP TABLE IF EXISTS context_summary;
CREATE TABLE context_summary (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    conversation_id BIGINT       NOT NULL COMMENT '所属对话ID(关联conversation.id)',
    summary_text    MEDIUMTEXT   NULL COMMENT '摘要文本内容',
    covered_from    BIGINT       NOT NULL COMMENT '覆盖起始消息ID',
    covered_to      BIGINT       NOT NULL COMMENT '覆盖结束消息ID',
    version         INT          NOT NULL DEFAULT 1 COMMENT '摘要版本号(递增)',
    approved_at     DATETIME     NULL COMMENT '审批通过时间(NULL=未审批)',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_conv (conversation_id)
) ENGINE=InnoDB COMMENT='上下文摘要版本';

-- ---------------------------- Agent 任务 ----------------------------
DROP TABLE IF EXISTS agent_task;
CREATE TABLE agent_task (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id           BIGINT       NOT NULL COMMENT '所属用户ID',
    title             VARCHAR(256) NOT NULL COMMENT '任务标题',
    selected_model_id BIGINT       NULL COMMENT '执行任务的模型ID(关联model_profile.id)',
    conversation_id   BIGINT       NULL COMMENT '关联对话ID(关联conversation.id)',
    skill_id          BIGINT       NULL COMMENT '关联技能ID(关联skill_definition.id)',
    status            VARCHAR(32)  NOT NULL DEFAULT 'CREATED' COMMENT '任务状态(CREATED/PLANNING/RUNNING/PAUSED/SUCCESS/FAILED/CANCELLED)',
    current_step_id   VARCHAR(64)  NULL COMMENT '当前执行步骤ID',
    plan_version      INT          NOT NULL DEFAULT 0 COMMENT '计划版本号',
    started_at        DATETIME     NULL COMMENT '任务开始时间',
    finished_at       DATETIME     NULL COMMENT '任务完成时间',
    error_code        VARCHAR(64)  NULL COMMENT '错误码',
    error_msg         VARCHAR(512) NULL COMMENT '错误信息',
    result_json       MEDIUMTEXT   NULL COMMENT '任务结果(JSON格式)',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB COMMENT='Agent任务(按用户隔离)';

DROP TABLE IF EXISTS agent_plan;
CREATE TABLE agent_plan (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id           BIGINT       NOT NULL COMMENT '所属任务ID(关联agent_task.id)',
    version           INT          NOT NULL COMMENT '计划版本号',
    schema_version    VARCHAR(16)  NOT NULL DEFAULT '1.0' COMMENT '计划Schema版本',
    raw_response_ref  VARCHAR(512) NULL COMMENT '模型原始响应引用',
    validated_json    MEDIUMTEXT   NULL COMMENT '校验后的计划JSON',
    validation_status VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '校验状态(PENDING/VALID/INVALID)',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task (task_id)
) ENGINE=InnoDB COMMENT='Agent执行计划及版本';

DROP TABLE IF EXISTS agent_task_step;
CREATE TABLE agent_task_step (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id         BIGINT       NOT NULL COMMENT '所属任务ID(关联agent_task.id)',
    plan_id         BIGINT       NOT NULL COMMENT '所属计划ID(关联agent_plan.id)',
    step_id         VARCHAR(64)  NOT NULL COMMENT '步骤唯一标识(计划内唯一)',
    action_type     VARCHAR(32)  NOT NULL COMMENT '动作类型(MODEL_CALL/TOOL_CALL/CONDITIONAL/LOOP等)',
    target_id       VARCHAR(128) NOT NULL COMMENT '目标标识(模型ID或工具ID)',
    arguments_json  MEDIUMTEXT   NULL COMMENT '步骤参数(JSON格式)',
    depends_on_json VARCHAR(512) NULL COMMENT '依赖步骤ID列表(JSON数组)',
    risk_level      VARCHAR(16)  NOT NULL DEFAULT 'LOW' COMMENT '风险等级(LOW/MEDIUM/HIGH)',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '步骤状态(PENDING/RUNNING/SUCCESS/FAILED/SKIPPED)',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    started_at      DATETIME     NULL COMMENT '步骤开始时间',
    finished_at     DATETIME     NULL COMMENT '步骤完成时间',
    duration_ms     BIGINT       NULL COMMENT '执行耗时(毫秒)',
    PRIMARY KEY (id),
    KEY idx_task (task_id)
) ENGINE=InnoDB COMMENT='计划步骤执行记录';

DROP TABLE IF EXISTS agent_task_event;
CREATE TABLE agent_task_event (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id           BIGINT       NOT NULL COMMENT '所属任务ID(雪花算法生成)',
    seq               BIGINT       NOT NULL COMMENT '事件序号(同一task_id内递增)',
    event_type        VARCHAR(32)  NOT NULL COMMENT '事件类型(USER_MESSAGE/MODEL_REPLY/TOOL_CALL等)',
    level             VARCHAR(16)  NOT NULL DEFAULT 'INFO' COMMENT '日志级别(INFO/WARN/ERROR)',
    step_id           VARCHAR(64)  NULL COMMENT '关联步骤ID',
    status            VARCHAR(32)  NULL COMMENT '事件状态',
    message           VARCHAR(512) NULL COMMENT '事件描述信息',
    payload_json      MEDIUMTEXT   NULL COMMENT '事件负载数据(JSON格式)',
    duration_ms       BIGINT       NULL COMMENT '耗时(毫秒)',
    model_id          BIGINT       NULL COMMENT '关联模型ID',
    tool_id           VARCHAR(128) NULL COMMENT '关联工具ID',
    requires_approval TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否需要审批(0=否 1=是)',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_seq (task_id, seq),
    KEY idx_task (task_id)
) ENGINE=InnoDB COMMENT='任务事件时间线';

-- ---------------------------- 工具（全局共享） ----------------------------
DROP TABLE IF EXISTS tool_definition;
CREATE TABLE tool_definition (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tool_id            VARCHAR(128) NOT NULL COMMENT '工具唯一标识(如workspace.read_text)',
    name               VARCHAR(128) NOT NULL COMMENT '工具名称',
    description        VARCHAR(512) NULL COMMENT '工具描述',
    category           VARCHAR(32)  NULL COMMENT '工具分类(FILE/TEXT/HTTP/DB等)',
    input_schema_json  MEDIUMTEXT   NULL COMMENT '输入参数Schema(JSON格式)',
    output_schema_json MEDIUMTEXT   NULL COMMENT '输出结果Schema(JSON格式)',
    risk_level         VARCHAR(16)  NOT NULL DEFAULT 'LOW' COMMENT '风险等级(LOW/MEDIUM/HIGH)',
    enabled            TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用(0=禁用 1=启用)',
    is_system          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否系统默认(0=否 1=是,系统默认仅admin可配置)',
    timeout_ms         BIGINT       NOT NULL DEFAULT 30000 COMMENT '执行超时时间(毫秒)',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tool_id (tool_id)
) ENGINE=InnoDB COMMENT='内置工具元数据(全局)';

DROP TABLE IF EXISTS tool_execution;
CREATE TABLE tool_execution (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id           BIGINT       NOT NULL COMMENT '所属任务ID(关联agent_task.id)',
    step_id           VARCHAR(64)  NULL COMMENT '关联步骤ID',
    tool_id           VARCHAR(128) NOT NULL COMMENT '执行的工具ID(关联tool_definition.tool_id)',
    args_summary      VARCHAR(512) NULL COMMENT '参数摘要',
    result_summary    MEDIUMTEXT   NULL COMMENT '结果摘要',
    status            VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '执行状态(PENDING/RUNNING/SUCCESS/FAILED)',
    requires_approval TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否需要审批(0=否 1=是)',
    approved_at       DATETIME     NULL COMMENT '审批时间',
    duration_ms       BIGINT       NULL COMMENT '执行耗时(毫秒)',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task (task_id)
) ENGINE=InnoDB COMMENT='工具执行审计';

-- ---------------------------- MCP（按用户隔离） ----------------------------
DROP TABLE IF EXISTS mcp_server_config;
CREATE TABLE mcp_server_config (
    id                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id                 BIGINT       NULL COMMENT '所属用户ID(NULL=系统默认配置,所有用户共享)',
    name                    VARCHAR(128) NOT NULL COMMENT 'MCP Server名称',
    transport_type          VARCHAR(32)  NOT NULL COMMENT '传输类型(STDIO/SSE/HTTP)',
    endpoint_or_command_ref VARCHAR(512) NULL COMMENT '端点地址或命令路径',
    enabled                 TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用(0=禁用 1=启用)',
    connection_status       VARCHAR(32)  NOT NULL DEFAULT 'DISCONNECTED' COMMENT '连接状态(DISCONNECTED/CONNECTING/CONNECTED/ERROR)',
    trust_level             VARCHAR(16)  NOT NULL DEFAULT 'LOW' COMMENT '信任等级(LOW/MEDIUM/HIGH)',
    is_system               TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否系统默认(0=否 1=是,系统默认用户不可见仅admin可见)',
    last_checked_at         DATETIME     NULL COMMENT '最后检测连接时间',
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_name (user_id, name)
) ENGINE=InnoDB COMMENT='MCP Server配置(NULL user_id=系统默认,否则按用户隔离)';

DROP TABLE IF EXISTS mcp_capability;
CREATE TABLE mcp_capability (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    server_id       BIGINT       NOT NULL COMMENT '所属MCP Server ID(关联mcp_server_config.id)',
    capability_type VARCHAR(16)  NOT NULL COMMENT '能力类型(TOOL/RESOURCE/PROMPT)',
    capability_name VARCHAR(128) NOT NULL COMMENT '能力名称',
    description     VARCHAR(512) NULL COMMENT '能力描述',
    schema_json     MEDIUMTEXT   NULL COMMENT '能力Schema(JSON格式)',
    enabled         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用(0=禁用 1=启用)',
    risk_level      VARCHAR(16)  NOT NULL DEFAULT 'LOW' COMMENT '风险等级(LOW/MEDIUM/HIGH)',
    PRIMARY KEY (id),
    KEY idx_server (server_id)
) ENGINE=InnoDB COMMENT='MCP发现的能力';

-- ---------------------------- Skills（按用户隔离） ----------------------------
DROP TABLE IF EXISTS skill_definition;
CREATE TABLE skill_definition (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id          BIGINT       NULL COMMENT '所属用户ID(NULL=系统默认配置,所有用户共享)',
    skill_id         VARCHAR(128) NOT NULL COMMENT '技能唯一标识',
    name             VARCHAR(128) NOT NULL COMMENT '技能名称',
    version          VARCHAR(32)  NOT NULL DEFAULT '1.0.0' COMMENT '技能版本号',
    description      TEXT NULL COMMENT '技能描述',
    source_path      VARCHAR(512) NOT NULL COMMENT '技能源文件路径',
    content_hash     VARCHAR(64)  NULL COMMENT '内容哈希值(变更检测)',
    enabled          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用(0=禁用 1=启用)',
    is_system        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否系统默认(0=否 1=是,系统默认用户不可见仅admin可见)',
    validation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '校验状态(PENDING/VALID/INVALID)',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_skill_id (user_id, skill_id)
) ENGINE=InnoDB COMMENT='Skill元数据(NULL user_id=系统默认,否则按用户隔离)';

-- ---------------------------- 提示模板 ----------------------------
DROP TABLE IF EXISTS prompt_template;
CREATE TABLE prompt_template (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name               VARCHAR(128) NOT NULL COMMENT '模板名称(唯一)',
    template_content   MEDIUMTEXT   NULL COMMENT '模板内容(支持变量占位符)',
    output_schema_json MEDIUMTEXT   NULL COMMENT '输出Schema(JSON格式)',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB COMMENT='提示模板';

-- ---------------------------- 审批记录 ----------------------------
DROP TABLE IF EXISTS approval_record;
CREATE TABLE approval_record (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id        BIGINT       NOT NULL COMMENT '所属任务ID(关联agent_task.id)',
    step_id        VARCHAR(64)  NULL COMMENT '关联步骤ID',
    plan_version   INT          NOT NULL COMMENT '计划版本号',
    action_summary VARCHAR(512) NULL COMMENT '审批动作摘要',
    decision       VARCHAR(16)  NOT NULL COMMENT '审批决定(APPROVED/REJECTED)',
    decision_by    VARCHAR(128) NULL COMMENT '审批人',
    decided_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    PRIMARY KEY (id),
    KEY idx_task (task_id)
) ENGINE=InnoDB COMMENT='审批记录';

-- ---------------------------- 系统设置（全局） ----------------------------
DROP TABLE IF EXISTS sys_setting;
CREATE TABLE sys_setting (
    setting_key   VARCHAR(128) NOT NULL COMMENT '配置键(唯一)',
    setting_value VARCHAR(1024) NULL COMMENT '配置值',
    description   VARCHAR(256) NULL COMMENT '配置描述',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB COMMENT='系统非敏感配置';

-- ---------------------------- 初始数据 (幂等,可重复执行) ----------------------------
-- 管理员账号 (用户名: admin, 密码: admin123, SHA-256哈希)
INSERT INTO user (username, password_hash, display_name, role) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '系统管理员', 'ADMIN')
ON DUPLICATE KEY UPDATE role = 'ADMIN';

-- 系统默认工具定义
INSERT INTO tool_definition (tool_id, name, description, category, risk_level, enabled, is_system, timeout_ms, input_schema_json) VALUES
('bash', '执行Shell命令', '在工作目录下执行bash命令,返回stdout+stderr合并输出', 'SHELL', 'MEDIUM', 1, 1, 30000,
 '{"type":"object","properties":{"command":{"type":"string","description":"要执行的bash命令"}},"required":["command"]}'),
('file.read', '读取文件', '读取工作目录下指定文件内容', 'FILE', 'LOW', 1, 1, 10000,
 '{"type":"object","properties":{"path":{"type":"string","description":"文件路径(相对工作目录)"}},"required":["path"]}'),
('file.write', '写入文件', '将内容写入工作目录下指定文件', 'FILE', 'MEDIUM', 1, 1, 10000,
 '{"type":"object","properties":{"path":{"type":"string","description":"文件路径"},"content":{"type":"string","description":"文件内容"}},"required":["path","content"]}'),
('file.list', '列出目录文件', '列出指定目录下的文件和子目录', 'FILE', 'LOW', 1, 1, 10000,
 '{"type":"object","properties":{"path":{"type":"string","description":"目录路径(默认工作目录)"}}}'),
('file.search', '搜索文件内容', '在工作目录下搜索文件内容(grep)', 'FILE', 'LOW', 1, 1, 15000,
 '{"type":"object","properties":{"pattern":{"type":"string","description":"搜索正则表达式"},"path":{"type":"string","description":"搜索路径"}},"required":["pattern"]}'),
('http.get', 'HTTP GET请求', '发送HTTP GET请求', 'HTTP', 'MEDIUM', 0, 1, 20000,
 '{"type":"object","properties":{"url":{"type":"string","description":"请求URL"}},"required":["url"]}'),
('http.post', 'HTTP POST请求', '发送HTTP POST请求', 'HTTP', 'HIGH', 0, 1, 20000,
 '{"type":"object","properties":{"url":{"type":"string"},"body":{"type":"string"}},"required":["url"]}'),
('json.parse', '解析JSON', '解析并校验JSON文本', 'TEXT', 'LOW', 1, 1, 5000,
 '{"type":"object","properties":{"text":{"type":"string","description":"要解析的JSON文本"}},"required":["text"]}'),
('db.query_readonly', '只读数据库查询', '只读参数化SQL查询', 'DB', 'HIGH', 0, 1, 30000,
 '{"type":"object","properties":{"sql":{"type":"string"},"params":{"type":"array"}},"required":["sql"]}')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- 系统默认技能定义 (user_id=NULL 表示系统级,所有用户共享,仅admin可配置)
-- 每个 skill 按标准格式定义: 触发条件 + 调用格式 + 输入规范 + 输出规范 + 使用约束
INSERT INTO skill_definition (user_id, skill_id, name, version, description, source_path, enabled, is_system, validation_status) VALUES
(NULL, 'tool-calling-protocol', '工具调用协议', '1.0.0',
 '【触发条件】模型需要执行命令、读写文件或调用外部工具时。\n【调用格式】在回复中嵌入以下围栏代码块:\n  - 执行Shell命令: ```bash\\n<命令>\\n```\n  - 读取文件: ```file:read <文件路径>\\n```\n  - 写入文件: ```file:write <文件路径>\\n<文件内容>\\n```\n【输入规范】bash围栏内为任意shell命令; file:read后跟单个文件路径参数; file:write后跟文件路径和文件内容(路径与内容间换行分隔)。\n【输出规范】后端解析围栏内容执行对应工具,执行结果以"工具: <结果>"格式作为TOOL角色消息返回给模型。bash成功返回stdout+stderr合并输出及退出码; file:read返回文件文本内容; file:write返回写入路径确认。\n【使用约束】每次回复可包含多个围栏块,后端按顺序执行。命令在工作目录下执行。文件路径支持相对路径(基于工作目录)和绝对路径。',
 'system://tool-calling-protocol', 1, 1, 'VALID'),
(NULL, 'plan-generation-protocol', '计划生成协议', '1.0.0',
 '【触发条件】任务复杂需要分步执行、多工具协作或需要审批时。\n【调用格式】在回复中输出```json```围栏包裹的AgentPlan JSON:\n```json\\n{\\n  "schemaVersion": "1.0",\\n  "taskSummary": "任务摘要",\\n  "steps": [{"stepId":"s1","actionType":"tool_call","target":"工具名","arguments":{},"dependsOn":[],"riskLevel":"LOW","expectedOutput":"预期"}],\\n  "completionCriteria": ["条件1"],\\n  "requiresApproval": false\\n}\\n```\n【字段规范】schemaVersion固定"1.0"; taskSummary为任务总体描述; steps为步骤数组,每步含: stepId(唯一标识,如s1/s2)、actionType(操作类型,如tool_call)、target(目标工具名)、arguments(工具参数对象)、dependsOn(前置stepId数组,空数组表示无依赖)、riskLevel(LOW/MEDIUM/HIGH)、expectedOutput(预期输出描述); completionCriteria为完成判定条件数组; requiresApproval为布尔值,true表示需用户审批后执行。\n【使用约束】stepId不可重复; dependsOn引用的stepId必须存在; 依赖关系不可形成环; riskLevel为HIGH的步骤requiresApproval必须为true。',
 'system://plan-generation-protocol', 1, 1, 'VALID'),
(NULL, 'bash-result-format', 'Bash结果格式约定', '1.0.0',
 '【触发条件】模型通过```bash```围栏执行命令后,后端返回结果时遵循此格式。\n【输出规范】后端执行bash命令后,以TOOL角色消息返回结果:\n  - 执行成功: "工具: 退出码: 0\\n<stdout和stderr合并输出>"\n  - 执行失败: "工具: 退出码: <非零退出码>\\n<stdout和stderr合并输出>"\n  - 执行异常: "工具: 执行异常: <异常信息>"\n【使用约束】模型根据退出码判断执行状态: 0为成功,非0为失败。合并输出中stdout在前stderr在后。空输出时显示"(执行成功,无输出)"。超时(默认30秒)视为失败。模型应基于退出码和输出内容决定下一步操作: 成功则继续,失败则分析错误原因并调整命令重试或换方案。',
 'system://bash-result-format', 1, 1, 'VALID'),
(NULL, 'context-management', '上下文管理技能', '1.0.0',
 '【触发条件】模型需要获取、组织或管理上下文信息时。\n【行为规范】\n1. 工作目录: 系统在每次请求的[系统提示]中提供当前工作目录路径,所有文件操作默认在此目录下执行。\n2. 对话历史: 系统自动提供最近6条对话消息(用户/模型/工具角色标注),每条消息内容截断至500字符,更早历史标注"(更早历史省略)"。\n3. 工具列表: 系统在[系统提示]中列出所有可用工具名称和简短描述,模型据此选择合适工具。\n4. MCP服务: 系统在[系统提示]中列出已连接的MCP服务器名称,模型可调用其能力。\n5. 技能列表: 系统在[系统提示]中列出已启用的技能名称,模型遵循对应协议。\n【使用约束】模型不应重复请求已读文件内容; 不应在单次回复中发送过多工具调用(建议不超过5个); 上下文信息由系统注入,模型无需自行查询。',
 'system://context-management', 1, 1, 'VALID'),
(NULL, 'file-operation-protocol', '文件操作协议', '1.0.0',
 '【触发条件】模型使用```file:read```或```file:write```围栏操作文件时。\n【路径解析规则】\n  - 相对路径(如"src/main.java"): 解析为 {工作目录}/{相对路径}, 如工作目录为"/home/user/project"则解析为"/home/user/project/src/main.java"\n  - 绝对路径(如"/tmp/test.txt"): 直接使用,不经过工作目录\n  - 路径含".."或".": 按标准路径规范化处理\n【file:read规范】读取指定路径的文本文件内容,返回UTF-8字符串。文件不存在时返回错误"文件不存在: <路径>"。仅支持文本文件,二进制文件返回错误。\n【file:write规范】将指定内容写入指定路径。自动创建不存在的父目录(如写入"a/b/c.txt"时自动创建a/b/目录)。写入成功返回"已写入: <路径>"。覆盖已有文件不提示。\n【使用约束】文件操作在工作目录下执行(除非使用绝对路径); 大文件读写可能超时; 模型应先file:read确认文件内容再决定是否file:write修改。',
 'system://file-operation-protocol', 1, 1, 'VALID'),
(NULL, 'mcp-capability-protocol', 'MCP能力调用协议', '1.0.0',
 '【触发条件】模型需要调用MCP(Model Context Protocol)服务器提供的外部能力时。\n【能力类型】MCP服务器提供三类能力:\n  - TOOL: 可调用的工具(如filesystem.read_file、fetch.get_url)\n  - RESOURCE: 可读取的资源(如文件系统路径、数据库表)\n  - PROMPT: 预定义的提示模板\n【调用格式】模型在回复中输出: ```mcp:call <服务器名>.<能力名>\\n<参数JSON>\\n```\n  示例: ```mcp:call filesystem-mcp.read_file\\n{"path": "src/main.java"}\\n```\n【输入规范】参数JSON格式取决于具体能力的Schema定义,系统在[系统提示]中列出已连接MCP服务器名称。\n【输出规范】后端转发调用到对应MCP服务器,返回结果以TOOL角色消息格式返回给模型。调用失败时返回错误信息。\n【使用约束】仅CONNECTION_STATUS为CONNECTED且ENABLED为true的MCP服务器可调用; HIGH风险级别能力需审批; 调用超时默认30秒。',
 'system://mcp-capability-protocol', 1, 1, 'VALID')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- 系统默认MCP配置 (user_id=NULL 表示系统级)
INSERT INTO mcp_server_config (user_id, name, transport_type, endpoint_or_command_ref, enabled, connection_status, trust_level, is_system) VALUES
(NULL, 'filesystem-mcp', 'STDIO', 'npx @modelcontextprotocol/server-filesystem', 1, 'DISCONNECTED', 'LOW', 1),
(NULL, 'fetch-mcp', 'STDIO', 'npx @modelcontextprotocol/server-fetch', 0, 'DISCONNECTED', 'LOW', 1)
ON DUPLICATE KEY UPDATE transport_type = VALUES(transport_type);


INSERT INTO sys_setting (setting_key, setting_value, description) VALUES
('agent.maxSteps', '50', '单任务最大步骤数'),
('agent.maxModelRounds', '20', '单任务最大模型轮次'),
('agent.maxToolCalls', '100', '单任务最大工具调用数'),
('agent.timeoutMs', '600000', '单任务总超时毫秒'),
('agent.retryMax', '2', '步骤最大重试次数'),
('agent.concurrency', '2', '跨Profile并行任务数'),
('context.warnThreshold', '0.70', '上下文预算警告阈值'),
('context.trimThreshold', '0.85', '上下文预算裁剪阈值'),
('server.host', 'localhost', '服务监听地址'),
('jwt.secret', 'free-agent-secret-key-change-in-production', 'JWT签名密钥'),
('jwt.expireHours', '72', 'JWT过期小时数')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), description = VALUES(description);