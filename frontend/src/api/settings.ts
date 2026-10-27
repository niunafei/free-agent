import { get, patch } from './request'

export const settingsApi = {
  getAll: () => get<Record<string, string>>('/settings'),
  updateAgent: (settings: Record<string, string>) => patch<void>('/settings/agent', settings)
}