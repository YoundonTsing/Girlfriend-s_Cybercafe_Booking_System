import { wxApiClient } from '@/utils/wxApiClient'
import type { Order, PageResponse, ApiResponse, CreateOrderRequest, CreateOrderData } from '@/types'

// 创建订单
export const createOrder = (data: CreateOrderRequest | CreateOrderData): Promise<ApiResponse<string>> => {
  return wxApiClient.post('/api/order/create', data)
}

// 获取订单详情（通过订单ID）
export const getOrderDetail = (orderId: string | number): Promise<ApiResponse<Order>> => {
  return wxApiClient.get(`/api/order/detail/${orderId}`)
}

// 根据订单号获取订单详情
export const getOrderDetailByOrderNo = (orderNo: string): Promise<ApiResponse<Order>> => {
  return wxApiClient.get(`/api/order/detail/by-order-no/${orderNo}`)
}

// 分页查询用户订单列表
export const getOrderList = (params?: {
  status?: number
  page?: number
  size?: number
}): Promise<ApiResponse<PageResponse<Order>>> => {
  const searchParams = new URLSearchParams()
  if (params?.status !== undefined) searchParams.append('status', params.status.toString())
  if (params?.page !== undefined) searchParams.append('page', params.page.toString())
  if (params?.size !== undefined) searchParams.append('size', params.size.toString())
  
  const queryString = searchParams.toString()
  const url = queryString ? `/api/order/user/page?${queryString}` : '/api/order/user/page'
  return wxApiClient.get(url)
}

// 支付订单
export const payOrder = (orderNo: string, payType: number): Promise<ApiResponse<boolean>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('orderNo', orderNo)
  searchParams.append('payType', payType.toString())
  
  return wxApiClient.post(`/api/order/pay?${searchParams.toString()}`)
}

// 取消订单
export const cancelOrder = (orderNo: string): Promise<ApiResponse<boolean>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('orderNo', orderNo)
  
  return wxApiClient.post(`/api/order/cancel?${searchParams.toString()}`)
}