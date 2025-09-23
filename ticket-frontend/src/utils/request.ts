import axios from 'axios'
import type { AxiosResponse, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'
import { getToken } from '@/utils/auth'
import type { ApiResponse } from '@/types'

// 创建axios实例
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

// 简单生成 TraceId
function genTraceId(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

// 请求拦截器
service.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = 'Bearer ' + getToken()
      // 添加用户ID头部，某些接口可能需要
      if (userStore.userInfo?.userId) {
        config.headers['X-User-Id'] = userStore.userInfo.userId.toString()
      }
    }

    // 追加 TraceId 与请求起始时间
    try {
      const traceId = genTraceId()
      ;(config as any).headers = (config as any).headers || {}
      ;(config as any).headers['X-Trace-Id'] = traceId
      ;(config as any).metadata = { startTime: Date.now(), traceId }
      const method = (config.method || 'get').toUpperCase()
      const url = (config.baseURL || '') + (config.url || '')
      // 控制台分组便于阅读
      console.groupCollapsed(`[HTTP] ${method} ${url} - start #${traceId}`)
      console.log('request', { method, url: config.url, baseURL: config.baseURL, params: config.params, data: config.data, headers: config.headers })
      console.groupEnd()
    } catch {}
    return config
  },
  (error) => {
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    try {
      const meta = (response.config as any)?.metadata
      const dur = meta?.startTime ? `${Date.now() - meta.startTime}ms` : 'n/a'
      const method = (response.config.method || 'get').toUpperCase()
      const url = (response.config.baseURL || '') + (response.config.url || '')
      console.groupCollapsed(`[HTTP] ${method} ${url} - ok #${meta?.traceId || ''} (${dur})`)
      console.log('response', { status: response.status, data: res })
      console.groupEnd()
    } catch {}
    
    // 如果返回的状态码不是200，说明接口请求有误
    if (res.code !== 200) {
      ElMessage({
        message: res.message || '系统错误',
        type: 'error',
        duration: 5 * 1000
      })

      // 401: 未登录或token过期
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.resetToken()
        // 跳转到登录页面，并携带当前页面路径
        const currentPath = window.location.pathname + window.location.search
        window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
      return Promise.reject(new Error(res.message || '系统错误'))
    } else {
      return res
    }
  },
  (error) => {
    try {
      const cfg: any = (error && error.config) || {}
      const meta = cfg.metadata || {}
      const dur = meta?.startTime ? `${Date.now() - meta.startTime}ms` : 'n/a'
      const method = ((cfg.method as string) || 'get').toUpperCase()
      const url = (cfg.baseURL || '') + (cfg.url || '')
      console.groupCollapsed(`[HTTP] ${method} ${url} - error #${meta?.traceId || ''} (${dur})`)
      console.log('error', { message: error?.message, status: error?.response?.status, data: error?.response?.data })
      console.groupEnd()
    } catch {}
    console.log('err' + error)

    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
      const userStore = useUserStore()
      userStore.resetToken()
      ElMessage({
        message: '登录已过期，请重新登录',
        type: 'error',
        duration: 3 * 1000
      })
      // 跳转到登录页面，并携带当前页面路径
      const currentPath = window.location.pathname + window.location.search
      window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
      return Promise.reject(new Error('登录已过期'))
    }

    ElMessage({
      message: error.message || '请求错误',
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service