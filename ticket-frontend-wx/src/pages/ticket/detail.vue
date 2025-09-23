<template>
  <view class="ticket-detail">
    <view class="loading" v-if="loading">
      <text>加载中...</text>
    </view>
    
    <view class="ticket-info" v-else-if="ticket">
      <view class="ticket-header">
        <image class="cover" :src="ticket.coverImg" mode="aspectFill" />
        <view class="info">
          <text class="title">{{ ticket.title }}</text>
          <text class="venue">{{ ticket.venueName }}</text>
          <text class="time">{{ ticket.showTime }}</text>
          <text class="price">￥{{ ticket.minPrice }} - ￥{{ ticket.maxPrice }}</text>
        </view>
      </view>
      
      <view class="ticket-description">
        <text class="desc-title">演出介绍</text>
        <text class="desc-content">{{ ticket.description }}</text>
      </view>
      
      <view class="ticket-notice" v-if="ticket.notice">
        <text class="notice-title">购票须知</text>
        <text class="notice-content">{{ ticket.notice }}</text>
      </view>
      
      <view class="ticket-images" v-if="ticket.detailImgs && ticket.detailImgs.length">
        <text class="images-title">详情图片</text>
        <view class="images-list">
          <image 
            v-for="(img, index) in ticket.detailImgs" 
            :key="index"
            :src="img" 
            mode="widthFix" 
            class="detail-img"
            @click="previewImage(img)"
          />
        </view>
      </view>
    </view>
    
    <view class="error" v-else>
      <text>演出信息不存在</text>
    </view>
    
    <view class="bottom-bar" v-if="ticket">
      <button class="buy-btn" @click="goToBuy">立即购买</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getShowDetail } from '@/api/show'
import type { Show } from '@/types'

const ticket = ref<Show | null>(null)
const loading = ref(true)

const previewImage = (current: string) => {
  if (ticket.value?.detailImgs) {
    uni.previewImage({
      current,
      urls: ticket.value.detailImgs
    })
  }
}

const goToBuy = () => {
  if (ticket.value) {
    uni.navigateTo({
      url: `/pages/booking/index?showId=${ticket.value.id}`
    })
  }
}

const loadTicketDetail = async () => {
  try {
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    const options = currentPage.options
    const showId = options.id
    
    if (showId) {
      const response = await getShowDetail(showId)
      ticket.value = response.data
    }
  } catch (error) {
    console.error('加载演出详情失败', error)
    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTicketDetail()
})
</script>

<style scoped>
.ticket-detail {
  padding-bottom: 120rpx;
}

.loading, .error {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
}

.ticket-header {
  display: flex;
  padding: 20rpx;
  background: #fff;
  margin-bottom: 20rpx;
}

.cover {
  width: 200rpx;
  height: 280rpx;
  border-radius: 10rpx;
  margin-right: 20rpx;
}

.info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
}

.venue, .time {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.price {
  font-size: 32rpx;
  color: #ff4757;
  font-weight: bold;
}

.ticket-description, .ticket-notice {
  background: #fff;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.desc-title, .notice-title, .images-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 15rpx;
  display: block;
}

.desc-content, .notice-content {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}

.ticket-images {
  background: #fff;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.images-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.detail-img {
  width: 100%;
  border-radius: 10rpx;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx;
  border-top: 1rpx solid #f0f0f0;
  z-index: 100;
}

.buy-btn {
  width: 100%;
  height: 80rpx;
  background: #ff4757;
  color: #fff;
  border: none;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: bold;
}

.buy-btn:active {
  background: #ff3742;
}
</style>