<template>
  <view class="register-container">
    <view class="register-content">
      <!-- 头部区域 -->
      <view class="header-section">
        <view class="back-btn" @tap="goBack">
          <text class="back-icon">←</text>
        </view>
        <text class="page-title">注册账号</text>
        <text class="page-desc">创建您的专属账号</text>
      </view>

      <!-- 注册表单 -->
      <view class="form-section">
        <view class="input-group">
          <view class="input-item">
            <view class="input-icon">👤</view>
            <input 
              type="text" 
              placeholder="请输入用户名" 
              v-model="registerForm.username"
              class="input-field"
              maxlength="20"
            />
          </view>
          
          <view class="input-item">
            <view class="input-icon">📱</view>
            <input 
              type="number" 
              placeholder="请输入手机号" 
              v-model="registerForm.phone"
              class="input-field"
              maxlength="11"
            />
          </view>
          
          <view class="input-item">
            <view class="input-icon">📧</view>
            <input 
              type="text" 
              placeholder="请输入邮箱（可选）" 
              v-model="registerForm.email"
              class="input-field"
            />
          </view>
          
          <view class="input-item">
            <view class="input-icon">🔒</view>
            <input 
              :type="showPassword ? 'text' : 'password'" 
              placeholder="请输入密码" 
              v-model="registerForm.password"
              class="input-field"
              maxlength="20"
            />
            <view class="password-toggle" @tap="togglePassword">
              {{ showPassword ? '👁️' : '👁️‍🗨️' }}
            </view>
          </view>
          
          <view class="input-item">
            <view class="input-icon">🔒</view>
            <input 
              :type="showConfirmPassword ? 'text' : 'password'" 
              placeholder="请确认密码" 
              v-model="registerForm.confirmPassword"
              class="input-field"
              maxlength="20"
            />
            <view class="password-toggle" @tap="toggleConfirmPassword">
              {{ showConfirmPassword ? '👁️' : '👁️‍🗨️' }}
            </view>
          </view>
        </view>

        <!-- 验证码区域 -->
        <view class="verification-section">
          <view class="input-item">
            <view class="input-icon">🔢</view>
            <input 
              type="number" 
              placeholder="请输入验证码" 
              v-model="registerForm.verificationCode"
              class="input-field"
              maxlength="6"
            />
            <button 
              class="send-code-btn" 
              :class="{ disabled: !canSendCode || countdown > 0 }"
              :disabled="!canSendCode || countdown > 0"
              @tap="sendVerificationCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </button>
          </view>
        </view>

        <!-- 协议同意 -->
        <view class="agreement-section">
          <view class="checkbox-item" @tap="toggleAgreement">
            <view class="checkbox" :class="{ checked: agreedToTerms }">
              <text v-if="agreedToTerms" class="check-icon">✓</text>
            </view>
            <text class="agreement-text">
              我已阅读并同意
              <text class="link" @tap.stop="viewTerms">《用户协议》</text>
              和
              <text class="link" @tap.stop="viewPrivacy">《隐私政策》</text>
            </text>
          </view>
        </view>

        <!-- 注册按钮 -->
        <button 
          class="register-btn" 
          :class="{ disabled: !canRegister }"
          :disabled="!canRegister || loading"
          @tap="handleRegister"
        >
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <!-- 登录链接 -->
        <view class="login-link-section">
          <text class="login-text">已有账号？</text>
          <text class="login-link" @tap="goToLogin">立即登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 响应式数据
const registerForm = ref({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: ''
})

const showPassword = ref(false)
const showConfirmPassword = ref(false)
const loading = ref(false)
const agreedToTerms = ref(false)
const countdown = ref(0)
let countdownTimer: any = null

// 计算属性
const canSendCode = computed(() => {
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(registerForm.value.phone)
})

const canRegister = computed(() => {
  return (
    registerForm.value.username.trim() &&
    registerForm.value.phone.trim() &&
    registerForm.value.password.trim() &&
    registerForm.value.confirmPassword.trim() &&
    registerForm.value.verificationCode.trim() &&
    agreedToTerms.value
  )
})

// 切换密码显示
const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const toggleConfirmPassword = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

// 切换协议同意状态
const toggleAgreement = () => {
  agreedToTerms.value = !agreedToTerms.value
}

// 发送验证码
const sendVerificationCode = async () => {
  if (!canSendCode.value || countdown.value > 0) return
  
  try {
    // 这里应该调用发送验证码的API
    // await api.sendVerificationCode({ phone: registerForm.value.phone })
    
    uni.showToast({
      title: '验证码已发送',
      icon: 'success'
    })
    
    // 开始倒计时
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer)
      }
    }, 1000)
    
  } catch (error: any) {
    console.error('发送验证码失败', error)
    uni.showToast({
      title: error.message || '发送失败',
      icon: 'none'
    })
  }
}

// 处理注册
const handleRegister = async () => {
  if (!canRegister.value || loading.value) return
  
  try {
    loading.value = true
    
    // 表单验证
    if (!registerForm.value.username.trim()) {
      uni.showToast({
        title: '请输入用户名',
        icon: 'none'
      })
      return
    }
    
    if (registerForm.value.username.length < 2) {
      uni.showToast({
        title: '用户名长度不能少于2位',
        icon: 'none'
      })
      return
    }
    
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(registerForm.value.phone)) {
      uni.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }
    
    if (registerForm.value.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.value.email)) {
      uni.showToast({
        title: '请输入正确的邮箱格式',
        icon: 'none'
      })
      return
    }
    
    if (registerForm.value.password.length < 6) {
      uni.showToast({
        title: '密码长度不能少于6位',
        icon: 'none'
      })
      return
    }
    
    if (registerForm.value.password !== registerForm.value.confirmPassword) {
      uni.showToast({
        title: '两次输入的密码不一致',
        icon: 'none'
      })
      return
    }
    
    if (!registerForm.value.verificationCode.trim()) {
      uni.showToast({
        title: '请输入验证码',
        icon: 'none'
      })
      return
    }
    
    if (!agreedToTerms.value) {
      uni.showToast({
        title: '请同意用户协议和隐私政策',
        icon: 'none'
      })
      return
    }
    
    // 调用注册接口
    await userStore.register({
      username: registerForm.value.username.trim(),
      phone: registerForm.value.phone.trim(),
      email: registerForm.value.email.trim(),
      password: registerForm.value.password.trim(),
      verificationCode: registerForm.value.verificationCode.trim()
    })
    
    uni.showToast({
      title: '注册成功',
      icon: 'success'
    })
    
    // 延迟跳转到登录页面
    setTimeout(() => {
      uni.redirectTo({
        url: '/pages/login/index'
      })
    }, 1500)
    
  } catch (error: any) {
    console.error('注册失败', error)
    uni.showToast({
      title: error.message || '注册失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 返回上一页
const goBack = () => {
  uni.navigateBack()
}

// 跳转到登录页面
const goToLogin = () => {
  uni.redirectTo({
    url: '/pages/login/index'
  })
}

// 查看用户协议
const viewTerms = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 查看隐私政策
const viewPrivacy = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 页面卸载时清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx;
}

.register-content {
  width: 100%;
  max-width: 600rpx;
  margin: 0 auto;
}

/* 头部区域 */
.header-section {
  text-align: center;
  margin-bottom: 60rpx;
  position: relative;
}

.back-btn {
  position: absolute;
  left: 0;
  top: 0;
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  backdrop-filter: blur(10rpx);
}

.back-icon {
  font-size: 40rpx;
  color: white;
  font-weight: bold;
}

.page-title {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: white;
  margin-bottom: 16rpx;
  text-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.3);
}

.page-desc {
  display: block;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 表单区域 */
.form-section {
  background: white;
  border-radius: 30rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.1);
}

.input-group {
  margin-bottom: 40rpx;
}

.input-item {
  position: relative;
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border-radius: 50rpx;
  padding: 0 30rpx;
  margin-bottom: 30rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s;
}

.input-item:focus-within {
  border-color: #007aff;
  background: white;
  box-shadow: 0 0 0 6rpx rgba(0, 122, 255, 0.1);
}

.input-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
  color: #999;
}

.input-field {
  flex: 1;
  height: 100rpx;
  font-size: 32rpx;
  color: #333;
  background: transparent;
}

.password-toggle {
  font-size: 36rpx;
  color: #999;
  padding: 10rpx;
}

/* 验证码区域 */
.verification-section {
  margin-bottom: 40rpx;
}

.send-code-btn {
  background: #007aff;
  color: white;
  border: none;
  border-radius: 30rpx;
  padding: 16rpx 32rpx;
  font-size: 26rpx;
  white-space: nowrap;
}

.send-code-btn.disabled {
  background: #d9d9d9;
  color: #999;
}

/* 协议区域 */
.agreement-section {
  margin-bottom: 40rpx;
}

.checkbox-item {
  display: flex;
  align-items: flex-start;
}

.checkbox {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid #d9d9d9;
  border-radius: 6rpx;
  margin-right: 16rpx;
  margin-top: 4rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.checkbox.checked {
  background: #007aff;
  border-color: #007aff;
}

.check-icon {
  color: white;
  font-size: 24rpx;
  font-weight: bold;
}

.agreement-text {
  flex: 1;
  font-size: 28rpx;
  color: #666;
  line-height: 1.5;
}

.link {
  color: #007aff;
}

.register-btn {
  width: 100%;
  height: 100rpx;
  background: linear-gradient(45deg, #007aff, #0056cc);
  color: white;
  border: none;
  border-radius: 50rpx;
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 40rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 122, 255, 0.3);
  transition: all 0.3s;
}

.register-btn:active {
  transform: translateY(2rpx);
  box-shadow: 0 5rpx 15rpx rgba(0, 122, 255, 0.3);
}

.register-btn.disabled {
  background: #d9d9d9;
  color: #999;
  box-shadow: none;
}

/* 登录链接 */
.login-link-section {
  text-align: center;
}

.login-text {
  font-size: 28rpx;
  color: #666;
  margin-right: 16rpx;
}

.login-link {
  font-size: 28rpx;
  color: #007aff;
}
</style>