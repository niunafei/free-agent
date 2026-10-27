import { get, patch } from './request'
import type { ToolDefinition } from '@/types'

export const toolsApi = {
  list: () => get<ToolDefinition[]>('/tools'),
  update: (id: number, data: { enabled?: boolean; timeoutMs?: number }) =>
    patch<ToolDefinition>(`/tools/${id}`, data)
}