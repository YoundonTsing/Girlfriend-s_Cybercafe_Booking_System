// API 配置文件

// API 基础配置
export const API_CONFIG = {
  // 基础URL
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api',
  
  // 超时时间
  TIMEOUT: 10000,
  
  // 重试次数
  RETRY_COUNT: 3,
  
  // 重试延迟（毫秒）
  RETRY_DELAY: 1000
}

// 服务端点配置
export const API_ENDPOINTS = {
  // 用户服务
  USER: {
    LOGIN: '/user/auth/login',
    REGISTER: '/user/auth/register',
    WECHAT_LOGIN: '/user/auth/wechat-login',
    LOGOUT: '/user/auth/logout',
    REFRESH_TOKEN: '/user/auth/refresh-token',
    CHANGE_PASSWORD: '/user/auth/change-password',
    PROFILE: '/user/profile',
    STATS: '/user/stats'
  },
  
  // 演出服务
  SHOW: {
    SHOWS: '/show/shows',
    SHOW_DETAIL: (id: number) => `/show/shows/${id}`,
    SESSIONS: '/show/sessions',
    SESSION_DETAIL: (id: number) => `/show/sessions/${id}`,
    SHOW_SESSIONS: (showId: number) => `/show/sessions/show/${showId}`,
    VENUES: '/show/venues',
    VENUE_DETAIL: (id: number) => `/show/venues/${id}`,
    SEAT_AREAS: '/show/seat-areas',
    VENUE_SEAT_AREAS: (venueId: number) => `/show/seat-areas/venue/${venueId}`,
    SEATS: '/show/seats',
    AREA_SEATS: (areaId: number) => `/show/seats/area/${areaId}`,
    SEARCH: '/show/shows/search',
    HOT_SHOWS: '/show/shows/hot',
    RECOMMEND_SHOWS: '/show/shows/recommend'
  },
  
  // 订单服务
  ORDER: {
    ORDERS: '/order/orders',
    ORDER_DETAIL: (id: string) => `/order/orders/${id}`,
    ORDER_BY_NO: (orderNo: string) => `/order/orders/no/${orderNo}`,
    ORDER_STATUS: (id: string) => `/order/orders/${id}/status`,
    PAY_ORDER: (id: string) => `/order/orders/${id}/pay`,
    CANCEL_ORDER: (id: string) => `/order/orders/${id}/cancel`,
    CONFIRM_ORDER: (id: string) => `/order/orders/${id}/confirm`,
    REFUND_ORDER: (id: string) => `/order/orders/${id}/refund`,
    ORDER_STATS: '/order/orders/stats'
  },
  
  // 票务服务
  TICKET: {
    LOCK: '/ticket/lock',
    RELEASE: '/ticket/release',
    INVENTORY: '/ticket/inventory',
    PRICE: '/ticket/price',
    USER_TICKETS: '/ticket/user/tickets',
    TICKET_DETAIL: (id: string) => `/ticket/tickets/${id}`,
    VALIDATE: '/ticket/validate',
    ELECTRONIC: (orderId: string) => `/ticket/electronic/${orderId}`,
    QR_CODE: (ticketId: string) => `/ticket/qr/${ticketId}`
  },
  
  // 支付服务
  PAYMENT: {
    CREATE: '/payment/create',
    QUERY: '/payment/query',
    CALLBACK: '/payment/callback',
    REFUND: '/payment/refund',
    REFUND_QUERY: '/payment/refund/query',
    METHODS: '/payment/methods',
    HISTORY: '/payment/user/history',
    WECHAT_UNIFIED_ORDER: '/payment/wechat/unified-order',
    WECHAT_QUERY: '/payment/wechat/query',
    ALIPAY_CREATE: '/payment/alipay/create',
    ALIPAY_QUERY: '/payment/alipay/query'
  },
  
  // 通知服务
  NOTIFICATION: {
    SEND_SMS: '/notification/sms/send',
    SEND_EMAIL: '/notification/email/send',
    SEND_MESSAGE: '/notification/message/send',
    MESSAGES: '/notification/messages',
    MESSAGE_DETAIL: (id: string) => `/notification/messages/${id}`,
    MARK_READ: (id: string) => `/notification/messages/${id}/read`,
    BATCH_READ: '/notification/messages/batch-read',
    DELETE_MESSAGE: (id: string) => `/notification/messages/${id}`,
    UNREAD_COUNT: '/notification/messages/unread-count',
    SETTINGS: '/notification/settings',
    PUSH_SUBSCRIBE: '/notification/push/subscribe',
    PUSH_UNSUBSCRIBE: '/notification/push/unsubscribe'
  }
}

// HTTP 状态码
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  METHOD_NOT_ALLOWED: 405,
  CONFLICT: 409,
  INTERNAL_SERVER_ERROR: 500,
  BAD_GATEWAY: 502,
  SERVICE_UNAVAILABLE: 503,
  GATEWAY_TIMEOUT: 504
}

// 业务状态码
export const BUSINESS_CODE = {
  SUCCESS: 200,
  PARAM_ERROR: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,
  
  // 业务相关
  USER_NOT_EXIST: 1001,
  PASSWORD_ERROR: 1002,
  TOKEN_EXPIRED: 1003,
  TOKEN_INVALID: 1004,
  
  SHOW_NOT_EXIST: 2001,
  SHOW_NOT_ON_SALE: 2002,
  SEAT_NOT_AVAILABLE: 2003,
  INVENTORY_INSUFFICIENT: 2004,
  
  ORDER_NOT_EXIST: 3001,
  ORDER_STATUS_ERROR: 3002,
  ORDER_EXPIRED: 3003,
  
  PAYMENT_FAILED: 4001,
  REFUND_FAILED: 4002
}