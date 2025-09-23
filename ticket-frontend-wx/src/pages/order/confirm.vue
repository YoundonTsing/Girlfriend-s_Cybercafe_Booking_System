<template>
  <view class="order-confirm-container">
    <!-- 机位信息 -->
    <view class="show-info-section">
      <view class="show-card">
        <image :src="orderInfo.show.poster" class="show-poster" mode="aspectFill" />
        <view class="show-details">
          <text class="show-title">{{ getTypeDisplayName(orderInfo.show.type) }}</text>
          <text class="show-time">营业时间：10:00-24:00</text>
          <text class="show-venue">{{ orderInfo.show.venue }}</text>
        </view>
      </view>
    </view>

    <!-- 预订信息 -->
    <view class="seats-info-section">
      <view class="section-header">
        <text class="section-title">预订信息</text>
        <text class="seat-count">共{{ orderInfo.seats.length }}个机位</text>
      </view>
      <view class="seats-list">
        <view class="seat-item" v-for="seat in orderInfo.seats" :key="seat.id">
          <view class="seat-info">
            <text class="seat-position">{{ seat.rowName }}区{{ seat.number }}号机位</text>
            <text class="seat-type">{{ seat.typeName }}</text>
          </view>
          <text class="seat-price">¥{{ seat.price }}/小时</text>
        </view>
      </view>
    </view>

    <!-- 预订人信息 -->
    <view class="buyer-info-section">
      <view class="section-header">
        <text class="section-title">预订人信息</text>
        <text class="edit-btn" @tap="editBuyerInfo">编辑</text>
      </view>
      <view class="buyer-form">
        <view class="form-item">
          <text class="form-label">姓名</text>
          <input 
            type="text" 
            placeholder="请输入真实姓名" 
            v-model="buyerInfo.name"
            class="form-input"
            maxlength="20"
          />
        </view>
        <view class="form-item">
          <text class="form-label">手机号</text>
          <input 
            type="number" 
            placeholder="请输入手机号" 
            v-model="buyerInfo.phone"
            class="form-input"
            maxlength="11"
          />
        </view>
        <view class="form-item">
          <text class="form-label">身份证号</text>
          <input 
            type="text" 
            placeholder="请输入身份证号" 
            v-model="buyerInfo.idCard"
            class="form-input"
            maxlength="18"
          />
        </view>
      </view>
    </view>

    <!-- 优惠券 -->
    <view class="coupon-section">
      <view class="coupon-item" @tap="selectCoupon">
        <view class="coupon-info">
          <text class="coupon-icon">🎫</text>
          <text class="coupon-text">
            {{ selectedCoupon ? `已选择：${selectedCoupon.name}` : '选择优惠券' }}
          </text>
        </view>
        <view class="coupon-action">
          <text class="coupon-discount" v-if="selectedCoupon">-¥{{ selectedCoupon.discount }}</text>
          <text class="arrow-icon">></text>
        </view>
      </view>
    </view>

    <!-- 费用明细 -->
    <view class="cost-detail-section">
      <view class="section-header">
        <text class="section-title">费用明细</text>
      </view>
      <view class="cost-list">
        <view class="cost-item">
          <text class="cost-label">机位费用</text>
          <text class="cost-value">¥{{ ticketPrice }}</text>
        </view>
        <view class="cost-item">
          <text class="cost-label">服务费</text>
          <text class="cost-value">¥{{ servicePrice }}</text>
        </view>
        <view class="cost-item" v-if="selectedCoupon">
          <text class="cost-label">优惠券</text>
          <text class="cost-value discount">-¥{{ selectedCoupon.discount }}</text>
        </view>
        <view class="cost-item total">
          <text class="cost-label">实付金额</text>
          <text class="cost-value">¥{{ totalPrice }}</text>
        </view>
      </view>
    </view>

    <!-- 注意事项 -->
    <view class="notice-section">
      <view class="section-header">
        <text class="section-title">预订须知</text>
      </view>
      <view class="notice-content">
        <text class="notice-item">• 请确保预订信息准确无误，机位预订后不支持退换</text>
        <text class="notice-item">• 请提前15分钟到场办理入场手续</text>
        <text class="notice-item">• 机位预订最少1小时起订</text>
        <text class="notice-item">• 使用期间请遵守场馆规定，爱护设备</text>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="price-info">
        <text class="total-label">实付金额</text>
        <text class="total-amount">¥{{ totalPrice }}</text>
      </view>
      <button 
        class="pay-btn" 
        :class="{ disabled: !canPay }"
        :disabled="!canPay || loading"
        @tap="handlePay"
      >
        {{ loading ? '提交中...' : '立即支付' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 获取机位类型显示名称
const getTypeDisplayName = (type: string) => {
  const typeMap: Record<string, string> = {
    'NEWBIE': '新客电竞机位',
    'INTERMEDIATE': '中级电竞机位',
    'ADVANCED': '高级电竞机位',
    'VIP_ROOM': '包厢电竞机位',
    'SVIP': 'SVIP电竞机位'
  }
  return typeMap[type] || '电竞机位'
}

// 页面参数
const props = defineProps<{
  showId: string
  ticketTypeId: string
  seats: string
}>()

// 响应式数据
const orderInfo = ref({
  show: {
    id: '',
    title: '',
    poster: '',
    showTime: '',
    venue: ''
  },
  seats: [] as any[]
})

const buyerInfo = ref({
  name: '',
  phone: '',
  idCard: ''
})

const selectedCoupon = ref(null as any)
const loading = ref(false)
const servicePrice = ref(10) // 服务费

// 计算属性
const ticketPrice = computed(() => {
  return orderInfo.value.seats.reduce((sum, seat) => sum + seat.price, 0)
})

const totalPrice = computed(() => {
  let total = ticketPrice.value + servicePrice.value
  if (selectedCoupon.value) {
    total -= selectedCoupon.value.discount
  }
  return Math.max(total, 0)
})

const canPay = computed(() => {
  return (
    buyerInfo.value.name.trim() &&
    buyerInfo.value.phone.trim() &&
    buyerInfo.value.idCard.trim() &&
    orderInfo.value.seats.length > 0
  )
})

// 编辑购票人信息
const editBuyerInfo = () => {
  // 可以弹出编辑对话框或跳转到编辑页面
  uni.showToast({
    title: '请在下方填写信息',
    icon: 'none'
  })
}

// 选择优惠券
const selectCoupon = () => {
  uni.showActionSheet({
    itemList: ['满100减20优惠券', '满200减50优惠券', '不使用优惠券'],
    success: (res) => {
      if (res.tapIndex === 0) {
        selectedCoupon.value = {
          id: '1',
          name: '满100减20优惠券',
          discount: 20
        }
      } else if (res.tapIndex === 1) {
        selectedCoupon.value = {
          id: '2',
          name: '满200减50优惠券',
          discount: 50
        }
      } else {
        selectedCoupon.value = null
      }
    }
  })
}

// 处理支付
const handlePay = async () => {
  if (!canPay.value || loading.value) return
  
  try {
    loading.value = true
    
    // 表单验证
    if (!buyerInfo.value.name.trim()) {
      uni.showToast({
        title: '请输入购票人姓名',
        icon: 'none'
      })
      return
    }
    
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(buyerInfo.value.phone)) {
      uni.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }
    
    const idCardRegex = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/
    if (!idCardRegex.test(buyerInfo.value.idCard)) {
      uni.showToast({
        title: '请输入正确的身份证号',
        icon: 'none'
      })
      return
    }
    
    // 创建订单
    const orderData = {
      showId: props.showId,
      ticketTypeId: props.ticketTypeId,
      seats: orderInfo.value.seats.map(seat => seat.id),
      buyerInfo: buyerInfo.value,
      couponId: selectedCoupon.value?.id,
      totalPrice: totalPrice.value
    }
    
    // 这里应该调用创建订单的API
    // const response = await api.createOrder(orderData)
    
    // 模拟创建订单成功
    const orderId = 'ORDER_' + Date.now()
    
    uni.showToast({
      title: '订单创建成功',
      icon: 'success'
    })
    
    // 跳转到支付页面
    setTimeout(() => {
      uni.redirectTo({
        url: `/pages/payment/index?orderId=${orderId}&amount=${totalPrice.value}`
      })
    }, 1500)
    
  } catch (error: any) {
    console.error('创建订单失败', error)
    uni.showToast({
      title: error.message || '创建订单失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 加载订单信息
const loadOrderInfo = async () => {
  try {
    // 解析座位ID
    const seatIds = JSON.parse(props.seats || '[]')
    
    // 这里应该调用API获取演出和座位信息
    // const [showResponse, seatsResponse] = await Promise.all([
    //   api.getShowDetail(props.showId),
    //   api.getSeatDetails(seatIds)
    // ])
    
    // 模拟数据
    orderInfo.value = {
      show: {
        id: props.showId,
        title: '经典音乐会',
        poster: '/static/images/show-poster.jpg',
        showTime: '2024-03-15 19:30',
        venue: '大剧院音乐厅'
      },
      seats: seatIds.map((id: string, index: number) => ({
        id,
        rowName: String.fromCharCode(65 + Math.floor(index / 10)),
        number: (index % 10) + 1,
        typeName: '普通票',
        price: 180
      }))
    }
    
    // 如果用户已登录，自动填充购票人信息
    if (userStore.isLoggedIn && userStore.userInfo) {
      buyerInfo.value = {
        name: userStore.userInfo.name || '',
        phone: userStore.userInfo.phone || '',
        idCard: userStore.userInfo.idCard || ''
      }
    }
    
  } catch (error) {
    console.error('加载订单信息失败', error)
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
  
  if (options.showId) {
    Object.assign(props, {
      showId: options.showId,
      ticketTypeId: options.ticketTypeId || '',
      seats: options.seats || '[]'
    })
  }
  
  loadOrderInfo()
})
</script>

<style scoped>
.order-confirm-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

/* 演出信息 */
.show-info-section {
  background: white;
  margin-bottom: 20rpx;
}

.show-card {
  display: flex;
  align-items: center;
  padding: 30rpx;
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
.show-venue {
  display: block;
  font-size: 28rpx;
  color: #666;
  margin-bottom: 8rpx;
}

/* 通用区块样式 */
.seats-info-section,
.buyer-info-section,
.coupon-section,
.cost-detail-section,
.notice-section {
  background: white;
  margin-bottom: 20rpx;
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

.seat-count {
  font-size: 28rpx;
  color: #666;
}

.edit-btn {
  font-size: 28rpx;
  color: #1890ff;
}

/* 座位信息 */
.seats-list {
  border-radius: 12rpx;
  overflow: hidden;
}

.seat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.seat-item:last-child {
  border-bottom: none;
}

.seat-info {
  display: flex;
  flex-direction: column;
}

.seat-position {
  font-size: 32rpx;
  color: #333;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.seat-type {
  font-size: 26rpx;
  color: #666;
}

.seat-price {
  font-size: 32rpx;
  color: #ff4d4f;
  font-weight: bold;
}

/* 购票人信息 */
.buyer-form {
  border-radius: 12rpx;
  overflow: hidden;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.form-item:last-child {
  border-bottom: none;
}

.form-label {
  width: 160rpx;
  font-size: 32rpx;
  color: #333;
}

.form-input {
  flex: 1;
  font-size: 32rpx;
  color: #333;
}

/* 优惠券 */
.coupon-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
}

.coupon-info {
  display: flex;
  align-items: center;
}

.coupon-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.coupon-text {
  font-size: 32rpx;
  color: #333;
}

.coupon-action {
  display: flex;
  align-items: center;
}

.coupon-discount {
  font-size: 28rpx;
  color: #ff4d4f;
  margin-right: 16rpx;
}

.arrow-icon {
  font-size: 32rpx;
  color: #999;
}

/* 费用明细 */
.cost-list {
  border-radius: 12rpx;
  overflow: hidden;
}

.cost-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.cost-item:last-child {
  border-bottom: none;
}

.cost-item.total {
  border-top: 2rpx solid #f0f0f0;
  margin-top: 16rpx;
  padding-top: 24rpx;
}

.cost-label {
  font-size: 32rpx;
  color: #333;
}

.cost-item.total .cost-label {
  font-weight: bold;
}

.cost-value {
  font-size: 32rpx;
  color: #333;
}

.cost-value.discount {
  color: #52c41a;
}

.cost-item.total .cost-value {
  font-size: 36rpx;
  color: #ff4d4f;
  font-weight: bold;
}

/* 注意事项 */
.notice-content {
  background: #fafafa;
  border-radius: 12rpx;
  padding: 24rpx;
}

.notice-item {
  display: block;
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16rpx;
}

.notice-item:last-child {
  margin-bottom: 0;
}

/* 底部操作栏 */
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

.price-info {
  display: flex;
  flex-direction: column;
}

.total-label {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.total-amount {
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
}

.pay-btn.disabled {
  background: #d9d9d9;
  color: #999;
  box-shadow: none;
}
</style>