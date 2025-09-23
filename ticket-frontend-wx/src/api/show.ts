import { wxApiClient } from '@/utils/wxApiClient'
import type { Show, PageResponse, ApiResponse, ShowListParams } from '@/types'

// 查询演出列表
export const getShowList = (params?: {
  page?: number
  size?: number
  sort?: string
  status?: string
  type?: number
  city?: string
  keyword?: string
}): Promise<ApiResponse<PageResponse<Show>>> => {
  const searchParams = new URLSearchParams()
  if (params?.page !== undefined) searchParams.append('page', params.page.toString())
  if (params?.size !== undefined) searchParams.append('size', params.size.toString())
  if (params?.sort) searchParams.append('sort', params.sort)
  if (params?.status) searchParams.append('status', params.status)
  if (params?.type !== undefined) searchParams.append('type', params.type.toString())
  if (params?.city) searchParams.append('city', params.city)
  if (params?.keyword) searchParams.append('keyword', params.keyword)
  
  const queryString = searchParams.toString()
  const url = queryString ? `/api/show/list?${queryString}` : '/api/show/list'
  return wxApiClient.get(url)
}

// 分页查询演出列表
export const getShowPage = (params?: {
  page?: number
  size?: number
  type?: number
  city?: string
  keyword?: string
}): Promise<ApiResponse<PageResponse<Show>>> => {
  const searchParams = new URLSearchParams()
  if (params?.page !== undefined) searchParams.append('page', params.page.toString())
  if (params?.size !== undefined) searchParams.append('size', params.size.toString())
  if (params?.type !== undefined) searchParams.append('type', params.type.toString())
  if (params?.city) searchParams.append('city', params.city)
  if (params?.keyword) searchParams.append('keyword', params.keyword)
  
  const queryString = searchParams.toString()
  const url = queryString ? `/api/show/page?${queryString}` : '/api/show/page'
  return wxApiClient.get(url)
}

// 获取演出详情
export const getShowDetail = (id: number | string): Promise<ApiResponse<Show>> => {
  return wxApiClient.get(`/api/show/${id}`)
}

// 获取演出信息（用于订单服务调用）
export const getShowInfo = (showId: number | string, sessionId: number | string): Promise<ApiResponse<any>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('showId', showId.toString())
  searchParams.append('sessionId', sessionId.toString())
  
  return wxApiClient.get(`/api/show/info?${searchParams.toString()}`)
}

// 获取热门演出列表
export const getHotShows = (): Promise<ApiResponse<Show[]>> => {
  return wxApiClient.get('/api/show/hot')
}

// 获取推荐演出列表
export const getRecommendShows = (): Promise<ApiResponse<Show[]>> => {
  return wxApiClient.get('/api/show/recommend')
}