<template>
  <el-dialog
    v-model="visible"
    title="选择支付方式"
    width="600px"
    class="payment-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose">
    
    <div class="payment-container">
      <!-- 订单信息 -->
      <div class="order-summary">
        <h3>订单信息</h3>
        <div class="summary-item">
          <span class="label">订单号：</span>
          <span class="value">{{ orderInfo.orderNo }}</span>
        </div>
        <div class="summary-item">
          <span class="label">支付金额：</span>
          <span class="amount">¥{{ orderInfo.payAmount || orderInfo.totalAmount }}</span>
        </div>
      </div>

      <!-- 支付方式选择 -->
      <div class="payment-methods" v-if="!selectedMethod">
        <h3>请选择支付方式</h3>
        <div class="method-buttons">
          <div class="method-card" @click="selectPaymentMethod('alipay')">
            <div class="method-icon">
              <img src="/images/alipay.jpg" alt="支付宝" />
            </div>
            <div class="method-name">支付宝支付</div>
            <div class="method-desc">安全快捷，支持花呗分期</div>
          </div>
          
          <div class="method-card" @click="selectPaymentMethod('wechat')">
            <div class="method-icon">
              <img src="/images/paywechat.jpg" alt="微信支付" />
            </div>
            <div class="method-name">微信支付</div>
            <div class="method-desc">微信扫码，便捷支付</div>
          </div>
        </div>
      </div>

      <!-- 支付二维码 -->
      <div class="qr-payment" v-if="selectedMethod">
        <div class="payment-header">
          <el-button 
            type="text" 
            @click="goBack" 
            class="back-btn">
            <i class="el-icon-arrow-left"></i> 返回选择
          </el-button>
          <h3>{{ getPaymentTitle() }}</h3>
        </div>
        
        <div class="qr-container">
          <div class="qr-code">
            <img :src="getQRCodeImage()" :alt="getPaymentTitle()" />
          </div>
          <div class="qr-tips">
            <p class="tip-title">请使用{{ getPaymentAppName() }}扫描二维码完成支付</p>
            <p class="amount-display">支付金额：<span class="pay-amount">¥{{ orderInfo.payAmount || orderInfo.totalAmount }}</span></p>
            <div class="payment-tips">
              <div class="tip-item">
                <i class="el-icon-info"></i>
                <span>请在{{ getPaymentAppName() }}中确认支付金额</span>
              </div>
              <div class="tip-item">
                <i class="el-icon-warning"></i>
                <span>支付完成后，系统将自动检测到支付状态</span>
              </div>
              <div class="tip-item" v-if="selectedMethod === 'alipay'">
                <i class="el-icon-star-on"></i>
                <span>支持花呗分期，让支付更轻松</span>
              </div>
              <div class="tip-item" v-if="selectedMethod === 'wechat'">
                <i class="el-icon-star-on"></i>
                <span>微信支付，安全便捷</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 支付状态检测 -->
        <div class="payment-status">
          <div class="status-indicator" :class="paymentStatusClass">
            <i :class="paymentStatusIcon"></i>
            <span>{{ paymentStatusText }}</span>
          </div>
          <div class="countdown-timer" v-if="countdown > 0">
            <i class="el-icon-time"></i>
            请在 {{ formatCountdown(countdown) }} 内完成支付
          </div>
        </div>

        <!-- 支付结果 -->
        <div class="payment-result" v-if="paymentResult">
          <div class="result-icon" :class="paymentResult === 'success' ? 'success' : 'failed'">
            <i :class="paymentResult === 'success' ? 'el-icon-success' : 'el-icon-error'"></i>
          </div>
          <div class="result-text">
            {{ paymentResult === 'success' ? '支付成功！' : '支付失败，请重试' }}
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" v-if="!selectedMethod">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmPayment" 
          v-if="selectedMethod && !paymentResult"
          :loading="checking">
          我已完成支付
        </el-button>
        <el-button 
          type="primary" 
          @click="handlePaymentSuccess" 
          v-if="paymentResult === 'success'">
          确定
        </el-button>
        <el-button 
          @click="retryPayment" 
          v-if="paymentResult === 'failed'">
          重新支付
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: 'PaymentDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    orderInfo: {
      type: Object,
      required: true
    }
  },
  emits: ['update:modelValue', 'payment-success', 'payment-failed'],
  data() {
    return {
      selectedMethod: null,
      paymentResult: null,
      checking: false,
      countdown: 300, // 5分钟倒计时
      timer: null,
      checkTimer: null
    }
  },
  computed: {
    visible: {
      get() {
        return this.modelValue
      },
      set(value) {
        this.$emit('update:modelValue', value)
      }
    },
    paymentStatusClass() {
      if (this.paymentResult === 'success') return 'success'
      if (this.paymentResult === 'failed') return 'failed'
      return 'waiting'
    },
    paymentStatusIcon() {
      if (this.paymentResult === 'success') return 'el-icon-success'
      if (this.paymentResult === 'failed') return 'el-icon-error'
      return 'el-icon-loading'
    },
    paymentStatusText() {
      if (this.paymentResult === 'success') return '支付成功'
      if (this.paymentResult === 'failed') return '支付失败'
      return '等待支付中...'
    }
  },
  watch: {
    modelValue(newVal) {
      if (newVal) {
        this.resetPayment()
        this.startCountdown()
      } else {
        this.clearTimers()
      }
    }
  },
  methods: {
    selectPaymentMethod(method) {
      this.selectedMethod = method
      this.startPaymentCheck()
    },
    
    goBack() {
      this.selectedMethod = null
      this.paymentResult = null
      this.clearTimers()
    },
    
    getPaymentTitle() {
      return this.selectedMethod === 'alipay' ? '支付宝支付' : '微信支付'
    },
    
    getPaymentAppName() {
      return this.selectedMethod === 'alipay' ? '支付宝' : '微信'
    },
    
    getQRCodeImage() {
      return this.selectedMethod === 'alipay' 
        ? '/images/alipay.jpg' 
        : '/images/paywechat.jpg'
    },
    
    startCountdown() {
      this.countdown = 300
      this.timer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          this.clearTimers()
          this.paymentResult = 'failed'
        }
      }, 1000)
    },
    
    startPaymentCheck() {
      // 自动检测支付状态
      this.checkTimer = setInterval(async () => {
        try {
          await this.checkPaymentStatus()
        } catch (error) {
          console.error('检查支付状态失败:', error)
        }
      }, 3000) // 每3秒检查一次
    },
    
    async checkPaymentStatus() {
      // 这里应该调用后端API检查支付状态
      // 现在使用模拟逻辑，实际项目中应该调用真实的API
      try {
        // 模拟API调用
        const response = await new Promise((resolve) => {
          setTimeout(() => {
            // 模拟不同的支付状态，提高成功率
            const random = Math.random()
            if (random < 0.15) { // 15%概率支付成功（提高成功率）
              resolve({ status: 'success' })
            } else if (random < 0.18) { // 3%概率支付失败
              resolve({ status: 'failed' })
            } else {
              resolve({ status: 'pending' }) // 其他情况为等待中
            }
          }, 100)
        })
        
        if (response.status === 'success') {
          this.paymentResult = 'success'
          this.clearTimers()
        } else if (response.status === 'failed') {
          this.paymentResult = 'failed'
          this.clearTimers()
        }
        // pending状态继续等待
      } catch (error) {
        console.error('支付状态检查失败:', error)
      }
    },
    
    formatCountdown(seconds) {
      const minutes = Math.floor(seconds / 60)
      const remainingSeconds = seconds % 60
      return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
    },
    
    async confirmPayment() {
      this.checking = true
      try {
        // 模拟支付确认过程
        this.$message({
          message: '正在确认支付状态，请稍候...',
          type: 'info',
          duration: 2000
        })
        
        // 模拟网络延迟
        await new Promise(resolve => setTimeout(resolve, 1500))
        
        // 用户点击"我已完成支付"后，直接确认支付成功
        this.paymentResult = 'success'
        this.clearTimers()
        
        this.$message({
          message: '支付确认成功！',
          type: 'success',
          duration: 2000
        })
        
      } catch (error) {
        console.error('确认支付失败:', error)
        this.$message({
          message: '支付确认失败，请重试',
          type: 'error',
          duration: 3000
        })
      } finally {
        this.checking = false
      }
    },
    
    handlePaymentSuccess() {
      this.$emit('payment-success', {
        orderNo: this.orderInfo.orderNo,
        paymentMethod: this.selectedMethod,
        paymentType: this.selectedMethod === 'alipay' ? 1 : 2
      })
      this.handleClose()
    },
    
    retryPayment() {
      this.paymentResult = null
      this.selectedMethod = null
      this.startCountdown()
    },
    
    handleClose() {
      this.clearTimers()
      this.resetPayment()
      this.visible = false
    },
    
    resetPayment() {
      this.selectedMethod = null
      this.paymentResult = null
      this.checking = false
      this.countdown = 300
    },
    
    clearTimers() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
      if (this.checkTimer) {
        clearInterval(this.checkTimer)
        this.checkTimer = null
      }
    }
  },
  beforeUnmount() {
    this.clearTimers()
  }
}
</script>

<style scoped>
.payment-dialog :deep(.el-dialog) {
  background: var(--ai-gradient-card);
  border: 1px solid var(--ai-border-primary);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.payment-dialog :deep(.el-dialog__header) {
  background: transparent;
  border-bottom: 1px solid var(--ai-border-primary);
  padding: 20px 24px;
}

.payment-dialog :deep(.el-dialog__title) {
  color: var(--ai-nvidia-green);
  font-size: 20px;
  font-weight: 600;
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

.payment-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.payment-container {
  color: var(--ai-text-primary);
}

/* 订单信息 */
.order-summary {
  background: rgba(118, 185, 0, 0.05);
  border: 1px solid var(--ai-border-primary);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.order-summary h3 {
  margin: 0 0 16px 0;
  color: var(--ai-nvidia-green);
  font-size: 16px;
  font-weight: 600;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.summary-item:last-child {
  margin-bottom: 0;
}

.summary-item .label {
  color: var(--ai-text-secondary);
  font-size: 14px;
}

.summary-item .value {
  color: var(--ai-text-primary);
  font-weight: 500;
}

.summary-item .amount {
  color: var(--ai-nvidia-green);
  font-size: 18px;
  font-weight: 700;
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

/* 支付方式选择 */
.payment-methods h3 {
  margin: 0 0 20px 0;
  color: var(--ai-text-primary);
  font-size: 18px;
  font-weight: 600;
  text-align: center;
}

.method-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.method-card {
  flex: 1;
  max-width: 200px;
  background: var(--ai-gradient-card);
  border: 2px solid var(--ai-border-primary);
  border-radius: 16px;
  padding: 24px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.method-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(118, 185, 0, 0.1), transparent);
  transition: left 0.5s ease;
}

.method-card:hover {
  border-color: var(--ai-nvidia-green);
  transform: translateY(-4px);
  box-shadow: 0 8px 25px var(--ai-shadow-green);
}

.method-card:hover::before {
  left: 100%;
}

.method-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 16px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}

.method-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.method-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ai-text-primary);
  margin-bottom: 8px;
}

.method-desc {
  font-size: 12px;
  color: var(--ai-text-secondary);
  line-height: 1.4;
}

/* 二维码支付 */
.payment-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
}

.back-btn {
  color: var(--ai-text-secondary) !important;
  font-size: 14px;
  padding: 0;
}

.back-btn:hover {
  color: var(--ai-nvidia-green) !important;
}

.payment-header h3 {
  flex: 1;
  text-align: center;
  margin: 0;
  color: var(--ai-text-primary);
  font-size: 18px;
  font-weight: 600;
}

.qr-container {
  display: flex;
  gap: 32px;
  align-items: center;
  margin-bottom: 24px;
}

.qr-code {
  flex-shrink: 0;
  width: 200px;
  height: 200px;
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-code img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-tips {
  flex: 1;
}

.tip-title {
  font-size: 16px;
  color: var(--ai-text-primary);
  margin-bottom: 16px;
  line-height: 1.5;
}

.amount-display {
  font-size: 14px;
  color: var(--ai-text-secondary);
  margin: 0;
}

.pay-amount {
  color: var(--ai-nvidia-green);
  font-size: 20px;
  font-weight: 700;
  text-shadow: 0 0 5px var(--ai-shadow-green);
}

.payment-tips {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--ai-text-secondary);
  line-height: 1.4;
}

.tip-item i {
  color: var(--ai-nvidia-green);
  font-size: 14px;
  flex-shrink: 0;
}

/* 支付状态 */
.payment-status {
  text-align: center;
  margin-bottom: 24px;
}

.status-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
}

.status-indicator.waiting {
  color: #ffd700;
}

.status-indicator.waiting i {
  animation: spin 1s linear infinite;
}

.status-indicator.success {
  color: var(--ai-nvidia-green);
}

.status-indicator.failed {
  color: #ff4757;
}

.countdown-timer {
  color: var(--ai-text-secondary);
  font-size: 14px;
}

.countdown-timer i {
  margin-right: 4px;
}

/* 支付结果 */
.payment-result {
  text-align: center;
  margin: 24px 0;
}

.result-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.result-icon.success {
  color: var(--ai-nvidia-green);
}

.result-icon.failed {
  color: #ff4757;
}

.result-text {
  font-size: 18px;
  font-weight: 600;
}

.result-icon.success + .result-text {
  color: var(--ai-nvidia-green);
}

.result-icon.failed + .result-text {
  color: #ff4757;
}

/* 底部按钮 */
.dialog-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--ai-border-primary);
  text-align: right;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .method-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .method-card {
    max-width: 300px;
  }
  
  .qr-container {
    flex-direction: column;
    text-align: center;
    gap: 20px;
  }
  
  .qr-code {
    width: 180px;
    height: 180px;
  }
}
</style>