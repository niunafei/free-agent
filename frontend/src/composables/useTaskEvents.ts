import { ref, onUnmounted } from 'vue'
import { createTaskEventSource } from '@/api/sse'
import type { TaskEvent } from '@/types'

export function useTaskEvents(taskId: number | null) {
  const events = ref<TaskEvent[]>([])
  let es: EventSource | null = null

  function start(id: number, lastEventId: number | null = null) {
    stop()
    es = createTaskEventSource(id, lastEventId, (e) => events.value.push(e))
  }

  function stop() {
    es?.close()
    es = null
  }

  if (taskId) start(taskId)
  onUnmounted(stop)

  return { events, start, stop }
}