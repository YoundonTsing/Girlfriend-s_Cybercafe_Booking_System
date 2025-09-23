<template>
  <div class="app-wrapper">
    <!-- 头部导航 -->
    <div class="navbar">
      <div class="logo">
        <router-link to="/">
          <h1 class="mobile-title">自由点网咖预约系统</h1>
        </router-link>
      </div>
      
      <!-- 桌面端菜单 -->
      <div class="menu desktop-only">
        <el-menu mode="horizontal" :router="true" :default-active="activeMenu" background-color="transparent" text-color="#ffffff" active-text-color="#76b900">
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item index="/show/list">机位列表</el-menu-item>
          <el-menu-item index="/order/list">我的预约</el-menu-item>
        </el-menu>
      </div>
      
      <!-- 移动端汉堡菜单按钮 -->
      <div class="mobile-menu-button mobile-only">
        <el-button type="text" @click="toggleMobileMenu" class="hamburger-button">
          <i :class="showMobileMenu ? 'el-icon-close' : 'el-icon-menu'"></i>
        </el-button>
      </div>
      
      <div class="right-menu desktop-only">
        <template v-if="!userInfo">
          <router-link to="/login">
            <el-button type="text" class="touch-button">登录</el-button>
          </router-link>
          <router-link to="/register">
            <el-button type="text" class="touch-button">注册</el-button>
          </router-link>
        </template>
        <template v-else>
          <el-dropdown trigger="click" @command="handleCommand" class="user-dropdown">
            <span class="el-dropdown-link">
              <el-avatar :size="32" class="user-avatar">
                {{ (userInfo.nickname || userInfo.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ userInfo.nickname || userInfo.username }}</span>
              <i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <i class="el-icon-user"></i>
                  个人信息
                </el-dropdown-item>
                <el-dropdown-item command="orders">
                  <i class="el-icon-tickets"></i>
                  我的订单
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <i class="el-icon-setting"></i>
                  系统设置
                </el-dropdown-item>
                <el-dropdown-item divided></el-dropdown-item>
                <el-dropdown-item command="help">
                  <i class="el-icon-question"></i>
                  帮助中心
                </el-dropdown-item>
                <el-dropdown-item command="feedback">
                  <i class="el-icon-chat-dot-square"></i>
                  意见反馈
                </el-dropdown-item>
                <el-dropdown-item divided></el-dropdown-item>
                <el-dropdown-item command="logout">
                  <i class="el-icon-switch-button"></i>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </div>
    </div>
    
    <!-- 移动端下拉菜单 -->
    <div class="mobile-menu" :class="{ 'show': showMobileMenu }" v-show="showMobileMenu">
      <div class="mobile-menu-content">
        <div class="mobile-nav-items">
          <router-link to="/home" @click="closeMobileMenu" class="mobile-nav-item">
            <i class="el-icon-house"></i>
            <span>首页</span>
          </router-link>
          <router-link to="/show/list" @click="closeMobileMenu" class="mobile-nav-item">
            <i class="el-icon-monitor"></i>
            <span>机位列表</span>
          </router-link>
          <router-link to="/order/list" @click="closeMobileMenu" class="mobile-nav-item">
            <i class="el-icon-tickets"></i>
            <span>我的预约</span>
          </router-link>
        </div>
        
        <div class="mobile-user-section">
          <template v-if="!userInfo">
            <router-link to="/login" @click="closeMobileMenu" class="mobile-auth-button">
              <i class="el-icon-user"></i>
              <span>登录</span>
            </router-link>
            <router-link to="/register" @click="closeMobileMenu" class="mobile-auth-button">
              <i class="el-icon-user-solid"></i>
              <span>注册</span>
            </router-link>
          </template>
          <template v-else>
            <div class="mobile-user-info">
              <el-avatar :size="40" class="user-avatar">
                {{ (userInfo.nickname || userInfo.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ userInfo.nickname || userInfo.username }}</span>
            </div>
            <div class="mobile-user-actions">
              <div class="mobile-nav-item" @click="handleMobileCommand('profile')">
                <i class="el-icon-user"></i>
                <span>个人信息</span>
              </div>
              <div class="mobile-nav-item" @click="handleMobileCommand('orders')">
                <i class="el-icon-tickets"></i>
                <span>我的订单</span>
              </div>
              <div class="mobile-nav-item" @click="handleMobileCommand('settings')">
                <i class="el-icon-setting"></i>
                <span>系统设置</span>
              </div>
              <div class="mobile-nav-item" @click="handleMobileCommand('help')">
                <i class="el-icon-question"></i>
                <span>帮助中心</span>
              </div>
              <div class="mobile-nav-item" @click="handleMobileCommand('feedback')">
                <i class="el-icon-chat-dot-square"></i>
                <span>意见反馈</span>
              </div>
              <div class="mobile-nav-item logout" @click="handleMobileCommand('logout')">
                <i class="el-icon-switch-button"></i>
                <span>退出登录</span>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
    
    <!-- 主体内容 -->
    <div class="main-container">
      <app-main />
    </div>
    
    <!-- 页脚 -->
    <div class="footer">
      <p>© 2025 哈尔滨自由点网咖预约系统 All Rights Reserved</p>
    </div>
    
    <!-- Token过期警告 -->
    <TokenExpiryWarning 
      v-model="showExpiryWarning"
      :remaining-time="remainingTime"
      @extend="handleExtendToken"
      @logout="handleForceLogout"
    />
    
    <!-- 系统设置弹窗 -->
    <SettingsDialog v-model="showSettings" />
    
    <!-- 帮助中心弹窗 -->
    <HelpDialog v-model="showHelp" />
    
    <!-- 意见反馈弹窗 -->
    <FeedbackDialog v-model="showFeedback" />
  </div>
</template>

<script>
import { useUserStore } from '@/stores/user'
import AppMain from './components/AppMain.vue'
import TokenExpiryWarning from '@/components/TokenExpiryWarning.vue'
import SettingsDialog from '@/components/SettingsDialog.vue'
import HelpDialog from '@/components/HelpDialog.vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import { isTokenExpiringSoon } from '@/utils/auth'

export default {
  name: 'Layout',
  components: {
    AppMain,
    TokenExpiryWarning,
    SettingsDialog,
    HelpDialog,
    FeedbackDialog
  },
  setup() {
    const userStore = useUserStore()
    
    return {
      userStore
    }
  },
  data() {
    return {
      showExpiryWarning: false,
      remainingTime: 300, // 5分钟
      warningTimer: null,
      showSettings: false,
      showHelp: false,
      showFeedback: false,
      showMobileMenu: false
    }
  },
  watch: {
    showSettings(val) {
      console.log('showSettings changed to:', val)
    },
    showHelp(val) {
      console.log('showHelp changed to:', val)
    },
    showFeedback(val) {
      console.log('showFeedback changed to:', val)
    }
  },
  mounted() {
    // 开始定期检查Token过期状态
    this.startTokenCheck()
  },
  beforeUnmount() {
    this.stopTokenCheck()
  },
  
  computed: {
    userInfo() {
      return this.userStore.userInfo
    },
    activeMenu() {
      const { meta, path } = this.$route
      if (meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    }
  },
  methods: {
    handleCommand(command) {
      console.log('Menu command clicked:', command)
      switch (command) {
        case 'profile':
          this.$router.push('/user/profile')
          break
        case 'orders':
          this.$router.push('/order/list')
          break
        case 'settings':
          console.log('Opening settings dialog')
          this.showSettings = true
          break
        case 'help':
          console.log('Opening help dialog')
          this.showHelp = true
          break
        case 'feedback':
          console.log('Opening feedback dialog')
          this.showFeedback = true
          break
        case 'logout':
          this.handleLogout()
          break
        default:
          console.log('Unknown command:', command)
      }
    },
    
    handleLogout() {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.userStore.logout().then(() => {
          this.$message.success('已安全退出')
          this.$router.push('/login')
        }).catch(() => {
          // 即使后端登出失败，也要清除本地token
          this.userStore.resetToken()
          this.$message.success('已安全退出')
          this.$router.push('/login')
        })
      }).catch(() => {
        // 用户取消登出
      })
    },
    
    // 开始Token过期检查
    startTokenCheck() {
      this.warningTimer = setInterval(() => {
        if (this.userStore.token && isTokenExpiringSoon()) {
          this.showExpiryWarning = true
          this.stopTokenCheck() // 停止检查，避免重复警告
        }
      }, 30000) // 每30秒检查一次
    },
    
    // 停止Token过期检查
    stopTokenCheck() {
      if (this.warningTimer) {
        clearInterval(this.warningTimer)
        this.warningTimer = null
      }
    },
    
    // 处理延长Token
    async handleExtendToken() {
      try {
        // 通过重新获取用户信息来刷新Token活跃状态
        await this.userStore.getInfo()
        this.startTokenCheck() // 重新开始检查
      } catch (error) {
        this.$message.error('延长登录失败，请重新登录')
        this.handleForceLogout()
      }
    },
    
    // 处理强制登出
    handleForceLogout() {
      this.stopTokenCheck()
      this.userStore.resetToken()
      this.$router.push('/login')
    },
    
    // 移动端菜单控制
    toggleMobileMenu() {
      this.showMobileMenu = !this.showMobileMenu
    },
    
    closeMobileMenu() {
      this.showMobileMenu = false
    },
    
    // 处理移动端菜单命令
    handleMobileCommand(command) {
      this.closeMobileMenu()
      this.handleCommand(command)
    }
  }
}
</script>

<style scoped>
.app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  height: 60px;
  background: linear-gradient(135deg, #0a0e17 0%, #162344 50%, #0a2e3d 100%);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  border-bottom: 1px solid rgba(118, 185, 0, 0.2);
  position: relative;
}

.navbar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    linear-gradient(90deg, transparent 95%, rgba(118, 185, 0, 0.1) 95.5%),
    linear-gradient(transparent 95%, rgba(118, 185, 0, 0.1) 95.5%);
  background-size: 30px 30px;
  opacity: 0.3;
  pointer-events: none;
}

.logo {
  width: 280px;
  position: relative;
  z-index: 2;
}

.logo a {
  text-decoration: none;
  color: #fff;
}

.logo h1 {
  margin: 0;
  font-size: 20px;
  text-shadow: 0 0 10px #76b900, 0 0 20px rgba(118, 185, 0, 0.5);
  background: linear-gradient(45deg, rgb(102, 14, 14), #76b900);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: logoGlow 3s ease-in-out infinite alternate;
}

@keyframes logoGlow {
  0% {
    text-shadow: 0 0 10px #76b900, 0 0 20px rgba(118, 185, 0, 0.5);
  }
  100% {
    text-shadow: 0 0 20px #76b900, 0 0 30px rgba(118, 185, 0, 0.8);
  }
}

.menu {
  flex: 1;
  position: relative;
  z-index: 2;
}

/* 菜单项科技感样式 */
.menu :deep(.el-menu-item) {
  background: transparent !important;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(118, 185, 0, 0.1) !important;
  color: #76b900 !important;
  border-bottom-color: #76b900;
  box-shadow: 0 0 10px rgba(118, 185, 0, 0.3);
  text-shadow: 0 0 5px rgba(118, 185, 0, 0.5);
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(118, 185, 0, 0.15) !important;
  color: #76b900 !important;
  border-bottom-color: #76b900;
  box-shadow: 0 0 15px rgba(118, 185, 0, 0.4);
  text-shadow: 0 0 8px rgba(118, 185, 0, 0.6);
}

.right-menu {
  display: flex;
  align-items: center;
  position: relative;
  z-index: 2;
}

.right-menu .el-button {
  color: #ffffff;
  margin-left: 15px;
  border: 1px solid rgba(0, 179, 255, 0.4);
  background: rgba(0, 179, 255, 0.1);
  backdrop-filter: blur(5px);
  transition: all 0.3s ease;
}

.right-menu .el-button:hover {
  color: #ffffff;
  border-color: #00b3ff;
  background: rgba(0, 179, 255, 0.2);
  box-shadow: 0 0 10px rgba(0, 179, 255, 0.4);
  text-shadow: 0 0 5px #00b3ff;
}

.user-dropdown .el-dropdown-link {
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid rgba(118, 185, 0, 0.3);
  background: rgba(118, 185, 0, 0.1);
  transition: all 0.3s ease;
}

.user-dropdown .el-dropdown-link:hover {
  background: rgba(118, 185, 0, 0.2);
  border-color: #76b900;
  box-shadow: 0 0 10px rgba(118, 185, 0, 0.4);
}

.user-avatar {
  margin-right: 8px;
  background: linear-gradient(45deg, #76b900, #9dd33a);
  color: #fff;
  font-weight: bold;
  box-shadow: 0 0 8px rgba(118, 185, 0, 0.3);
}

.user-name {
  margin-right: 8px;
  font-size: 14px;
  text-shadow: 0 0 5px rgba(118, 185, 0, 0.5);
}

.main-container {
  flex: 1;
  padding: 20px;
  background: linear-gradient(135deg, #0a0e17 0%, #162344 50%, #0a2e3d 100%);
  position: relative;
  min-height: calc(100vh - 120px);
}

.main-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    linear-gradient(90deg, transparent 97%, rgba(118, 185, 0, 0.05) 97.5%),
    linear-gradient(transparent 97%, rgba(0, 179, 255, 0.05) 97.5%);
  background-size: 40px 40px;
  opacity: 0.3;
  pointer-events: none;
}

.footer {
  height: 60px;
  background: linear-gradient(135deg, #0a0e17 0%, #162344 50%, #0a2e3d 100%);
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  border-top: 1px solid rgba(118, 185, 0, 0.2);
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.3);
  position: relative;
}

.footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    linear-gradient(90deg, transparent 95%, rgba(0, 179, 255, 0.1) 95.5%),
    linear-gradient(transparent 95%, rgba(0, 179, 255, 0.1) 95.5%);
  background-size: 30px 30px;
  opacity: 0.2;
  pointer-events: none;
}

.footer p {
  position: relative;
  z-index: 2;
  color: #ffffff;
  text-shadow: 0 0 5px rgba(0, 179, 255, 0.5);
}

/* 移动端样式适配 */
@media (max-width: 768px) {
  .navbar {
    height: var(--mobile-navbar-height);
    padding: 0 var(--mobile-container-padding);
  }
  
  .logo {
    width: auto;
    flex: 1;
  }
  
  .logo h1 {
    font-size: var(--mobile-font-lg);
    letter-spacing: 1px;
  }
  
  .main-container {
    padding: var(--mobile-container-padding);
    min-height: calc(100vh - var(--mobile-navbar-height) - 60px);
  }
  
  .footer {
    padding: 0 var(--mobile-container-padding);
  }
  
  .footer p {
    font-size: var(--mobile-font-sm);
    text-align: center;
  }
}

/* 移动端汉堡菜单按钮 */
.mobile-menu-button {
  display: flex;
  align-items: center;
  position: relative;
  z-index: 2;
}

.hamburger-button {
  min-width: var(--touch-target-min) !important;
  min-height: var(--touch-target-min) !important;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff !important;
  font-size: 20px;
  border: 1px solid rgba(118, 185, 0, 0.3);
  background: rgba(118, 185, 0, 0.1);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.hamburger-button:hover {
  background: rgba(118, 185, 0, 0.2) !important;
  border-color: #76b900;
  box-shadow: 0 0 10px rgba(118, 185, 0, 0.4);
}

/* 移动端下拉菜单 */
.mobile-menu {
  position: fixed;
  top: var(--mobile-navbar-height);
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #0a0e17 0%, #162344 50%, #0a2e3d 100%);
  border-top: 1px solid rgba(118, 185, 0, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
  z-index: 1000;
  transform: translateY(-100%);
  transition: transform 0.3s ease;
  max-height: calc(100vh - var(--mobile-navbar-height));
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.mobile-menu.show {
  transform: translateY(0);
}

.mobile-menu-content {
  padding: var(--mobile-spacing-base);
}

.mobile-nav-items {
  border-bottom: 1px solid rgba(118, 185, 0, 0.2);
  padding-bottom: var(--mobile-spacing-base);
  margin-bottom: var(--mobile-spacing-base);
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  padding: var(--mobile-spacing-base);
  color: #ffffff;
  text-decoration: none;
  border-radius: 8px;
  margin-bottom: var(--mobile-spacing-sm);
  min-height: var(--touch-target-min);
  background: rgba(118, 185, 0, 0.05);
  border: 1px solid rgba(118, 185, 0, 0.2);
  transition: all 0.3s ease;
  cursor: pointer;
}

.mobile-nav-item:hover,
.mobile-nav-item.router-link-active {
  background: rgba(118, 185, 0, 0.15);
  border-color: #76b900;
  box-shadow: 0 0 10px rgba(118, 185, 0, 0.3);
  color: #76b900;
  text-shadow: 0 0 5px rgba(118, 185, 0, 0.5);
}

.mobile-nav-item.logout {
  background: rgba(255, 71, 87, 0.1);
  border-color: rgba(255, 71, 87, 0.3);
  color: #ff4757;
}

.mobile-nav-item.logout:hover {
  background: rgba(255, 71, 87, 0.2);
  border-color: #ff4757;
  box-shadow: 0 0 10px rgba(255, 71, 87, 0.4);
}

.mobile-nav-item i {
  margin-right: var(--mobile-spacing-base);
  font-size: 18px;
  width: 20px;
  text-align: center;
}

.mobile-nav-item span {
  font-size: var(--mobile-font-base);
  font-weight: 500;
}

.mobile-user-section {
  padding-top: var(--mobile-spacing-base);
}

.mobile-auth-button {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--mobile-spacing-base);
  margin-bottom: var(--mobile-spacing-sm);
  color: #ffffff;
  text-decoration: none;
  border-radius: 8px;
  min-height: var(--touch-target-min);
  background: rgba(0, 179, 255, 0.1);
  border: 1px solid rgba(0, 179, 255, 0.3);
  transition: all 0.3s ease;
  font-size: var(--mobile-font-base);
  font-weight: 500;
}

.mobile-auth-button:hover {
  background: rgba(0, 179, 255, 0.2);
  border-color: #00b3ff;
  box-shadow: 0 0 10px rgba(0, 179, 255, 0.4);
  color: #00b3ff;
  text-shadow: 0 0 5px rgba(0, 179, 255, 0.5);
}

.mobile-auth-button i {
  margin-right: var(--mobile-spacing-sm);
  font-size: 16px;
}

.mobile-user-info {
  display: flex;
  align-items: center;
  padding: var(--mobile-spacing-base);
  background: rgba(118, 185, 0, 0.1);
  border: 1px solid rgba(118, 185, 0, 0.3);
  border-radius: 8px;
  margin-bottom: var(--mobile-spacing-base);
}

.mobile-user-info .user-avatar {
  margin-right: var(--mobile-spacing-base);
}

.mobile-user-info .user-name {
  font-size: var(--mobile-font-base);
  font-weight: 500;
  color: #76b900;
  text-shadow: 0 0 5px rgba(118, 185, 0, 0.5);
}

.mobile-user-actions {
  display: flex;
  flex-direction: column;
}

@media (max-width: 480px) {
  .logo h1 {
    font-size: var(--mobile-font-base);
  }
  
  .mobile-menu-content {
    padding: var(--mobile-spacing-sm);
  }
  
  .mobile-nav-item {
    padding: var(--mobile-spacing-sm) var(--mobile-spacing-base);
  }
  
  .mobile-nav-item span {
    font-size: var(--mobile-font-sm);
  }
}
</style>