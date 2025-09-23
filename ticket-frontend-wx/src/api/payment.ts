import { wxApiClient } from '@/utils/wxApiClient'
import type { ApiResponse } from '@/types'

// 注意：支付相关API需要后端实现对应的控制器
// 目前基于订单服务的支付接口进行适配

// 支付相关接口类型定义
export interface PaymentCreateRequest {
  orderId: string
  paymentMethod: 'WECHAT_PAY' | 'ALIPAY' | 'BANK_CARD'
  amount: number
  returnUrl?: string
  notifyUrl?: string
}

export interface PaymentQueryRequest {
  paymentId?: string
  orderId?: string
  transactionId?: string
}

export interface RefundRequest {
  paymentId: string
  refundAmount: number
  refundReason: string
}

export interface RefundQueryRequest {
  refundId?: string
  paymentId?: string
}

// 创建支付（预留接口）
export const createPayment = (data: PaymentCreateRequest): Promise<ApiResponse<any>> => {
  return wxApiClient.post('/api/payment/create', data)
}

// 查询支付状态（预留接口）
export const queryPaymentStatus = (params: PaymentQueryRequest): Promise<ApiResponse<any>> => {
  const searchParams = new URLSearchParams()
  if (params.paymentId) searchParams.append('paymentId', params.paymentId)
  if (params.orderId) searchParams.append('orderId', params.orderId)
  if (params.transactionId) searchParams.append('transactionId', params.transactionId)
  
  const queryString = searchParams.toString()
  const url = queryString ? `/api/payment/query?${queryString}` : '/api/payment/query'
  return wxApiClient.get(url)
}

// 支付回调处理（预留接口）
export const paymentCallback = (data: any): Promise<ApiResponse<any>> => {
  return wxApiClient.post('/api/payment/callback', data)
}

// 申请退款（预留接口）
export const requestRefund = (data: RefundRequest): Promise<ApiResponse<any>> => {
  return wxApiClient.post('/api/payment/refund', data)
}

// 查询退款状态（预留接口）
export const queryRefundStatus = (params: RefundQueryRequest): Promise<ApiResponse<any>> => {
  const searchParams = new URLSearchParams()
  if (params.refundId) searchParams.append('refundId', params.refundId)
  if (params.paymentId) searchParams.append('paymentId', params.paymentId)
  
  const queryString = searchParams.toString()
  const url = queryString ? `/api/payment/refund/query?${queryString}` : '/api/payment/refund/query'
  return wxApiClient.get(url)
}

// 获取支付方式列表（预留接口）
export const getPaymentMethods = (): Promise<ApiResponse<any>> => {
  return wxApiClient.get('/api/payment/methods')
}

// 获取用户支付记录（预留接口）
export const getUserPaymentHistory = (params?: { 
  page?: number
  limit?: number
  status?: string 
}): Promise<ApiResponse<any>> => {
  const searchParams = new URLSearchParams()
  if (params?.page !== undefined) searchParams.append('page', params.page.toString())
  if (params?.limit !== undefined) searchParams.append('limit', params.limit.toString())
  if (params?.status) searchParams.append('status', params.status)
  
  const queryString = searchParams.toString()
  const url = queryString ? `/api/payment/user/history?${queryString}` : '/api/payment/user/history'
  return wxApiClient.get(url)
}