import { webApiClient } from '@/utils/webApiClient'
import type { Order, PageResponse, ApiResponse, CreateOrderRequest, CreateOrderData } from '@/types'
import { convertOrderPageResponse, convertOrderResponse } from '@/utils/orderAdapter'

// 创建订单
export function createOrder(data: CreateOrderRequest | CreateOrderData): Promise<ApiResponse<Order>> {
  return webApiClient.post('/order/create', data)
}

// 获取订单详情（通过订单ID）
export function getOrderDetail(id: number | string): Promise<ApiResponse<Order>> {
  return webApiClient.get(`/order/detail/${id}`).then(response => {
    if (response.data) {
      response.data = convertOrderResponse(response.data)
    }
    return response
  })
}

// 获取订单详情（通过订单号）
export function getOrderDetailByOrderNo(orderNo: string): Promise<ApiResponse<Order>> {
  return webApiClient.get(`/order/detail/by-order-no/${orderNo}`).then(response => {
    if (response.data) {
      response.data = convertOrderResponse(response.data)
    }
    return response
  })
}

// 获取订单列表
export function getOrderList(params?: Record<string, any>): Promise<ApiResponse<PageResponse<Order>>> {
  return webApiClient.get('/order/user/page', params).then(response => {
    if (response.data) {
      response.data = convertOrderPageResponse(response.data)
    }
    return response
  })
}

// 支付订单
export function payOrder(orderNo: string, payType: number = 1): Promise<ApiResponse<boolean>> {
  return webApiClient.post(`/order/pay?orderNo=${orderNo}&payType=${payType}`)
}

// 取消订单
export function cancelOrder(orderNo: string): Promise<ApiResponse<boolean>> {
  return webApiClient.post(`/order/cancel?orderNo=${orderNo}`)
}