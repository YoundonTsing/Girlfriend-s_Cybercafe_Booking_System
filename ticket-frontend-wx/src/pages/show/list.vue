<template>
  <view class="show-list-container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input">
        <input 
          type="text" 
          placeholder="搜索机位类型" 
          v-model="searchKeyword" 
          @confirm="handleSearch"
          confirm-type="search"
        />
        <text class="search-icon" @tap="handleSearch">🔍</text>
      </view>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-bar">
      <scroll-view class="filter-scroll" scroll-x="true">
        <view class="filter-item" 
          :class="{ active: currentType === '' }" 
          @tap="filterByType('')"
        >
          全部机位
        </view>
        <view class="filter-item" 
          :class="{ active: currentType === 'NEWBIE' }" 
          @tap="filterByType('NEWBIE')"
        >
          新客电竞机位
        </view>
        <view class="filter-item" 
          :class="{ active: currentType === 'INTERMEDIATE' }" 
          @tap="filterByType('INTERMEDIATE')"
        >
          中级电竞机位
        </view>
        <view class="filter-item" 
          :class="{ active: currentType === 'ADVANCED' }" 
          @tap="filterByType('ADVANCED')"
        >
          高级电竞机位
        </view>
        <view class="filter-item" 
          :class="{ active: currentType === 'VIP_ROOM' }" 
          @tap="filterByType('VIP_ROOM')"
        >
          包厢电竞机位
        </view>
        <view class="filter-item" 
          :class="{ active: currentType === 'SVIP' }" 
          @tap="filterByType('SVIP')"
        >
          SVIP电竞机位
        </view>
      </scroll-view>
    </view>

    <!-- 机位列表 -->
    <scroll-view 
      class="show-scroll" 
      scroll-y="true" 
      @scrolltolower="loadMore"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="show-list">
        <view v-for="show in showList" :key="show.id" class="show-item" @tap="goToDetail(show.id)">
          <view class="show-card">
            <view class="show-image">
              <image :src="show.posterUrl" class="image" mode="aspectFill" />
              <view class="show-status" :class="getStatusClass(show.status)">
                {{ getStatusText(show.status) }}
              </view>
            </view>
            <view class="show-info">
              <text class="show-title">{{ getTypeDisplayName(show.type) }}</text>
              <text class="show-time">营业时间：10:00-24:00</text>
              <text class="show-venue">{{ show.venue }}</text>
              <view class="show-bottom">
                <text class="show-price">¥{{ show.minPrice }}/小时 起</text>
                <text class="show-sales">剩余 {{ show.remaining || 0 }} 个机位</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      
      <view class="no-more" v-if="noMore && showList.length > 0">
        <text>没有更多了</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="showList.length === 0 && !loading">
        <text class="empty-text">暂无机位信息</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getShowList } from '@/api/show'
import { getShowPoster } from '@/utils/imageMapping'

// 响应式数据
const showList = ref([])
const searchKeyword = ref('')
const currentType = ref('') // 机位类型筛选
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const refreshing = ref(false)
const noMore = ref(false)

// 页面加载时的参数
const pageParams = ref<any>({})

// 页面加载
onLoad((options) => {
  pageParams.value = options || {}
  if (options?.type) {
    currentType.value = options.type
  }
  loadShowList(true)
})

// 加载机位列表
const loadShowList = async (reset = false) => {
  if (loading.value) return
  
  loading.value = true
  
  try {
    if (reset) {
      currentPage.value = 1
      showList.value = []
      noMore.value = false
    }
    
    const params: any = {
      page: currentPage.value,
      limit: pageSize.value
    }
    
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    
    if (currentType.value) {
      params.type = currentType.value
    }
    
    const response = await getShowList(params)
    console.log('API响应数据:', response)
    console.log('响应数据结构:', response.data)
    
    const { records, total } = response.data
    console.log('机位记录:', records)
    console.log('总数:', total)
    
    // 为每个机位添加对应的图片
    const processedRecords = records.map((show: any) => {
      const displayName = getTypeDisplayName(show.type)
      const posterUrl = getShowPoster(displayName)
      console.log(`机位 ${show.id}: type=${show.type}, displayName=${displayName}, posterUrl=${posterUrl}`)
      return {
        ...show,
        posterUrl
      }
    })
    
    console.log('处理后的记录:', processedRecords)
    
    if (reset) {
      showList.value = processedRecords
    } else {
      showList.value.push(...processedRecords)
    }
    
    // 判断是否还有更多数据
    noMore.value = showList.value.length >= total
    
  } catch (error) {
    console.error('获取机位列表失败', error)
    uni.showToast({
      title: '获取机位列表失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// 搜索
const handleSearch = () => {
  loadShowList(true)
}

// 按机位类型筛选
const filterByType = (type: string) => {
  currentType.value = type
  loadShowList(true)
}

// 下拉刷新
const onRefresh = () => {
  refreshing.value = true
  loadShowList(true)
}

// 加载更多
const loadMore = () => {
  if (!loading.value && !noMore.value) {
    currentPage.value++
    loadShowList()
  }
}

// 跳转到机位详情
const goToDetail = (id: number) => {
  uni.navigateTo({
    url: `/pages/show/detail?id=${id}`
  })
}

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
</script>

<style scoped>
.show-list-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8f8f8;
}

/* 搜索栏 */
.search-bar {
  padding: 20rpx;
  background-color: white;
  border-bottom: 1rpx solid #eee;
}

.search-input {
  position: relative;
  background-color: #f5f5f5;
  border-radius: 50rpx;
  padding: 20rpx 60rpx 20rpx 30rpx;
}

.search-input input {
  width: 100%;
  font-size: 28rpx;
  background: transparent;
}

.search-icon {
  position: absolute;
  right: 30rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 32rpx;
  color: #999;
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

/* 演出列表 */
.show-scroll {
  flex: 1;
  padding: 20rpx;
}

.show-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.show-item {
  background: white;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
}

.show-card {
  width: 100%;
}

.show-image {
  position: relative;
  width: 100%;
  height: 240rpx;
}

.image {
  width: 100%;
  height: 100%;
}

.show-status {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
  color: white;
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

.show-info {
  padding: 20rpx;
}

.show-title {
  display: block;
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.show-time,
.show-venue {
  display: block;
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.show-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10rpx;
}

.show-price {
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: bold;
}

.show-sales {
  font-size: 22rpx;
  color: #999;
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

.empty-text {
  font-size: 32rpx;
  color: #999;
}
</style>