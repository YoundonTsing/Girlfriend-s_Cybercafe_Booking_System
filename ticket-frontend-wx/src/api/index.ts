// API 模块统一导出

// 用户相关API
export * from './user'

// 演出相关API
export * from './show'

// 座位相关API
export * from './seat'

// 订单相关API
export * from './order'

// 票务相关API
export * from './ticket'

// 支付相关API
export * from './payment'

// 通知相关API
export * from './notification'

// 默认导出（保持向后兼容）
import * as userApi from './user'
import * as showApi from './show'
import * as seatApi from './seat'
import * as orderApi from './order'
import * as ticketApi from './ticket'
import * as paymentApi from './payment'
import * as notificationApi from './notification'

export {
  userApi,
  showApi,
  seatApi,
  orderApi,
  ticketApi,
  paymentApi,
  notificationApi
}

// 便捷的默认导出
export default {
  user: userApi,
  show: showApi,
  seat: seatApi,
  order: orderApi,
  ticket: ticketApi,
  payment: paymentApi,
  notification: notificationApi
}