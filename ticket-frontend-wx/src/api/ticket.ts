import { wxApiClient } from '@/utils/wxApiClient'
import type { ApiResponse } from '@/types'

// 票档类型
export interface Ticket {
  id: number
  showId: number
  sessionId: number
  name: string
  price: number
  totalStock: number
  availableStock: number
  lockedStock: number
  description?: string
  status: number
  createTime?: string
  updateTime?: string
}

// 获取票档价格
export const getTicketPrice = (ticketId: number | string): Promise<ApiResponse<number>> => {
  return wxApiClient.get(`/api/ticket/price/${ticketId}`)
}

// 锁定票档库存
export const lockTicketStock = (params: {
  ticketId: number
  quantity: number
}): Promise<ApiResponse<boolean>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('ticketId', params.ticketId.toString())
  searchParams.append('quantity', params.quantity.toString())
  
  return wxApiClient.put(`/api/ticket/lock?${searchParams.toString()}`)
}

// 释放票档库存
export const unlockTicketStock = (params: {
  ticketId: number
  quantity: number
}): Promise<ApiResponse<boolean>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('ticketId', params.ticketId.toString())
  searchParams.append('quantity', params.quantity.toString())
  
  return wxApiClient.put(`/api/ticket/unlock?${searchParams.toString()}`)
}

// 扣减票档库存
export const deductTicketStock = (params: {
  ticketId: number
  quantity: number
}): Promise<ApiResponse<boolean>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('ticketId', params.ticketId.toString())
  searchParams.append('quantity', params.quantity.toString())
  
  return wxApiClient.put(`/api/ticket/deduct?${searchParams.toString()}`)
}

// 获取票档列表
export const getTicketList = (params: {
  showId: number
  sessionId: number
}): Promise<ApiResponse<Ticket[]>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('showId', params.showId.toString())
  searchParams.append('sessionId', params.sessionId.toString())
  
  return wxApiClient.get(`/api/ticket/list?${searchParams.toString()}`)
}

// 创建票档（管理功能）
export const createTicket = (data: Partial<Ticket>): Promise<ApiResponse<number>> => {
  return wxApiClient.post('/api/ticket', data)
}

// 更新票档信息（管理功能）
export const updateTicket = (data: Ticket): Promise<ApiResponse<void>> => {
  return wxApiClient.put('/api/ticket', data)
}

// 删除票档（管理功能）
export const deleteTicket = (id: number | string): Promise<ApiResponse<void>> => {
  return wxApiClient.delete(`/api/ticket/${id}`)
}