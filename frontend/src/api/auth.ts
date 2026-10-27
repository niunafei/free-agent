import { get, post } from './request'

export interface AuthResult {
  token: string
  userId: number
  username: string
  displayName: string
}

export const authApi = {
  register: (data: { username: string; password: string; displayName?: string }) =>
    post<AuthResult>('/auth/register', data),
  login: (data: { username: string; password: string }) =>
    post<AuthResult>('/auth/login', data),
  me: () => get<Record<string, unknown>>('/auth/me')
}