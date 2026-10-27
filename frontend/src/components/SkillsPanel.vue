<template>
  <div class="panel">
    <el-button type="primary" size="small" @click="dialogVisible = true" style="margin-bottom:12px">导入 Skill</el-button>
    <el-table :data="skills" size="small" border>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="validationStatus" label="校验" width="80" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" @change="(v: boolean) => toggle(row.id, v)" />
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" title="导入 Skill" width="400px" append-to-body>
      <el-form label-width="70px"><el-form-item label="路径"><el-input v-model="sourcePath" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="importSkill">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { skillsApi } from '@/api/skills'
import type { SkillDefinition } from '@/types'

const skills = ref<SkillDefinition[]>([])
const dialogVisible = ref(false)
const sourcePath = ref('')

async function load() { skills.value = await skillsApi.list() }
async function importSkill() { await skillsApi.importSkill(sourcePath.value); ElMessage.success('已导入'); dialogVisible.value = false; await load() }
async function toggle(id: number, enabled: boolean) { await skillsApi.update(id, { enabled }); await load() }
onMounted(load)
</script>

<style scoped>
.panel { padding: 0 4px; }
</style>