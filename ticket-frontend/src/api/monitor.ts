import request from '@/utils/request'

export interface MonitorStats {
  totalStock: number
  availableStock: number
  lockedStock: number
  soldStock: number
  stockUtilization: number
  lowStockCount: number
  outOfStockCount: number
}

export interface SuccessRateStats {
  totalOperations: number
  successfulOperations: number
  failedOperations: number
  successRate: number
  lockSuccessRate: number
  unlockSuccessRate: number
  deductSuccessRate: number
}

export interface PerformanceStats {
  averageResponseTime: number
  maxResponseTime: number
  minResponseTime: number
  requestsPerSecond: number
  activeConnections: number
}

export interface ExceptionStats {
  totalExceptions: number
  stockInsufficientCount: number
  concurrencyConflictCount: number
  systemErrorCount: number
  networkErrorCount: number
}

export interface HealthStatus {
  status: 'UP' | 'DOWN' | 'DEGRADED'
  database: 'UP' | 'DOWN'
  redis: 'UP' | 'DOWN'
  nacos: 'UP' | 'DOWN'
  uptime: number
  timestamp: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const monitorApi = {
  /**
   * 获取库存水位统计
   */
  getStockStats(): Promise<ApiResponse<MonitorStats>> {
    return request({
      url: '/api/stock/monitor/stats',
      method: 'get'
    })
  },

  /**
   * 获取操作成功率统计
   */
  getSuccessRateStats(): Promise<ApiResponse<SuccessRateStats>> {
    return request({
      url: '/api/stock/monitor/success-rate',
      method: 'get'
    })
  },

  /**
   * 获取性能统计
   */
  getPerformanceStats(): Promise<ApiResponse<PerformanceStats>> {
    return request({
      url: '/api/stock/monitor/performance',
      method: 'get'
    })
  },

  /**
   * 获取异常统计
   */
  getExceptionStats(): Promise<ApiResponse<ExceptionStats>> {
    return request({
      url: '/api/stock/monitor/exceptions',
      method: 'get'
    })
  },

  /**
   * 获取健康状态
   */
  getHealthStatus(): Promise<ApiResponse<HealthStatus>> {
    return request({
      url: '/api/stock/monitor/health',
      method: 'get'
    })
  },

  /**
   * 获取Prometheus指标
   */
  getPrometheusMetrics(): Promise<string> {
    return request({
      url: '/api/stock/monitor/metrics',
      method: 'get',
      responseType: 'text'
    })
  }
}