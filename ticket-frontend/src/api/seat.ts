import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface SeatArea {
  id: number
  name: string
  areaCode: string
  floorLevel: number
  areaType: string
  areaTypeName: string
  price: number
  nightPrice: number
  totalSeats: number
  availableSeats: number
  description: string
  selectable: boolean
}

export interface Seat {
  id: number
  seatCode: string
  rowNum: string
  seatNum: string
  xCoordinate: number
  yCoordinate: number
  seatType: number
  status: number // 0-维护，1-可选，2-已锁定，3-已占用
  price: number
  lockUserId?: number
  lockedByCurrentUser: boolean
}

export interface SeatLayout {
  areaId: number
  areaName: string
  floorLevel: number
  areaType: string
  totalRows: number
  totalCols: number
  seats: Seat[]
  layoutConfig?: string
  description: string
}

export interface SeatLockRequest {
  seatIds: number[]
}

// 获取座位区域列表
export function getSeatAreas(showType: number, showId: number): Promise<ApiResponse<SeatArea[]>> {
  return request({
    url: '/seat/areas',
    method: 'get',
    params: { showType, showId }
  })
}

// 获取座位布局
export function getSeatLayout(areaId: number, showId: number, sessionId: number): Promise<ApiResponse<SeatLayout>> {
  return request({
    url: `/seat/layout/${areaId}`,
    method: 'get',
    params: { showId, sessionId }
  })
}

// 锁定座位
export function lockSeats(seatIds: number[]): Promise<ApiResponse<boolean>> {
  return request({
    url: '/seat/lock',
    method: 'post',
    data: { seatIds }
  })
}

// 释放座位锁定
export function releaseSeats(seatIds: number[]): Promise<ApiResponse<boolean>> {
  return request({
    url: '/seat/release',
    method: 'post',
    data: { seatIds }
  })
}