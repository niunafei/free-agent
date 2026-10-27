import { get, post, patch } from './request'
import type { ModelProfile, LoginStatusResult } from '@/types'

export const modelsApi = {
  list: () => get<ModelProfile[]>('/models'),
  create: (data: { provider: string; displayName: string; accountAlias: string; adapterType?: string; baseUrl?: string }) =>
    post<ModelProfile>('/models', data),
  update: (id: number, data: { displayName?: string; baseUrl?: string; enabled?: boolean }) =>
    patch<ModelProfile>(`/models/${id}`, data),
  connect: (id: number) => post<void>(`/models/${id}/connect`),
  check: (id: number) => post<ModelProfile>(`/models/${id}/check`),
  disconnect: (id: number) => post<void>(`/models/${id}/disconnect`),
  loginStatus: (id: number) => get<LoginStatusResult>(`/models/${id}/login-status`)
}