import { wxApiClient } from '@/utils/wxApiClient'
import type { ApiResponse } from '@/types'

// 座位区域类型
export interface SeatAreaVO {
  id: number
  name: string
  description?: string
  basePrice: number
  totalSeats: number
  availableSeats: number
}

// 座位布局类型
export interface SeatLayoutVO {
  areaId: number
  areaName: string
  rows: SeatRow[]
  totalSeats: number
  availableSeats: number
}

// 座位行类型
export interface SeatRow {
  rowName: string
  seats: Seat[]
}

// 座位类型
export interface Seat {
  id: number
  rowName: string
  seatNumber: string
  status: 'available' | 'occupied' | 'locked' | 'selected'
  price: number
  lockedByCurrentUser?: boolean
}

// 座位锁定请求类型
export interface SeatLockRequest {
  seatIds: number[]
  showId: number
  sessionId: number
}

// 根据机位类型获取可选座位区域
export const getSeatAreas = (params: {
  showType: number
  showId: number
}): Promise<ApiResponse<SeatAreaVO[]>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('showType', params.showType.toString())
  searchParams.append('showId', params.showId.toString())
  
  return wxApiClient.get(`/api/seat/areas?${searchParams.toString()}`)
}

// 获取座位区域的布局信息
export const getSeatLayout = (
  areaId: number | string,
  params: {
    showId: number
    sessionId: number
  }
): Promise<ApiResponse<SeatLayoutVO>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('showId', params.showId.toString())
  searchParams.append('sessionId', params.sessionId.toString())
  
  return wxApiClient.get(`/api/seat/layout/${areaId}?${searchParams.toString()}`)
}

// 锁定选中的座位
export const lockSeats = (data: SeatLockRequest): Promise<ApiResponse<boolean>> => {
  return wxApiClient.post('/api/seat/lock', data)
}

// 释放座位锁定
export const releaseSeats = (data: SeatLockRequest): Promise<ApiResponse<boolean>> => {
  return wxApiClient.post('/api/seat/release', data)
}