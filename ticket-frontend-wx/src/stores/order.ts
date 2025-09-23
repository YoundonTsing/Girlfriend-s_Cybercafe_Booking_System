import { defineStore } from 'pinia'
import { getOrderList, getOrderDetail, getOrderDetailByOrderNo, createOrder, cancelOrder, payOrder } from '@/api/order'
import type { Order, PageResponse } from '@/types'

interface OrderState {
  orderList: Order[]
  currentOrder: Order | null
  total: number
  loading: boolean
}

export const useOrderStore = defineStore('order', {
  state: (): OrderState => ({
    orderList: [],
    currentOrder: null,
    total: 0,
    loading: false
  }),

  getters: {
    pendingOrders: (state): Order[] => state.orderList.filter(order => order.status === 0),
    paidOrders: (state): Order[] => state.orderList.filter(order => order.status === 1),
    completedOrders: (state): Order[] => state.orderList.filter(order => order.status === 3)
  },

  actions: {
    // 获取订单列表
    async fetchOrderList(params: Record<string, any> = {}): Promise<PageResponse<Order>> {
      this.loading = true
      try {
        const response = await getOrderList(params)
        const { data } = response
        this.orderList = data.records || []
        this.total = data.total || 0
        return data
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    // 获取订单详情（通过订单ID）
    async fetchOrderDetail(id: number): Promise<Order> {
      this.loading = true
      try {
        const response = await getOrderDetail(id)
        const { data } = response
        this.currentOrder = data
        return data
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    // 获取订单详情（通过订单号）
    async fetchOrderDetailByOrderNo(orderNo: string): Promise<Order> {
      this.loading = true
      try {
        const response = await getOrderDetailByOrderNo(orderNo)
        const { data } = response
        this.currentOrder = data
        return data
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    // 创建订单
    async createOrder(orderData: any): Promise<Order> {
      try {
        const response = await createOrder(orderData)
        const { data } = response
        return data
      } catch (error) {
        throw error
      }
    },

    // 支付订单
    async payOrder(orderNo: string, paymentType: number = 1): Promise<void> {
      try {
        const response = await payOrder(orderNo, paymentType)
        // 支付成功后更新列表中的订单状态
        const index = this.orderList.findIndex(order => order.orderNo === orderNo)
        if (index !== -1) {
          this.orderList[index].status = 1 // 已支付状态
        }
      } catch (error) {
        throw error
      }
    },

    // 取消订单
    async cancelOrder(orderNo: string): Promise<void> {
      try {
        const response = await cancelOrder(orderNo)
        // 取消成功后更新列表中的订单状态
        const index = this.orderList.findIndex(order => order.orderNo === orderNo)
        if (index !== -1) {
          this.orderList[index].status = 2 // 已取消状态
        }
      } catch (error) {
        throw error
      }
    },

    // 清空当前订单
    clearCurrentOrder(): void {
      this.currentOrder = null
    }
  }
})