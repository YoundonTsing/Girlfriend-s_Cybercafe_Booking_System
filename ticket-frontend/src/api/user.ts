import { webApiClient } from '@/utils/webApiClient'
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
export function login(data: UserLoginForm | LoginRequest): Promise<ApiResponse<{ token: string }>> {
  return webApiClient.post('/user/login', data)
}

// 用户注册
export function register(data: UserRegisterForm | RegisterRequest): Promise<ApiResponse<User>> {
  return webApiClient.post('/user/register', data)
}

// 获取用户信息
export function getUserInfo(): Promise<ApiResponse<User>> {
  return webApiClient.get('/user/info')
}

// 用户登出
export function logout(): Promise<ApiResponse<void>> {
  return webApiClient.post('/user/logout')
}

// 修改用户信息
export function updateUserInfo(data: Partial<User> | UpdateUserRequest): Promise<ApiResponse<User>> {
  return webApiClient.put('/user/info', data)
}

// 修改密码
export function changePassword(data: ChangePasswordRequest | { oldPassword: string; newPassword: string }): Promise<ApiResponse<void>> {
  return webApiClient.put('/user/password', data)
}

// 获取用户统计信息
export function getUserStats(): Promise<ApiResponse<any>> {
  return webApiClient.get('/user/stats')
}

// 刷新Token
export function refreshToken(): Promise<ApiResponse<{ token: string }>> {
  return webApiClient.post('/user/auth/refresh-token')
}