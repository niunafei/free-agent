import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    const result = response.data as Result<unknown>
    if (result.code !== 0) {
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(new Error(result.message))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
    if (!error.response) {
      ElMessage.error('连接已断开，请检查后端服务是否正常运行')
    } else {
      ElMessage.error(error.response?.data?.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export async function get<T>(url: string): Promise<T> {
  const res = await instance.get<Result<T>>(url)
  return res.data.data
}

export async function post<T>(url: string, data?: unknown): Promise<T> {
  const res = await instance.post<Result<T>>(url, data)
  return res.data.data
}

export async function patch<T>(url: string, data?: unknown): Promise<T> {
  const res = await instance.patch<Result<T>>(url, data)
  return res.data.data
}

export async function del<T>(url: string): Promise<T> {
  const res = await instance.delete<Result<T>>(url)
  return res.data.data
}

export default instance