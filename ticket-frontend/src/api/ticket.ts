import request from '@/utils/request'

export interface TicketStockInfo {
  ticket: {
    id: number
    name: string
    price: number
    status: number
    showId: number
    sessionId: number
  }
  stock: {
    totalStock: number
    availableStock: number
    lockedStock: number
    soldStock: number
    version: number
  } | null
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const ticketApi = {
  /**
   * 获取单个票档库存信息
   */
  getTicketStock(ticketId: number): Promise<ApiResponse<TicketStockInfo>> {
    return request({
      url: `/ticket/stock/${ticketId}`,
      method: 'get'
    })
  },

  /**
   * 获取所有票档库存信息
   */
  getAllTicketStock(): Promise<ApiResponse<TicketStockInfo[]>> {
    return request({
      url: '/ticket/stock/all',
      method: 'get'
    })
  },

  /**
   * 锁定票档库存
   */
  lockStock(ticketId: number, quantity: number): Promise<ApiResponse<boolean>> {
    return request({
      url: '/ticket/lock',
      method: 'put',
      params: {
        ticketId,
        quantity
      }
    })
  },

  /**
   * 释放票档库存
   */
  unlockStock(ticketId: number, quantity: number): Promise<ApiResponse<boolean>> {
    return request({
      url: '/ticket/unlock',
      method: 'put',
      params: {
        ticketId,
        quantity
      }
    })
  },

  /**
   * 扣减票档库存
   */
  deductStock(ticketId: number, quantity: number): Promise<ApiResponse<boolean>> {
    return request({
      url: '/ticket/deduct',
      method: 'put',
      params: {
        ticketId,
        quantity
      }
    })
  },

  /**
   * 初始化票档库存
   */
  initializeStock(ticketId: number, totalStock: number): Promise<ApiResponse<boolean>> {
    return request({
      url: '/ticket/stock/init',
      method: 'post',
      data: {
        ticketId,
        totalStock
      }
    })
  },

  /**
   * 获取票档列表
   */
  getTicketList(showId: number, sessionId: number): Promise<ApiResponse<any[]>> {
    return request({
      url: '/ticket/list',
      method: 'get',
      params: {
        showId,
        sessionId
      }
    })
  }
}

export default ticketApi