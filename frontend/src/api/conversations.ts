import { get, post, patch } from './request'
import type { ContextStats } from '@/types'

export const conversationsApi = {
  context: (id: number) => get<ContextStats>(`/conversations/${id}/context`),
  updateSettings: (id: number, data: { contextBudget?: number; contextStrategy?: string; keepRecentTurn?: number }) =>
    patch<void>(`/conversations/${id}/context/settings`, data),
  summarize: (id: number) => post<Record<string, unknown>>(`/conversations/${id}/context/summarize`),
  reset: (id: number) => post<void>(`/conversations/${id}/reset`)
}