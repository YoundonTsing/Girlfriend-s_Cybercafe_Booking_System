/**
 * 用户认证相关的业务逻辑组合函数
 * 平台无关，可在Web和小程序中复用
 */
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores'
import type { UserLoginForm, UserRegisterForm } from '@/types'

export function useUserAuth() {
  const userStore = useUserStore()
  
  // 响应式状态
  const loading = ref(false)
  const error = ref<string | null>(null)
  
  // 计算属性
  const isLoggedIn = computed(() => userStore.isLoggedIn)
  const userInfo = computed(() => userStore.userInfo)
  
  // 登录
  const login = async (loginForm: UserLoginForm) => {
    loading.value = true
    error.value = null
    
    try {
      await userStore.login(loginForm)
      await userStore.getInfo()
      return true
    } catch (err: any) {
      error.value = err.message || '登录失败'
      return false
    } finally {
      loading.value = false
    }
  }
  
  // 注册
  const register = async (registerForm: UserRegisterForm) => {
    loading.value = true
    error.value = null
    
    try {
      await userStore.register(registerForm)
      return true
    } catch (err: any) {
      error.value = err.message || '注册失败'
      return false
    } finally {
      loading.value = false
    }
  }
  
  // 登出
  const logout = async () => {
    loading.value = true
    error.value = null
    
    try {
      await userStore.logout()
      return true
    } catch (err: any) {
      error.value = err.message || '登出失败'
      return false
    } finally {
      loading.value = false
    }
  }
  
  // 清除错误
  const clearError = () => {
    error.value = null
  }
  
  return {
    // 状态
    loading,
    error,
    isLoggedIn,
    userInfo,
    
    // 方法
    login,
    register,
    logout,
    clearError
  }
}