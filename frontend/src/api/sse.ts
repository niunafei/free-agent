import type { TaskEvent } from '@/types'

export function createTaskEventSource(
  taskId: number,
  lastEventId: number | null,
  onEvent: (event: TaskEvent) => void,
  onError?: (e: Event) => void
): EventSource {
  const url = lastEventId
    ? `/api/tasks/${taskId}/events?lastEventId=${lastEventId}`
    : `/api/tasks/${taskId}/events`
  const es = new EventSource(url)

  es.onmessage = (ev) => {
    try {
      const data = JSON.parse(ev.data) as TaskEvent
      onEvent(data)
    } catch {
      // ignore parse error
    }
  }

  if (onError) {
    es.onerror = (e) => onError(e)
  }

  return es
}