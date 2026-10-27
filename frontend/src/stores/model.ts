import { defineStore } from 'pinia'
import { ref } from 'vue'
import { modelsApi } from '@/api/models'
import type { ModelProfile } from '@/types'

export const useModelStore = defineStore('model', () => {
  const models = ref<ModelProfile[]>([])
  const loading = ref(false)

  async function load() {
    loading.value = true
    try { models.value = await modelsApi.list() } finally { loading.value = false }
  }

  return { models, loading, load }
})