import { defineStore } from 'pinia'
import { ref } from 'vue'
import { settingsApi } from '@/api/settings'

export const useAppStore = defineStore('app', () => {
  const settings = ref<Record<string, string>>({})

  async function loadSettings() {
    try { settings.value = await settingsApi.getAll() } catch { /* ignore */ }
  }

  return { settings, loadSettings }
})