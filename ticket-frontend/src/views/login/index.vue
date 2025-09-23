<template>
  <div class="login-container">
    <div id="particles-js"></div>
    
    <div class="neon-border border-top"></div>
    <div class="neon-border border-right"></div>
    <div class="neon-border border-bottom"></div>
    <div class="neon-border border-left"></div>
    
    <div class="corner corner-tl"></div>
    <div class="corner corner-tr"></div>
    <div class="corner corner-bl"></div>
    <div class="corner corner-br"></div>
    
    <div class="poster">
      <!-- Valorant Logo -->
      <div class="valorant-logo">
        <svg viewBox="0 0 100 100" fill="#ff4655">
          <path d="M50.445 4.937c-5.058 0-9.164 4.106-9.164 9.164 0 3.088 1.536 5.81 3.876 7.52L43.65 57.47c-1.04 1.04-1.613 2.45-1.613 3.943v22.5c0 2.906 2.36 5.266 5.266 5.266h5.266c2.906 0 5.266-2.36 5.266-5.266V61.412c0-1.493-.573-2.903-1.613-3.943l-2.895-35.81c2.34-1.71 3.876-4.432 3.876-7.52 0-5.058-4.106-9.164-9.164-9.164zm0 12.219c1.66 0 2.995 1.336 2.995 2.995a2.998 2.998 0 1 1-5.996 0c0-1.659 1.336-2.995 2.995-2.995z"/>
        </svg>
    </div>
    
      <div class="header">
        <h1>自由点电竞</h1>
        <p>极致游戏体验，尽在卓威专区</p>
        </div>
      
      <div class="content">
        <!-- 登录表单区域 -->
        <div class="section login-section">
          <div class="section-icon"><i class="fas fa-user-circle"></i></div>
          <div class="section-content">
            <h2 class="section-title">战术准备</h2>
            <p class="section-description">输入凭证以进入战场</p>
            
            <!-- 登录表单 -->
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
                    class="valorant-input"
                />
              </el-form-item>
          <el-form-item prop="password">
                <el-input 
              v-model="currentForm.password" 
                  type="password" 
                  prefix-icon="el-icon-lock" 
                  placeholder="请输入密码"
              size="large"
                    class="valorant-input"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
            </el-form>
      </div>
      
      <!-- 登录方式切换 -->
            <div class="login-switch">
        <el-link 
          type="primary" 
          @click="switchLoginType"
          :underline="false"
                class="switch-link"
        >
          {{ loginType === 'username' ? '使用手机号登录' : '使用用户名登录' }}
        </el-link>
      </div>
          </div>
        </div>
        
        <!-- 注册区域 -->
        <div class="section register-section">
          <div class="section-icon"><i class="fas fa-user-plus"></i></div>
          <div class="section-content">
            <h2 class="section-title">新兵招募</h2>
            <p class="section-description">还没有账号？</p>
            <router-link to="/register" class="register-link">立即加入战队</router-link>
          </div>
        </div>
        
        <!-- 进入战场按钮 -->
        <div class="battle-entry">
          <button 
            class="cta-button"
            :class="{ disabled: loading }"
            :disabled="loading"
            @click="handleLogin"
          >
            {{ loading ? '连接中...' : '进入战场' }}
          </button>
        </div>
      </div>
      
      <div class="footer"></div>
    
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
    // 初始化粒子效果
    this.initParticles()
  },
  methods: {
    initParticles() {
      // 使用particles.js库创建粒子效果
      if (typeof particlesJS !== 'undefined') {
        particlesJS('particles-js', {
          particles: {
            number: { value: 100, density: { enable: true, value_area: 800 } },
            color: { value: "#ff4655" },
            shape: { 
              type: "polygon",
              polygon: { nb_sides: 5 }
            },
            opacity: {
              value: 0.5,
              random: true,
              anim: { enable: true, speed: 1, opacity_min: 0.1, sync: false }
            },
            size: {
              value: 3,
              random: true,
              anim: { enable: true, speed: 3, size_min: 0.1, sync: false }
            },
            line_linked: {
              enable: true,
              distance: 150,
              color: "#12e2dc",
              opacity: 0.4,
              width: 1
            },
            move: {
              enable: true,
              speed: 3,
              direction: "none",
              random: true,
              straight: false,
              out_mode: "out",
              bounce: false,
              attract: { enable: false, rotateX: 600, rotateY: 1200 }
            }
          },
          interactivity: {
            detect_on: "canvas",
            events: {
              onhover: { enable: true, mode: "grab" },
              onclick: { enable: true, mode: "push" },
              resize: true
            },
            modes: {
              grab: { distance: 140, line_linked: { opacity: 1 } },
              push: { particles_nb: 4 }
            }
          },
          retina_detect: true
        })
      }
      
      // 添加鼠标移动流光效果
      const poster = document.querySelector('.poster')
      if (poster) {
        document.addEventListener('mousemove', (e) => {
          const x = e.clientX / window.innerWidth
          const y = e.clientY / window.innerHeight
          
          poster.style.boxShadow = `
            ${20 * x}px ${20 * y}px 40px rgba(255, 70, 85, 0.2),
            ${-20 * (1 - x)}px ${-20 * (1 - y)}px 80px rgba(18, 226, 220, 0.1)
          `
        })
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
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Valorant', 'Arial Narrow Bold', 'Helvetica Neue', 'PingFang SC', sans-serif;
}

@font-face {
  font-family: 'Valorant';
  src: local('Arial Narrow Bold'), local('Helvetica Neue');
}

.login-container {
  overflow-x: hidden;
  background: #0c0e14;
  color: #ffffff;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  position: relative;
}

#particles-js {
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  z-index: -1;
}

.neon-border {
  position: absolute;
  pointer-events: none;
  z-index: 2;
}

.border-top {
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #ff4655, #0c0e14, #12e2dc, #0c0e14, #ff4655);
  box-shadow: 0 0 10px #ff4655, 0 0 20px #ff4655;
  animation: borderFlow 6s linear infinite;
}

.border-right {
  top: 0;
  right: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(0deg, #ff4655, #0c0e14, #12e2dc, #0c0e14, #ff4655);
  box-shadow: 0 0 10px #12e2dc, 0 0 20px #12e2dc;
  animation: borderFlow 6s linear infinite;
}

.border-bottom {
  bottom: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(270deg, #ff4655, #0c0e14, #12e2dc, #0c0e14, #ff4655);
  box-shadow: 0 0 10px #12e2dc, 0 0 20px #12e2dc;
  animation: borderFlow 6s linear infinite;
}

.border-left {
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #ff4655, #0c0e14, #12e2dc, #0c0e14, #ff4655);
  box-shadow: 0 0 10px #ff4655, 0 0 20px #ff4655;
  animation: borderFlow 6s linear infinite;
}

.corner {
  position: absolute;
  width: 20px;
  height: 20px;
  z-index: 3;
}

.corner-tl {
  top: 0;
  left: 0;
  border-top: 4px solid #ff4655;
  border-left: 4px solid #ff4655;
  box-shadow: -2px -2px 6px rgba(255, 70, 85, 0.8);
}

.corner-tr {
  top: 0;
  right: 0;
  border-top: 4px solid #ff4655;
  border-right: 4px solid #ff4655;
  box-shadow: 2px -2px 6px rgba(255, 70, 85, 0.8);
}

.corner-bl {
  bottom: 0;
  left: 0;
  border-bottom: 4px solid #ff4655;
  border-left: 4px solid #ff4655;
  box-shadow: -2px 2px 6px rgba(255, 70, 85, 0.8);
}

.corner-br {
  bottom: 0;
  right: 0;
  border-bottom: 4px solid #ff4655;
  border-right: 4px solid #ff4655;
  box-shadow: 2px 2px 6px rgba(255, 70, 85, 0.8);
}

.poster {
  width: 85vw;
  max-width: 850px;
  height: auto;
  min-height: 90vh;
  background: rgba(13, 17, 28, 0.85);
  border: 1px solid rgba(255, 70, 85, 0.5);
  border-radius: 4px;
  overflow: visible;
  margin: 40px auto;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  padding: 40px 20px;
  box-shadow: 0 0 40px rgba(255, 70, 85, 0.2), 
              0 0 80px rgba(18, 226, 220, 0.1),
              inset 0 0 40px rgba(13, 17, 28, 0.7);
  backdrop-filter: blur(5px);
}

.header {
  padding: 40px 20px;
  text-align: center;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  margin-bottom: 40px;
  width: 100%;
}

.header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 10%;
  width: 80%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #ff4655, #12e2dc, #ff4655, transparent);
}

.header h1 {
  font-size: 5.5rem;
  font-weight: 900;
  letter-spacing: 4px;
  margin: 0;
  text-transform: uppercase;
  line-height: 1.2;
  color: transparent;
  background: linear-gradient(45deg, #ff4655, #ffffff, #12e2dc);
  -webkit-background-clip: text;
  background-clip: text;
  text-shadow: 0 0 10px rgba(255, 70, 85, 0.5);
  font-family: 'Valorant', 'Arial Narrow Bold', sans-serif;
  position: relative;
  padding: 0 10px;
}

.header h1::before, .header h1::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30px;
  height: 4px;
  background: #ff4655;
  box-shadow: 0 0 10px #ff4655;
}

.header h1::before {
  left: -40px;
}

.header h1::after {
  right: -40px;
}

.header p {
  font-size: 2.5rem;
  opacity: 0.9;
  font-weight: 600;
  line-height: 1.4;
  color: #12e2dc;
  font-family: 'Valorant', 'Arial Narrow Bold', sans-serif;
  letter-spacing: 2px;
  margin-top: 20px;
  text-shadow: 0 0 8px rgba(18, 226, 220, 0.6);
}

.content {
  padding: 0 40px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  text-align: left;
  flex: 1;
  width: 100%;
  max-width: 600px;
  margin: 20px 0;
}

.section {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  text-align: left;
  width: 100%;
  background: rgba(18, 226, 220, 0.05);
  border-radius: 4px;
  padding: 25px;
  border: 1px solid rgba(255, 70, 85, 0.3);
  transition: all 0.3s ease;
  margin-bottom: 25px;
  position: relative;
  overflow: hidden;
}

.section::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 70, 85, 0.1), transparent);
  transition: all 0.6s ease;
}

.section:hover {
  background: rgba(18, 226, 220, 0.1);
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(255, 70, 85, 0.2);
}

.section:hover::before {
  left: 100%;
}

.section-icon {
  font-size: 3.5rem;
  color: #ff4655;
  margin-right: 25px;
  width: 60px;
  text-align: center;
  text-shadow: 0 0 10px rgba(255, 70, 85, 0.7);
  flex-shrink: 0;
}

.section-content {
  flex: 1;
}

.section-title {
  font-size: 3.2rem;
  font-weight: 700;
  margin: 0 0 8px 0;
  color: #ffffff;
  line-height: 1.3;
  font-family: 'Valorant', 'Arial Narrow Bold', sans-serif;
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.section-description {
  font-size: 2.2rem;
  color: #12e2dc;
  font-weight: 400;
  line-height: 1.4;
  font-family: 'Valorant', 'Arial Narrow Bold', sans-serif;
  margin: 0 0 20px 0;
  text-shadow: 0 0 5px rgba(18, 226, 220, 0.5);
}

/* 专门为登录区域调整字体并居中 */
.login-section .section-title {
  font-size: 2.6rem;
  text-align: center;
}

.login-section .section-description {
  font-size: 1.8rem;
  text-align: center;
}

.login-section .section-content {
  text-align: center;
}

/* 专门为注册区域调整较小字体 */
.register-section .section-title {
  font-size: 2.4rem;
}

.register-section .section-description {
  font-size: 1.6rem;
}

/* 登录表单样式 */
.login-form {
  width: 100%;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.valorant-input) {
  width: 100%;
}

.login-form :deep(.valorant-input .el-input__inner) {
  background: rgba(13, 17, 28, 0.8);
  border: 2px solid rgba(255, 70, 85, 0.4);
  color: #ffffff;
  font-size: 1.4rem;
  padding: 12px 15px;
  border-radius: 0;
  transition: all 0.3s ease;
}

.login-form :deep(.valorant-input .el-input__inner:focus) {
  border-color: #ff4655;
  box-shadow: 0 0 10px rgba(255, 70, 85, 0.5);
}

.login-form :deep(.valorant-input .el-input__inner::placeholder) {
  color: #8892b0;
  font-size: 1.1rem;
}

.login-switch {
  margin-top: 15px;
  text-align: center;
}

.switch-link {
  color: #12e2dc !important;
  font-size: 1.2rem;
  text-shadow: 0 0 5px rgba(18, 226, 220, 0.5);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.switch-link:hover {
  color: #ffffff !important;
  text-shadow: 0 0 10px rgba(18, 226, 220, 0.8);
}

/* 注册链接样式 */
.register-link {
  color: #12e2dc;
  text-decoration: none;
  font-size: 1.4rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  transition: all 0.3s ease;
  display: inline-block;
  padding: 10px 0;
  text-shadow: 0 0 5px rgba(18, 226, 220, 0.5);
}

.register-link:hover {
  color: #ffffff;
  text-shadow: 0 0 10px rgba(18, 226, 220, 0.8);
  transform: translateX(5px);
}

/* 进入战场按钮区域 */
.battle-entry {
  padding: 0;
  background: transparent;
  text-align: center;
  border: none;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  margin-top: 30px;
  width: 100%;
}

.footer {
  padding: 0;
  background: transparent;
  margin-top: 20px;
  width: 100%;
}

.cta-button {
  display: inline-block;
  padding: 20px 60px;
  background: linear-gradient(135deg, #ff4655 0%, #ce2a39 100%);
  color: #ffffff;
  text-decoration: none;
  border: 2px solid #ffffff;
  border-radius: 0;
  font-weight: 900;
  font-size: 2.5rem;
  letter-spacing: 2px;
  transition: all 0.3s;
  box-shadow: 0 0 20px rgba(255, 70, 85, 0.5);
  text-shadow: none;
  line-height: 1.4;
  font-family: 'Valorant', 'Arial Narrow Bold', sans-serif;
  text-transform: uppercase;
  position: relative;
  overflow: hidden;
  z-index: 1;
  cursor: pointer;
}

.cta-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: all 0.6s ease;
}

.cta-button:hover {
  background: linear-gradient(135deg, #ff5765 0%, #df3a4a 100%);
  transform: translateY(-3px);
  box-shadow: 0 0 30px rgba(255, 70, 85, 0.8);
}

.cta-button:hover::before {
  left: 100%;
}

.cta-button.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.valorant-logo {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 80px;
  height: 80px;
  opacity: 0.7;
  animation: pulse 3s infinite ease-in-out;
}

/* 底部备案信息样式 */
.footer-info {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
  z-index: 10;
  color: rgba(255, 255, 255, 0.4);
  font-size: 10px;
  line-height: 1.6;
  font-family: 'Arial', sans-serif;
}

.footer-links,
.footer-address {
  margin: 5px 0;
}

.footer-info .divider {
  margin: 0 8px;
  color: rgba(255, 70, 85, 0.5);
}

.footer-info span {
  transition: color 0.3s ease;
}

.footer-info span:hover {
  color: #ff4655;
  text-shadow: 0 0 5px rgba(255, 70, 85, 0.5);
}

@keyframes borderFlow {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 100% 0;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.7;
  }
  50% {
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .poster {
    width: 95vw;
    padding: 30px 15px;
  }
  
  .header h1 {
    font-size: 3.5rem;
    letter-spacing: 2px;
  }
  
  .header h1::before, .header h1::after {
    width: 15px;
  }
  
  .header h1::before {
    left: -25px;
  }
  
  .header h1::after {
    right: -25px;
  }
  
  .header p {
    font-size: 1.8rem;
  }
  
  .section-title {
    font-size: 2.4rem;
  }
  
  .section-description {
    font-size: 1.8rem;
  }
  
  .login-section .section-title {
    font-size: 2.2rem;
  }
  
  .login-section .section-description {
    font-size: 1.5rem;
  }
  
  .switch-link {
    font-size: 1.1rem !important;
  }
  
  .register-section .section-title {
    font-size: 2rem;
  }
  
  .register-section .section-description {
    font-size: 1.4rem;
  }
  
  .register-link {
    font-size: 1.2rem;
  }
  
  .content {
    padding: 0 20px;
  }
  
  .section {
    padding: 20px;
  }
  
  .section-icon {
    font-size: 2.8rem;
    margin-right: 15px;
  }
  
  .cta-button {
    padding: 15px 40px;
    font-size: 2rem;
  }
  
  .valorant-logo {
    width: 50px;
    height: 50px;
    top: 15px;
    right: 15px;
  }
  
  .footer-info {
    font-size: 9px;
    padding: 0 10px;
  }
  
  .footer-info .divider {
    margin: 0 5px;
  }
}

@media (max-width: 480px) {
  .header h1 {
    font-size: 2.5rem;
    letter-spacing: 1px;
  }
  
  .header p {
    font-size: 1.4rem;
  }
  
  .section-title {
    font-size: 2rem;
  }
  
  .section-description {
    font-size: 1.4rem;
  }
  
  .login-section .section-title {
    font-size: 1.9rem;
  }
  
  .login-section .section-description {
    font-size: 1.3rem;
  }
  
  .switch-link {
    font-size: 1rem !important;
  }
  
  .register-section .section-title {
    font-size: 1.7rem;
  }
  
  .register-section .section-description {
    font-size: 1.2rem;
  }
  
  .register-link {
    font-size: 1.1rem;
  }
  
  .cta-button {
    padding: 12px 30px;
    font-size: 1.8rem;
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
  
  .footer-info {
    font-size: 8px;
  }
}
</style>