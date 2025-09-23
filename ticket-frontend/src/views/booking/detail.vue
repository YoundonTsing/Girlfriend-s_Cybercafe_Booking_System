<template>
  <div class="booking-detail-container" v-loading="loading">
    <div class="booking-detail" v-if="show && selectedTimeSlot">
      <!-- 返回按钮 -->
      <div class="back-section">
        <el-button @click="goBack" icon="el-icon-arrow-left" type="text" class="back-btn">
          返回机位详情
        </el-button>
      </div>

      <!-- 预约信息总览 -->
      <div class="booking-overview">
        <div class="overview-card">
          <div class="card-header">
            <h2>预约确认</h2>
            <div class="status-badge">
              <i class="el-icon-time"></i>
              <span>待确认</span>
            </div>
          </div>
          
          <div class="overview-content">
            <div class="seat-info">
              <div class="seat-image">
                <img :src="show.posterUrl || '/images/seat_default.svg'" alt="机位图片">
              </div>
              <div class="seat-details">
                <h3>{{ show.name || show.title }}</h3>
                <p class="seat-type">{{ getShowTypeText(show.type) }}</p>
                <p class="seat-location">{{ show.venueName || show.venue }}</p>
              </div>
            </div>
            
            <div class="package-info">
              <div class="package-badge">
                <span class="package-name">{{ selectedTimeSlot.priceName }}</span>
                <span class="package-price">¥{{ selectedTimeSlot.price }}</span>
              </div>
              <p class="package-desc">{{ selectedTimeSlot.description }}</p>
              
              <!-- 座位信息（仅座位预约显示） -->
              <div v-if="selectedTimeSlot.seatIds" class="seat-booking-info">
                <div class="seat-info-row">
                  <span class="info-label">选择座位：</span>
                  <span class="info-value">{{ selectedTimeSlot.seatIds }}号座位</span>
                </div>
                <div class="seat-info-row">
                  <span class="info-label">使用时间：</span>
                  <span class="info-value">{{ formatTime(selectedTimeSlot.startTime) }} - {{ formatTime(selectedTimeSlot.endTime) }}</span>
                </div>
                <div class="seat-info-row">
                  <span class="info-label">使用时长：</span>
                  <span class="info-value">{{ selectedTimeSlot.duration }}小时</span>
                </div>
                <div v-if="selectedTimeSlot.nightSurcharge > 0" class="seat-info-row">
                  <span class="info-label">夜间加价：</span>
                  <span class="info-value night-surcharge">+¥{{ selectedTimeSlot.nightSurcharge }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 预约表单 -->
      <div class="booking-form-section">
        <div class="form-card">
          <div class="form-header">
            <h3>预约信息</h3>
            <p>请填写完整的预约信息以确保顺利使用</p>
          </div>
          
          <el-form :model="orderForm" :rules="formRules" ref="bookingForm" label-width="120px" class="booking-form">
            <div class="form-row">
              <el-form-item label="预约数量" prop="quantity" class="form-item-half">
                <el-input-number 
                  v-model="orderForm.quantity" 
                  :min="1" 
                  :max="selectedTimeSlot ? Math.min(selectedTimeSlot.stock, 4) : 4"
                  controls-position="right"
                  class="quantity-input">
                </el-input-number>
                <span class="form-tip">最多预约{{ selectedTimeSlot ? Math.min(selectedTimeSlot.stock, 4) : 4 }}台机位</span>
              </el-form-item>
              
              <el-form-item label="预约时间" prop="bookingDate" class="form-item-half">
                <el-date-picker
                  v-model="orderForm.bookingDate"
                  type="datetime"
                  placeholder="选择预约时间"
                  :disabled-date="disabledDate"
                  format="YYYY年MM月DD日 HH:mm"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :shortcuts="dateShortcuts"
                  class="full-width">
                </el-date-picker>
              </el-form-item>
            </div>

            <el-form-item label="联系电话" prop="contactPhone">
              <el-input
                v-model="orderForm.contactPhone"
                placeholder="请输入您的手机号码"
                maxlength="11"
                class="contact-input">
                <template #prefix>
                  <i class="el-icon-phone"></i>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="备注信息">
              <el-input
                v-model="orderForm.remark"
                type="textarea"
                placeholder="如有特殊需求请在此说明（可选）"
                :rows="4"
                maxlength="200"
                show-word-limit
                class="remark-input">
              </el-input>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 费用明细 -->
      <div class="cost-section">
        <div class="cost-card">
          <h3>费用明细</h3>
          <div class="cost-details">
            <div class="cost-item">
              <span class="cost-label">{{ selectedTimeSlot.priceName }}</span>
              <span class="cost-value">¥{{ selectedTimeSlot.price }}/台</span>
            </div>
            <div class="cost-item">
              <span class="cost-label">预约数量</span>
              <span class="cost-value">{{ orderForm.quantity }}台</span>
            </div>
            <div class="cost-item total">
              <span class="cost-label">总计金额</span>
              <span class="cost-value total-amount">¥{{ getTotalAmount() }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-section">
        <div class="action-buttons">
          <el-button @click="goBack" size="large" class="cancel-btn">
            取消预约
          </el-button>
          <el-button type="primary" @click="confirmBooking" :loading="orderLoading" size="large" class="confirm-btn">
            <i class="el-icon-check" v-if="!orderLoading"></i>
            确认预约
          </el-button>
        </div>
        
        <div class="booking-tips">
          <h4>预约须知：</h4>
          <ul>
            <li>请提前30分钟到店，凭预约码入座</li>
            <li>超时15分钟未到店，系统将自动释放机位</li>
            <li>支持提前1小时免费取消预约</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { useShowStore } from '@/stores/show'
import { useUserStore } from '@/stores/user'
import { useOrderStore } from '@/stores/order'

export default {
  name: 'BookingDetail',
  setup() {
    const showStore = useShowStore()
    const userStore = useUserStore()
    const orderStore = useOrderStore()
    return {
      showStore,
      userStore,
      orderStore
    }
  },
  data() {
    return {
      show: null,
      selectedTimeSlot: null,
      orderForm: {
        quantity: 1,
        bookingDate: '',
        contactPhone: '',
        remark: ''
      },
      orderLoading: false,
      loading: false,
      dateShortcuts: [
        {
          text: '今天',
          value: new Date()
        },
        {
          text: '明天',
          value: () => {
            const date = new Date()
            date.setTime(date.getTime() + 3600 * 1000 * 24)
            return date
          }
        },
        {
          text: '后天',
          value: () => {
            const date = new Date()
            date.setTime(date.getTime() + 3600 * 1000 * 24 * 2)
            return date
          }
        }
      ],
      formRules: {
        quantity: [
          { required: true, message: '请选择预约数量', trigger: 'change' }
        ],
        bookingDate: [
          { required: true, message: '请选择预约时间', trigger: 'change' }
        ],
        contactPhone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    isLoggedIn() {
      return this.userStore.isLoggedIn
    }
  },
  async created() {
    console.log('预约页面初始化')
    console.log('当前登录状态:', this.isLoggedIn)
    console.log('当前token:', this.userStore.token)
    console.log('当前用户信息:', this.userStore.userInfo)
    
    // 确保用户信息已加载
    if (this.isLoggedIn && !this.userStore.userInfo?.userId) {
      console.log('用户已登录但缺少用户信息，尝试获取用户信息')
      try {
        const userInfo = await this.userStore.getInfo()
        console.log('获取用户信息成功:', userInfo)
        console.log('更新后的用户信息:', this.userStore.userInfo)
      } catch (error) {
        console.error('获取用户信息失败:', error)
        this.$message.error('获取用户信息失败，请重新登录')
        this.$router.push('/login?redirect=' + this.$route.fullPath)
        return
      }
    }
    
    await this.initBookingDetail()
  },
  methods: {
    async initBookingDetail() {
      // 从路由参数获取信息
      const { showId, timeSlotId } = this.$route.params
      const timeSlotData = this.$route.query
      
      console.log('路由参数:', { showId, timeSlotId })
      console.log('查询参数:', timeSlotData)
      
      if (!showId) {
        this.$message.error('缺少演出信息')
        this.$router.push('/show')
        return
      }
      
      this.loading = true
      
      try {
        // 获取演出详情
        await this.showStore.fetchShowDetail(showId)
        this.show = this.showStore.currentShow
        console.log('演出信息:', this.show)
        
        // 获取票档列表
        const ticketResponse = await this.$http.get(`/ticket/list?showId=${showId}&sessionId=${timeSlotId}`)
        const tickets = ticketResponse.data
        console.log('票档列表:', tickets)
        
        // 选择第一个可用的票档作为默认票档
        let selectedTicket = null
        if (tickets && tickets.length > 0) {
          selectedTicket = tickets[0] // 使用第一个票档
        }
        
        // 处理座位预约数据
        if (timeSlotData.seatIds) {
          // 来自座位选择页面的数据
          this.selectedTimeSlot = {
            id: selectedTicket ? selectedTicket.id : timeSlotId, // 使用正确的票档ID
            ticketId: selectedTicket ? selectedTicket.id : timeSlotId, // 明确的票档ID
            priceName: timeSlotData.priceName,
            price: parseFloat(timeSlotData.price),
            description: timeSlotData.description,
            stock: parseInt(timeSlotData.stock) || 1,
            // 座位相关信息
            seatIds: timeSlotData.seatIds,
            areaId: timeSlotData.areaId,
            startTime: timeSlotData.startTime,
            endTime: timeSlotData.endTime, // 保留原始endTime，但在创建订单时重新计算
            duration: parseInt(timeSlotData.duration),
            basePrice: parseFloat(timeSlotData.basePrice),
            nightSurcharge: parseFloat(timeSlotData.nightSurcharge)
          }
          
          // 自动填充预约表单
          this.orderForm.quantity = parseInt(timeSlotData.stock) || 1
          this.orderForm.bookingDate = timeSlotData.startTime
          
        } else if (timeSlotData.priceName) {
          // 传统预约数据
          this.selectedTimeSlot = {
            id: selectedTicket ? selectedTicket.id : timeSlotId,
            ticketId: selectedTicket ? selectedTicket.id : timeSlotId, // 明确的票档ID
            priceName: timeSlotData.priceName,
            price: parseFloat(timeSlotData.price),
            description: timeSlotData.description,
            stock: parseInt(timeSlotData.stock) || 5
          }
        } else {
          // 如果没有时段数据，使用默认数据或票档数据
          if (selectedTicket) {
            this.selectedTimeSlot = {
              id: selectedTicket.id,
              ticketId: selectedTicket.id,
              priceName: selectedTicket.name,
              price: selectedTicket.price,
              description: selectedTicket.name,
              stock: selectedTicket.remainCount || 5
            }
          } else {
            this.selectedTimeSlot = this.getDefaultTimeSlots().find(slot => slot.id == timeSlotId) || this.getDefaultTimeSlots()[0]
          }
        }
        
        console.log('最终选择的时段信息:', this.selectedTimeSlot)
        
      } catch (error) {
        console.error('获取数据失败:', error)
        this.$message.error('获取数据失败')
      } finally {
        this.loading = false
      }
      
      // 获取用户信息并自动填充手机号
      this.loadUserInfo()
    },
    
    getShowTypeText(type) {
      switch (type) {
        case 1: return '新客电竞机位'
        case 2: return '中级电竞机位'
        case 3: return '高级电竞机位'
        case 4: return '包厢电竞机位'
        case 5: return 'SVIP电竞机位'
        default: return '电竞机位'
      }
    },
    
    getDefaultTimeSlots() {
      return [
        { id: 1, priceName: '2小时套餐', price: 20, description: '适合短时间游戏', stock: 5 },
        { id: 2, priceName: '4小时套餐', price: 35, description: '半天游戏时光', stock: 3 },
        { id: 3, priceName: '6小时套餐', price: 50, description: '深度游戏体验', stock: 2 },
        { id: 4, priceName: '包夜套餐', price: 80, description: '通宵达旦（22:00-08:00）', stock: 1 }
      ]
    },

    // 加载用户信息并自动填充手机号
    async loadUserInfo() {
      try {
        // 从用户store获取用户信息
        if (this.userStore.userInfo && this.userStore.userInfo.phone) {
          this.orderForm.contactPhone = this.userStore.userInfo.phone
        } else {
          // 如果store中没有用户信息，尝试获取
          await this.userStore.getInfo()
          if (this.userStore.userInfo && this.userStore.userInfo.phone) {
            this.orderForm.contactPhone = this.userStore.userInfo.phone
          }
        }
      } catch (error) {
        console.warn('获取用户信息失败:', error)
        // 不影响主流程，只是无法自动填充手机号
      }
    },

    // 格式化时间显示
    formatTime(timeString) {
      if (!timeString) return ''
      const date = new Date(timeString)
      return date.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    
    getTotalAmount() {
      if (!this.selectedTimeSlot || !this.orderForm.quantity) {
        return 0
      }
      return (this.selectedTimeSlot.price * this.orderForm.quantity).toFixed(2)
    },
    
    disabledDate(time) {
      // 禁用今天之前的日期
      return time.getTime() < Date.now() - 8.64e7
    },
    
    goBack() {
      this.$router.go(-1)
    },
    
    confirmBooking() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录')
        this.$router.push('/login?redirect=' + this.$route.fullPath)
        return
      }

      // 检查用户信息
      if (!this.userStore.userInfo || !this.userStore.userInfo.userId) {
        this.$message.error('用户信息获取失败，请重新登录')
        this.$router.push('/login?redirect=' + this.$route.fullPath)
        return
      }

      // 表单验证
      this.$refs.bookingForm.validate((valid) => {
        if (!valid) {
          return false
        }

        this.orderLoading = true
        
        // 计算正确的结束时间：开始时间 + 使用时长
        let calculatedEndTime = null
        if (this.orderForm.bookingDate && this.selectedTimeSlot.duration) {
          const startTime = new Date(this.orderForm.bookingDate)
          const endTime = new Date(startTime.getTime() + this.selectedTimeSlot.duration * 60 * 60 * 1000)
          calculatedEndTime = endTime.toISOString().slice(0, 19).replace('T', ' ')
        }
        
        const bookingData = {
          userId: Number(this.userStore.userInfo.userId),
          showId: Number(this.show.id),
          sessionId: Number(this.$route.params.timeSlotId),
          ticketId: Number(this.selectedTimeSlot.ticketId || this.selectedTimeSlot.id || this.$route.params.timeSlotId), // 使用正确的票档ID
          quantity: Number(this.orderForm.quantity),
          bookingDate: this.orderForm.bookingDate,
          bookingEndTime: calculatedEndTime || this.selectedTimeSlot.endTime, // 优先使用计算的结束时间
          bookingDuration: this.selectedTimeSlot.duration,
          contactPhone: this.orderForm.contactPhone,
          remark: this.orderForm.remark || '',
          // 传递座位选择页面计算的价格信息
          totalPrice: this.selectedTimeSlot.price,
          basePrice: this.selectedTimeSlot.basePrice,
          nightSurcharge: this.selectedTimeSlot.nightSurcharge,
          // 传递座位ID信息（如果有选择座位）
          seatId: this.selectedTimeSlot.seatIds ? Number(this.selectedTimeSlot.seatIds.split(',')[0]) : null
        }
        
        console.log('用户信息:', this.userStore.userInfo)
        console.log('selectedTimeSlot:', this.selectedTimeSlot)
        console.log('发送预约数据:', bookingData)
        
        // 验证关键数据
        if (!bookingData.userId || !bookingData.showId || !bookingData.ticketId) {
          this.$message.error('数据验证失败，请刷新页面重试')
          this.orderLoading = false
          return
        }

        this.orderStore.createOrder(bookingData)
          .then(response => {
            console.log('预约成功响应:', response)
            this.$message({
              message: '预约成功！请按时到店使用机位。',
              type: 'success',
              duration: 3000
            })
            // response 是订单号（orderNo），使用现有的订单详情路由
            this.$router.push(`/order/detail/${response}`)
          })
          .catch(error => {
            console.error('预约失败详细信息：', {
              message: error.message,
              status: error.status,
              data: error.data,
              response: error.response,
              config: error.config
            })
            
            let errorMessage = '预约失败，请稍后重试'
            if (error.response?.data?.message) {
              errorMessage = error.response.data.message
            } else if (error.data?.message) {
              errorMessage = error.data.message
            } else if (error.message) {
              errorMessage = error.message
            }
            
            this.$message.error(errorMessage)
          })
          .finally(() => {
            this.orderLoading = false
          })
      })
    }
  }
}
</script>

<style scoped>
.booking-detail-container {
  min-height: 100vh;
  background: var(--ai-gradient-bg);
  padding: 20px;
  color: var(--ai-text-primary);
}

.back-section {
  margin-bottom: 20px;
}

.back-btn {
  color: var(--ai-nvidia-green) !important;
  font-size: 16px;
  padding: 8px 16px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(118, 185, 0, 0.1) !important;
  transform: translateX(-4px);
}

.booking-overview {
  margin-bottom: 30px;
}

.overview-card {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--ai-border-primary);
}

.card-header h2 {
  margin: 0;
  color: var(--ai-nvidia-green);
  font-size: 24px;
  font-weight: 700;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(118, 185, 0, 0.1);
  border: 1px solid var(--ai-nvidia-green);
  border-radius: 20px;
  color: var(--ai-nvidia-green);
  font-size: 14px;
  font-weight: 600;
}

.overview-content {
  display: flex;
  gap: 30px;
  align-items: center;
}

.seat-info {
  display: flex;
  gap: 16px;
  align-items: center;
  flex: 1;
}

.seat-image {
  width: 80px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid var(--ai-border-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.seat-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.seat-details h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: var(--ai-text-primary);
}

.seat-type {
  margin: 4px 0;
  color: var(--ai-nvidia-green);
  font-weight: 600;
  font-size: 14px;
}

.seat-location {
  margin: 4px 0;
  color: var(--ai-text-secondary);
  font-size: 14px;
}

.package-info {
  text-align: right;
}

.package-badge {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  margin-bottom: 8px;
}

.package-name {
  color: var(--ai-text-primary);
  font-size: 18px;
  font-weight: 600;
}

.package-price {
  color: #fbbf24;
  font-size: 24px;
  font-weight: 700;
  text-shadow: 0 0 5px rgba(251, 191, 36, 0.3);
}

.package-desc {
  color: var(--ai-text-secondary);
  font-size: 14px;
  margin: 0;
}

.booking-form-section {
  margin-bottom: 30px;
}

.form-card {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.form-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--ai-border-primary);
}

.form-header h3 {
  margin: 0 0 8px 0;
  color: var(--ai-nvidia-green);
  font-size: 20px;
  font-weight: 600;
}

.form-header p {
  margin: 0;
  color: var(--ai-text-secondary);
  font-size: 14px;
}

.form-row {
  display: flex;
  gap: 24px;
}

.form-item-half {
  flex: 1;
}

.booking-form :deep(.el-form-item__label) {
  color: var(--ai-text-secondary) !important;
  font-weight: 500;
}

.booking-form :deep(.el-input__inner),
.booking-form :deep(.el-textarea__inner) {
  background: rgba(10, 14, 23, 0.6) !important;
  border: 1px solid var(--ai-border-primary) !important;
  color: var(--ai-text-primary) !important;
  border-radius: 8px;
}

.booking-form :deep(.el-input__inner:focus),
.booking-form :deep(.el-textarea__inner:focus) {
  border-color: var(--ai-nvidia-green) !important;
  box-shadow: 0 0 0 2px rgba(118, 185, 0, 0.2) !important;
}

.quantity-input :deep(.el-input-number__input) {
  text-align: center;
  color: var(--ai-nvidia-green) !important;
  font-weight: 600;
}

.form-tip {
  display: block;
  margin-top: 4px;
  color: var(--ai-text-secondary);
  font-size: 12px;
}

.full-width {
  width: 100%;
}

.cost-section {
  margin-bottom: 30px;
}

.cost-card {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.cost-card h3 {
  margin: 0 0 16px 0;
  color: var(--ai-nvidia-green);
  font-size: 18px;
  font-weight: 600;
}

.cost-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cost-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.cost-item.total {
  border-top: 1px solid var(--ai-border-primary);
  padding-top: 16px;
  margin-top: 8px;
}

.cost-label {
  color: var(--ai-text-secondary);
  font-size: 14px;
}

.cost-value {
  color: var(--ai-text-primary);
  font-weight: 500;
}

.cost-item.total .cost-label,
.cost-item.total .cost-value {
  font-size: 18px;
  font-weight: 600;
}

.total-amount {
  color: #fbbf24 !important;
  font-size: 24px !important;
  text-shadow: 0 0 5px rgba(251, 191, 36, 0.3);
}

.action-section {
  display: flex;
  gap: 30px;
  align-items: flex-start;
}

.action-buttons {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.cancel-btn {
  background: rgba(100, 116, 139, 0.2) !important;
  border: 1px solid rgba(100, 116, 139, 0.4) !important;
  color: var(--ai-text-secondary) !important;
  border-radius: 25px;
  padding: 12px 32px;
  font-size: 16px;
  font-weight: 600;
}

.cancel-btn:hover {
  background: rgba(100, 116, 139, 0.3) !important;
  border-color: rgba(100, 116, 139, 0.6) !important;
}

.confirm-btn {
  background: linear-gradient(45deg, var(--ai-nvidia-green), #9dd33a) !important;
  border: none !important;
  color: white !important;
  border-radius: 25px;
  padding: 12px 32px;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(118, 185, 0, 0.4);
  transition: all 0.3s ease;
}

.confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(118, 185, 0, 0.6) !important;
}

.confirm-btn i {
  margin-right: 6px;
}

.booking-tips {
  flex: 1;
  background: rgba(22, 35, 68, 0.3);
  border: 1px solid var(--ai-border-primary);
  border-radius: 12px;
  padding: 20px;
}

.booking-tips h4 {
  margin: 0 0 12px 0;
  color: var(--ai-nvidia-green);
  font-size: 16px;
  font-weight: 600;
}

.booking-tips ul {
  margin: 0;
  padding-left: 20px;
  color: var(--ai-text-secondary);
}

.booking-tips li {
  margin: 6px 0;
  font-size: 14px;
  line-height: 1.5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .overview-content {
    flex-direction: column;
    gap: 20px;
  }
  
  .package-info {
    text-align: left;
  }
  
  .form-row {
    flex-direction: column;
    gap: 0;
  }
  
  .action-section {
    flex-direction: column;
    gap: 20px;
  }
  
  .action-buttons {
    width: 100%;
    justify-content: center;
  }
}
</style>