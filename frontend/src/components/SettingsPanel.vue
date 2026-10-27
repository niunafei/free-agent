<template>
  <div class="panel">
    <el-table :data="rows" size="small" border>
      <el-table-column prop="key" label="配置项" />
      <el-table-column prop="value" label="值" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { settingsApi } from '@/api/settings'

const settings = ref<Record<string, string>>({})
const rows = computed(() => Object.entries(settings.value).map(([key, value]) => ({ key, value })))
onMounted(async () => { try { settings.value = await settingsApi.getAll() } catch { /* ignore */ } })
</script>

<style scoped>
.panel { padding: 0 4px; }
</style>