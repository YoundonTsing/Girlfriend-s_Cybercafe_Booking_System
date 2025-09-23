import router from './router'
import { useUserStore } from './stores/user'
import { ElMessage } from 'element-plus'
import { isTokenExpired } from './utils/auth'

// 白名单路由（不需要登录验证的路由）
const whiteList = ['/login', '/register']

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  if (token) {
    // 检查Token是否过期
    if (isTokenExpired()) {
      userStore.resetToken()
      ElMessage.error('登录状态已过期，请重新登录')
      next(`/login?redirect=${to.path}`)
      return
    }

    // 已登录且Token有效
    if (to.path === '/login') {
      // 如果已登录，访问登录页则重定向到首页
      next({ path: '/' })
    } else {
      // 检查是否有用户信息
      if (!userStore.userInfo?.userId) {
        try {
          // 获取用户信息
          await userStore.getInfo()
          next()
        } catch (error) {
          // 获取用户信息失败，清除token并跳转到登录页
          userStore.resetToken()
          ElMessage.error('登录状态已过期，请重新登录')
          next(`/login?redirect=${to.path}`)
        }
      } else {
        next()
      }
    }
  } else {
    // 未登录
    if (whiteList.includes(to.path)) {
      // 在白名单中，直接通过
      next()
    } else {
      // 不在白名单中，跳转到登录页
      next(`/login?redirect=${to.path}`)
    }
  }
})

// 全局后置守卫
router.afterEach((to) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 票务系统` : '票务系统'
})