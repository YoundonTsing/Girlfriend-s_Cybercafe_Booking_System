<template>
  <div class="seat-map-container">
    <div class="seat-grid" :style="gridStyle" v-if="layout.seats && layout.seats.length > 0">
      <div
        v-for="seat in layout.seats"
        :key="seat.id"
        class="seat-item"
        :class="getSeatClass(seat)"
        :style="getSeatStyle(seat)"
        @click="handleSeatClick(seat)"
        @mouseenter="handleSeatHover(seat)"
        :title="getSeatTooltip(seat)">
        {{ seat.seatNum }}
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else class="empty-seats">
      <el-empty description="暂无座位信息" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  layout: {
    type: Object,
    default: () => ({})
  },
  selectedSeats: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['seat-click', 'seat-hover'])

const gridStyle = computed(() => {
  if (!props.layout.totalCols || props.layout.totalCols === 0) {
    // 如果没有指定列数，使用自适应网格
    return {
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fill, minmax(50px, 1fr))',
      gap: '8px',
      justifyContent: 'center',
      padding: '20px'
    }
  }
  
  return {
    display: 'grid',
    gridTemplateColumns: `repeat(${props.layout.totalCols}, 1fr)`,
    gap: '8px',
    justifyContent: 'center',
    padding: '20px'
  }
})

const getSeatClass = (seat) => {
  const classes = ['seat']
  
  // 基于状态的样式
  switch (seat.status) {
    case 0:
      classes.push('maintenance')
      break
    case 1:
      classes.push('available')
      break
    case 2:
      if (seat.lockedByCurrentUser) {
        classes.push('selected')
      } else {
        // 被其他用户锁定的座位显示为红色
        classes.push('locked-by-others')
      }
      break
    case 3:
      classes.push('occupied')
      break
    default:
      classes.push('unavailable')
  }
  
  // 检查是否在已选列表中
  if (props.selectedSeats.some(s => s.id === seat.id)) {
    classes.push('selected')
  }
  
  // 基于座位类型的样式
  switch (seat.seatType) {
    case 2:
      classes.push('vip-seat')
      break
    case 3:
      classes.push('svip-seat')
      break
    default:
      classes.push('normal-seat')
  }
  
  return classes
}

const getSeatStyle = (seat) => {
  const style = {}
  
  // 如果有坐标信息，使用绝对定位
  if (seat.xCoordinate !== undefined && seat.yCoordinate !== undefined && 
      props.layout.totalCols && props.layout.totalRows) {
    style.gridColumn = seat.xCoordinate + 1
    style.gridRow = seat.yCoordinate + 1
  }
  
  return style
}

const getSeatTooltip = (seat) => {
  let tooltip = `${seat.seatCode || seat.rowNum + seat.seatNum}`
  
  if (seat.price) {
    tooltip += ` - ¥${seat.price}/小时`
  }
  
  switch (seat.status) {
    case 0:
      tooltip += ' (维护中)'
      break
    case 1:
      tooltip += ' (可选)'
      break
    case 2:
      if (seat.lockedByCurrentUser) {
        tooltip += ' (已选中)'
      } else {
        tooltip += ' (已被锁定)'
      }
      break
    case 3:
      tooltip += ' (已占用)'
      break
  }
  
  return tooltip
}

const handleSeatClick = (seat) => {
  emit('seat-click', seat)
}

const handleSeatHover = (seat) => {
  emit('seat-hover', seat)
}
</script>

<style scoped>
.seat-map-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
  overflow: auto;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.1);
}

.seat-grid {
  min-width: 400px;
  max-width: 100%;
  margin: 0 auto;
}

.seat-item {
  width: 45px;
  height: 45px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  position: relative;
  user-select: none;
}

.seat-item:hover {
  transform: scale(1.1);
  z-index: 10;
}

/* 座位状态样式 */
.seat-item.available {
  background: linear-gradient(45deg, #67C23A, #85CE61);
  color: white;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
}

.seat-item.available:hover {
  background: linear-gradient(45deg, #85CE61, #95D475);
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.5);
}

.seat-item.selected {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  color: white;
  border-color: #ffffff;
  box-shadow: 0 4px 15px var(--ai-shadow-green);
  animation: pulse 2s infinite;
}

.seat-item.selected:hover {
  background: linear-gradient(45deg, #9dd33a, #b8e856);
  box-shadow: 0 6px 20px var(--ai-shadow-green);
}

.seat-item.locked {
  background: linear-gradient(45deg, #E6A23C, #F7BA2A);
  color: white;
  cursor: not-allowed;
  box-shadow: 0 2px 8px rgba(230, 162, 60, 0.3);
}

.seat-item.locked-by-others {
  background: linear-gradient(45deg, #FF4757, #FF6B7A);
  color: white;
  cursor: not-allowed;
  box-shadow: 0 2px 8px rgba(255, 71, 87, 0.4);
  border: 2px solid #FF3742;
}

.seat-item.occupied {
  background: linear-gradient(45deg, #F56C6C, #F78989);
  color: white;
  cursor: not-allowed;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
}

.seat-item.maintenance {
  background: linear-gradient(45deg, #909399, #B1B3B8);
  color: white;
  cursor: not-allowed;
  box-shadow: 0 2px 8px rgba(144, 147, 153, 0.3);
}

.seat-item.unavailable {
  background: #C0C4CC;
  color: #909399;
  cursor: not-allowed;
}

/* 座位类型样式增强 */
.seat-item.vip-seat {
  border: 2px solid #FFD700;
  position: relative;
}

.seat-item.vip-seat::before {
  content: 'V';
  position: absolute;
  top: -5px;
  right: -5px;
  width: 12px;
  height: 12px;
  background: #FFD700;
  color: #000;
  font-size: 8px;
  font-weight: bold;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.seat-item.svip-seat {
  border: 2px solid #FF6B6B;
  position: relative;
}

.seat-item.svip-seat::before {
  content: 'S';
  position: absolute;
  top: -5px;
  right: -5px;
  width: 12px;
  height: 12px;
  background: #FF6B6B;
  color: white;
  font-size: 8px;
  font-weight: bold;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 脉冲动画 */
@keyframes pulse {
  0% {
    box-shadow: 0 4px 15px var(--ai-shadow-green);
  }
  50% {
    box-shadow: 0 4px 25px var(--ai-shadow-green), 0 0 15px var(--ai-nvidia-green);
  }
  100% {
    box-shadow: 0 4px 15px var(--ai-shadow-green);
  }
}

.empty-seats {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
  color: var(--ai-text-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .seat-item {
    width: 35px;
    height: 35px;
    font-size: 10px;
  }
  
  .seat-grid {
    gap: 6px;
    padding: 15px;
  }
}

@media (max-width: 480px) {
  .seat-item {
    width: 30px;
    height: 30px;
    font-size: 9px;
  }
  
  .seat-grid {
    gap: 4px;
    padding: 10px;
  }
}
</style>