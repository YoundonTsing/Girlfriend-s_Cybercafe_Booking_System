/**
 * 小程序平台API客户端实现
 */
import { getToken } from '@/utils/auth'
import type { ApiResponse } from '@/types'
import { BaseApiClient, type PlatformAdapter, type RequestConfig } from './apiClient'

// 小程序平台适配器
class WxPlatformAdapter implements PlatformAdapter {
  storage = {
    getItem: (key: string) => uni.getStorageSync(key) || null,
    setItem: (key: string, value: string) => uni.setStorageSync(key, value),
    removeItem: (key: string) => uni.removeStorageSync(key)
  }

  async request(config: RequestConfig): Promise<any> {
    return new Promise((resolve, reject) => {
      uni.request({
        url: config.url,
        method: config.method as any,
        data: config.data,
        header: config.headers,
        timeout: config.timeout,
        success: (res) => resolve(res.data),
        fail: (err) => reject(err)
      })
    })
  }

  showMessage(message: string, type: 'success' | 'error' | 'warning' = 'none') {
    const iconMap = {
      success: 'success',
      error: 'error', 
      warning: 'none'
    }
    
    uni.showToast({
      title: message,
      icon: iconMap[type] as any,
      duration: 2000
    })
  }
}

// 小程序API客户端
export class WxApiClient extends BaseApiClient {
  constructor(baseURL?: string, timeout?: number) {
    const adapter = new WxPlatformAdapter()
    super(adapter, baseURL, timeout)
    
    // 设置默认拦截器
    this.setupDefaultInterceptors()
  }

  private setupDefaultInterceptors() {
    // 添加认证请求拦截器
    this.addRequestInterceptor((config: RequestConfig) => {
      const token = getToken()
      if (token) {
        config.headers = config.headers || {}
        config.headers['Authorization'] = 'Bearer ' + token
      }
      
      // 生成TraceId
      const traceId = this.genTraceId()
      config.headers = config.headers || {}
      config.headers['X-Trace-Id'] = traceId
      
      // 设置默认Content-Type
      config.headers['Content-Type'] = config.headers['Content-Type'] || 'application/json'
      
      // 日志记录
      const method = (config.method || 'GET').toUpperCase()
      console.groupCollapsed(`[HTTP] ${method} ${config.url} - start #${traceId}`)
      console.log('request', { 
        method, 
        url: config.url, 
        baseURL: this.baseURL, 
        params: config.params, 
        data: config.data, 
        headers: config.headers 
      })
      console.groupEnd()
      
      return config
    })

    // 添加响应拦截器
    this.addResponseInterceptor((response: ApiResponse) => {
      // 统一错误处理
      if (response.code !== 200) {
        const message = response.message || '请求失败'
        if (response.code === 401) {
          this.adapter.showMessage('登录已过期，请重新登录', 'error')
          // 可以在这里触发登出逻辑
        } else if (response.code === 403) {
          this.adapter.showMessage('权限不足', 'error')
        } else if (response.code >= 500) {
          this.adapter.showMessage('服务器错误，请稍后再试', 'error')
        } else {
          this.adapter.showMessage(message, 'error')
        }
        throw new Error(message)
      }
      
      return response
    })

    // 添加错误处理器
    this.addErrorHandler((error: any) => {
      console.error('API请求错误:', error)
      const message = error.message || '网络错误'
      this.adapter.showMessage(message, 'error')
    })
  }

  private genTraceId(): string {
    return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
  }

  protected async makeRequest<T>(config: RequestConfig): Promise<ApiResponse<T>> {
    try {
      const processedConfig = this.processRequestConfig(config)
      
      // 构建完整URL
      let fullUrl = processedConfig.url.startsWith('http') 
        ? processedConfig.url 
        : this.baseURL + processedConfig.url
      
      // 处理GET请求参数
      if (processedConfig.params && processedConfig.method === 'GET') {
        const queryString = Object.keys(processedConfig.params)
          .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(processedConfig.params![key])}`)
          .join('&')
        if (queryString) {
          fullUrl += (fullUrl.includes('?') ? '&' : '?') + queryString
        }
      }
      
      const requestConfig: RequestConfig = {
        ...processedConfig,
        url: fullUrl,
        timeout: this.timeout
      }
      
      const response = await this.adapter.request(requestConfig)
      const processedResponse = this.processResponse(response)
      
      // 成功日志
      const traceId = processedConfig.headers?.['X-Trace-Id']
      const method = (processedConfig.method || 'GET').toUpperCase()
      console.groupCollapsed(`[HTTP] ${method} ${processedConfig.url} - success #${traceId}`)
      console.log('response', { data: processedResponse })
      console.groupEnd()
      
      return processedResponse
    } catch (error) {
      this.handleError(error)
      throw error
    }
  }
}

// 创建默认的小程序API客户端实例
export const wxApiClient = new WxApiClient()