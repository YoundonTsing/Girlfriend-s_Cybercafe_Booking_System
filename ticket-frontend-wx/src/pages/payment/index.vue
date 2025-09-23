<template>
  <view class="payment-container">
    <!-- 订单信息 -->
    <view class="order-info-section">
      <view class="order-header">
        <text class="order-title">订单支付</text>
        <text class="order-number">订单号：{{ orderId }}</text>
      </view>
      <view class="amount-info">
        <text class="amount-label">支付金额</text>
        <text class="amount-value">¥{{ paymentAmount }}</text>
      </view>
    </view>

    <!-- 支付方式 -->
    <view class="payment-methods-section">
      <view class="section-header">
        <text class="section-title">选择支付方式</text>
      </view>
      <view class="payment-methods">
        <view 
          class="payment-method" 
          :class="{ active: selectedMethod === 'wechat' }"
          @tap="selectPaymentMethod('wechat')"
        >
          <view class="method-info">
            <view class="method-icon wechat">💬</view>
            <view class="method-details">
              <text class="method-name">微信支付</text>
              <text class="method-desc">推荐使用微信支付</text>
            </view>
          </view>
          <view class="method-radio" :class="{ checked: selectedMethod === 'wechat' }">
            <text v-if="selectedMethod === 'wechat'" class="radio-dot">●</text>
          </view>
        </view>

        <view 
          class="payment-method" 
          :class="{ active: selectedMethod === 'alipay' }"
          @tap="selectPaymentMethod('alipay')"
        >
          <view class="method-info">
            <view class="method-icon alipay">💰</view>
            <view class="method-details">
              <text class="method-name">支付宝</text>
              <text class="method-desc">安全便捷的支付方式</text>
            </view>
          </view>
          <view class="method-radio" :class="{ checked: selectedMethod === 'alipay' }">
            <text v-if="selectedMethod === 'alipay'" class="radio-dot">●</text>
          </view>
        </view>

        <view 
          class="payment-method" 
          :class="{ active: selectedMethod === 'balance' }"
          @tap="selectPaymentMethod('balance')"
        >
          <view class="method-info">
            <view class="method-icon balance">💳</view>
            <view class="method-details">
              <text class="method-name">余额支付</text>
              <text class="method-desc">可用余额：¥{{ userBalance }}</text>
            </view>
          </view>
          <view class="method-radio" :class="{ checked: selectedMethod === 'balance' }">
            <text v-if="selectedMethod === 'balance'" class="radio-dot">●</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 支付安全提示 -->
    <view class="security-tips-section">
      <view class="section-header">
        <text class="section-title">支付安全提示</text>
      </view>
      <view class="tips-content">
        <view class="tip-item">
          <text class="tip-icon">🔒</text>
          <text class="tip-text">支付过程采用SSL加密，保障您的资金安全</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">⏰</text>
          <text class="tip-text">订单将在{{ countdown }}分钟后自动取消，请及时支付</text>
        </view>
        <view class="tip-item">
          <text class="tip-icon">📱</text>
          <text class="tip-text">支付完成后，电子票将发送至您的手机</text>
        </view>
      </view>
    </view>

    <!-- 订单详情 -->
    <view class="order-details-section">
      <view class="section-header" @tap="toggleOrderDetails">
        <text class="section-title">订单详情</text>
        <text class="toggle-icon" :class="{ expanded: showOrderDetails }">{{ showOrderDetails ? '▲' : '▼' }}</text>
      </view>
      <view class="order-details" v-if="showOrderDetails">
        <view class="detail-item">
          <text class="detail-label">演出名称</text>
          <text class="detail-value">{{ orderDetails.showTitle }}</text>
        </view>
        <view class="detail-item">
          <text class="detail-label">演出时间</text>
          <text class="detail-value">{{ orderDetails.showTime }}</text>
        </view>
        <view class="detail-item">
          <text class="detail-label">演出场馆</text>
          <text class="detail-value">{{ orderDetails.venue }}</text>
        </view>
        <view class="detail-item">
          <text class="detail-label">座位信息</text>
          <text class="detail-value">{{ orderDetails.seatInfo }}</text>
        </view>
        <view class="detail-item">
          <text class="detail-label">购票人</text>
          <text class="detail-value">{{ orderDetails.buyerName }}</text>
        </view>
        <view class="detail-item">
          <text class="detail-label">手机号</text>
          <text class="detail-value">{{ orderDetails.buyerPhone }}</text>
        </view>
      </view>
    </view>

    <!-- 底部支付按钮 -->
    <view class="bottom-bar">
      <view class="payment-info">
        <text class="payment-label">支付金额</text>
        <text class="payment-amount">¥{{ paymentAmount }}</text>
      </view>
      <button 
        class="pay-btn" 
        :class="{ disabled: !selectedMethod || loading }"
        :disabled="!selectedMethod || loading"
        @tap="handlePayment"
      >
        {{ loading ? '支付中...' : `立即支付 ¥${paymentAmount}` }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 页面参数
const props = defineProps<{
  orderId: string
  amount: string
}>()

// 响应式数据
const selectedMethod = ref('wechat')
const loading = ref(false)
const showOrderDetails = ref(false)
const countdown = ref(15) // 15分钟倒计时
const userBalance = ref(0)

const orderDetails = ref({
  showTitle: '',
  showTime: '',
  venue: '',
  seatInfo: '',
  buyerName: '',
  buyerPhone: ''
})

let countdownTimer: any = null

// 计算属性
const paymentAmount = computed(() => {
  return parseFloat(props.amount || '0').toFixed(2)
})

// 选择支付方式
const selectPaymentMethod = (method: string) => {
  selectedMethod.value = method
}

// 切换订单详情显示
const toggleOrderDetails = () => {
  showOrderDetails.value = !showOrderDetails.value
}

// 处理支付
const handlePayment = async () => {
  if (!selectedMethod.value || loading.value) return
  
  try {
    loading.value = true
    
    // 余额支付验证
    if (selectedMethod.value === 'balance') {
      if (userBalance.value < parseFloat(paymentAmount.value)) {
        uni.showToast({
          title: '余额不足，请选择其他支付方式',
          icon: 'none'
        })
        return
      }
    }
    
    // 根据支付方式调用不同的支付接口
    let paymentResult
    
    switch (selectedMethod.value) {
      case 'wechat':
        paymentResult = await handleWechatPay()
        break
      case 'alipay':
        paymentResult = await handleAlipay()
        break
      case 'balance':
        paymentResult = await handleBalancePay()
        break
      default:
        throw new Error('请选择支付方式')
    }
    
    if (paymentResult.success) {
      uni.showToast({
        title: '支付成功',
        icon: 'success'
      })
      
      // 跳转到支付成功页面
      setTimeout(() => {
        uni.redirectTo({
          url: `/pages/payment/success?orderId=${props.orderId}&amount=${paymentAmount.value}`
        })
      }, 1500)
    }
    
  } catch (error: any) {
    console.error('支付失败', error)
    uni.showToast({
      title: error.message || '支付失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 微信支付
const handleWechatPay = async () => {
  return new Promise((resolve, reject) => {
    // 这里应该调用微信支付API
    // 在小程序中使用 uni.requestPayment
    uni.requestPayment({
      provider: 'wxpay',
      timeStamp: String(Date.now()),
      nonceStr: 'random_string',
      package: 'prepay_id=wx_prepay_id',
      signType: 'MD5',
      paySign: 'payment_signature',
      success: (res) => {
        resolve({ success: true, data: res })
      },
      fail: (err) => {
        reject(new Error('微信支付失败'))
      }
    })
  })
}

// 支付宝支付
const handleAlipay = async () => {
  return new Promise((resolve, reject) => {
    // 这里应该调用支付宝支付API
    uni.requestPayment({
      provider: 'alipay',
      orderInfo: 'alipay_order_info',
      success: (res) => {
        resolve({ success: true, data: res })
      },
      fail: (err) => {
        reject(new Error('支付宝支付失败'))
      }
    })
  })
}

// 余额支付
const handleBalancePay = async () => {
  // 这里应该调用余额支付API
  // const response = await api.balancePay({ orderId: props.orderId, amount: paymentAmount.value })
  
  // 模拟余额支付
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ success: true })
    }, 1000)
  })
}

// 加载订单详情
const loadOrderDetails = async () => {
  try {
    // 这里应该调用API获取订单详情
    // const response = await api.getOrderDetail(props.orderId)
    
    // 模拟数据
    orderDetails.value = {
      showTitle: '经典音乐会',
      showTime: '2024-03-15 19:30',
      venue: '大剧院音乐厅',
      seatInfo: 'A排1-3号',
      buyerName: '张三',
      buyerPhone: '138****8888'
    }
    
  } catch (error) {
    console.error('加载订单详情失败', error)
  }
}

// 加载用户余额
const loadUserBalance = async () => {
  try {
    if (userStore.isLoggedIn) {
      // 这里应该调用API获取用户余额
      // const response = await api.getUserBalance()
      
      // 模拟数据
      userBalance.value = 500.00
    }
  } catch (error) {
    console.error('加载用户余额失败', error)
  }
}

// 开始倒计时
const startCountdown = () => {
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      // 订单超时，返回订单列表
      uni.showModal({
        title: '订单已超时',
        content: '订单支付时间已超时，订单已自动取消',
        showCancel: false,
        success: () => {
          uni.redirectTo({
            url: '/pages/order/list'
          })
        }
      })
    }
  }, 60000) // 每分钟更新一次
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
      amount: options.amount || '0'
    })
  }
  
  loadOrderDetails()
  loadUserBalance()
  startCountdown()
})

// 页面卸载时清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.payment-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

/* 订单信息 */
.order-info-section {
  background: linear-gradient(135deg, #1890ff, #0056cc);
  padding: 40rpx 30rpx;
  color: white;
}

.order-header {
  margin-bottom: 30rpx;
}

.order-title {
  display: block;
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 16rpx;
}

.order-number {
  display: block;
  font-size: 26rpx;
  opacity: 0.8;
}

.amount-info {
  text-align: center;
}

.amount-label {
  display: block;
  font-size: 28rpx;
  opacity: 0.8;
  margin-bottom: 16rpx;
}

.amount-value {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
}

/* 通用区块样式 */
.payment-methods-section,
.security-tips-section,
.order-details-section {
  background: white;
  margin: 20rpx 0;
  padding: 30rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 30rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.toggle-icon {
  font-size: 24rpx;
  color: #999;
  transition: transform 0.3s;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

/* 支付方式 */
.payment-methods {
  border-radius: 12rpx;
  overflow: hidden;
}

.payment-method {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
  transition: background-color 0.3s;
}

.payment-method:last-child {
  border-bottom: none;
}

.payment-method.active {
  background: #f0f9ff;
}

.method-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.method-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  margin-right: 24rpx;
}

.method-icon.wechat {
  background: #07c160;
}

.method-icon.alipay {
  background: #1677ff;
}

.method-icon.balance {
  background: #722ed1;
}

.method-details {
  display: flex;
  flex-direction: column;
}

.method-name {
  font-size: 32rpx;
  color: #333;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.method-desc {
  font-size: 26rpx;
  color: #666;
}

.method-radio {
  width: 40rpx;
  height: 40rpx;
  border: 2rpx solid #d9d9d9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.method-radio.checked {
  border-color: #1890ff;
  background: #1890ff;
}

.radio-dot {
  color: white;
  font-size: 24rpx;
}

/* 安全提示 */
.tips-content {
  background: #fafafa;
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

/* 订单详情 */
.order-details {
  border-radius: 12rpx;
  overflow: hidden;
}

.detail-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 28rpx;
  color: #666;
  width: 160rpx;
}

.detail-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
  text-align: right;
}

/* 底部支付按钮 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1rpx solid #eee;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.payment-info {
  display: flex;
  flex-direction: column;
}

.payment-label {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.payment-amount {
  font-size: 36rpx;
  font-weight: bold;
  color: #ff4d4f;
}

.pay-btn {
  background: linear-gradient(45deg, #1890ff, #0056cc);
  color: white;
  border: none;
  border-radius: 50rpx;
  padding: 24rpx 60rpx;
  font-size: 32rpx;
  font-weight: bold;
  box-shadow: 0 8rpx 24rpx rgba(24, 144, 255, 0.3);
  transition: all 0.3s;
}

.pay-btn:active {
  transform: translateY(2rpx);
  box-shadow: 0 4rpx 12rpx rgba(24, 144, 255, 0.3);
}

.pay-btn.disabled {
  background: #d9d9d9;
  color: #999;
  box-shadow: none;
}
</style>