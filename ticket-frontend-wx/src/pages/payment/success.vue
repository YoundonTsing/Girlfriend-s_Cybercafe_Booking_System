<template>
  <view class="payment-success-container">
    <!-- 成功状态 -->
    <view class="success-section">
      <view class="success-icon">
        <text class="icon">✅</text>
      </view>
      <text class="success-title">支付成功</text>
      <text class="success-desc">恭喜您，订单支付成功！</text>
    </view>

    <!-- 订单信息 -->
    <view class="order-info-section">
      <view class="info-item">
        <text class="info-label">订单号</text>
        <text class="info-value">{{ orderId }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">支付金额</text>
        <text class="info-value amount">¥{{ paymentAmount }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">支付时间</text>
        <text class="info-value">{{ paymentTime }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">支付方式</text>
        <text class="info-value">{{ paymentMethod }}</text>
      </view>
    </view>

    <!-- 演出信息 -->
    <view class="show-info-section">
      <view class="section-header">
        <text class="section-title">演出信息</text>
      </view>
      <view class="show-card">
        <image :src="showInfo.poster" class="show-poster" mode="aspectFill" />
        <view class="show-details">
          <text class="show-title">{{ showInfo.title }}</text>
          <text class="show-time">{{ showInfo.showTime }}</text>
          <text class="show-venue">{{ showInfo.venue }}</text>
          <text class="seat-info">{{ showInfo.seatInfo }}</text>
        </view>
      </view>
    </view>

    <!-- 电子票信息 -->
    <view class="ticket-info-section">
      <view class="section-header">
        <text class="section-title">电子票信息</text>
      </view>
      <view class="ticket-tips">
        <view class="tip-item">
          <text class="tip-icon">📱</text>
          <text class="tip-text">电子票已发送至您的手机，请注意查收</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">🎫</text>
          <text class="tip-text">演出当天请携带有效身份证件和电子票入场</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">⏰</text>
          <text class="tip-text">建议提前30分钟到达场馆，避免错过演出</text>
        </view>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="actions-section">
      <button class="action-btn secondary" @tap="viewTicket">
        查看电子票
      </button>
      <button class="action-btn primary" @tap="viewOrderDetail">
        查看订单详情
      </button>
    </view>

    <!-- 底部导航 -->
    <view class="bottom-nav">
      <button class="nav-btn" @tap="goHome">
        <text class="nav-icon">🏠</text>
        <text class="nav-text">返回首页</text>
      </button>
      <button class="nav-btn" @tap="goOrderList">
        <text class="nav-icon">📋</text>
        <text class="nav-text">我的订单</text>
      </button>
      <button class="nav-btn" @tap="continueBooking">
        <text class="nav-icon">🎭</text>
        <text class="nav-text">继续购票</text>
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

// 页面参数
const props = defineProps<{
  orderId: string
  amount: string
}>()

// 响应式数据
const paymentAmount = ref('0.00')
const paymentTime = ref('')
const paymentMethod = ref('微信支付')

const showInfo = ref({
  title: '',
  poster: '',
  showTime: '',
  venue: '',
  seatInfo: ''
})

// 查看电子票
const viewTicket = () => {
  uni.navigateTo({
    url: `/pages/ticket/detail?orderId=${props.orderId}`
  })
}

// 查看订单详情
const viewOrderDetail = () => {
  uni.navigateTo({
    url: `/pages/order/detail?orderId=${props.orderId}`
  })
}

// 返回首页
const goHome = () => {
  uni.switchTab({
    url: '/pages/index/index'
  })
}

// 我的订单
const goOrderList = () => {
  uni.switchTab({
    url: '/pages/order/list'
  })
}

// 继续购票
const continueBooking = () => {
  uni.switchTab({
    url: '/pages/show/list'
  })
}

// 加载支付信息
const loadPaymentInfo = async () => {
  try {
    // 这里应该调用API获取支付详情
    // const response = await api.getPaymentDetail(props.orderId)
    
    // 模拟数据
    paymentAmount.value = props.amount || '0.00'
    paymentTime.value = new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
    
    showInfo.value = {
      title: '经典音乐会',
      poster: '/static/images/show-poster.jpg',
      showTime: '2024-03-15 19:30',
      venue: '大剧院音乐厅',
      seatInfo: 'A排1-3号（共3张）'
    }
    
  } catch (error) {
    console.error('加载支付信息失败', error)
    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  }
}

// 页面加载
onMounted(() => {
  // 从页面参数获取数据
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options
  
  if (options.orderId) {
    Object.assign(props, {
      orderId: options.orderId,
      amount: options.amount || '0.00'
    })
  }
  
  loadPaymentInfo()
  
  // 显示支付成功动画
  uni.showToast({
    title: '支付成功',
    icon: 'success',
    duration: 2000
  })
})
</script>

<style scoped>
.payment-success-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 40rpx 30rpx;
}

/* 成功状态 */
.success-section {
  background: white;
  border-radius: 20rpx;
  padding: 60rpx 30rpx;
  text-align: center;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.success-icon {
  margin-bottom: 30rpx;
}

.icon {
  font-size: 120rpx;
  animation: bounce 1s ease-in-out;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-20rpx);
  }
  60% {
    transform: translateY(-10rpx);
  }
}

.success-title {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #52c41a;
  margin-bottom: 16rpx;
}

.success-desc {
  display: block;
  font-size: 28rpx;
  color: #666;
}

/* 订单信息 */
.order-info-section {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 32rpx;
  color: #666;
}

.info-value {
  font-size: 32rpx;
  color: #333;
  font-weight: bold;
}

.info-value.amount {
  color: #ff4d4f;
  font-size: 36rpx;
}

/* 演出信息 */
.show-info-section {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.section-header {
  margin-bottom: 30rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.show-card {
  display: flex;
  align-items: center;
}

.show-poster {
  width: 120rpx;
  height: 160rpx;
  border-radius: 12rpx;
  margin-right: 30rpx;
}

.show-details {
  flex: 1;
}

.show-title {
  display: block;
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 16rpx;
}

.show-time,
.show-venue,
.seat-info {
  display: block;
  font-size: 28rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.seat-info {
  color: #1890ff;
  font-weight: bold;
}

/* 电子票信息 */
.ticket-info-section {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.ticket-tips {
  background: #f0f9ff;
  border-radius: 12rpx;
  padding: 24rpx;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20rpx;
}

.tip-item:last-child {
  margin-bottom: 0;
}

.tip-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
  margin-top: 4rpx;
}

.tip-text {
  flex: 1;
  font-size: 28rpx;
  color: #666;
  line-height: 1.5;
}

/* 操作按钮 */
.actions-section {
  display: flex;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.action-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: bold;
  border: none;
  transition: all 0.3s;
}

.action-btn.primary {
  background: linear-gradient(45deg, #1890ff, #0056cc);
  color: white;
  box-shadow: 0 8rpx 24rpx rgba(24, 144, 255, 0.3);
}

.action-btn.secondary {
  background: white;
  color: #1890ff;
  border: 2rpx solid #1890ff;
}

.action-btn:active {
  transform: translateY(2rpx);
}

/* 底部导航 */
.bottom-nav {
  display: flex;
  justify-content: space-around;
  background: white;
  border-radius: 20rpx;
  padding: 30rpx 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.nav-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: transparent;
  border: none;
  padding: 16rpx;
  border-radius: 12rpx;
  transition: all 0.3s;
}

.nav-btn:active {
  background: #f0f0f0;
}

.nav-icon {
  font-size: 48rpx;
  margin-bottom: 12rpx;
}

.nav-text {
  font-size: 24rpx;
  color: #666;
}
</style>