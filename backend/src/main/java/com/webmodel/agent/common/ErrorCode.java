package com.webmodel.agent.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "ok"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "状态冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    MODEL_NOT_FOUND(1001, "模型不存在"),
    MODEL_NOT_READY(1002, "模型未就绪"),
    MODEL_ALREADY_CONNECTED(1003, "模型已连接"),

    TASK_NOT_FOUND(2001, "任务不存在"),
    TASK_STATUS_INVALID(2002, "任务状态不允许此操作"),
    TASK_PLAN_INVALID(2003, "计划校验失败"),

    TOOL_NOT_FOUND(3001, "工具不存在"),
    TOOL_DISABLED(3002, "工具已禁用"),
    TOOL_PARAM_INVALID(3003, "工具参数校验失败"),
    TOOL_RISK_REQUIRES_APPROVAL(3004, "高风险操作需要审批"),

    MCP_CONNECT_FAILED(4001, "MCP连接失败"),
    SKILL_INVALID(5001, "Skill校验失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}