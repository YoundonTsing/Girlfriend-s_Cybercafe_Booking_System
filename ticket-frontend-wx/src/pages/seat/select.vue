<template>
  <view class="seat-select-container">
    <!-- 头部信息 -->
    <view class="header-section">
      <view class="show-info">
        <image :src="showInfo.poster" class="show-poster" mode="aspectFill" />
        <view class="show-details">
          <text class="show-title">{{ showInfo.title }}</text>
          <text class="show-time">{{ showInfo.showTime }}</text>
          <text class="show-venue">{{ showInfo.venue }}</text>
        </view>
      </view>
    </view>

    <!-- 座位图区域 -->
    <view class="seat-map-section">
      <!-- 舞台/屏幕 -->
      <view class="stage">
        <text class="stage-text">舞台/屏幕</text>
      </view>

      <!-- 座位网格 -->
      <scroll-view 
        class="seat-grid-container" 
        scroll-x="true" 
        scroll-y="true"
        :scroll-left="scrollLeft"
        :scroll-top="scrollTop"
      >
        <view class="seat-grid" :style="{ width: gridWidth + 'rpx', height: gridHeight + 'rpx' }">
          <!-- 行号标识 -->
          <view class="row-labels">
            <view 
              v-for="row in seatMap.rows" 
              :key="row.id"
              class="row-label"
              :style="{ top: (row.index * seatSize + row.index * seatGap) + 'rpx' }"
            >
              {{ row.name }}
            </view>
          </view>

          <!-- 列号标识 -->
          <view class="col-labels">
            <view 
              v-for="col in seatMap.cols" 
              :key="col.id"
              class="col-label"
              :style="{ left: (col.index * seatSize + col.index * seatGap + 60) + 'rpx' }"
            >
              {{ col.name }}
            </view>
          </view>

          <!-- 座位 -->
          <view class="seats-container">
            <view 
              v-for="seat in seatMap.seats" 
              :key="seat.id"
              class="seat"
              :class="getSeatClass(seat)"
              :style="getSeatStyle(seat)"
              @tap="selectSeat(seat)"
            >
              <text class="seat-number">{{ seat.number }}</text>
            </view>
          </view>
        </view>
      </scroll-view>

      <!-- 缩放控制 -->
      <view class="zoom-controls">
        <button class="zoom-btn" @tap="zoomOut">-</button>
        <text class="zoom-text">{{ Math.round(zoomLevel * 100) }}%</text>
        <button class="zoom-btn" @tap="zoomIn">+</button>
      </view>
    </view>

    <!-- 座位图例 -->
    <view class="legend-section">
      <view class="legend-item">
        <view class="legend-seat available"></view>
        <text class="legend-text">可选</text>
      </view>
      <view class="legend-item">
        <view class="legend-seat selected"></view>
        <text class="legend-text">已选</text>
      </view>
      <view class="legend-item">
        <view class="legend-seat occupied"></view>
        <text class="legend-text">已售</text>
      </view>
      <view class="legend-item">
        <view class="legend-seat disabled"></view>
        <text class="legend-text">不可选</text>
      </view>
    </view>

    <!-- 已选座位信息 -->
    <view class="selected-seats-section" v-if="selectedSeats.length > 0">
      <view class="section-title">
        <text>已选座位 ({{ selectedSeats.length }})</text>
      </view>
      <scroll-view class="selected-seats-list" scroll-x="true">
        <view class="selected-seat-item" v-for="seat in selectedSeats" :key="seat.id">
          <text class="seat-info">{{ seat.rowName }}排{{ seat.number }}号</text>
          <text class="seat-price">¥{{ seat.price }}</text>
          <view class="remove-btn" @tap="removeSeat(seat)">
            <text class="remove-icon">×</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="price-info">
        <text class="total-seats">共{{ selectedSeats.length }}张</text>
        <text class="total-price">¥{{ totalPrice }}</text>
      </view>
      <button 
        class="confirm-btn" 
        :class="{ disabled: selectedSeats.length === 0 }"
        :disabled="selectedSeats.length === 0"
        @tap="confirmSelection"
      >
        确认选座
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getShowPoster } from '@/utils/imageMapping'

// 页面参数
const props = defineProps<{
  showId: string
  ticketTypeId: string
}>()

// 响应式数据
const showInfo = ref({
  id: '',
  title: '',
  poster: '',
  showTime: '',
  venue: ''
})

const seatMap = ref({
  rows: [] as any[],
  cols: [] as any[],
  seats: [] as any[]
})

const selectedSeats = ref([] as any[])
const loading = ref(false)
const zoomLevel = ref(1)
const scrollLeft = ref(0)
const scrollTop = ref(0)

// 座位样式配置
const seatSize = ref(60) // 基础座位大小
const seatGap = ref(10) // 座位间距

// 计算属性
const totalPrice = computed(() => {
  return selectedSeats.value.reduce((sum, seat) => sum + seat.price, 0)
})

const gridWidth = computed(() => {
  const maxCols = Math.max(...seatMap.value.seats.map(seat => seat.col))
  return (maxCols * seatSize.value + maxCols * seatGap.value + 120) * zoomLevel.value
})

const gridHeight = computed(() => {
  const maxRows = Math.max(...seatMap.value.seats.map(seat => seat.row))
  return (maxRows * seatSize.value + maxRows * seatGap.value + 100) * zoomLevel.value
})

// 获取座位样式类
const getSeatClass = (seat: any) => {
  const classes = ['seat-item']
  
  if (seat.status === 'available') {
    classes.push('available')
  } else if (seat.status === 'occupied') {
    classes.push('occupied')
  } else if (seat.status === 'disabled') {
    classes.push('disabled')
  }
  
  if (selectedSeats.value.some(s => s.id === seat.id)) {
    classes.push('selected')
  }
  
  return classes
}

// 获取座位样式
const getSeatStyle = (seat: any) => {
  const size = seatSize.value * zoomLevel.value
  const gap = seatGap.value * zoomLevel.value
  
  return {
    width: size + 'rpx',
    height: size + 'rpx',
    left: (seat.col * size + seat.col * gap + 60 * zoomLevel.value) + 'rpx',
    top: (seat.row * size + seat.row * gap + 40 * zoomLevel.value) + 'rpx',
    fontSize: (24 * zoomLevel.value) + 'rpx'
  }
}

// 选择座位
const selectSeat = (seat: any) => {
  if (seat.status !== 'available') return
  
  const index = selectedSeats.value.findIndex(s => s.id === seat.id)
  
  if (index > -1) {
    // 取消选择
    selectedSeats.value.splice(index, 1)
  } else {
    // 检查选座数量限制
    if (selectedSeats.value.length >= 6) {
      uni.showToast({
        title: '最多只能选择6个座位',
        icon: 'none'
      })
      return
    }
    
    // 添加选择
    selectedSeats.value.push({
      ...seat,
      rowName: seatMap.value.rows.find(r => r.index === seat.row)?.name || '',
      colName: seatMap.value.cols.find(c => c.index === seat.col)?.name || ''
    })
  }
}

// 移除座位
const removeSeat = (seat: any) => {
  const index = selectedSeats.value.findIndex(s => s.id === seat.id)
  if (index > -1) {
    selectedSeats.value.splice(index, 1)
  }
}

// 缩放控制
const zoomIn = () => {
  if (zoomLevel.value < 2) {
    zoomLevel.value += 0.2
  }
}

const zoomOut = () => {
  if (zoomLevel.value > 0.5) {
    zoomLevel.value -= 0.2
  }
}

// 确认选座
const confirmSelection = () => {
  if (selectedSeats.value.length === 0) return
  
  // 跳转到订单确认页面
  uni.navigateTo({
    url: `/pages/order/confirm?showId=${props.showId}&ticketTypeId=${props.ticketTypeId}&seats=${JSON.stringify(selectedSeats.value.map(s => s.id))}`
  })
}

// 加载演出信息
const loadShowInfo = async () => {
  try {
    // 这里应该调用API获取演出信息
    // const response = await api.getShowDetail(props.showId)
    
    // 模拟数据
    showInfo.value = {
      id: props.showId,
      title: '经典音乐会',
      poster: getShowPoster('经典音乐会'),
      showTime: '2024-03-15 19:30',
      venue: '大剧院音乐厅'
    }
  } catch (error) {
    console.error('加载演出信息失败', error)
    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  }
}

// 加载座位图
const loadSeatMap = async () => {
  try {
    loading.value = true
    
    // 这里应该调用API获取座位图数据
    // const response = await api.getSeatMap(props.showId, props.ticketTypeId)
    
    // 模拟数据
    const rows = Array.from({ length: 20 }, (_, i) => ({
      id: `row_${i}`,
      index: i,
      name: String.fromCharCode(65 + i) // A, B, C...
    }))
    
    const cols = Array.from({ length: 30 }, (_, i) => ({
      id: `col_${i}`,
      index: i,
      name: (i + 1).toString()
    }))
    
    const seats = []
    for (let row = 0; row < 20; row++) {
      for (let col = 0; col < 30; col++) {
        // 模拟一些过道和不可用座位
        if (col === 14 || col === 15) continue // 中间过道
        if (row > 15 && (col < 5 || col > 24)) continue // 后排边缘
        
        const seatId = `${row}_${col}`
        const status = Math.random() > 0.8 ? 'occupied' : 'available'
        
        seats.push({
          id: seatId,
          row,
          col,
          number: col + 1,
          status,
          price: row < 5 ? 280 : row < 10 ? 180 : 120
        })
      }
    }
    
    seatMap.value = { rows, cols, seats }
    
  } catch (error) {
    console.error('加载座位图失败', error)
    uni.showToast({
      title: '加载座位图失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
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
      ticketTypeId: options.ticketTypeId || ''
    })
  }
  
  loadShowInfo()
  loadSeatMap()
})
</script>

<style scoped>
.seat-select-container {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 头部信息 */
.header-section {
  background: white;
  padding: 30rpx;
  border-bottom: 1rpx solid #eee;
}

.show-info {
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
.show-venue {
  display: block;
  font-size: 28rpx;
  color: #666;
  margin-bottom: 8rpx;
}

/* 座位图区域 */
.seat-map-section {
  flex: 1;
  background: white;
  margin: 20rpx;
  border-radius: 20rpx;
  padding: 30rpx;
  position: relative;
}

.stage {
  background: linear-gradient(45deg, #ff6b6b, #ffa500);
  height: 80rpx;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40rpx;
}

.stage-text {
  color: white;
  font-size: 28rpx;
  font-weight: bold;
}

.seat-grid-container {
  height: 600rpx;
  border: 1rpx solid #eee;
  border-radius: 12rpx;
  position: relative;
  overflow: hidden;
}

.seat-grid {
  position: relative;
  min-width: 100%;
  min-height: 100%;
}

.row-labels {
  position: absolute;
  left: 0;
  top: 0;
  width: 50rpx;
}

.row-label {
  position: absolute;
  width: 50rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #666;
  font-weight: bold;
}

.col-labels {
  position: absolute;
  top: 0;
  left: 0;
  height: 40rpx;
}

.col-label {
  position: absolute;
  width: 60rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #666;
  font-weight: bold;
}

.seats-container {
  position: relative;
  margin-top: 40rpx;
  margin-left: 60rpx;
}

.seat-item {
  position: absolute;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2rpx solid transparent;
}

.seat-item.available {
  background: #e8f5e8;
  border-color: #52c41a;
  color: #52c41a;
}

.seat-item.selected {
  background: #1890ff;
  border-color: #1890ff;
  color: white;
  transform: scale(1.1);
}

.seat-item.occupied {
  background: #f5f5f5;
  border-color: #d9d9d9;
  color: #999;
}

.seat-item.disabled {
  background: #fff2f0;
  border-color: #ffccc7;
  color: #ff4d4f;
}

.seat-number {
  font-size: 20rpx;
  font-weight: bold;
}

.zoom-controls {
  position: absolute;
  bottom: 20rpx;
  right: 20rpx;
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 30rpx;
  padding: 10rpx 20rpx;
}

.zoom-btn {
  width: 60rpx;
  height: 60rpx;
  background: transparent;
  color: white;
  border: 2rpx solid white;
  border-radius: 50%;
  font-size: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.zoom-text {
  color: white;
  font-size: 24rpx;
  margin: 0 20rpx;
}

/* 座位图例 */
.legend-section {
  background: white;
  margin: 0 20rpx;
  border-radius: 20rpx;
  padding: 30rpx;
  display: flex;
  justify-content: space-around;
}

.legend-item {
  display: flex;
  align-items: center;
  flex-direction: column;
}

.legend-seat {
  width: 40rpx;
  height: 40rpx;
  border-radius: 6rpx;
  margin-bottom: 10rpx;
  border: 2rpx solid;
}

.legend-seat.available {
  background: #e8f5e8;
  border-color: #52c41a;
}

.legend-seat.selected {
  background: #1890ff;
  border-color: #1890ff;
}

.legend-seat.occupied {
  background: #f5f5f5;
  border-color: #d9d9d9;
}

.legend-seat.disabled {
  background: #fff2f0;
  border-color: #ffccc7;
}

.legend-text {
  font-size: 24rpx;
  color: #666;
}

/* 已选座位 */
.selected-seats-section {
  background: white;
  margin: 20rpx;
  border-radius: 20rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.selected-seats-list {
  white-space: nowrap;
}

.selected-seat-item {
  display: inline-block;
  background: #f0f9ff;
  border: 2rpx solid #1890ff;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-right: 20rpx;
  position: relative;
  vertical-align: top;
}

.seat-info {
  display: block;
  font-size: 28rpx;
  color: #1890ff;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.seat-price {
  display: block;
  font-size: 24rpx;
  color: #666;
}

.remove-btn {
  position: absolute;
  top: -10rpx;
  right: -10rpx;
  width: 40rpx;
  height: 40rpx;
  background: #ff4d4f;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-icon {
  color: white;
  font-size: 24rpx;
  font-weight: bold;
}

/* 底部操作栏 */
.bottom-bar {
  background: white;
  padding: 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1rpx solid #eee;
}

.price-info {
  display: flex;
  flex-direction: column;
}

.total-seats {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.total-price {
  font-size: 36rpx;
  font-weight: bold;
  color: #ff4d4f;
}

.confirm-btn {
  background: linear-gradient(45deg, #1890ff, #0056cc);
  color: white;
  border: none;
  border-radius: 50rpx;
  padding: 24rpx 60rpx;
  font-size: 32rpx;
  font-weight: bold;
}

.confirm-btn.disabled {
  background: #d9d9d9;
  color: #999;
}
</style>