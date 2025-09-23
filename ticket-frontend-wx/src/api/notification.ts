import { wxApiClient } from '@/utils/wxApiClient'
import type { ApiResponse, PageResponse } from '@/types'

// 注意：通知相关API需要后端实现对应的控制器
// 这些是预留接口，等待后端服务实现

// 通知相关接口类型定义
export interface SendSmsRequest {
  phone: string
  templateCode: string
  params?: Record<string, any>
}

export interface SendEmailRequest {
  email: string
  subject: string
  content: string
  templateCode?: string
  params?: Record<string, any>
}

export interface SendInAppMessageRequest {
  userId: number
  title: string
  content: string
  type?: 'SYSTEM' | 'ORDER' | 'PROMOTION'
  relatedId?: string
}

export interface MessageQuery {
  type?: string
  status?: 'UNREAD' | 'READ'
  page?: number
  limit?: number
}

// 发送短信（预留接口）
export const sendSms = (data: SendSmsRequest): Promise<ApiResponse<any>> => {
  return wxApiClient.post('/api/notification/sms/send', data)
}

// 发送邮件（预留接口）
export const sendEmail = (data: SendEmailRequest): Promise<ApiResponse<any>> => {
  return wxApiClient.post('/api/notification/email/send', data)
}

// 发送站内信（预留接口）
export const sendInAppMessage = (data: SendInAppMessageRequest): Promise<ApiResponse<any>> => {
  return wxApiClient.post('/api/notification/message/send', data)
}

// 获取用户消息列表（预留接口）
export const getUserMessages = (params?: MessageQuery): Promise<ApiResponse<PageResponse<any>>> => {
  const searchParams = new URLSearchParams()
  if (params?.type) searchParams.append('type', params.type)
  if (params?.status) searchParams.append('status', params.status)
  if (params?.page !== undefined) searchParams.append('page', params.page.toString())
  if (params?.limit !== undefined) searchParams.append('limit', params.limit.toString())
  
  const queryString = searchParams.toString()
  const url = queryString ? `/api/notification/messages?${queryString}` : '/api/notification/messages'
  return wxApiClient.get(url)
}

// 获取消息详情（预留接口）
export const getMessageDetail = (messageId: string): Promise<ApiResponse<any>> => {
  return wxApiClient.get(`/api/notification/messages/${messageId}`)
}

// 标记消息为已读（预留接口）
export const markMessageAsRead = (messageId: string): Promise<ApiResponse<void>> => {
  return wxApiClient.put(`/api/notification/messages/${messageId}/read`)
}

// 批量标记消息为已读（预留接口）
export const markMessagesAsRead = (messageIds: string[]): Promise<ApiResponse<void>> => {
  return wxApiClient.put('/api/notification/messages/batch-read', { messageIds })
}

// 删除消息（预留接口）
export const deleteMessage = (messageId: string): Promise<ApiResponse<void>> => {
  return wxApiClient.delete(`/api/notification/messages/${messageId}`)
}

// 获取未读消息数量（预留接口）
export const getUnreadMessageCount = (): Promise<ApiResponse<number>> => {
  return wxApiClient.get('/api/notification/messages/unread-count')
}

// 获取消息设置（预留接口）
export const getMessageSettings = (): Promise<ApiResponse<any>> => {
  return wxApiClient.get('/api/notification/settings')
}

// 更新消息设置（预留接口）
export const updateMessageSettings = (data: {
  smsEnabled?: boolean
  emailEnabled?: boolean
  pushEnabled?: boolean
  orderNotification?: boolean
  promotionNotification?: boolean
}): Promise<ApiResponse<void>> => {
  return wxApiClient.put('/api/notification/settings', data)
}

// 订阅推送（预留接口）
export const subscribePush = (data: { endpoint: string; keys: any }): Promise<ApiResponse<any>> => {
  return wxApiClient.post('/api/notification/push/subscribe', data)
}

// 取消订阅推送（预留接口）
export const unsubscribePush = (endpoint: string): Promise<ApiResponse<void>> => {
  return wxApiClient.post('/api/notification/push/unsubscribe', { endpoint })
}