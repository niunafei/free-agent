import { get, post, patch, del } from './request'
import type { Conversation, ConversationMessage, ContextStats } from '@/types'

export const chatApi = {
  conversations: () => get<Conversation[]>('/chat/conversations'),
  createConversation: (data: { title?: string; modelId?: number; workingDir?: string }) =>
    post<Conversation>('/chat/conversations', data),
  renameConversation: (id: number, data: { title?: string; modelId?: number; workingDir?: string }) =>
    patch<Conversation>(`/chat/conversations/${id}`, data),
  deleteConversation: (id: number) => del<void>(`/chat/conversations/${id}`),
  messages: (id: number) => get<ConversationMessage[]>(`/chat/conversations/${id}/messages`),
  sendMessage: (id: number, content: string, modelId?: number) =>
    post<ConversationMessage>(`/chat/conversations/${id}/messages`, { content, modelId }),
  context: (id: number) => get<ContextStats>(`/chat/conversations/${id}/context`)
}