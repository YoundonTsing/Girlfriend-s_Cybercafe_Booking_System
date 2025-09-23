<template>
  <div class="order-detail-container" v-loading="loading">
    <el-card class="order-card" v-if="order">
      <div slot="header">
        <span>订单详情</span>
        <el-tag :type="getOrderStatusType(order.status)" class="float-right">
          {{ getOrderStatusText(order.status) }}
        </el-tag>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="24">
          <div class="order-info">
            <p><span class="label">订单编号：</span>{{ order.orderNo }}</p>
            <p><span class="label">创建时间：</span>{{ order.createTime }}</p>
            <p><span class="label">支付时间：</span>{{ order.payTime || '未支付' }}</p>
            <p><span class="label">订单金额：</span>¥{{ order.totalAmount }}</p>
            <p v-if="order.payAmount"><span class="label">实付金额：</span>¥{{ order.payAmount }}</p>
            <p v-if="order.discountAmount && order.discountAmount > 0"><span class="label">优惠金额：</span>¥{{ order.discountAmount }}</p>
          </div>
        </el-col>
      </el-row>
      
      <el-divider content-position="center" class="tech-divider">
        <span class="divider-title">预约信息</span>
      </el-divider>
      
      <div class="show-section">
        <el-row :gutter="24">
        <el-col :xs="24" :sm="8" :md="6" :lg="4">
            <div class="show-image-container">
          <img :src="getShowPoster(order.showName)" class="show-image">
              <div class="image-overlay"></div>
            </div>
        </el-col>
        <el-col :xs="24" :sm="16" :md="18" :lg="20">
          <div class="show-info">
              <h3 class="show-title">{{ order.showName }}</h3>
              <div class="show-details">
                <div class="detail-item">
                  <i class="el-icon-date detail-icon"></i>
                  <span class="detail-label">演出时间：</span>
                  <span class="detail-value">{{ formatDate(order.showTime) }}</span>
                </div>
                <!-- 网咖预约时间信息 -->
                <div v-if="order.bookingDate" class="detail-item">
                  <i class="el-icon-time detail-icon"></i>
                  <span class="detail-label">预约时间：</span>
                  <span class="detail-value">{{ formatDateTime(order.bookingDate) }}</span>
                </div>
                <div v-if="order.bookingEndTime" class="detail-item">
                  <i class="el-icon-time detail-icon"></i>
                  <span class="detail-label">结束时间：</span>
                  <span class="detail-value">{{ formatDateTime(order.bookingEndTime) }}</span>
                </div>
                <div v-if="order.bookingDuration" class="detail-item">
                  <i class="el-icon-timer detail-icon"></i>
                  <span class="detail-label">使用时长：</span>
                  <span class="detail-value">{{ order.bookingDuration }}小时</span>
                </div>
                <div class="detail-item">
                  <i class="el-icon-location-information detail-icon"></i>
                  <span class="detail-label">演出场馆：</span>
                  <span class="detail-value">{{ order.venue }}</span>
                </div>
                <div class="detail-item">
                  <i class="el-icon-tickets detail-icon"></i>
                  <span class="detail-label">票档：</span>
                  <span class="detail-value">{{ order.ticketName || '标准票' }}</span>
                </div>
                <div class="detail-item">
                  <i class="el-icon-shopping-cart-full detail-icon"></i>
                  <span class="detail-label">数量：</span>
                  <span class="detail-value">{{ order.quantity || 1 }}张</span>
                </div>
                <div class="detail-item">
                  <i class="el-icon-money detail-icon"></i>
                  <span class="detail-label">单价：</span>
                  <span class="detail-value price">¥{{ order.price || (order.totalAmount / (order.quantity || 1)).toFixed(2) }}</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 网咖预约信息 -->
      <el-divider content-position="center" v-if="order.bookingDate || order.contactPhone || order.remark">预约信息</el-divider>
      
      <el-row :gutter="20" v-if="order.bookingDate || order.contactPhone || order.remark">
        <el-col :span="24">
          <div class="booking-info">
            <p v-if="order.bookingDate"><i class="el-icon-time"></i> 预约时间：{{ formatDate(order.bookingDate) }}</p>
            <p v-if="order.contactPhone"><i class="el-icon-phone"></i> 联系电话：{{ order.contactPhone }}</p>
            <p v-if="order.remark"><i class="el-icon-edit-outline"></i> 备注信息：{{ order.remark }}</p>
          </div>
        </el-col>
      </el-row>
      
      <el-divider content-position="center" class="tech-divider">
        <span class="divider-title">操作</span>
      </el-divider>
      
      <div class="action-section">
        <div class="action-buttons">
        <el-button 
            class="pay-btn"
          type="primary" 
          @click="handlePay" 
          v-if="order.status === 0"
            :loading="payLoading"
            size="large">
            <i class="el-icon-wallet"></i>
          立即支付
        </el-button>
        <el-button 
            class="cancel-btn"
          @click="handleCancel" 
          v-if="order.status === 0"
            :loading="cancelLoading"
            size="large">
            <i class="el-icon-close"></i>
          取消订单
        </el-button>
          <el-button 
            class="back-btn"
            @click="goBack"
            size="large">
            <i class="el-icon-back"></i>
            返回订单列表
          </el-button>
        </div>
      </div>
      
      <el-divider content-position="center" class="countdown-divider" v-if="order.status === 0">
        <span class="countdown">
          <i class="el-icon-time"></i>
          支付倒计时：{{ countdownText }}
        </span>
      </el-divider>
    </el-card>
    
    <!-- 支付对话框 -->
    <PaymentDialog 
      v-model="showPaymentDialog"
      :order-info="order"
      @payment-success="handlePaymentSuccess"
      @payment-failed="handlePaymentFailed" />
  </div>
</template>

<script>
import { useOrderStore } from '@/stores/order'
import PaymentDialog from '@/components/PaymentDialog.vue'

export default {
  name: 'OrderDetail',
  components: {
    PaymentDialog
  },
  setup() {
    const orderStore = useOrderStore()
    return {
      orderStore
    }
  },
  data() {
    return {
      countdown: 0,
      countdownTimer: null,
      payLoading: false,
      cancelLoading: false,
      showPaymentDialog: false
    }
  },
  computed: {
    order() {
      return this.orderStore.currentOrder
    },
    loading() {
      return this.orderStore.loading
    },
    countdownText() {
      if (!this.countdown) return '00:00'
      const minutes = Math.floor(this.countdown / 60)
      const seconds = this.countdown % 60
      return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
    }
  },
  created() {
    const orderNo = this.$route.params.orderNo
    this.orderStore.fetchOrderDetailByOrderNo(orderNo).then(() => {
      if (this.order && this.order.status === 0) {
        this.startCountdown()
      }
    })
  },
  beforeDestroy() {
    this.clearCountdown()
  },
  methods: {
    getOrderStatusType(status) {
      switch (status) {
        case 0:
        case 'CREATED':
          return 'warning'
        case 1:
        case 'PAID':
          return 'success'
        case 2:
        case 'CANCELED':
          return 'info'
        case 'TIMEOUT':
          return 'danger'
        default:
          return 'info'
      }
    },
    getOrderStatusText(status) {
      switch (status) {
        case 0:
        case 'CREATED':
          return '待支付'
        case 1:
        case 'PAID':
          return '已支付'
        case 2:
        case 'CANCELED':
          return '已取消'
        case 'TIMEOUT':
          return '已超时'
        default:
          return '未知状态'
      }
    },
    getShowPoster(showName) {
      // 根据演出名称返回对应的本地SVG图片
      if (!showName) {
        return '/images/seat_intermediate.svg'
      }
      
      const name = showName.toLowerCase()
      
      // 网咖机位类型匹配
      if (name.includes('svip') || name.includes('超级vip') || name.includes('至尊')) {
        return '/images/seat_svip.svg'
      } else if (name.includes('vip') || name.includes('包间') || name.includes('豪华')) {
        return '/images/seat_vip_room.svg'
      } else if (name.includes('高级') || name.includes('advanced') || name.includes('高端')) {
        return '/images/seat_advanced.svg'
      } else if (name.includes('新手') || name.includes('newbie') || name.includes('入门')) {
        return '/images/seat_newbie.svg'
      } else if (name.includes('中级') || name.includes('intermediate') || name.includes('标准')) {
        return '/images/seat_intermediate.svg'
      }
      
      // 传统演出类型匹配（也使用座位图标）
      if (name.includes('音乐') || name.includes('演唱会') || name.includes('concert')) {
        return '/images/seat_svip.svg' // 音乐会使用最高级座位
      } else if (name.includes('话剧') || name.includes('戏剧') || name.includes('drama')) {
        return '/images/seat_vip_room.svg' // 话剧使用VIP包间
      } else if (name.includes('体育') || name.includes('nba') || name.includes('sport')) {
        return '/images/seat_advanced.svg' // 体育赛事使用高级座位
      } else if (name.includes('电影') || name.includes('movie') || name.includes('cinema')) {
        return '/images/seat_intermediate.svg' // 电影使用标准座位
      }
      
      // 默认返回中级座位
      return '/images/seat_intermediate.svg'
    },
    formatDate(dateTime) {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    formatDateTime(dateTime) {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    },
    startCountdown() {
      if (!this.order || !this.order.createTime) return
      
      // 计算剩余支付时间（假设15分钟支付时间）
      const createTime = new Date(this.order.createTime).getTime()
      const now = new Date().getTime()
      const expireTime = createTime + 15 * 60 * 1000
      
      this.countdown = Math.max(0, Math.floor((expireTime - now) / 1000))
      
      if (this.countdown <= 0) {
        this.orderStore.fetchOrderDetailByOrderNo(this.order.orderNo)
        return
      }
      
      this.clearCountdown()
      this.countdownTimer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          this.clearCountdown()
          this.orderStore.fetchOrderDetailByOrderNo(this.order.orderNo)
        }
      }, 1000)
    },
    clearCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
    },
    handlePay() {
      if (!this.order) return
      
      // 打开支付对话框
      this.showPaymentDialog = true
    },
    handleCancel() {
      if (!this.order) return
      
      this.$confirm('确定要取消订单吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.cancelLoading = true
        this.orderStore.cancelOrder(this.order.orderNo)
          .then(() => {
            this.$message.success('订单已取消')
            this.orderStore.fetchOrderDetailByOrderNo(this.order.orderNo)
          })
          .catch(error => {
            this.$message.error(error.message || '取消订单失败')
          })
          .finally(() => {
            this.cancelLoading = false
          })
      }).catch(() => {})
    },
    goBack() {
      this.$router.push('/order/list')
    },
    
    async handlePaymentSuccess(paymentData) {
      try {
        // 调用后端API完成支付
        await this.orderStore.payOrder(paymentData.orderNo, paymentData.paymentType)
        
        this.$message({
          message: '支付成功！',
          type: 'success',
          duration: 3000
        })
        
        // 刷新订单详情
        await this.orderStore.fetchOrderDetailByOrderNo(this.order.orderNo)
        
        // 清除倒计时
        this.clearCountdown()
        
      } catch (error) {
        console.error('支付确认失败:', error)
        this.$message.error('支付确认失败，请联系客服')
      }
    },
    
    handlePaymentFailed(error) {
      console.error('支付失败:', error)
      this.$message.error('支付失败，请重试')
    }
  }
}
</script>

<style scoped>
.order-detail-container {
  padding: 20px;
}

.order-card {
  margin-bottom: 20px;
}

.float-right {
  float: right;
}

.order-info {
  margin-bottom: 20px;
}

.label {
  font-weight: bold;
  display: inline-block;
  width: 100px;
}

.show-image {
  width: 100%;
  border-radius: 4px;
}

/* 科技主题分割线样式 */
.tech-divider :deep(.el-divider__text) {
  background: var(--ai-gradient-card) !important;
  color: var(--ai-nvidia-green) !important;
  border: 1px solid var(--ai-border-primary);
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 600;
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

.divider-title {
  color: var(--ai-nvidia-green);
  font-weight: 600;
  font-size: 16px;
}

/* 演出信息区域样式 */
.show-section {
  margin: 24px 0;
  padding: 20px;
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.show-image-container {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4);
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
}

.show-image {
  width: 100%;
  height: auto;
  min-height: 150px;
  border-radius: 8px;
  transition: transform 0.3s ease;
  background: transparent;
  object-fit: contain;
}

/* SVG图片优化 */
.show-image[src$=".svg"] {
  background: linear-gradient(135deg, rgba(16, 20, 33, 0.95), rgba(28, 35, 56, 0.9));
  padding: 10px;
  object-fit: contain;
  object-position: center;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, 
    rgba(118, 185, 0, 0.1) 0%, 
    rgba(0, 179, 255, 0.05) 50%, 
    transparent 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.show-image-container:hover .image-overlay {
  opacity: 1;
}

.show-image-container:hover .show-image {
  transform: scale(1.1);
}

.show-image-container:hover {
  border-color: var(--ai-nvidia-green);
  box-shadow: 
    0 6px 20px rgba(0, 0, 0, 0.5),
    0 0 20px var(--ai-shadow-green);
}

.show-info {
  padding-left: 20px;
}

.show-title {
  margin: 0 0 20px 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--ai-text-primary);
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.show-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(118, 185, 0, 0.1);
  transition: all 0.3s ease;
}

.detail-item:hover {
  background: rgba(118, 185, 0, 0.05);
  border-radius: 6px;
  padding-left: 12px;
  border-bottom-color: rgba(118, 185, 0, 0.3);
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-icon {
  color: var(--ai-nvidia-green);
  font-size: 16px;
  margin-right: 12px;
  width: 20px;
  text-align: center;
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

.detail-label {
  color: var(--ai-text-secondary);
  font-size: 14px;
  min-width: 80px;
  margin-right: 8px;
}

.detail-value {
  color: var(--ai-text-primary);
  font-weight: 500;
  flex: 1;
}

.detail-value.price {
  color: var(--ai-nvidia-green);
  font-weight: 700;
  font-size: 16px;
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

/* 预约信息区域样式 */
.booking-section {
  margin: 24px 0;
  padding: 20px;
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-secondary);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 179, 255, 0.2);
}

.booking-info .detail-item:hover {
  background: rgba(0, 179, 255, 0.05);
  border-bottom-color: rgba(0, 179, 255, 0.3);
}

.booking-info .detail-icon {
  color: var(--ai-tech-blue);
  text-shadow: 0 0 5px var(--ai-shadow-blue);
}

/* 操作区域样式 */
.action-section {
  margin: 30px 0;
  padding: 24px;
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.action-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}

.pay-btn {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a) !important;
  border: none !important;
  color: #ffffff !important;
  font-weight: 600;
  font-size: 16px;
  padding: 14px 32px !important;
  border-radius: 25px !important;
  box-shadow: 0 4px 15px var(--ai-shadow-green);
  transition: all 0.3s ease;
  text-shadow: none;
}

.pay-btn:hover {
  background: linear-gradient(45deg, #9dd33a, #b8e55c) !important;
  box-shadow: 0 6px 20px var(--ai-shadow-green) !important;
  transform: translateY(-2px);
}

.cancel-btn {
  background: rgba(255, 71, 87, 0.2) !important;
  border: 1px solid rgba(255, 71, 87, 0.4) !important;
  color: #ff4757 !important;
  font-weight: 600;
  font-size: 16px;
  padding: 14px 32px !important;
  border-radius: 25px !important;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  background: rgba(255, 71, 87, 0.3) !important;
  border-color: rgba(255, 71, 87, 0.6) !important;
  box-shadow: 0 4px 15px rgba(255, 71, 87, 0.3) !important;
  transform: translateY(-2px);
}

.back-btn {
  background: rgba(136, 146, 176, 0.2) !important;
  border: 1px solid rgba(136, 146, 176, 0.4) !important;
  color: var(--ai-text-secondary) !important;
  font-weight: 600;
  font-size: 16px;
  padding: 14px 32px !important;
  border-radius: 25px !important;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(136, 146, 176, 0.3) !important;
  border-color: rgba(136, 146, 176, 0.6) !important;
  color: var(--ai-text-primary) !important;
  box-shadow: 0 4px 15px rgba(136, 146, 176, 0.3) !important;
  transform: translateY(-2px);
}

/* 倒计时样式 */
.countdown-divider :deep(.el-divider__text) {
  background: linear-gradient(45deg, rgba(255, 215, 0, 0.2), rgba(255, 193, 7, 0.1)) !important;
  border: 1px solid rgba(255, 215, 0, 0.4);
}

.countdown {
  color: #ffd700 !important;
  font-weight: 700;
  font-size: 18px;
  text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);
  animation: pulse 2s infinite;
}

.countdown i {
  margin-right: 8px;
  animation: rotate 2s linear infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .show-info {
    padding-left: 0;
    margin-top: 20px;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 12px;
  }
  
  .action-buttons .el-button {
    width: 100%;
    max-width: 280px;
  }
  
  .show-title {
    font-size: 20px;
  }
  
  .detail-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .detail-label {
    min-width: auto;
  }
}
</style>