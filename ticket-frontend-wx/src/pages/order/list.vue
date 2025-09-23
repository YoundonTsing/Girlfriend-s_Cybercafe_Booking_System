<template>
  <view class="order-list-container">
    <!-- 状态筛选栏 -->
    <view class="filter-bar">
      <scroll-view class="filter-scroll" scroll-x="true">
        <view class="filter-item" 
          :class="{ active: currentStatus === '' }" 
          @tap="filterByStatus('')"
        >
          全部
        </view>
        <view class="filter-item" 
          :class="{ active: currentStatus === 'PENDING' }" 
          @tap="filterByStatus('PENDING')"
        >
          待支付
        </view>
        <view class="filter-item" 
          :class="{ active: currentStatus === 'PAID' }" 
          @tap="filterByStatus('PAID')"
        >
          已支付
        </view>
        <view class="filter-item" 
          :class="{ active: currentStatus === 'CANCELLED' }" 
          @tap="filterByStatus('CANCELLED')"
        >
          已取消
        </view>
      </scroll-view>
    </view>

    <!-- 订单列表 -->
    <scroll-view 
      class="order-scroll" 
      scroll-y="true" 
      @scrolltolower="loadMore"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="order-list">
        <view v-for="order in orderList" :key="order.id" class="order-item" @tap="goToDetail(order.id)">
          <view class="order-card">
            <!-- 订单头部 -->
            <view class="order-header">
              <text class="order-number">订单号：{{ order.orderNumber }}</text>
              <text class="order-status" :class="getStatusClass(order.status)">
                {{ getStatusText(order.status) }}
              </text>
            </view>

            <!-- 演出信息 -->
            <view class="show-info">
              <image :src="order.showPosterUrl" class="show-poster" mode="aspectFill" />
              <view class="show-details">
                <text class="show-title">{{ order.showTitle }}</text>
                <text class="show-time">{{ order.showTime }}</text>
                <text class="show-venue">{{ order.venueName }}</text>
                <text class="ticket-info">{{ order.ticketTypeName }} × {{ order.ticketCount }}张</text>
              </view>
            </view>

            <!-- 订单底部 -->
            <view class="order-footer">
              <view class="order-total">
                <text class="total-label">实付金额：</text>
                <text class="total-amount">¥{{ order.totalAmount }}</text>
              </view>
              <view class="order-actions">
                <button 
                  v-if="order.status === 'PENDING'" 
                  class="action-btn cancel-btn" 
                  @tap.stop="cancelOrder(order.id)"
                >
                  取消订单
                </button>
                <button 
                  v-if="order.status === 'PENDING'" 
                  class="action-btn pay-btn" 
                  @tap.stop="payOrder(order.id)"
                >
                  立即支付
                </button>
                <button 
                  v-if="order.status === 'PAID'" 
                  class="action-btn detail-btn" 
                  @tap.stop="goToDetail(order.id)"
                >
                  查看详情
                </button>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      
      <view class="no-more" v-if="noMore && orderList.length > 0">
        <text>没有更多了</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="orderList.length === 0 && !loading">
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无订单记录</text>
        <button class="go-shopping-btn" @tap="goToHome">
          去看看演出
        </button>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderList, cancelOrder as cancelOrderApi, payOrder as payOrderApi } from '@/api/order'

// 响应式数据
const orderList = ref<any[]>([])
const currentStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const refreshing = ref(false)
const noMore = ref(false)

// 页面挂载
onMounted(() => {
  loadOrderList(true)
})

// 加载订单列表
const loadOrderList = async (reset = false) => {
  if (loading.value) return
  
  loading.value = true
  
  try {
    if (reset) {
      currentPage.value = 1
      orderList.value = []
      noMore.value = false
    }
    
    const params: any = {
      page: currentPage.value,
      size: pageSize.value
    }
    
    if (currentStatus.value) {
      params.status = currentStatus.value
    }
    
    const response = await getOrderList(params)
    const { records, total } = response.data
    
    if (reset) {
      orderList.value = records
    } else {
      orderList.value.push(...records)
    }
    
    // 判断是否还有更多数据
    noMore.value = orderList.value.length >= total
    
  } catch (error) {
    console.error('获取订单列表失败', error)
    uni.showToast({
      title: '获取订单列表失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// 按状态筛选
const filterByStatus = (status: string) => {
  currentStatus.value = status
  loadOrderList(true)
}

// 下拉刷新
const onRefresh = () => {
  refreshing.value = true
  loadOrderList(true)
}

// 加载更多
const loadMore = () => {
  if (!loading.value && !noMore.value) {
    currentPage.value++
    loadOrderList()
  }
}

// 取消订单
const cancelOrder = async (orderId: number) => {
  try {
    const result = await uni.showModal({
      title: '确认取消',
      content: '确定要取消这个订单吗？',
      confirmText: '确定',
      cancelText: '取消'
    })
    
    if (result.confirm) {
      // 需要传递订单号而不是订单ID
      const order = orderList.value.find(o => o.id === orderId)
      if (order && order.orderNo) {
        await cancelOrderApi(order.orderNo)
        uni.showToast({
          title: '订单已取消',
          icon: 'success'
        })
        loadOrderList(true)
      }
    }
  } catch (error) {
    console.error('取消订单失败', error)
    uni.showToast({
      title: '取消订单失败',
      icon: 'none'
    })
  }
}

// 支付订单
const payOrder = async (orderId: number) => {
  try {
    uni.showLoading({
      title: '发起支付...'
    })
    
    // 需要传递订单号而不是订单ID
    const order = orderList.value.find(o => o.id === orderId)
    if (order && order.orderNo) {
      const payResult = await payOrderApi(order.orderNo)
      
      uni.hideLoading()
      
      if (payResult) {
        // 这里应该调用微信支付，暂时用模拟
        const modalResult = await uni.showModal({
          title: '支付确认',
          content: '确定要支付这个订单吗？',
          confirmText: '支付',
          cancelText: '取消'
        })
        
        if (modalResult.confirm) {
          uni.showToast({
            title: '支付成功',
            icon: 'success'
          })
          loadOrderList(true)
        }
      }
    }
  } catch (error) {
    uni.hideLoading()
    console.error('支付失败', error)
    uni.showToast({
      title: '支付失败',
      icon: 'none'
    })
  }
}

// 跳转到订单详情
const goToDetail = (orderId: number) => {
  uni.navigateTo({
    url: `/pages/order/detail?id=${orderId}`
  })
}

// 跳转到首页
const goToHome = () => {
  uni.switchTab({
    url: '/pages/index/index'
  })
}

// 获取状态样式类
const getStatusClass = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': 'status-pending',
    'PAID': 'status-paid',
    'CANCELLED': 'status-cancelled',
    'REFUNDED': 'status-refunded'
  }
  return statusMap[status] || ''
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待支付',
    'PAID': '已支付',
    'CANCELLED': '已取消',
    'REFUNDED': '已退款'
  }
  return statusMap[status] || ''
}
</script>

<style scoped>
.order-list-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8f8f8;
}

/* 筛选栏 */
.filter-bar {
  background-color: white;
  border-bottom: 1rpx solid #eee;
}

.filter-scroll {
  white-space: nowrap;
  padding: 20rpx 0;
}

.filter-item {
  display: inline-block;
  padding: 16rpx 32rpx;
  margin: 0 20rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  color: #666;
  background-color: #f5f5f5;
  transition: all 0.3s;
}

.filter-item.active {
  background-color: #007aff;
  color: white;
}

/* 订单列表 */
.order-scroll {
  flex: 1;
  padding: 20rpx;
}

.order-item {
  margin-bottom: 20rpx;
}

.order-card {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
}

/* 订单头部 */
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.order-number {
  font-size: 26rpx;
  color: #666;
}

.order-status {
  font-size: 26rpx;
  font-weight: bold;
}

.status-pending {
  color: #ff8c00;
}

.status-paid {
  color: #52c41a;
}

.status-cancelled {
  color: #999;
}

.status-refunded {
  color: #1890ff;
}

/* 演出信息 */
.show-info {
  display: flex;
  margin-bottom: 30rpx;
}

.show-poster {
  width: 120rpx;
  height: 160rpx;
  border-radius: 10rpx;
  margin-right: 20rpx;
}

.show-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.show-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.show-time,
.show-venue,
.ticket-info {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.ticket-info {
  color: #007aff;
  font-weight: 500;
}

/* 订单底部 */
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-total {
  flex: 1;
}

.total-label {
  font-size: 26rpx;
  color: #666;
}

.total-amount {
  font-size: 32rpx;
  color: #ff4d4f;
  font-weight: bold;
}

.order-actions {
  display: flex;
  gap: 20rpx;
}

.action-btn {
  padding: 16rpx 32rpx;
  border-radius: 40rpx;
  font-size: 26rpx;
  border: 1rpx solid #d9d9d9;
  background: white;
  color: #666;
}

.cancel-btn {
  border-color: #d9d9d9;
  color: #666;
}

.pay-btn {
  border-color: #007aff;
  background: #007aff;
  color: white;
}

.detail-btn {
  border-color: #007aff;
  color: #007aff;
}

/* 加载状态 */
.loading-more,
.no-more {
  text-align: center;
  padding: 40rpx;
  color: #999;
  font-size: 28rpx;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 200rpx 40rpx;
}

.empty-icon {
  display: block;
  font-size: 120rpx;
  margin-bottom: 40rpx;
}

.empty-text {
  display: block;
  font-size: 32rpx;
  color: #999;
  margin-bottom: 60rpx;
}

.go-shopping-btn {
  padding: 24rpx 60rpx;
  background: #007aff;
  color: white;
  border: none;
  border-radius: 50rpx;
  font-size: 28rpx;
}
</style>