import { ref, onMounted, onUnmounted } from 'vue'
import { modelsApi } from '@/api/models'
import type { ModelProfile } from '@/types'

export function useModelStatus(intervalMs = 15000) {
  const models = ref<ModelProfile[]>([])
  let timer: ReturnType<typeof setInterval> | null = null

  async function refresh() {
    try { models.value = await modelsApi.list() } catch { /* ignore */ }
  }

  onMounted(() => {
    refresh()
    timer = setInterval(refresh, intervalMs)
  })
  onUnmounted(() => { if (timer) clearInterval(timer) })

  return { models, refresh }
}