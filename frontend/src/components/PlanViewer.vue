<template>
  <el-card class="plan-viewer card-hover" shadow="never">
    <template #header><span>执行计划预览</span></template>
    <el-empty v-if="!plan" description="暂无计划" />
    <div v-else>
      <p class="summary">{{ plan.taskSummary }}</p>
      <el-table :data="plan.steps" size="small" border>
        <el-table-column prop="stepId" label="步骤" width="80" />
        <el-table-column prop="actionType" label="类型" width="100" />
        <el-table-column prop="target" label="目标" />
        <el-table-column prop="riskLevel" label="风险" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="riskType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ plan: any | null }>()

function riskType(level: string): string {
  if (level === 'CRITICAL' || level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
}
</script>

<style scoped>
.summary { color: #606266; margin: 0 0 12px; }
</style>