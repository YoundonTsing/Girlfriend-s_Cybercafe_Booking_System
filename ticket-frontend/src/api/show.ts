import { webApiClient } from '@/utils/webApiClient'
import type { Show, PageResponse, ApiResponse, Venue, TicketType, ShowListParams } from '@/types'

// 获取演出列表/机位列表
export function getShowList(params?: ShowListParams | Record<string, any>): Promise<ApiResponse<PageResponse<Show>>> {
  return webApiClient.get('/show/list', params)
}

// 获取演出详情/机位详情
export function getShowDetail(id: string | number): Promise<ApiResponse<Show>> {
  return webApiClient.get(`/show/detail/${id}`)
}

// 获取机位时段列表
export function getShowSessions(showId: string | number): Promise<ApiResponse<any[]>> {
  return webApiClient.get(`/show/sessions/show/${showId}`)
}

// 获取时段详情
export function getSessionDetail(sessionId: string | number): Promise<ApiResponse<any>> {
  return webApiClient.get(`/show/sessions/${sessionId}`)
}

// 获取演出票档信息/机位价格信息
export function getShowPrices(showId: string | number): Promise<ApiResponse<TicketType[]>> {
  return webApiClient.get(`/show/shows/${showId}/prices`)
}

// 获取演出场馆信息
export function getVenueInfo(venueId: string | number): Promise<ApiResponse<Venue>> {
  return webApiClient.get(`/show/venues/${venueId}`)
}

// 获取场馆列表
export function getVenueList(params?: any): Promise<ApiResponse<PageResponse<Venue>>> {
  return webApiClient.get('/show/venues', params)
}

// 获取座位区域信息
export function getSeatAreas(venueId: string | number): Promise<ApiResponse<any[]>> {
  return webApiClient.get(`/show/seat-areas/venue/${venueId}`)
}

// 获取座位信息
export function getSeats(areaId: string | number): Promise<ApiResponse<any[]>> {
  return webApiClient.get(`/show/seats/area/${areaId}`)
}

// 搜索演出
export function searchShows(params: { keyword: string; category?: string; city?: string }): Promise<ApiResponse<PageResponse<Show>>> {
  return webApiClient.get('/show/shows/search', params)
}

// 获取热门演出列表/热门机位
export function getHotShows(params?: { limit?: number }): Promise<ApiResponse<Show[]>> {
  return webApiClient.get('/show/hot', params)
}

// 获取推荐演出列表/推荐机位
export function getRecommendShows(params?: { limit?: number }): Promise<ApiResponse<Show[]>> {
  return webApiClient.get('/show/recommend', params)
}