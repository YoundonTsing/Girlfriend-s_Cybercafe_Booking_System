<template>
  <view class="profile-container">
    <scroll-view class="profile-scroll" scroll-y="true">
      <!-- 用户信息卡片 -->
      <view class="user-card">
        <view class="user-info">
          <image 
            :src="userInfo.avatar || '/static/images/default-avatar.png'" 
            class="user-avatar" 
            mode="aspectFill"
          />
          <view class="user-details">
            <text class="user-name">{{ userInfo.username || '未登录' }}</text>
            <text class="user-phone">{{ userInfo.phone || '未绑定手机号' }}</text>
          </view>
          <view class="user-actions">
            <text class="edit-btn" @tap="editProfile" v-if="isLoggedIn">编辑</text>
            <text class="login-btn" @tap="goToLogin" v-else>登录</text>
          </view>
        </view>
      </view>

      <!-- 统计信息 -->
      <view class="stats-section" v-if="isLoggedIn">
        <view class="stats-grid">
          <view class="stats-item" @tap="goToOrderList">
            <text class="stats-number">{{ userStats.totalOrders || 0 }}</text>
            <text class="stats-label">总订单</text>
          </view>
          <view class="stats-item" @tap="goToOrderList('PAID')">
            <text class="stats-number">{{ userStats.paidOrders || 0 }}</text>
            <text class="stats-label">已支付</text>
          </view>
          <view class="stats-item" @tap="goToOrderList('PENDING')">
            <text class="stats-number">{{ userStats.pendingOrders || 0 }}</text>
            <text class="stats-label">待支付</text>
          </view>
        </view>
      </view>

      <!-- 快捷订单 -->
      <view class="quick-orders-section" v-if="isLoggedIn">
        <view class="section-header">
          <text class="section-title">我的订单</text>
          <text class="section-more" @tap="goToOrderList">查看全部</text>
        </view>
        <view class="order-types">
          <view class="order-type-item" @tap="goToOrderList('PENDING')">
            <view class="order-icon pending-icon">💰</view>
            <text class="order-type-text">待支付</text>
            <view class="order-badge" v-if="userStats.pendingOrders > 0">
              {{ userStats.pendingOrders }}
            </view>
          </view>
          <view class="order-type-item" @tap="goToOrderList('PAID')">
            <view class="order-icon paid-icon">✅</view>
            <text class="order-type-text">已支付</text>
          </view>
          <view class="order-type-item" @tap="goToOrderList('CANCELLED')">
            <view class="order-icon cancelled-icon">❌</view>
            <text class="order-type-text">已取消</text>
          </view>
          <view class="order-type-item" @tap="goToOrderList('REFUNDED')">
            <view class="order-icon refunded-icon">🔄</view>
            <text class="order-type-text">已退款</text>
          </view>
        </view>
      </view>

      <!-- 功能菜单 -->
      <view class="menu-section">
        <view class="menu-group">
          <view class="menu-item" @tap="goToFavorites" v-if="isLoggedIn">
            <view class="menu-icon">❤️</view>
            <text class="menu-text">我的收藏</text>
            <text class="menu-arrow">></text>
          </view>
          <view class="menu-item" @tap="goToSettings">
            <view class="menu-icon">⚙️</view>
            <text class="menu-text">设置</text>
            <text class="menu-arrow">></text>
          </view>
          <view class="menu-item" @tap="goToHelp">
            <view class="menu-icon">❓</view>
            <text class="menu-text">帮助与反馈</text>
            <text class="menu-arrow">></text>
          </view>
          <view class="menu-item" @tap="goToAbout">
            <view class="menu-icon">ℹ️</view>
            <text class="menu-text">关于我们</text>
            <text class="menu-arrow">></text>
          </view>
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="logout-section" v-if="isLoggedIn">
        <button class="logout-btn" @tap="handleLogout">
          退出登录
        </button>
      </view>

      <!-- 底部占位 -->
      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getOrderList } from '@/api/order'

const userStore = useUserStore()

// 响应式数据
const userStats = ref({
  totalOrders: 0,
  paidOrders: 0,
  pendingOrders: 0,
  cancelledOrders: 0
})

// 计算属性
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

// 页面挂载
onMounted(() => {
  if (isLoggedIn.value) {
    loadUserStats()
  }
})

// 加载用户统计信息
const loadUserStats = async () => {
  try {
    // 获取用户所有订单来计算统计信息
    const response = await getOrderList({ page: 1, size: 1000 })
    const orders = response.data.records || []
    
    // 计算统计信息
    userStats.value = {
      totalOrders: orders.length,
      paidOrders: orders.filter(order => order.status === 2).length, // 假设状态2是已支付
      pendingOrders: orders.filter(order => order.status === 1).length, // 假设状态1是待支付
      cancelledOrders: orders.filter(order => order.status === 3).length // 假设状态3是已取消
    }
  } catch (error) {
    console.error('获取用户统计失败', error)
    // 如果获取失败，设置默认值
    userStats.value = {
      totalOrders: 0,
      paidOrders: 0,
      pendingOrders: 0,
      cancelledOrders: 0
    }
  }
}

// 编辑个人资料
const editProfile = () => {
  uni.navigateTo({
    url: '/pages/user/edit'
  })
}

// 跳转到登录页
const goToLogin = () => {
  uni.navigateTo({
    url: '/pages/login/index'
  })
}

// 跳转到订单列表
const goToOrderList = (status?: string) => {
  const url = status ? `/pages/order/list?status=${status}` : '/pages/order/list'
  uni.switchTab({
    url: '/pages/order/list'
  })
}

// 跳转到收藏页面
const goToFavorites = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 跳转到设置页面
const goToSettings = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 跳转到帮助页面
const goToHelp = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 跳转到关于页面
const goToAbout = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 退出登录
const handleLogout = async () => {
  try {
    const result = await uni.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      confirmText: '确定',
      cancelText: '取消'
    })
    
    if (result.confirm) {
      await userStore.logout()
      uni.showToast({
        title: '已退出登录',
        icon: 'success'
      })
      
      // 跳转到首页
      setTimeout(() => {
        uni.switchTab({
          url: '/pages/index/index'
        })
      }, 1500)
    }
  } catch (error) {
    console.error('退出登录失败', error)
    uni.showToast({
      title: '退出登录失败',
      icon: 'none'
    })
  }
}
</script>

<style scoped>
.profile-container {
  height: 100vh;
  background-color: #f8f8f8;
}

.profile-scroll {
  height: 100%;
}

/* 用户信息卡片 */
.user-card {
  background: linear-gradient(135deg, #007aff, #0056cc);
  padding: 60rpx 40rpx 40rpx;
  margin-bottom: 20rpx;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  margin-right: 30rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
}

.user-details {
  flex: 1;
}

.user-name {
  display: block;
  font-size: 36rpx;
  font-weight: bold;
  color: white;
  margin-bottom: 10rpx;
}

.user-phone {
  display: block;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.user-actions {
  margin-left: 20rpx;
}

.edit-btn,
.login-btn {
  padding: 16rpx 32rpx;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border-radius: 40rpx;
  font-size: 26rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.3);
}

/* 统计信息 */
.stats-section {
  background: white;
  margin-bottom: 20rpx;
  padding: 40rpx;
}

.stats-grid {
  display: flex;
  justify-content: space-around;
}

.stats-item {
  text-align: center;
}

.stats-number {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #007aff;
  margin-bottom: 10rpx;
}

.stats-label {
  display: block;
  font-size: 26rpx;
  color: #666;
}

/* 快捷订单 */
.quick-orders-section {
  background: white;
  margin-bottom: 20rpx;
  padding: 40rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.section-more {
  font-size: 26rpx;
  color: #007aff;
}

.order-types {
  display: flex;
  justify-content: space-around;
}

.order-type-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx;
}

.order-icon {
  font-size: 48rpx;
  margin-bottom: 16rpx;
}

.order-type-text {
  font-size: 24rpx;
  color: #666;
}

.order-badge {
  position: absolute;
  top: 10rpx;
  right: 10rpx;
  background: #ff4d4f;
  color: white;
  font-size: 20rpx;
  padding: 4rpx 8rpx;
  border-radius: 20rpx;
  min-width: 32rpx;
  text-align: center;
}

/* 功能菜单 */
.menu-section {
  margin-bottom: 20rpx;
}

.menu-group {
  background: white;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 40rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 30rpx;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.menu-arrow {
  font-size: 28rpx;
  color: #ccc;
}

/* 退出登录 */
.logout-section {
  padding: 40rpx;
  margin-bottom: 20rpx;
}

.logout-btn {
  width: 100%;
  padding: 32rpx;
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 20rpx;
  font-size: 32rpx;
  font-weight: bold;
}

/* 底部占位 */
.bottom-placeholder {
  height: 120rpx;
}
</style>