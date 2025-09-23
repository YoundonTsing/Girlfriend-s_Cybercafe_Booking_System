import { wxApiClient } from '@/utils/wxApiClient'
import type { 
  User, 
  UserLoginForm, 
  UserRegisterForm, 
  LoginRequest,
  RegisterRequest,
  UpdateUserRequest,
  ChangePasswordRequest,
  ApiResponse 
} from '@/types'

// 用户登录
export const login = (data: LoginRequest | UserLoginForm): Promise<ApiResponse<{ token: string; userInfo: User }>> => {
  return wxApiClient.post('/api/user/login', data)
}

// 用户注册
export const register = (data: RegisterRequest | UserRegisterForm): Promise<ApiResponse<void>> => {
  return wxApiClient.post('/api/user/register', data)
}

// 获取用户信息（当前登录用户）
export const getUserInfo = (): Promise<ApiResponse<User>> => {
  return wxApiClient.get('/api/user/info')
}

// 获取当前登录用户信息
export const getCurrentUserInfo = (): Promise<ApiResponse<User>> => {
  return wxApiClient.get('/api/user/info/current')
}

// 获取指定用户信息
export const getUserInfoById = (userId: string | number): Promise<ApiResponse<User>> => {
  return wxApiClient.get(`/api/user/info/${userId}`)
}

// 更新用户信息
export const updateUserInfo = (data: UpdateUserRequest): Promise<ApiResponse<void>> => {
  return wxApiClient.put('/api/user/info', data)
}

// 修改密码
export const changePassword = (data: {
  userId: number
  oldPassword: string
  newPassword: string
}): Promise<ApiResponse<void>> => {
  const searchParams = new URLSearchParams()
  searchParams.append('userId', data.userId.toString())
  searchParams.append('oldPassword', data.oldPassword)
  searchParams.append('newPassword', data.newPassword)
  
  return wxApiClient.put(`/api/user/password?${searchParams.toString()}`)
}

// 用户登出
export const logout = (): Promise<ApiResponse<void>> => {
  return wxApiClient.post('/api/user/logout')
}