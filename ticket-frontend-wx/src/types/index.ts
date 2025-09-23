// 用户相关类型
export interface User {
  id: string
  userId?: string // 兼容字段
  username: string
  nickname: string
  phone: string
  email: string
  avatar?: string
  gender?: number
  birthday?: string
}

export interface UserLoginForm {
  username: string
  password: string
}

export interface LoginRequest {
  username: string
  password: string
  loginType?: string
}

export interface RegisterRequest {
  username: string
  password: string
  phone: string
  email: string
}

export interface UpdateUserRequest {
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  birthday?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface UserRegisterForm {
  username: string
  password: string
  confirmPassword: string
  phone: string
  email: string
  nickname: string
}

// 电竞机位相关类型
export interface Show {
  id: string
  name: string
  title?: string // 兼容字段
  type?: 'NEWBIE' | 'INTERMEDIATE' | 'ADVANCED' | 'VIP_ROOM' | 'SVIP' // 机位类型：新客/中级/高级/包厢/SVIP
  categoryId?: string
  description: string
  coverImg?: string
  detailImgs?: string[]
  duration?: number
  notice?: string
  posterUrl: string
  venue?: string
  venueName?: string
  city?: string
  startTime?: string
  endTime?: string
  showTime?: string
  minPrice: number // 每小时最低价格
  maxPrice: number // 每小时最高价格
  status: ShowStatus
  createTime?: string
  updateTime?: string
  isHot?: boolean
}

export type ShowStatus = 'ON_SALE' | 'SOLD_OUT' | 'UPCOMING' | 'ENDED'

export interface ShowSession {
  id: string
  showId: string
  venueId: string
  sessionTime: string
  status: number
  createTime: string
  updateTime: string
}

export interface Venue {
  id: string
  name: string
  province: string
  city: string
  district: string
  address: string
  description: string
  contactPhone: string
  trafficInfo: string
}

// 订单相关类型
export interface Order {
  id: string
  orderNo: string
  userId: string
  totalAmount: number
  payAmount: number
  discountAmount: number
  status: OrderStatus
  payType?: number
  payTime?: string
  expireTime: string
  remark?: string
  createTime: string
  updateTime: string
  items: OrderItem[]
}

export type OrderStatus = 0 | 1 | 2 | 3 | 4 // 0-待支付，1-已支付，2-已取消，3-已完成，4-已退款

export interface OrderItem {
  id: string
  orderId: string
  orderNo: string
  showId: string
  showTitle: string
  sessionId: string
  sessionTime: string
  venueId: string
  venueName: string
  ticketTypeId: string
  ticketTypeName: string
  seatId?: string
  seatInfo?: string
  price: number
  quantity: number
  subtotal: number
}

// 票务相关类型
// 机位类型定义
export interface TicketType {
  id: string
  showId: string
  sessionId: string
  areaId: string
  name: string // 机位名称：新客电竞机位/中级电竞机位/高级电竞机位/包厢电竞机位/SVIP电竞机位
  price: number // 每小时价格
  stock: number
  remaining: number
  limitPerUser: number
  status: number
}

// API响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 通用类型
export interface SelectOption {
  label: string
  value: string | number
}

export interface Category {
  name: string
  icon: string
  type: string
}

// 订单相关新增类型
// 创建机位预订订单
export interface CreateOrderRequest {
  userId: string
  showId: string // 机位ID
  sessionId: string // 时段ID
  ticketId: string // 机位类型ID
  quantity: number // 预订数量（机位数）
  bookingDate?: string // 预订日期
  bookingEndTime?: string // 预订结束时间
  bookingDuration?: number // 预订时长（小时）
  contactPhone?: string // 联系电话
  remark?: string // 备注
  totalPrice?: number // 总价格
  basePrice?: number // 基础价格
  nightSurcharge?: number // 夜间附加费
}

export interface CreateOrderData {
  sessionId: number
  tickets: {
    ticketTypeId: number
    seatIds?: number[]
    quantity: number
  }[]
  contactInfo: {
    name: string
    phone: string
    email?: string
  }
  remark?: string
}

export interface OrderListParams {
  status?: OrderStatus
  page?: number
  limit?: number
  startTime?: string
  endTime?: string
}

// 演出相关新增类型
export interface ShowListParams {
  keyword?: string
  category?: string
  city?: string
  page?: number
  limit?: number
  status?: ShowStatus
  minPrice?: number
  maxPrice?: number
}

// 座位相关类型
export interface SeatArea {
  id: number
  venueId: number
  name: string
  description?: string
  floor: number
  totalSeats: number
  availableSeats: number
}

export interface Seat {
  id: number
  areaId: number
  row: string
  number: string
  status: 'AVAILABLE' | 'LOCKED' | 'SOLD'
  price?: number
}

// 票务相关类型
export interface Ticket {
  id: string
  orderId: string
  ticketCode: string
  status: 'VALID' | 'USED' | 'EXPIRED' | 'REFUNDED'
  qrCode?: string
  seatInfo?: string
  createTime: string
  useTime?: string
}

// 支付相关类型
export interface Payment {
  id: string
  orderId: string
  paymentMethod: string
  amount: number
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
  transactionId?: string
  createTime: string
  payTime?: string
}

// 通知相关类型
export interface Message {
  id: string
  userId: number
  title: string
  content: string
  type: 'SYSTEM' | 'ORDER' | 'PROMOTION'
  status: 'UNREAD' | 'READ'
  relatedId?: string
  createTime: string
  readTime?: string
}