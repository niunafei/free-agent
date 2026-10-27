import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AgentTask, TaskEvent } from '@/types'

export const useTaskStore = defineStore('task', () => {
  const currentTask = ref<AgentTask | null>(null)
  const events = ref<TaskEvent[]>([])

  function clear() {
    currentTask.value = null
    events.value = []
  }

  return { currentTask, events, clear }
})