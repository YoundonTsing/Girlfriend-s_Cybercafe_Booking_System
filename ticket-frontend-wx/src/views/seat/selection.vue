<template>
  <div class="seat-selection-container">
    <!-- 顶部信息栏 -->
    <div class="show-info-header">
      <div class="show-details">
        <h2>{{ showInfo.name || '选择座位' }}</h2>
        <p>{{ showInfo.venue || '网咖一号店' }} | {{ formatDate(new Date()) }}</p>
      </div>
      <div class="selected-info">
        <span>已选座位: {{ selectedSeats.length }}</span>
        <span>总价: ¥{{ totalPrice.toFixed(2) }}</span>
      </div>
    </div>

    <!-- 楼层选择 -->
    <div class="floor-selector" v-if="availableFloors.length > 1">
      <el-radio-group v-model="currentFloor" @change="handleFloorChange">
        <el-radio-button 
          v-for="floor in availableFloors" 
          :key="floor.level"
          :label="floor.level">
          {{ floor.name }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 座位区域选择 -->
    <div class="area-selector" v-if="currentFloorAreas.length > 1">
      <el-select v-model="currentAreaId" @change="handleAreaChange" placeholder="选择区域">
        <el-option
          v-for="area in currentFloorAreas"
          :key="area.id"
          :label="area.name"
          :value="area.id"
          :disabled="!area.selectable">
          <span>{{ area.name }}</span>
          <span style="float: right; color: #8492a6;">
            ¥{{ area.price }}/小时
            <span v-if="area.availableSeats === 0" style="color: #f56c6c;">（已满）</span>
          </span>
        </el-option>
      </el-select>
    </div>

    <!-- 座位布局图 -->
    <div class="seat-layout-container" v-if="currentLayout.seats">
      <div class="area-info">
        <h3>{{ currentLayout.areaName }}</h3>
        <p>{{ currentLayout.description }}</p>
      </div>
      
      <SeatMap 
        :layout="currentLayout"
        :selected-seats="selectedSeats"
        @seat-click="handleSeatClick"
        @seat-hover="handleSeatHover"
      />
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container" v-loading="loading" element-loading-text="加载座位信息中...">
      <div class="loading-placeholder">加载中...</div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && (!currentLayout.seats || currentLayout.seats.length === 0)" class="empty-state">
      <el-empty description="暂无可用座位" />
    </div>

    <!-- 图例 -->
    <div class="seat-legend" v-if="currentLayout.seats && currentLayout.seats.length > 0">
      <div class="legend-item">
        <div class="seat-icon available"></div>
        <span>可选</span>
      </div>
      <div class="legend-item">
        <div class="seat-icon selected"></div>
        <span>已选</span>
      </div>
      <div class="legend-item">
        <div class="seat-icon locked"></div>
        <span>已锁定</span>
      </div>
      <div class="legend-item">
        <div class="seat-icon occupied"></div>
        <span>已占用</span>
      </div>
      <div class="legend-item">
        <div class="seat-icon maintenance"></div>
        <span>维护中</span>
      </div>
    </div>

    <!-- 时间选择和价格计算 -->
    <div v-if="selectedSeats.length > 0" class="booking-panel">
      <el-card class="time-price-card">
        <template #header>
          <div class="card-header">
            <span>预约信息</span>
            <el-tag type="success">已选 {{ selectedSeats.length }} 个座位</el-tag>
          </div>
        </template>
        
        <div class="booking-form">
          <div class="form-row">
            <label>开始时间：</label>
            <el-date-picker
              v-model="bookingStartTime"
              type="datetime"
              placeholder="选择开始时间"
              :disabled-date="disabledDate"
              :disabled-hours="disabledHours"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </div>
          
          <div class="form-row">
            <label>使用时长：</label>
            <el-select v-model="bookingDuration" placeholder="选择时长">
              <el-option label="2小时" :value="2" />
              <el-option label="3小时" :value="3" />
              <el-option label="4小时" :value="4" />
              <el-option label="5小时" :value="5" />
              <el-option label="6小时" :value="6" />
              <el-option label="7小时" :value="7" />
              <el-option label="8小时" :value="8" />
            </el-select>
          </div>
          
          <div class="price-summary">
            <div class="price-item">
              <span>基础价格：</span>
              <span>¥{{ basePrice }}</span>
            </div>
            <div class="price-item" v-if="nightSurcharge > 0">
              <span>夜间加价：</span>
              <span>¥{{ nightSurcharge }}</span>
            </div>
            <div class="price-item total">
              <span>总计：</span>
              <span class="total-price">¥{{ totalPrice }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-actions">
      <el-button @click="goBack">返回</el-button>
      <el-button @click="clearSelection" :disabled="selectedSeats.length === 0">
        清空选择
      </el-button>
      <el-button 
        type="primary" 
        @click="confirmBooking"
        :disabled="!canConfirm"
        :loading="confirming">
        确认预约 ¥{{ totalPrice }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SeatMap from '@/components/SeatMap.vue'
import { getSeatAreas, getSeatLayout, lockSeats, releaseSeats } from '@/api/seat'
import { getShowDetail } from '@/api/show'

const route = useRoute()
const router = useRouter()

// 响应式数据
const showInfo = ref({})
const availableFloors = ref([])
const currentFloor = ref(1)
const currentAreaId = ref(null)
const currentLayout = ref({})
const selectedSeats = ref([])
const loading = ref(false)
const confirming = ref(false)
const lockTimer = ref(null)

// 预约相关数据
const bookingStartTime = ref('')
const bookingDuration = ref(2)
const currentAreaPrice = ref(0)

// 设置默认开始时间（当前时间+1小时，整点）
const setDefaultStartTime = () => {
  const now = new Date()
  now.setHours(now.getHours() + 1, 0, 0, 0) // 下一个整点
  bookingStartTime.value = now.toISOString().slice(0, 19).replace('T', ' ')
}

// 计算属性
const currentFloorAreas = computed(() => {
  return availableFloors.value.find(f => f.level === currentFloor.value)?.areas || []
})

// 价格计算相关计算属性
const basePrice = computed(() => {
  if (!selectedSeats.value.length || !bookingDuration.value) return 0
  const hourlyPrice = currentAreaPrice.value || 10 // 默认10元/小时
  return hourlyPrice * bookingDuration.value * selectedSeats.value.length
})

const nightSurcharge = computed(() => {
  if (!bookingStartTime.value || !bookingDuration.value) return 0
  // 计算夜间时段的加价（0:00-7:00 每小时+3元）
  const startTime = new Date(bookingStartTime.value)
  let surcharge = 0
  
  for (let i = 0; i < bookingDuration.value; i++) {
    const currentHour = new Date(startTime.getTime() + i * 60 * 60 * 1000)
    const hour = currentHour.getHours()
    if (hour >= 0 && hour < 7) {
      surcharge += 3 * selectedSeats.value.length
    }
  }
  
  return surcharge
})

const totalPrice = computed(() => {
  return basePrice.value + nightSurcharge.value
})

const canConfirm = computed(() => {
  return selectedSeats.value.length > 0 && 
         bookingStartTime.value && 
         bookingDuration.value
})

// 生命周期
onMounted(() => {
  initializeData()
  startLockRefreshTimer()
  setDefaultStartTime()
})

onUnmounted(() => {
  clearLockTimer()
  releaseAllSeats()
})

// 监听路由参数变化
watch(() => route.params, (newParams) => {
  if (newParams.showId && newParams.sessionId) {
    initializeData()
  }
}, { deep: true })

// 方法
const initializeData = async () => {
  try {
    loading.value = true
    const showId = parseInt(route.params.showId)
    const sessionId = parseInt(route.params.sessionId)
    
    // 获取演出信息
    try {
      const showResponse = await getShowDetail(showId)
      showInfo.value = showResponse.data
    } catch (error) {
      console.warn('获取演出信息失败:', error)
      showInfo.value = { name: '机位预约', venue: '网咖一号店' }
    }
    
    // 获取可用座位区域
    const areasResponse = await getSeatAreas(showInfo.value.type || showId, showId)
    processAreasData(areasResponse.data)
    
    // 默认选择第一个楼层和区域
    if (availableFloors.value.length > 0) {
      currentFloor.value = availableFloors.value[0].level
      if (currentFloorAreas.value.length > 0) {
        currentAreaId.value = currentFloorAreas.value[0].id
        await loadSeatLayout()
      }
    }
  } catch (error) {
    console.error('初始化数据失败:', error)
    ElMessage.error('加载座位信息失败')
  } finally {
    loading.value = false
  }
}

const processAreasData = (areas) => {
  // 按楼层分组座位区域
  const floorMap = new Map()
  
  areas.forEach(area => {
    if (!floorMap.has(area.floorLevel)) {
      floorMap.set(area.floorLevel, {
        level: area.floorLevel,
        name: `${area.floorLevel}楼`,
        areas: []
      })
    }
    floorMap.get(area.floorLevel).areas.push(area)
  })
  
  availableFloors.value = Array.from(floorMap.values()).sort((a, b) => a.level - b.level)
}

const handleFloorChange = () => {
  if (currentFloorAreas.value.length > 0) {
    // 清空当前选择
    releaseAllSeats()
    selectedSeats.value = []
    
    // 选择第一个可用区域
    const firstSelectableArea = currentFloorAreas.value.find(area => area.selectable)
    currentAreaId.value = firstSelectableArea ? firstSelectableArea.id : currentFloorAreas.value[0].id
    loadSeatLayout()
  }
}

const handleAreaChange = () => {
  // 清空当前选择
  releaseAllSeats()
  selectedSeats.value = []
  loadSeatLayout()
}

const loadSeatLayout = async () => {
  if (!currentAreaId.value) return
  
  try {
    loading.value = true
    const response = await getSeatLayout(
      currentAreaId.value, 
      parseInt(route.params.showId), 
      parseInt(route.params.sessionId)
    )
    currentLayout.value = response.data
    
    // 设置当前区域价格
    const currentArea = availableFloors.value
      .flatMap(f => f.areas)
      .find(a => a.id === currentAreaId.value)
    
    if (currentArea) {
      currentAreaPrice.value = currentArea.price || 10
    }
    
  } catch (error) {
    console.error('加载座位布局失败:', error)
    ElMessage.error('加载座位布局失败')
  } finally {
    loading.value = false
  }
}

const handleSeatClick = async (seat) => {
  if (seat.status === 0 || seat.status === 3) {
    ElMessage.warning('该座位不可选择')
    return
  }
  
  if (seat.status === 2 && !seat.lockedByCurrentUser) {
    ElMessage.warning('该座位已被其他用户选择')
    return
  }
  
  const isSelected = selectedSeats.value.some(s => s.id === seat.id)
  
  if (isSelected) {
    // 取消选择
    await releaseSeatLock(seat)
    selectedSeats.value = selectedSeats.value.filter(s => s.id !== seat.id)
    // 更新座位状态
    updateSeatStatus(seat.id, 1)
  } else {
    // 选择座位
    try {
      const success = await lockSeats([seat.id])
      if (success.data) {
        selectedSeats.value.push(seat)
        // 更新座位状态
        updateSeatStatus(seat.id, 2)
        ElMessage.success('座位选择成功')
      } else {
        ElMessage.warning('座位已被其他用户选择')
      }
    } catch (error) {
      console.error('选择座位失败:', error)
      ElMessage.error('选择座位失败')
    }
  }
}

const handleSeatHover = (seat) => {
  // 可以在这里显示座位详细信息的提示
}

const updateSeatStatus = (seatId, newStatus) => {
  const seat = currentLayout.value.seats.find(s => s.id === seatId)
  if (seat) {
    seat.status = newStatus
  }
}

const releaseSeatLock = async (seat) => {
  try {
    await releaseSeats([seat.id])
  } catch (error) {
    console.error('释放座位锁定失败:', error)
  }
}

const clearSelection = async () => {
  if (selectedSeats.value.length === 0) return
  
  try {
    const seatIds = selectedSeats.value.map(s => s.id)
    await releaseSeats(seatIds)
    
    // 更新座位状态
    seatIds.forEach(seatId => {
      updateSeatStatus(seatId, 1)
    })
    
    selectedSeats.value = []
    ElMessage.success('已清空选择')
  } catch (error) {
    console.error('清空选择失败:', error)
    ElMessage.error('清空选择失败')
  }
}

// 时间选择限制方法
const disabledDate = (time) => {
  // 只能预约未来3天内
  const now = new Date()
  const maxDate = new Date()
  maxDate.setDate(now.getDate() + 3)
  
  return time.getTime() < now.getTime() || time.getTime() > maxDate.getTime()
}

const disabledHours = () => {
  // 如果选择的是今天，禁用已过去的小时
  if (!bookingStartTime.value) return []
  
  const selectedDate = new Date(bookingStartTime.value)
  const now = new Date()
  
  if (selectedDate.toDateString() === now.toDateString()) {
    const currentHour = now.getHours()
    const disabledHours = []
    for (let i = 0; i < currentHour; i++) {
      disabledHours.push(i)
    }
    return disabledHours
  }
  
  return []
}

const confirmBooking = async () => {
  console.log('confirmBooking 方法被调用')
  console.log('canConfirm.value:', canConfirm.value)
  console.log('selectedSeats:', selectedSeats.value)
  console.log('bookingStartTime:', bookingStartTime.value)
  console.log('bookingDuration:', bookingDuration.value)
  
  if (!canConfirm.value) {
    ElMessage.warning('请完善预约信息')
    return
  }
  
  try {
    const endTime = new Date(new Date(bookingStartTime.value).getTime() + bookingDuration.value * 60 * 60 * 1000)
    
    await ElMessageBox.confirm(
      `确认预约信息：
      座位：${selectedSeats.value.length} 个
      时间：${bookingStartTime.value} - ${endTime.toLocaleString()}
      时长：${bookingDuration.value} 小时
      总价：¥${totalPrice.value}`,
      '确认预约',
      {
        confirmButtonText: '确认预约',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    confirming.value = true
    
    // 准备预约数据
    const bookingData = {
      showId: parseInt(route.params.showId),
      sessionId: parseInt(route.params.sessionId),
      seatIds: selectedSeats.value.map(s => s.id),
      areaId: currentAreaId.value,
      startTime: bookingStartTime.value,
      endTime: endTime.toISOString().slice(0, 19).replace('T', ' '),
      duration: bookingDuration.value,
      totalPrice: totalPrice.value,
      basePrice: basePrice.value,
      nightSurcharge: nightSurcharge.value
    }
    
    console.log('准备跳转到订单页面，预约数据：', bookingData)
    
    // 跳转到订单确认页面，传递预约数据
    router.push({
      name: 'BookingDetail',
      params: {
        showId: route.params.showId,
        timeSlotId: route.params.sessionId  // 路由参数名称要匹配
      },
      query: {
        seatIds: bookingData.seatIds.join(','),
        areaId: bookingData.areaId,
        startTime: bookingData.startTime,
        endTime: bookingData.endTime,
        duration: bookingData.duration,
        priceName: `${bookingDuration.value}小时套餐`,
        price: totalPrice.value,
        basePrice: basePrice.value,
        nightSurcharge: nightSurcharge.value,
        description: `${selectedSeats.value.length}个座位，${bookingDuration.value}小时使用`,
        stock: selectedSeats.value.length
      }
    })
    
    console.log('跳转成功')
    ElMessage.success('正在跳转到订单确认页面...')
    
  } catch (error) {
    console.error('确认预约过程中发生错误:', error)
    if (error.message && error.message !== 'cancel') {
      ElMessage.error('跳转失败，请重试')
    }
  } finally {
    confirming.value = false
  }
}

const goBack = () => {
  router.go(-1)
}

const startLockRefreshTimer = () => {
  // 每4分钟刷新一次锁定状态
  lockTimer.value = setInterval(() => {
    refreshSeatLocks()
  }, 240000)
}

const refreshSeatLocks = async () => {
  if (selectedSeats.value.length > 0) {
    const seatIds = selectedSeats.value.map(s => s.id)
    try {
      await lockSeats(seatIds) // 续期锁定
    } catch (error) {
      console.error('刷新座位锁定失败:', error)
    }
  }
}

const clearLockTimer = () => {
  if (lockTimer.value) {
    clearInterval(lockTimer.value)
    lockTimer.value = null
  }
}

const releaseAllSeats = async () => {
  if (selectedSeats.value.length > 0) {
    const seatIds = selectedSeats.value.map(s => s.id)
    try {
      await releaseSeats(seatIds)
    } catch (error) {
      console.error('释放座位锁定失败:', error)
    }
  }
}

const formatDate = (date) => {
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.seat-selection-container {
  padding: 20px;
  min-height: 100vh;
  background: var(--ai-gradient-bg);
  padding-bottom: 100px; /* 为底部操作栏留出空间 */
}

.show-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: var(--ai-gradient-card);
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid var(--ai-border-primary);
}

.show-details h2 {
  margin: 0 0 8px 0;
  color: var(--ai-text-primary);
  font-size: 24px;
  text-shadow: 0 0 10px var(--ai-shadow-green);
}

.show-details p {
  margin: 0;
  color: var(--ai-text-secondary);
  font-size: 14px;
}

.selected-info {
  text-align: right;
  color: var(--ai-nvidia-green);
  font-weight: bold;
}

.selected-info span {
  display: block;
  margin-bottom: 5px;
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

.floor-selector, .area-selector {
  margin-bottom: 20px;
  text-align: center;
}

.floor-selector :deep(.el-radio-button__inner) {
  border-color: var(--ai-border-primary);
  background: var(--ai-gradient-card);
  color: var(--ai-text-primary);
  transition: all 0.3s ease;
}

.floor-selector :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  border-color: var(--ai-nvidia-green);
  color: #ffffff;
  box-shadow: 0 0 10px var(--ai-shadow-green);
}

.area-selector :deep(.el-select) {
  width: 300px;
}

.area-selector :deep(.el-input__inner) {
  background: var(--ai-gradient-card);
  border-color: var(--ai-border-primary);
  color: var(--ai-text-primary);
}

.seat-layout-container {
  background: var(--ai-gradient-card);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  min-height: 400px;
  border: 1px solid var(--ai-border-primary);
}

.area-info {
  text-align: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--ai-border-primary);
}

.area-info h3 {
  margin: 0 0 10px 0;
  color: var(--ai-text-primary);
  font-size: 20px;
  text-shadow: 0 0 8px var(--ai-shadow-green);
}

.area-info p {
  margin: 0;
  color: var(--ai-text-secondary);
  font-size: 14px;
}

.loading-container, .empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
  background: var(--ai-gradient-card);
  border-radius: 12px;
  margin-bottom: 20px;
}

.seat-legend {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ai-text-primary);
}

.seat-icon {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.seat-icon.available { 
  background: linear-gradient(45deg, #67C23A, #85CE61);
  box-shadow: 0 0 5px rgba(103, 194, 58, 0.4);
}

.seat-icon.selected { 
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  box-shadow: 0 0 8px var(--ai-shadow-green);
}

.seat-icon.locked { 
  background: linear-gradient(45deg, #E6A23C, #F7BA2A);
  box-shadow: 0 0 5px rgba(230, 162, 60, 0.4);
}

.seat-icon.occupied { 
  background: linear-gradient(45deg, #F56C6C, #F78989);
  box-shadow: 0 0 5px rgba(245, 108, 108, 0.4);
}

.seat-icon.maintenance { 
  background: linear-gradient(45deg, #909399, #B1B3B8);
  box-shadow: 0 0 5px rgba(144, 147, 153, 0.4);
}

.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  background: var(--ai-gradient-card);
  border-top: 1px solid var(--ai-border-primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 15px;
  z-index: 100;
  backdrop-filter: blur(10px);
}

.bottom-actions :deep(.el-button) {
  border-radius: 25px;
  padding: 12px 24px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.bottom-actions :deep(.el-button--primary) {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  border-color: var(--ai-nvidia-green);
  box-shadow: 0 4px 15px var(--ai-shadow-green);
}

.bottom-actions :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px var(--ai-shadow-green);
}

.bottom-actions :deep(.el-button:not(.el-button--primary)) {
  background: var(--ai-gradient-card);
  border-color: var(--ai-border-primary);
  color: var(--ai-text-primary);
}

.bottom-actions :deep(.el-button:not(.el-button--primary):hover) {
  border-color: var(--ai-nvidia-green);
  color: var(--ai-nvidia-green);
  box-shadow: 0 0 10px var(--ai-shadow-green);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .seat-selection-container {
    padding: 10px;
  }
  
  .show-info-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  .seat-legend {
    gap: 15px;
    font-size: 12px;
  }
  
  .bottom-actions {
    flex-direction: column;
    gap: 10px;
  }
  
  .bottom-actions :deep(.el-button) {
    width: 100%;
  }
}

/* 预约面板样式 */
.booking-panel {
  margin: 20px 0;
  padding: 0 20px;
}

.time-price-card {
  border-radius: 15px;
  border: 1px solid rgba(118, 185, 0, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--ai-primary-color);
}

.booking-form {
  padding: 20px 0;
}

.form-row {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  gap: 15px;
}

.form-row label {
  min-width: 80px;
  font-weight: 500;
  color: var(--ai-text-primary);
}

.price-summary {
  margin-top: 25px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(118, 185, 0, 0.05), rgba(102, 204, 204, 0.05));
  border-radius: 10px;
  border: 1px solid rgba(118, 185, 0, 0.1);
}

.price-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 5px 0;
  color: var(--ai-text-primary);
}

.price-item.total {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 2px solid rgba(118, 185, 0, 0.2);
  font-weight: 600;
  font-size: 18px;
}

.total-price {
  color: var(--ai-nvidia-green);
  font-weight: 700;
  font-size: 20px;
}

.price-item:last-child {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .booking-panel {
    margin: 15px 0;
    padding: 0 15px;
  }
  
  .form-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .form-row label {
    min-width: auto;
  }
}
</style>