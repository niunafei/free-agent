<template>
  <el-card class="context-panel card-hover" shadow="never">
    <template #header><span>上下文面板</span></template>
    <el-descriptions :column="2" border size="small">
      <el-descriptions-item label="消息数量">{{ stats?.messageCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="字符数">{{ stats?.totalChars ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="Token 估算">{{ stats?.totalTokenEstimate ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="预算上限">{{ stats?.contextBudget ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="策略">{{ stats?.contextStrategy ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="预算占用">
        <el-progress :percentage="usagePercent" :status="usageStatus" :stroke-width="14" />
      </el-descriptions-item>
    </el-descriptions>
    <div v-if="warning" class="warning-tip">
      <el-alert :title="warning" type="warning" show-icon :closable="false" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ContextStats } from '@/types'

const props = defineProps<{ stats: ContextStats | null }>()

const usagePercent = computed(() => {
  if (!props.stats) return 0
  return Math.min(100, Math.round(props.stats.budgetUsage * 100))
})

const usageStatus = computed(() => {
  if (usagePercent.value >= 100) return 'exception'
  if (usagePercent.value >= 85) return 'warning'
  return 'success'
})

const warning = computed(() => {
  if (!props.stats) return ''
  if (usagePercent.value >= 100) return '已超出预算，请裁剪或生成摘要'
  if (usagePercent.value >= 85) return '接近预算阈值，建议生成摘要'
  if (usagePercent.value >= 70) return '上下文接近阈值'
  return ''
})
</script>

<style scoped>
.warning-tip { margin-top: 12px; }
</style>