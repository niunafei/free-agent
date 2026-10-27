<template>
  <div class="panel">
    <el-button type="primary" size="small" @click="dialogVisible = true" style="margin-bottom:12px">添加 Server</el-button>
    <el-table :data="servers" size="small" border>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="transportType" label="传输" width="100" />
      <el-table-column prop="connectionStatus" label="状态" width="100" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="connect(row.id)">连接</el-button>
          <el-button size="small" type="danger" @click="disconnect(row.id)">断开</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" title="添加 MCP Server" width="400px" append-to-body>
      <el-form :model="form" label-width="70px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="传输"><el-select v-model="form.transportType"><el-option label="STDIO" value="STDIO" /><el-option label="SSE" value="SSE" /><el-option label="HTTP" value="HTTP" /></el-select></el-form-item>
        <el-form-item label="端点"><el-input v-model="form.endpointOrCommandRef" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="add">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { mcpApi } from '@/api/mcp'
import type { McpServerConfig } from '@/types'

const servers = ref<McpServerConfig[]>([])
const dialogVisible = ref(false)
const form = reactive({ name: '', transportType: 'STDIO', endpointOrCommandRef: '' })

async function load() { servers.value = await mcpApi.list() }
async function add() { await mcpApi.create({ ...form }); dialogVisible.value = false; await load() }
async function connect(id: number) { await mcpApi.connect(id); ElMessage.success('已连接'); await load() }
async function disconnect(id: number) { await mcpApi.disconnect(id); ElMessage.success('已断开'); await load() }
onMounted(load)
</script>

<style scoped>
.panel { padding: 0 4px; }
</style>