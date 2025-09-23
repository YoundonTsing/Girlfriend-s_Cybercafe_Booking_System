<template>
  <view class="order-detail">
    <view class="loading" v-if="loading">
      <text>加载中...</text>
    </view>
    
    <view class="order-info" v-else-if="order">
      <view class="order-header">
        <text class="order-no">订单号：{{ order.orderNo }}</text>
        <text class="order-status" :class="getStatusClass(order.status)">{{ getStatusText(order.status) }}</text>
      </view>
      
      <view class="order-items">
        <view class="item" v-for="item in order.items" :key="item.id">
          <text class="show-title">{{ item.showTitle }}</text>
          <text class="session-time">{{ item.sessionTime }}</text>
          <text class="venue">{{ item.venueName }}</text>
          <text class="ticket-type">{{ item.ticketTypeName }}</text>
          <text class="quantity">数量：{{ item.quantity }}</text>
          <text class="price">￥{{ item.price }}</text>
        </view>
      </view>
      
      <view class="order-summary">
        <text class="total">总金额：￥{{ order.totalAmount }}</text>
        <text class="pay-amount">实付金额：￥{{ order.payAmount }}</text>
      </view>
    </view>
    
    <view class="error" v-else>
      <text>订单不存在或已删除</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getOrderDetail } from '@/api/order'
import { useUserStore } from '@/stores/user'
import type { Order } from '@/types'

const order = ref<Order | null>(null)
const loading = ref(true)
const orderId = ref<string>('')

const getStatusText = (status: number) => {
  const statusMap = {
    0: '待支付',
    1: '已支付',
    2: '已取消',
    3: '已完成',
    4: '已退款'
  }
  return statusMap[status] || '未知状态'
}

const getStatusClass = (status: number) => {
  const classMap = {
    0: 'pending',
    1: 'paid',
    2: 'cancelled',
    3: 'completed',
    4: 'refunded'
  }
  return classMap[status] || ''
}

const loadOrderDetail = async () => {
  try {
    console.log('开始加载订单详情，订单ID:', orderId.value)
    
    if (orderId.value) {
      // 直接传递字符串类型的orderId，与API定义保持一致
      const response = await getOrderDetail(orderId.value)
      order.value = response.data
      console.log('订单详情加载成功:', response.data)
    } else {
      console.error('订单ID为空')
      uni.showToast({
        title: '订单ID获取失败',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('加载订单详情失败', error)
    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 使用 onLoad 生命周期获取路由参数
onLoad((options: any) => {
  console.log('页面参数:', options)
  orderId.value = options.id || ''
  
  // 检查登录状态
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) {
    console.log('用户未登录，跳转到登录页面')
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    setTimeout(() => {
      uni.reLaunch({
        url: '/pages/login/index'
      })
    }, 1500)
    return
  }
  
  loadOrderDetail()
})
</script>

<style scoped>
.order-detail {
  padding: 20rpx;
}

.loading, .error {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  background: #fff;
  border-radius: 10rpx;
  margin-bottom: 20rpx;
}

.order-no {
  font-size: 28rpx;
  color: #333;
}

.order-status {
  font-size: 24rpx;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
}

.order-status.pending {
  background: #fff3cd;
  color: #856404;
}

.order-status.paid {
  background: #d4edda;
  color: #155724;
}

.order-status.cancelled {
  background: #f8d7da;
  color: #721c24;
}

.order-status.completed {
  background: #cce5ff;
  color: #004085;
}

.order-items {
  background: #fff;
  border-radius: 10rpx;
  margin-bottom: 20rpx;
}

.item {
  padding: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.item:last-child {
  border-bottom: none;
}

.show-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 10rpx;
}

.session-time, .venue, .ticket-type {
  font-size: 26rpx;
  color: #666;
  display: block;
  margin-bottom: 5rpx;
}

.quantity, .price {
  font-size: 28rpx;
  color: #333;
  display: inline-block;
  margin-right: 20rpx;
}

.price {
  color: #ff4757;
  font-weight: bold;
}

.order-summary {
  background: #fff;
  border-radius: 10rpx;
  padding: 20rpx;
}

.total, .pay-amount {
  font-size: 30rpx;
  color: #333;
  display: block;
  margin-bottom: 10rpx;
}

.pay-amount {
  color: #ff4757;
  font-weight: bold;
  font-size: 36rpx;
}
</style>