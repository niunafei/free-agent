<template>
  <el-tag :type="tagType" size="small" effect="light">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const statusMap: Record<string, { type: string; label: string }> = {
  NOT_CONFIGURED: { type: 'info', label: '未配置' },
  DISCONNECTED: { type: 'info', label: '未连接' },
  CONNECTING: { type: 'warning', label: '连接中' },
  LOGIN_REQUIRED: { type: 'warning', label: '待登录' },
  WAITING_USER: { type: 'warning', label: '等待用户' },
  READY: { type: 'success', label: '就绪' },
  SESSION_EXPIRED: { type: 'danger', label: '会话过期' },
  CHALLENGE_REQUIRED: { type: 'danger', label: '需验证' },
  RATE_LIMITED: { type: 'danger', label: '限流' },
  PAGE_CHANGED: { type: 'danger', label: '页面异常' },
  ERROR: { type: 'danger', label: '错误' },
  DISABLED: { type: 'info', label: '已禁用' },
  CREATED: { type: 'info', label: '已创建' },
  QUEUED: { type: 'info', label: '排队中' },
  CHECKING_MODEL: { type: 'warning', label: '检查模型' },
  PLANNING: { type: 'warning', label: '规划中' },
  VALIDATING_PLAN: { type: 'warning', label: '校验计划' },
  WAITING_APPROVAL: { type: 'warning', label: '待审批' },
  RUNNING_STEP: { type: 'success', label: '执行中' },
  WAITING_MODEL: { type: 'warning', label: '等待模型' },
  SUMMARIZING_CONTEXT: { type: 'warning', label: '压缩上下文' },
  PAUSED: { type: 'info', label: '已暂停' },
  CANCEL_REQUESTED: { type: 'warning', label: '取消中' },
  SUCCEEDED: { type: 'success', label: '成功' },
  FAILED: { type: 'danger', label: '失败' },
  CANCELLED: { type: 'info', label: '已取消' }
}

const tagType = computed(() => (statusMap[props.status]?.type as any) || 'info')
const label = computed(() => statusMap[props.status]?.label || props.status)
</script>