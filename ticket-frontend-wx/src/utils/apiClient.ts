/**
 * 统一API调用接口
 * 抽象不同平台的HTTP客户端差异
 */
import type { ApiResponse } from '@/types'

// 统一的API客户端接口
export interface ApiClient {
  get<T = any>(url: string, params?: any): Promise<ApiResponse<T>>
  post<T = any>(url: string, data?: any): Promise<ApiResponse<T>>
  put<T = any>(url: string, data?: any): Promise<ApiResponse<T>>
  delete<T = any>(url: string, params?: any): Promise<ApiResponse<T>>
  patch<T = any>(url: string, data?: any): Promise<ApiResponse<T>>
}

// 请求配置接口
export interface RequestConfig {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  data?: any
  params?: any
  headers?: Record<string, string>
  timeout?: number
}

// 错误处理接口
export interface ErrorHandler {
  (error: any): void
}

// 响应拦截器
export interface ResponseInterceptor {
  (response: any): any
}

// 请求拦截器
export interface RequestInterceptor {
  (config: RequestConfig): RequestConfig
}

// 平台适配器接口
export interface PlatformAdapter {
  storage: {
    getItem(key: string): string | null
    setItem(key: string, value: string): void
    removeItem(key: string): void
  }
  request: (config: RequestConfig) => Promise<any>
  showMessage: (message: string, type?: 'success' | 'error' | 'warning') => void
}

// 基础API客户端抽象类
export abstract class BaseApiClient implements ApiClient {
  protected baseURL: string
  protected timeout: number
  protected adapter: PlatformAdapter
  protected requestInterceptors: RequestInterceptor[] = []
  protected responseInterceptors: ResponseInterceptor[] = []
  protected errorHandlers: ErrorHandler[] = []

  constructor(adapter: PlatformAdapter, baseURL?: string, timeout?: number) {
    this.adapter = adapter
    this.baseURL = baseURL || import.meta.env.VITE_API_BASE_URL || '/api'
    this.timeout = timeout || 10000
  }

  // 添加请求拦截器
  addRequestInterceptor(interceptor: RequestInterceptor): void {
    this.requestInterceptors.push(interceptor)
  }

  // 添加响应拦截器
  addResponseInterceptor(interceptor: ResponseInterceptor): void {
    this.responseInterceptors.push(interceptor)
  }

  // 添加错误处理器
  addErrorHandler(handler: ErrorHandler): void {
    this.errorHandlers.push(handler)
  }

  // 处理请求配置
  protected processRequestConfig(config: RequestConfig): RequestConfig {
    let processedConfig = { ...config }
    
    // 应用请求拦截器
    for (const interceptor of this.requestInterceptors) {
      processedConfig = interceptor(processedConfig)
    }
    
    return processedConfig
  }

  // 处理响应数据
  protected processResponse(response: any): any {
    let processedResponse = response
    
    // 应用响应拦截器
    for (const interceptor of this.responseInterceptors) {
      processedResponse = interceptor(processedResponse)
    }
    
    return processedResponse
  }

  // 处理错误
  protected handleError(error: any): void {
    for (const handler of this.errorHandlers) {
      handler(error)
    }
  }

  // 抽象方法，由具体平台实现
  protected abstract makeRequest<T>(config: RequestConfig): Promise<ApiResponse<T>>

  async get<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
    return this.makeRequest<T>({
      url,
      method: 'GET',
      params
    })
  }

  async post<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
    return this.makeRequest<T>({
      url,
      method: 'POST',
      data
    })
  }

  async put<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
    return this.makeRequest<T>({
      url,
      method: 'PUT',
      data
    })
  }

  async delete<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
    return this.makeRequest<T>({
      url,
      method: 'DELETE',
      params
    })
  }

  async patch<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
    return this.makeRequest<T>({
      url,
      method: 'PATCH',
      data
    })
  }
}

// 默认错误处理
export const defaultErrorHandler: ErrorHandler = (error: any) => {
  console.error('API请求错误:', error)
}

// 默认请求拦截器（添加认证头）
export const createAuthRequestInterceptor = (getToken: () => string | null): RequestInterceptor => {
  return (config: RequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  }
}

// 默认响应拦截器（统一错误处理）
export const createResponseInterceptor = (showMessage: (msg: string, type?: string) => void): ResponseInterceptor => {
  return (response: any) => {
    const { code, message } = response
    
    // 统一的错误码处理
    if (code !== 200) {
      if (code === 401) {
        showMessage('登录已过期，请重新登录', 'error')
        // 可以在这里触发登出逻辑
      } else if (code === 403) {
        showMessage('权限不足', 'error')
      } else if (code >= 500) {
        showMessage('服务器错误，请稍后再试', 'error')
      } else {
        showMessage(message || '请求失败', 'error')
      }
      throw new Error(message || '请求失败')
    }
    
    return response
  }
}