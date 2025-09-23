import { defineStore } from 'pinia'
import { login, logout, getUserInfo } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import type { User, UserLoginForm } from '@/types'

interface UserState {
  token: string
  userId: string
  username: string
  nickname: string
  phone: string
  email: string
}

export const useUserStore = defineStore('user', {
  state: (): UserState => {
    // 从localStorage恢复用户信息
    const savedUserInfo = localStorage.getItem('userInfo')
    let userInfo = {
      userId: '',
      username: '',
      nickname: '',
      phone: '',
      email: ''
    }
    
    if (savedUserInfo) {
      try {
        userInfo = JSON.parse(savedUserInfo)
      } catch (error) {
        console.error('解析用户信息失败:', error)
        localStorage.removeItem('userInfo')
      }
    }
    
    return {
      token: getToken() || '',
      ...userInfo
    }
  },

  getters: {
    isLoggedIn: (state): boolean => !!state.token,
    userInfo: (state): Partial<User> => ({
      id: state.userId, // 映射到User类型的id字段
      userId: state.userId, // 保持兼容性
      username: state.username,
      nickname: state.nickname,
      phone: state.phone,
      email: state.email
    })
  },

  actions: {
    // 用户登录
    async login(userInfo: UserLoginForm): Promise<void> {
      const { username, password } = userInfo
      try {
        const response = await login({ 
          username: username.trim(), 
          password: password 
        })
        const { data } = response
        this.token = data.token
        setToken(data.token)
      } catch (error) {
        throw error
      }
    },

    // 获取用户信息
    async getInfo(): Promise<User> {
      try {
        console.log('开始获取用户信息，当前token:', this.token)
        const response = await getUserInfo()
        console.log('getUserInfo API响应:', response)
        const { data } = response
        
        if (!data) {
          console.error('API返回的data为空')
          throw new Error('验证失败，请重新登录')
        }

        console.log('解析用户数据:', data)
        const { id, username, nickname, phone, email } = data
        
        this.userId = id.toString() // 后端返回的是id字段，转换为字符串
        this.username = username
        this.nickname = nickname
        this.phone = phone
        this.email = email
        
        // 将用户信息存储到localStorage，以便页面刷新后恢复
        localStorage.setItem('userInfo', JSON.stringify({
          userId: this.userId,
          username: this.username,
          nickname: this.nickname,
          phone: this.phone,
          email: this.email
        }))
        
        console.log('用户信息更新完成，userId:', this.userId)
        return data
      } catch (error) {
        console.error('获取用户信息异常:', error)
        throw error
      }
    },

    // 用户登出
    async logout(): Promise<void> {
      try {
        await logout()
        this.resetToken()
      } catch (error) {
        throw error
      }
    },

    // 重置token
    resetToken(): void {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.nickname = ''
      this.phone = ''
      this.email = ''
      removeToken()
      localStorage.removeItem('userInfo')
    }
  }
})