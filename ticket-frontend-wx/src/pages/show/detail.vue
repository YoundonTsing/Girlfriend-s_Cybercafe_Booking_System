<template>
  <view class="show-detail-container">
    <scroll-view class="detail-scroll" scroll-y="true" v-if="showDetail">
      <!-- 机位图片 -->
      <view class="poster-section">
        <image :src="showDetail.posterUrl" class="poster-image" mode="aspectFill" />
        <view class="poster-overlay">
          <view class="show-status" :class="getStatusClass(showDetail.status)">
            {{ getStatusText(showDetail.status) }}
          </view>
        </view>
      </view>

      <!-- 机位信息 -->
      <view class="info-section">
        <view class="show-header">
          <text class="show-title">{{ getTypeDisplayName(showDetail.type) }}</text>
          <text class="show-subtitle">{{ showDetail.subtitle || '高品质电竞体验' }}</text>
        </view>
        
        <view class="info-grid">
          <view class="info-item">
            <text class="info-label">营业时间</text>
            <text class="info-value">10:00-24:00</text>
          </view>
          <view class="info-item">
            <text class="info-label">场馆位置</text>
            <text class="info-value">{{ showDetail.venue || showDetail.venueName }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">最短时长</text>
            <text class="info-value">1小时起订</text>
          </view>
          <view class="info-item">
            <text class="info-label">价格范围</text>
            <text class="info-value price">¥{{ showDetail.minPrice }}/小时 - ¥{{ showDetail.maxPrice }}/小时</text>
          </view>
        </view>
      </view>

      <!-- 机位配置 -->
      <view class="ticket-section" v-if="ticketTypes.length > 0">
        <view class="section-title">
          <text>机位配置</text>
        </view>
        <view class="ticket-list">
          <view v-for="ticket in ticketTypes" :key="ticket.id" class="ticket-item">
            <view class="ticket-info">
              <text class="ticket-name">{{ ticket.typeName }}</text>
              <text class="ticket-area">{{ ticket.areaName }}</text>
            </view>
            <view class="ticket-right">
              <text class="ticket-price">¥{{ ticket.price }}/小时</text>
              <text class="ticket-stock" :class="{ 'sold-out': ticket.remainingCount === 0 }">
                {{ ticket.remainingCount > 0 ? `剩余${ticket.remainingCount}个` : '已满' }}
              </text>
            </view>
          </view>
        </view>
      </view>

      <!-- 机位详情 -->
      <view class="description-section">
        <view class="section-title">
          <text>机位详情</text>
        </view>
        <view class="description-content">
          <text class="description-text">{{ showDetail.description || '高配置电竞机位，提供专业游戏体验' }}</text>
        </view>
      </view>

      <!-- 预订须知 -->
      <view class="notice-section">
        <view class="section-title">
          <text>预订须知</text>
        </view>
        <view class="notice-content">
          <text class="notice-text">1. 机位预订最少1小时起订</text>
          <text class="notice-text">2. 请提前15分钟到场办理入场手续</text>
          <text class="notice-text">3. 机位预订后不支持退款</text>
          <text class="notice-text">4. 请妥善保管好您的预订凭证</text>
        </view>
      </view>

      <!-- 底部占位 -->
      <view class="bottom-placeholder"></view>
    </scroll-view>

    <!-- 加载状态 -->
    <view class="loading-state" v-if="loading">
      <text>加载中...</text>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar" v-if="showDetail">
      <view class="price-info">
        <text class="price-label">价格</text>
        <text class="price-value">¥{{ showDetail.minPrice }}/小时 起</text>
      </view>
      <view class="action-buttons">
        <button 
          class="buy-button" 
          :class="{ disabled: showDetail.status !== 'ON_SALE' }"
          @tap="goToSeatSelection"
          :disabled="showDetail.status !== 'ON_SALE'"
        >
          {{ getButtonText(showDetail.status) }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getShowDetail } from '@/api/show'

// 响应式数据
const showDetail = ref<any>(null)
const ticketTypes = ref<any[]>([])
const loading = ref(true)
const showId = ref<number>(0)

// 页面加载
onLoad(async (options) => {
  if (options?.id) {
    showId.value = parseInt(options.id)
    await loadShowDetail()
    await loadTicketTypes()
  }
})

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

// 加载机位详情
const loadShowDetail = async () => {
  try {
    loading.value = true
    const response = await getShowDetail(showId.value)
    showDetail.value = response.data
  } catch (error) {
    console.error('获取机位详情失败', error)
    uni.showToast({
      title: '获取机位详情失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 加载机位配置信息
const loadTicketTypes = async () => {
  try {
    // 暂时使用空数组，后续可以通过机位详情获取配置信息
    ticketTypes.value = []
  } catch (error) {
    console.error('获取机位配置失败', error)
  }
}

// 跳转到机位预订
const goToSeatSelection = () => {
  if (showDetail.value?.status !== 'ON_SALE') {
    return
  }
  
  uni.navigateTo({
    url: `/pages/seat/selection?showId=${showId.value}`
  })
}

// 获取状态样式类
const getStatusClass = (status: string) => {
  const statusMap: Record<string, string> = {
    'ON_SALE': 'status-available',
    'SOLD_OUT': 'status-full',
    'UPCOMING': 'status-upcoming',
    'ENDED': 'status-closed'
  }
  return statusMap[status] || ''
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'ON_SALE': '可预订',
    'SOLD_OUT': '已满',
    'UPCOMING': '即将开放',
    'ENDED': '已关闭'
  }
  return statusMap[status] || ''
}

// 获取按钮文本
const getButtonText = (status: string) => {
  const buttonMap: Record<string, string> = {
    'ON_SALE': '立即预订',
    'SOLD_OUT': '机位已满',
    'UPCOMING': '即将开放',
    'ENDED': '已关闭'
  }
  return buttonMap[status] || '立即预订'
}
</script>

<style scoped>
.show-detail-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8f8f8;
}

.detail-scroll {
  flex: 1;
}

/* 海报区域 */
.poster-section {
  position: relative;
  width: 100%;
  height: 500rpx;
}

.poster-image {
  width: 100%;
  height: 100%;
}

.poster-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(transparent 60%, rgba(0, 0, 0, 0.3));
}

.show-status {
  position: absolute;
  top: 40rpx;
  right: 40rpx;
  padding: 12rpx 24rpx;
  border-radius: 30rpx;
  font-size: 24rpx;
  color: white;
  font-weight: bold;
}

.status-on-sale {
  background-color: #52c41a;
}

.status-sold-out {
  background-color: #ff4d4f;
}

.status-upcoming {
  background-color: #1890ff;
}

.status-ended {
  background-color: #d9d9d9;
  color: #666;
}

/* 信息区域 */
.info-section {
  background: white;
  padding: 40rpx;
  margin-bottom: 20rpx;
}

.show-header {
  margin-bottom: 40rpx;
}

.show-title {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 16rpx;
  line-height: 1.4;
}

.show-subtitle {
  display: block;
  font-size: 28rpx;
  color: #666;
  line-height: 1.4;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30rpx;
}

.info-item {
  display: flex;
  flex-direction: column;
}

.info-label {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.info-value {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.info-value.price {
  color: #ff4d4f;
  font-weight: bold;
}

/* 票档区域 */
.ticket-section {
  background: white;
  margin-bottom: 20rpx;
}

.section-title {
  padding: 40rpx 40rpx 20rpx;
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  border-bottom: 1rpx solid #f0f0f0;
}

.ticket-list {
  padding: 0 40rpx 20rpx;
}

.ticket-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.ticket-item:last-child {
  border-bottom: none;
}

.ticket-info {
  flex: 1;
}

.ticket-name {
  display: block;
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 8rpx;
}

.ticket-area {
  display: block;
  font-size: 24rpx;
  color: #666;
}

.ticket-right {
  text-align: right;
}

.ticket-price {
  display: block;
  font-size: 32rpx;
  color: #ff4d4f;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.ticket-stock {
  display: block;
  font-size: 24rpx;
  color: #52c41a;
}

.ticket-stock.sold-out {
  color: #ff4d4f;
}

/* 详情区域 */
.description-section,
.notice-section {
  background: white;
  margin-bottom: 20rpx;
}

.description-content,
.notice-content {
  padding: 0 40rpx 40rpx;
}

.description-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
}

.notice-text {
  display: block;
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16rpx;
}

.notice-text:last-child {
  margin-bottom: 0;
}

/* 底部占位 */
.bottom-placeholder {
  height: 120rpx;
}

/* 加载状态 */
.loading-state {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 28rpx;
  color: #999;
}

/* 底部操作栏 */
.bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 40rpx;
  background: white;
  border-top: 1rpx solid #eee;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.price-info {
  flex: 1;
}

.price-label {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 4rpx;
}

.price-value {
  display: block;
  font-size: 32rpx;
  color: #ff4d4f;
  font-weight: bold;
}

.action-buttons {
  margin-left: 40rpx;
}

.buy-button {
  padding: 24rpx 60rpx;
  background: linear-gradient(45deg, #007aff, #0056cc);
  color: white;
  border: none;
  border-radius: 50rpx;
  font-size: 32rpx;
  font-weight: bold;
}

.buy-button.disabled {
  background: #d9d9d9;
  color: #999;
}
</style>