const TokenKey = 'ticket_token'
const TokenTimestampKey = 'ticket_token_timestamp'

export function getToken(): string | null {
  return localStorage.getItem(TokenKey)
}

export function setToken(token: string): void {
  localStorage.setItem(TokenKey, token)
  // 记录Token设置时间
  localStorage.setItem(TokenTimestampKey, Date.now().toString())
}

export function removeToken(): void {
  localStorage.removeItem(TokenKey)
  localStorage.removeItem(TokenTimestampKey)
}

// 检查Token是否即将过期（剩余时间少于5分钟）
export function isTokenExpiringSoon(): boolean {
  const timestamp = localStorage.getItem(TokenTimestampKey)
  if (!timestamp) {
    return true
  }
  
  const tokenTime = parseInt(timestamp)
  const now = Date.now()
  const elapsed = now - tokenTime
  
  // JWT有效期为2小时（7200秒），如果剩余时间少于5分钟，认为即将过期
  const tokenLifetime = 2 * 60 * 60 * 1000 // 2小时毫秒数
  const warningTime = 5 * 60 * 1000 // 5分钟毫秒数
  
  return elapsed > (tokenLifetime - warningTime)
}

// 检查Token是否已过期
export function isTokenExpired(): boolean {
  const timestamp = localStorage.getItem(TokenTimestampKey)
  if (!timestamp) {
    return true
  }
  
  const tokenTime = parseInt(timestamp)
  const now = Date.now()
  const elapsed = now - tokenTime
  
  // JWT有效期为2小时
  const tokenLifetime = 2 * 60 * 60 * 1000
  
  return elapsed > tokenLifetime
}