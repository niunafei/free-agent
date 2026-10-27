<template>
  <el-drawer v-model="show" :title="title" size="560px" direction="rtl">
    <component :is="panel" />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import ModelsPanel from '@/components/ModelsPanel.vue'
import ToolsPanel from '@/components/ToolsPanel.vue'
import McpPanel from '@/components/McpPanel.vue'
import SkillsPanel from '@/components/SkillsPanel.vue'
import SettingsPanel from '@/components/SettingsPanel.vue'

const props = defineProps<{ visible: boolean; type: string }>()
const emit = defineEmits<{ 'update:visible': [boolean] }>()

const show = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v)
})

const titleMap: Record<string, string> = {
  models: '模型账户管理', tools: '工具管理', mcp: 'MCP 管理', skills: 'Skills 管理', settings: '系统设置'
}
const title = computed(() => titleMap[props.type] || '管理')

const panel = computed(() => {
  switch (props.type) {
    case 'models': return ModelsPanel
    case 'tools': return ToolsPanel
    case 'mcp': return McpPanel
    case 'skills': return SkillsPanel
    case 'settings': return SettingsPanel
    default: return null
  }
})
</script>