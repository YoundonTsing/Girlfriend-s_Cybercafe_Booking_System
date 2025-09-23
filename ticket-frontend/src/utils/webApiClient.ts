/**
 * Web平台API客户端实现
 */
import axios from 'axios'
import type { AxiosResponse, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import type { ApiResponse } from '@/types'
import { BaseApiClient, type PlatformAdapter, type RequestConfig } from './apiClient'

// Web平台适配器
class WebPlatformAdapter implements PlatformAdapter {
  storage = {
    getItem: (key: string) => localStorage.getItem(key),
    setItem: (key: string, value: string) => localStorage.setItem(key, value),
    removeItem: (key: string) => localStorage.removeItem(key)
  }

  async request(config: RequestConfig): Promise<any> {
    const axiosConfig: AxiosRequestConfig = {
      url: config.url,
      method: config.method,
      data: config.data,
      params: config.params,
      headers: config.headers,
      timeout: config.timeout
    }
    
    const response: AxiosResponse = await axios(axiosConfig)
    return response.data
  }

  showMessage(message: string, type: 'success' | 'error' | 'warning' = 'info') {
    ElMessage({
      message,
      type
    })
  }
}

// Web平台API客户端
export class WebApiClient extends BaseApiClient {
  private axiosInstance: any

  constructor(baseURL?: string, timeout?: number) {
    const adapter = new WebPlatformAdapter()
    super(adapter, baseURL, timeout)
    
    // 创建axios实例
    this.axiosInstance = axios.create({
      baseURL: this.baseURL,
      timeout: this.timeout
    })

    // 设置默认拦截器
    this.setupDefaultInterceptors()
  }

  private setupDefaultInterceptors() {
    // 请求拦截器
    this.axiosInstance.interceptors.request.use(
      (config: AxiosRequestConfig) => {
        const token = getToken()
        if (token) {
          config.headers = config.headers || {}
          config.headers['Authorization'] = 'Bearer ' + token
          
          // 从token中解析用户ID并设置X-User-Id头
          try {
            const payload = JSON.parse(atob(token.split('.')[1]))
            if (payload.userId) {
              config.headers['X-User-Id'] = payload.userId.toString()
            }
          } catch (error) {
            console.warn('Failed to parse user ID from token:', error)
          }
        }
        
        // 生成TraceId
        const traceId = this.genTraceId()
        config.headers = config.headers || {}
        config.headers['X-Trace-Id'] = traceId
        
        // 日志记录
        const method = (config.method || 'get').toUpperCase()
        const url = (config.baseURL || '') + (config.url || '')
        console.groupCollapsed(`[HTTP] ${method} ${url} - start #${traceId}`)
        console.log('request', { method, url: config.url, baseURL: config.baseURL, params: config.params, data: config.data, headers: config.headers })
        console.groupEnd()
        
        return config
      },
      (error) => {
        console.error('Request error:', error)
        return Promise.reject(error)
      }
    )

    // 响应拦截器
    this.axiosInstance.interceptors.response.use(
      (response: AxiosResponse<ApiResponse>) => {
        const res = response.data
        const traceId = response.config.headers?.['X-Trace-Id']
        
        // 日志记录
        const method = (response.config.method || 'get').toUpperCase()
        const url = response.config.url
        console.groupCollapsed(`[HTTP] ${method} ${url} - success #${traceId}`)
        console.log('response', { data: res, status: response.status, headers: response.headers })
        console.groupEnd()
        
        // 统一错误处理
        if (res.code !== 200) {
          const message = res.message || '请求失败'
          if (res.code === 401) {
            ElMessage.error('登录已过期，请重新登录')
            // 可以在这里触发登出逻辑
          } else if (res.code === 403) {
            ElMessage.error('权限不足')
          } else if (res.code >= 500) {
            ElMessage.error('服务器错误，请稍后再试')
          } else {
            ElMessage.error(message)
          }
          return Promise.reject(new Error(message))
        }
        
        return res
      },
      (error) => {
        console.error('Response error:', error)
        const message = error.message || '网络错误'
        ElMessage.error(message)
        return Promise.reject(error)
      }
    )
  }

  private genTraceId(): string {
    return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
  }

  protected async makeRequest<T>(config: RequestConfig): Promise<ApiResponse<T>> {
    try {
      const processedConfig = this.processRequestConfig(config)
      
      const axiosConfig: AxiosRequestConfig = {
        url: processedConfig.url,
        method: processedConfig.method,
        data: processedConfig.data,
        params: processedConfig.params,
        headers: processedConfig.headers
      }
      
      const response = await this.axiosInstance(axiosConfig)
      return this.processResponse(response)
    } catch (error) {
      this.handleError(error)
      throw error
    }
  }
}

// 创建默认的Web API客户端实例
export const webApiClient = new WebApiClient()