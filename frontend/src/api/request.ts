import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// 统一 Axios 实例：FastAPI 后端（vite 已代理 /api -> http://localhost:8080）
const instance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 401 统一处理：清理登录态并回到登录页（避免登录接口自身触发循环） */
function isAuthPage(path: string) {
  return path.startsWith('/login') || path.startsWith('/admin/login')
}

function handleUnauthorized() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  if (!isAuthPage(window.location.pathname)) {
    ElMessage.error('登录已过期，请重新登录')
    window.location.assign('/login')
  }
}

instance.interceptors.response.use(
  (res) => {
    const body = res.data as { code?: number; message?: string; data?: unknown } | undefined
    // 兼容非信封响应（如 health）
    if (!body || typeof body !== 'object' || typeof body.code !== 'number') {
      return body as never
    }
    if (body.code === 0) {
      return body.data as never
    }
    if (body.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(body.message || '未登录')) as never
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败')) as never
  },
  (err) => {
    const resp = err?.response
    const msg = resp?.data?.message || (err?.code === 'ECONNABORTED' ? '请求超时，请确认后端服务已启动' : '网络异常，请稍后重试')
    if (resp?.status === 401) {
      handleUnauthorized()
    } else if (resp?.status !== 422) {
      ElMessage.error(msg)
    } else if (msg && msg !== '请求失败') {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  },
)

/** 类型化请求助手：拦截器已将信封解包为 data，返回 Promise<T> */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  return instance.request(config) as unknown as Promise<T>
}

export const http = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request<T>({ url, method: 'GET', params })
  },
  post<T>(url: string, data?: unknown): Promise<T> {
    return request<T>({ url, method: 'POST', data })
  },
  put<T>(url: string, data?: unknown): Promise<T> {
    return request<T>({ url, method: 'PUT', data })
  },
  delete<T>(url: string): Promise<T> {
    return request<T>({ url, method: 'DELETE' })
  },
}

export default instance
