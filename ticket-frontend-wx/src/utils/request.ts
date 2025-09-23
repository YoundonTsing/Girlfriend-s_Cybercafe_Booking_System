import { useUserStore } from '@/stores'
import { getToken } from '@/utils/auth'
import type { ApiResponse } from '@/types'

// 简单生成 TraceId
function genTraceId(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

// 请求配置接口
interface RequestConfig {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  params?: any
  header?: Record<string, string>
}

// 封装uni.request
function request<T = any>(config: RequestConfig): Promise<ApiResponse<T>> {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()
    const traceId = genTraceId()
    const startTime = Date.now()
    
    // 构建完整URL
    const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api'
    let fullUrl = config.url.startsWith('http') ? config.url : baseURL + config.url
    
    // 处理GET请求参数
    if (config.params && config.method === 'GET') {
      const queryString = Object.keys(config.params)
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(config.params![key])}`)
        .join('&')
      if (queryString) {
        fullUrl += (fullUrl.includes('?') ? '&' : '?') + queryString
      }
    }
    
    // 设置请求头
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-Trace-Id': traceId,
      ...config.header
    }
    
    // 添加认证头
    if (userStore.token) {
      header['Authorization'] = 'Bearer ' + getToken()
      if (userStore.userInfo?.userId) {
        header['X-User-Id'] = userStore.userInfo.userId.toString()
      }
    }
    
    const method = (config.method || 'GET').toUpperCase()
    
    // 请求日志
    console.groupCollapsed(`[HTTP] ${method} ${config.url} - start #${traceId}`)
    console.log('request', { method, url: config.url, baseURL, params: config.params, data: config.data, header })
    console.groupEnd()
    
    uni.request({
      url: fullUrl,
      method: config.method || 'GET',
      data: config.data,
      header,
      timeout: 10000,
      success: (res) => {
        const duration = `${Date.now() - startTime}ms`
        
        // 响应日志
        console.groupCollapsed(`[HTTP] ${method} ${config.url} - ok #${traceId} (${duration})`)
        console.log('response', { status: res.statusCode, data: res.data })
        console.groupEnd()
        
        if (res.statusCode === 200) {
          const apiRes = res.data as ApiResponse<T>
          
          // 检查业务状态码
          if (apiRes.code !== 200) {
            uni.showToast({
              title: apiRes.message || '系统错误',
              icon: 'none',
              duration: 3000
            })
            
            // 401: 未登录或token过期
            if (apiRes.code === 401) {
              userStore.resetToken()
              uni.reLaunch({
                url: '/pages/login/index'
              })
              reject(new Error('登录已过期，请重新登录'))
              return
            }
            reject(new Error(apiRes.message || '系统错误'))
            return
          }
          
          resolve(apiRes)
        } else {
          // HTTP状态码错误
          const duration = `${Date.now() - startTime}ms`
          console.groupCollapsed(`[HTTP] ${method} ${config.url} - error #${traceId} (${duration})`)
          console.log('error', { status: res.statusCode, data: res.data })
          console.groupEnd()
          
          if (res.statusCode === 401) {
            userStore.resetToken()
            uni.showToast({
              title: '登录已过期，请重新登录',
              icon: 'none',
              duration: 3000
            })
            setTimeout(() => {
              uni.reLaunch({
                url: '/pages/login/index'
              })
            }, 1500)
            reject(new Error('登录已过期'))
            return
          }
          
          uni.showToast({
            title: `请求失败 (${res.statusCode})`,
            icon: 'none',
            duration: 2000
          })
          reject(new Error(`HTTP ${res.statusCode}`))
        }
      },
      fail: (err) => {
        const duration = `${Date.now() - startTime}ms`
        console.groupCollapsed(`[HTTP] ${method} ${config.url} - error #${traceId} (${duration})`)
        console.log('error', err)
        console.groupEnd()
        
        uni.showToast({
          title: err.errMsg || '网络请求失败',
          icon: 'none',
          duration: 2000
        })
        reject(new Error(err.errMsg || '网络请求失败'))
      }
    })
  })
}

// 导出请求方法
const service = {
  get<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
    return request<T>({ url, method: 'GET', params })
  },
  
  post<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
    return request<T>({ url, method: 'POST', data })
  },
  
  put<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
    return request<T>({ url, method: 'PUT', data })
  },
  
  delete<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
    return request<T>({ url, method: 'DELETE', params })
  }
}

export default service