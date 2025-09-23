<template>
  <view class="login-container">
    <view class="login-content">
      <!-- Logo区域 -->
      <view class="logo-section">
        <image src="/static/images/logo.png" class="logo" mode="aspectFit" />
        <text class="app-name">自由点网咖预约系统</text>
        <text class="app-desc">便捷的网咖机位预约服务</text>
      </view>

      <!-- 登录表单 -->
      <view class="form-section">
        <view class="input-group">
          <view class="input-item">
            <view class="input-icon">👤</view>
            <input 
              type="text" 
              placeholder="请输入用户名/手机号" 
              v-model="loginForm.username"
              class="input-field"
              maxlength="20"
            />
          </view>
          <view class="input-item">
            <view class="input-icon">🔒</view>
            <input 
              :type="showPassword ? 'text' : 'password'" 
              placeholder="请输入密码" 
              v-model="loginForm.password"
              class="input-field"
              maxlength="20"
            />
            <view class="password-toggle" @tap="togglePassword">
              {{ showPassword ? '👁️' : '👁️‍🗨️' }}
            </view>
          </view>
        </view>

        <!-- 登录按钮 -->
        <button 
          class="login-btn" 
          :class="{ disabled: !canLogin }"
          :disabled="!canLogin || loading"
          @tap="handleLogin"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <!-- 其他操作 -->
        <view class="other-actions">
          <text class="forgot-password" @tap="forgotPassword">忘记密码？</text>
          <text class="register-link" @tap="goToRegister">注册账号</text>
        </view>
      </view>

      <!-- 第三方登录 -->
      <view class="third-party-section">
        <view class="divider">
          <text class="divider-text">其他登录方式</text>
        </view>
        <view class="third-party-buttons">
          <button class="wechat-login-btn" @tap="wechatLogin">
            <text class="wechat-icon">💬</text>
            <text>微信登录</text>
          </button>
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
const loginForm = ref({
  username: '',
  password: ''
})
const showPassword = ref(false)
const loading = ref(false)

// 计算属性
const canLogin = computed(() => {
  return loginForm.value.username.trim() && loginForm.value.password.trim()
})

// 切换密码显示
const togglePassword = () => {
  showPassword.value = !showPassword.value
}

// 处理登录
const handleLogin = async () => {
  if (!canLogin.value || loading.value) return
  
  try {
    loading.value = true
    
    // 表单验证
    if (!loginForm.value.username.trim()) {
      uni.showToast({
        title: '请输入用户名',
        icon: 'none'
      })
      return
    }
    
    if (!loginForm.value.password.trim()) {
      uni.showToast({
        title: '请输入密码',
        icon: 'none'
      })
      return
    }
    
    if (loginForm.value.password.length < 6) {
      uni.showToast({
        title: '密码长度不能少于6位',
        icon: 'none'
      })
      return
    }
    
    // 调用登录接口
    await userStore.login({
      username: loginForm.value.username.trim(),
      password: loginForm.value.password.trim()
    })
    
    uni.showToast({
      title: '登录成功',
      icon: 'success'
    })
    
    // 延迟跳转，让用户看到成功提示
    setTimeout(() => {
      // 返回上一页或跳转到首页
      const pages = getCurrentPages()
      if (pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.switchTab({
          url: '/pages/index/index'
        })
      }
    }, 1500)
    
  } catch (error: any) {
    console.error('登录失败', error)
    uni.showToast({
      title: error.message || '登录失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 忘记密码
const forgotPassword = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 跳转到注册页面
const goToRegister = () => {
  uni.navigateTo({
    url: '/pages/register/index'
  })
}

// 微信登录
const wechatLogin = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.login-content {
  width: 100%;
  max-width: 600rpx;
}

/* Logo区域 */
.logo-section {
  text-align: center;
  margin-bottom: 80rpx;
}

.logo {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 30rpx;
}

.app-name {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: white;
  margin-bottom: 16rpx;
  text-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.3);
}

.app-desc {
  display: block;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 表单区域 */
.form-section {
  background: white;
  border-radius: 30rpx;
  padding: 60rpx 40rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.1);
}

.input-group {
  margin-bottom: 60rpx;
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

.login-btn {
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

.login-btn:active {
  transform: translateY(2rpx);
  box-shadow: 0 5rpx 15rpx rgba(0, 122, 255, 0.3);
}

.login-btn.disabled {
  background: #d9d9d9;
  color: #999;
  box-shadow: none;
}

.other-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-password,
.register-link {
  font-size: 28rpx;
  color: #007aff;
}

/* 第三方登录 */
.third-party-section {
  text-align: center;
}

.divider {
  position: relative;
  margin-bottom: 40rpx;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1rpx;
  background: rgba(255, 255, 255, 0.3);
}

.divider-text {
  display: inline-block;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: rgba(255, 255, 255, 0.8);
  font-size: 26rpx;
  padding: 0 30rpx;
}

.third-party-buttons {
  display: flex;
  justify-content: center;
}

.wechat-login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #07c160;
  color: white;
  border: none;
  border-radius: 50rpx;
  padding: 24rpx 60rpx;
  font-size: 28rpx;
  box-shadow: 0 10rpx 30rpx rgba(7, 193, 96, 0.3);
}

.wechat-icon {
  font-size: 36rpx;
  margin-right: 16rpx;
}
</style>