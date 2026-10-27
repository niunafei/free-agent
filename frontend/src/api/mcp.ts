import { get, post } from './request'
import type { McpServerConfig, McpCapability } from '@/types'

export const mcpApi = {
  list: () => get<McpServerConfig[]>('/mcp/servers'),
  create: (data: { name: string; transportType: string; endpointOrCommandRef?: string; trustLevel?: string }) =>
    post<McpServerConfig>('/mcp/servers', data),
  connect: (id: number) => post<void>(`/mcp/servers/${id}/connect`),
  capabilities: (id: number) => get<McpCapability[]>(`/mcp/servers/${id}/capabilities`),
  disconnect: (id: number) => post<void>(`/mcp/servers/${id}/disconnect`)
}