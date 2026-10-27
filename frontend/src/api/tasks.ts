import { get, post } from './request'
import type { AgentTask, PageResult } from '@/types'

export const tasksApi = {
  page: (page: number, size: number) => get<PageResult<AgentTask>>(`/tasks?page=${page}&size=${size}`),
  get: (id: number) => get<AgentTask>(`/tasks/${id}`),
  create: (data: { title: string; selectedModelId?: number; conversationId?: number; skillId?: number }) =>
    post<AgentTask>('/tasks', data),
  approve: (id: number, data: { stepId?: string; planVersion?: number; decision: string }) =>
    post<void>(`/tasks/${id}/approve`, data),
  pause: (id: number) => post<void>(`/tasks/${id}/pause`),
  resume: (id: number) => post<void>(`/tasks/${id}/resume`),
  cancel: (id: number) => post<void>(`/tasks/${id}/cancel`)
}