<template>
  <!-- uni-app 不需要根节点 -->
</template>

<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onLaunch(async () => {
  console.log('App Launch')
  
  // 初始化token（确保store和本地存储同步）
  userStore.initToken()
  
  // 如果有token，尝试获取用户信息
  if (userStore.token) {
    try {
      console.log('检测到token，开始获取用户信息')
      await userStore.getInfo()
      console.log('用户信息获取成功')
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // 如果获取用户信息失败，清除无效token
      userStore.resetToken()
    }
  }
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style>
/*每个页面公共css */
page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  background-color: #f8f8f8;
}
</style>