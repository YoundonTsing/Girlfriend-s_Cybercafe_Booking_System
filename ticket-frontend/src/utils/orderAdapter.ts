import type { Order, OrderItem, PageResponse } from '@/types'

/**
 * 后端订单数据结构（与OrderVO对应）
 */
interface BackendOrderVO {
  id: number
  orderNo: string
  userId: number
  showId: number
  showName: string
  sessionId: number
  sessionName: string
  showTime: string
  venue: string
  ticketId: number
  ticketName: string
  price: number
  quantity: number
  totalAmount: number
  payAmount: number
  discountAmount: number
  status: number
  statusName: string
  payTime?: string
  payType?: number
  payTypeName?: string
  payNo?: string
  expireTime: string
  createTime: string
  remark?: string
  bookingDate?: string
  bookingEndTime?: string
  bookingDuration?: number
  contactPhone?: string
  seatId?: number
  seatInfo?: string
  items?: BackendOrderItemVO[]
}

interface BackendOrderItemVO {
  id: number
  orderId: number
  orderNo: string
  showId: number
  showTitle: string
  sessionId: number
  sessionTime: string
  venueId: number
  venueName: string
  ticketTypeId: number
  ticketTypeName: string
  seatId?: number
  seatInfo?: string
  price: number
  quantity: number
  subtotal: number
}

/**
 * 后端分页响应结构（MyBatis Plus Page对象）
 */
interface BackendPageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 转换后端订单项数据为前端格式
 */
function convertOrderItem(backendItem: BackendOrderItemVO): OrderItem {
  return {
    id: String(backendItem.id),
    orderId: String(backendItem.orderId),
    orderNo: backendItem.orderNo,
    showId: String(backendItem.showId),
    showTitle: backendItem.showTitle,
    sessionId: String(backendItem.sessionId),
    sessionTime: backendItem.sessionTime,
    venueId: String(backendItem.venueId),
    venueName: backendItem.venueName,
    ticketTypeId: String(backendItem.ticketTypeId),
    ticketTypeName: backendItem.ticketTypeName,
    seatId: backendItem.seatId ? String(backendItem.seatId) : undefined,
    seatInfo: backendItem.seatInfo,
    price: backendItem.price,
    quantity: backendItem.quantity,
    subtotal: backendItem.subtotal
  }
}

/**
 * 转换后端订单数据为前端格式
 */
function convertOrder(backendOrder: BackendOrderVO): Order {
  return {
    id: String(backendOrder.id),
    orderNo: backendOrder.orderNo,
    userId: String(backendOrder.userId),
    totalAmount: backendOrder.totalAmount,
    payAmount: backendOrder.payAmount,
    discountAmount: backendOrder.discountAmount,
    status: backendOrder.status as 0 | 1 | 2 | 3 | 4,
    payType: backendOrder.payType,
    payTime: backendOrder.payTime,
    expireTime: backendOrder.expireTime,
    remark: backendOrder.remark,
    createTime: backendOrder.createTime,
    updateTime: backendOrder.createTime, // 后端没有updateTime，使用createTime
    items: backendOrder.items ? backendOrder.items.map(convertOrderItem) : [],
    // 添加预约信息字段
    showId: backendOrder.showId ? String(backendOrder.showId) : undefined,
    showName: backendOrder.showName,
    sessionId: backendOrder.sessionId ? String(backendOrder.sessionId) : undefined,
    sessionName: backendOrder.sessionName,
    showTime: backendOrder.showTime,
    venue: backendOrder.venue,
    ticketId: backendOrder.ticketId ? String(backendOrder.ticketId) : undefined,
    ticketName: backendOrder.ticketName,
    price: backendOrder.price,
    quantity: backendOrder.quantity,
    bookingDate: backendOrder.bookingDate,
    bookingEndTime: backendOrder.bookingEndTime,
    bookingDuration: backendOrder.bookingDuration,
    contactPhone: backendOrder.contactPhone,
    seatId: backendOrder.seatId ? String(backendOrder.seatId) : undefined,
    seatInfo: backendOrder.seatInfo
  }
}

/**
 * 转换后端分页响应为前端格式
 */
export function convertOrderPageResponse(backendResponse: BackendPageResponse<BackendOrderVO>): PageResponse<Order> {
  return {
    records: backendResponse.records.map(convertOrder),
    total: backendResponse.total,
    size: backendResponse.size,
    current: backendResponse.current,
    pages: backendResponse.pages
  }
}

/**
 * 转换单个订单响应
 */
export function convertOrderResponse(backendOrder: BackendOrderVO): Order {
  return convertOrder(backendOrder)
}