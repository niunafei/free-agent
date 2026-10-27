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
    user_id                 BIGINT       NOT NULL COMMENT '所属用户ID',
    name                    VARCHAR(128) NOT NULL COMMENT 'MCP Server名称',
    transport_type          VARCHAR(32)  NOT NULL COMMENT '传输类型(STDIO/SSE/HTTP)',
    endpoint_or_command_ref VARCHAR(512) NULL COMMENT '端点地址或命令路径',
    enabled                 TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用(0=禁用 1=启用)',
    connection_status       VARCHAR(32)  NOT NULL DEFAULT 'DISCONNECTED' COMMENT '连接状态(DISCONNECTED/CONNECTING/CONNECTED/ERROR)',
    trust_level             VARCHAR(16)  NOT NULL DEFAULT 'LOW' COMMENT '信任等级(LOW/MEDIUM/HIGH)',
    last_checked_at         DATETIME     NULL COMMENT '最后检测连接时间',
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_name (user_id, name)
) ENGINE=InnoDB COMMENT='MCP Server配置(按用户隔离)';

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
    user_id          BIGINT       NOT NULL COMMENT '所属用户ID',
    skill_id         VARCHAR(128) NOT NULL COMMENT '技能唯一标识',
    name             VARCHAR(128) NOT NULL COMMENT '技能名称',
    version          VARCHAR(32)  NOT NULL DEFAULT '1.0.0' COMMENT '技能版本号',
    description      VARCHAR(512) NULL COMMENT '技能描述',
    source_path      VARCHAR(512) NOT NULL COMMENT '技能源文件路径',
    content_hash     VARCHAR(64)  NULL COMMENT '内容哈希值(变更检测)',
    enabled          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用(0=禁用 1=启用)',
    validation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '校验状态(PENDING/VALID/INVALID)',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_skill_id (user_id, skill_id)
) ENGINE=InnoDB COMMENT='Skill元数据(按用户隔离)';

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

-- ---------------------------- 初始数据 ----------------------------
INSERT INTO tool_definition (tool_id, name, description, category, risk_level, enabled, timeout_ms) VALUES
('workspace.list_files', '列出工作区文件', '列出指定目录下的文件', 'FILE', 'LOW', 1, 10000),
('workspace.read_text', '读取文本文件', '读取工作区内文本文件内容', 'FILE', 'LOW', 1, 10000),
('workspace.write_json', '写入JSON结果', '将JSON结果写入工作区文件', 'FILE', 'MEDIUM', 1, 15000),
('text.parse_json', '解析JSON', '解析并校验JSON文本', 'TEXT', 'LOW', 1, 5000),
('http.get', 'HTTP GET请求', '受限HTTP GET请求', 'HTTP', 'MEDIUM', 0, 20000),
('db.query_readonly', '只读数据库查询', '只读参数化SQL查询', 'DB', 'MEDIUM', 0, 30000);

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
('jwt.expireHours', '72', 'JWT过期小时数');