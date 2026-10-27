<template>
  <div class="panel">
    <el-table :data="tools" size="small" border>
      <el-table-column prop="toolId" label="工具ID" />
      <el-table-column prop="name" label="名称" width="140" />
      <el-table-column prop="riskLevel" label="风险" width="80" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" @change="(v: boolean) => toggle(row.id, v)" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { toolsApi } from '@/api/tools'
import type { ToolDefinition } from '@/types'

const tools = ref<ToolDefinition[]>([])
async function load() { tools.value = await toolsApi.list() }
async function toggle(id: number, enabled: boolean) { await toolsApi.update(id, { enabled }); await load() }
onMounted(load)
</script>

<style scoped>
.panel { padding: 0 4px; }
</style>