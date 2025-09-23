<template>
  <div class="login-container">
    <div class="gradient-bg"></div>
    <div class="circuit"></div>
    <div class="particles" id="particles"></div>
    
    <!-- 系统标题 -->
    <div class="system-title">
      <h1>自由点网咖预约系统</h1>
      <p class="subtitle">Freedom Internet Cafe Reservation System</p>
    </div>
    
    <div class="ai-icon">
      <div class="ai-brain"></div>
      <div class="ai-circuit"></div>
    </div>
    
    <div class="robot-icon">
      <div class="robot-head">
        <div class="robot-eye left"></div>
        <div class="robot-eye right"></div>
      </div>
      <div class="robot-body"></div>
      <div class="pulse"></div>
    </div>
    
    <el-card class="login-card">
      <template #header>
        <div class="text-center">
          <h2>登录</h2>
        </div>
      </template>
      
      <!-- 主要登录表单 -->
      <div class="login-form">
        <el-form 
          :model="currentForm" 
          :rules="currentRules" 
          ref="loginForm" 
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
                <el-input 
              v-model="currentForm.username" 
              :prefix-icon="loginType === 'username' ? 'el-icon-user' : 'el-icon-phone'"
              :placeholder="loginType === 'username' ? '请输入用户名' : '请输入手机号'"
              size="large"
                />
              </el-form-item>
          <el-form-item prop="password">
                <el-input 
              v-model="currentForm.password" 
                  type="password" 
                  prefix-icon="el-icon-lock" 
                  placeholder="请输入密码"
              size="large"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
            </el-form>
      </div>
      
      <!-- 登录按钮 -->
      <div class="login-actions">
        <el-button 
          type="primary" 
          :loading="loading" 
          @click="handleLogin" 
          size="large"
          style="width: 100%"
        >
          登录
        </el-button>
      </div>
      
      <!-- 注册链接 -->
      <div class="text-center register-link">
        <router-link to="/register">没有账号？立即注册</router-link>
      </div>
      
      <!-- 登录方式切换 -->
      <div class="text-center login-switch">
        <el-link 
          type="primary" 
          @click="switchLoginType"
          :underline="false"
        >
          {{ loginType === 'username' ? '使用手机号登录' : '使用用户名登录' }}
        </el-link>
      </div>
    </el-card>
    
    <!-- 底部备案信息 -->
    <div class="footer-info">
      <div class="footer-links">
        <span>© 2025 自由点网咖预约系统</span>
        <span class="divider">|</span>
        <span>黑ICP备2024000001号</span>
        <span class="divider">|</span>
        <span>黑公网安备23010202010001号</span>
      </div>
      <div class="footer-address">
        <span>哈尔滨市南岗区中央大街128号</span>
        <span class="divider">|</span>
        <span>服务热线：400-888-0451</span>
        <span class="divider">|</span>
        <span>营业执照：91230100MA1234567X</span>
      </div>
    </div>
  </div>
</template>

<script>
import { useUserStore } from '@/stores/user'

export default {
  name: 'Login',
  setup() {
    const userStore = useUserStore()
    return {
      userStore
    }
  },
  data() {
    return {
      loginType: 'username', // 当前登录方式
      loginForm: {
        username: '',
        password: ''
      },
      phoneForm: {
        phone: '',
        password: ''
      },
      usernameRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
        ]
      },
      phoneRules: {
        username: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  computed: {
    redirect() {
      return this.$route.query.redirect || '/'
    },
    currentForm() {
      return this.loginType === 'username' ? this.loginForm : { username: this.phoneForm.phone, password: this.phoneForm.password }
    },
    currentRules() {
      return this.loginType === 'username' ? this.usernameRules : this.phoneRules
    }
  },
  mounted() {
    // 创建粒子效果
    this.createParticles()
  },
  methods: {
    createParticles() {
      const particlesContainer = document.getElementById('particles')
      const particleCount = 50
      
      for (let i = 0; i < particleCount; i++) {
        const particle = document.createElement('div')
        particle.classList.add('particle')
        
        // 随机大小和位置
        const size = Math.random() * 5 + 2
        const posX = Math.random() * 100
        const posY = Math.random() * 100
        const delay = Math.random() * 15
        
        particle.style.width = `${size}px`
        particle.style.height = `${size}px`
        particle.style.left = `${posX}%`
        particle.style.top = `${posY}%`
        particle.style.animationDelay = `${delay}s`
        
        // 随机颜色 - 英伟达绿或科技蓝
        const colors = ['rgba(118, 185, 0, 0.5)', 'rgba(0, 179, 255, 0.5)']
        const color = colors[Math.floor(Math.random() * colors.length)]
        particle.style.background = color
        
        // 调整阴影颜色
        if (color.includes('179')) {
          particle.style.boxShadow = '0 0 10px #00b3ff, 0 0 20px #00b3ff'
      } else {
          particle.style.boxShadow = '0 0 10px #76b900, 0 0 20px #76b900'
        }
        
        particlesContainer.appendChild(particle)
      }
    },
    
    switchLoginType() {
      // 切换登录方式并清空表单
      this.loginType = this.loginType === 'username' ? 'phone' : 'username'
      this.loginForm = { username: '', password: '' }
      this.phoneForm = { phone: '', password: '' }
      
      // 清空验证状态
      this.$nextTick(() => {
        this.$refs.loginForm?.clearValidate()
      })
    },
    
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          
          // 构建登录数据
          const loginData = this.loginType === 'username' 
            ? { username: this.loginForm.username, password: this.loginForm.password, loginType: 'username' }
            : { username: this.phoneForm.phone, password: this.phoneForm.password, loginType: 'phone' }
          
          this.userStore.login(loginData)
            .then(() => {
              // 登录成功后获取用户信息
              return this.userStore.getInfo()
            })
            .then(() => {
              this.$message.success('登录成功')
              // 跳转到重定向页面或首页
              this.$router.push({ path: this.redirect })
              this.loading = false
            })
            .catch(error => {
              this.$message.error(error.message || '登录失败')
              this.loading = false
            })
        }
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #0a0e17;
  overflow: hidden;
  color: #fff;
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(125deg, #0a0e17 0%, #162344 50%, #0a2e3d 100%);
  z-index: -3;
}

.particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -2;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(118, 185, 0, 0.5);
  box-shadow: 0 0 10px #76b900, 0 0 20px #76b900;
  animation: float 15s infinite ease-in-out;
}

.circuit {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: -1;
  opacity: 0.1;
  background: 
    linear-gradient(90deg, transparent 95%, #76b900 95.5%),
    linear-gradient(transparent 95%, #76b900 95.5%);
  background-size: 50px 50px;
}

.robot-icon {
  position: absolute;
  width: 200px;
  height: 250px;
  bottom: 50px;
  right: 100px;
  opacity: 0.7;
  z-index: 1;
}

.robot-head {
  position: absolute;
  width: 80px;
  height: 80px;
  background: rgba(118, 185, 0, 0.2);
  border: 2px solid #76b900;
  border-radius: 20px;
  top: 0;
  left: 60px;
}

.robot-eye {
  position: absolute;
  width: 20px;
  height: 20px;
  background: #76b900;
  border-radius: 50%;
  top: 25px;
  animation: blink 4s infinite;
}

.robot-eye.left {
  left: 20px;
}

.robot-eye.right {
  right: 20px;
}

.robot-body {
  position: absolute;
  width: 120px;
  height: 120px;
  background: rgba(118, 185, 0, 0.1);
  border: 2px solid #76b900;
  border-top: none;
  top: 80px;
  left: 40px;
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
}

.pulse {
  position: absolute;
  width: 20px;
  height: 20px;
  background: #76b900;
  border-radius: 50%;
  bottom: 30px;
  left: 90px;
  animation: pulse 2s infinite;
}

.ai-icon {
  position: absolute;
  width: 150px;
  height: 150px;
  top: 50px;
  left: 100px;
  opacity: 0.7;
  z-index: 1;
}

.ai-brain {
  position: absolute;
  width: 100px;
  height: 120px;
  border: 2px solid #00b3ff;
  border-top: none;
  border-bottom-left-radius: 60px;
  border-bottom-right-radius: 60px;
  top: 15px;
  left: 25px;
}

.ai-circuit {
  position: absolute;
  width: 100%;
  height: 20px;
  background: linear-gradient(90deg, transparent, #00b3ff, transparent);
  bottom: 0;
  animation: circuit-flow 3s infinite linear;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) translateX(0);
  }
  25% {
    transform: translateY(-20px) translateX(10px);
  }
  50% {
    transform: translateY(10px) translateX(20px);
  }
  75% {
    transform: translateY(20px) translateX(-10px);
  }
}

@keyframes blink {
  0%, 95%, 98%, 100% {
    height: 20px;
  }
  96%, 97% {
    height: 2px;
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(3);
    opacity: 0;
  }
}

@keyframes circuit-flow {
  0% {
    background-position: -100px 0;
  }
  100% {
    background-position: 100px 0;
  }
}

/* 系统标题样式 */
.system-title {
  position: absolute;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
  z-index: 10;
}

.system-title h1 {
  margin: 0 0 10px 0;
  font-size: 3.5rem;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 
    0 0 20px #76b900,
    0 0 40px #76b900,
    0 0 60px rgba(118, 185, 0, 0.5);
  letter-spacing: 3px;
  background: linear-gradient(45deg, #ffffff, #76b900, #9dd33a);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: titlePulse 4s ease-in-out infinite alternate;
}

.system-title .subtitle {
  margin: 0;
  font-size: 1.2rem;
  color: #00b3ff;
  text-shadow: 0 0 10px rgba(0, 179, 255, 0.5);
  letter-spacing: 2px;
  opacity: 0.9;
  font-weight: 300;
}

@keyframes titlePulse {
  0% {
    text-shadow: 
      0 0 20px #76b900,
      0 0 40px #76b900,
      0 0 60px rgba(118, 185, 0, 0.5);
  }
  100% {
    text-shadow: 
      0 0 30px #76b900,
      0 0 60px #76b900,
      0 0 90px rgba(118, 185, 0, 0.8);
  }
}

/* 底部备案信息样式 */
.footer-info {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
  z-index: 10;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  line-height: 1.6;
}

.footer-links,
.footer-address {
  margin: 5px 0;
}

.footer-info .divider {
  margin: 0 10px;
  color: rgba(118, 185, 0, 0.5);
}

.footer-info span {
  transition: color 0.3s ease;
}

.footer-info span:hover {
  color: #76b900;
  text-shadow: 0 0 5px rgba(118, 185, 0, 0.5);
}

.login-card {
  width: 400px;
  border-radius: 12px;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(118, 185, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(118, 185, 0, 0.4);
  background: rgba(10, 14, 23, 0.9);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 10;
}

.login-card :deep(.el-card__header) {
  padding: 24px 24px 16px;
  border-bottom: none;
  background: transparent;
}

.login-card :deep(.el-card__body) {
  padding: 0 24px 24px;
  background: transparent;
}

.login-form {
  margin-bottom: 24px;
}

.login-actions {
  margin-bottom: 24px;
}

.register-link {
  margin-bottom: 12px;
  padding: 12px 0 8px;
  border-top: 1px solid rgba(118, 185, 0, 0.3);
}

.register-link a {
  color: #76b900;
  text-decoration: none;
  font-size: 14px;
  text-shadow: 0 0 4px rgba(118, 185, 0, 0.5);
}

.register-link a:hover {
  text-decoration: underline;
  color: #9dd33a;
  text-shadow: 0 0 8px rgba(118, 185, 0, 0.8);
}

.login-switch {
  margin-top: 8px;
}

.login-switch .el-link {
  font-size: 14px;
  color: #00b3ff !important;
  text-shadow: 0 0 2px rgba(0, 179, 255, 0.5);
}

.login-switch .el-link:hover {
  color: #66d9ff !important;
  text-shadow: 0 0 6px rgba(0, 179, 255, 0.8);
}

.text-center {
  text-align: center;
}

.el-form-item {
  margin-bottom: 20px;
}

h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 400;
  color: #ffffff;
  text-shadow: 0 0 10px #76b900, 0 0 20px #76b900;
  letter-spacing: 1px;
}

/* 调整输入框样式以配合AI主题 */
.login-card :deep(.el-input__inner) {
  background: rgba(10, 14, 23, 0.8);
  border: 1px solid rgba(118, 185, 0, 0.4);
  color: #ffffff;
}

.login-card :deep(.el-input__inner:focus) {
  border-color: #76b900;
  box-shadow: 0 0 0 2px rgba(118, 185, 0, 0.2);
}

.login-card :deep(.el-input__inner::placeholder) {
  color: #8892b0;
}

.login-card :deep(.el-button--primary) {
  background: linear-gradient(45deg, #76b900, #9dd33a);
  border: none;
  color: #ffffff;
  font-weight: 600;
  text-shadow: none;
}

.login-card :deep(.el-button--primary:hover) {
  background: linear-gradient(45deg, #9dd33a, #b8e55c);
  box-shadow: 0 0 15px rgba(118, 185, 0, 0.5);
}

@media (max-width: 768px) {
  .robot-icon, .ai-icon {
    transform: scale(0.7);
  }
  
  .login-card {
    width: 350px;
    margin: 0 20px;
  }
  
  .system-title h1 {
    font-size: 2.5rem;
    letter-spacing: 2px;
  }
  
  .system-title .subtitle {
    font-size: 1rem;
    letter-spacing: 1px;
  }
  
  .footer-info {
    font-size: 11px;
    padding: 0 10px;
  }
  
  .footer-info .divider {
    margin: 0 5px;
  }
}

@media (max-width: 480px) {
  .system-title h1 {
    font-size: 2rem;
    letter-spacing: 1px;
  }
  
  .system-title .subtitle {
    font-size: 0.9rem;
  }
  
  .footer-info {
    font-size: 10px;
  }
  
  .footer-links,
  .footer-address {
    display: flex;
    flex-direction: column;
    gap: 5px;
  }
  
  .footer-info .divider {
    display: none;
  }
}
</style>