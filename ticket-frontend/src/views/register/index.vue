<template>
  <div class="register-container">
    <div class="gradient-bg"></div>
    <div class="circuit"></div>
    <div class="particles" id="particles"></div>
    
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
    
    <el-card class="register-card">
      <div slot="header" class="text-center">
        <h2>用户注册</h2>
      </div>
      <el-form :model="registerForm" :rules="registerRules" ref="registerForm" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" prefix-icon="el-icon-user"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" prefix-icon="el-icon-lock"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" prefix-icon="el-icon-lock"></el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" prefix-icon="el-icon-user"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" prefix-icon="el-icon-mobile-phone"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" prefix-icon="el-icon-message"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleRegister" style="width: 100%">注册</el-button>
        </el-form-item>
        <div class="text-center">
          <router-link to="/login">已有账号？立即登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { register } from '@/api/user'

export default {
  name: 'Register',
  mounted() {
    // 创建粒子效果
    this.createParticles()
  },
  data() {
    const validatePassword = (rule, value, callback) => {
      if (value.length < 6) {
        callback(new Error('密码长度不能小于6位'))
      } else {
        callback()
      }
    }
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }
    const validatePhone = (rule, value, callback) => {
      const reg = /^1[3-9]\d{9}$/
      if (!reg.test(value)) {
        callback(new Error('请输入正确的手机号'))
      } else {
        callback()
      }
    }
    const validateEmail = (rule, value, callback) => {
      const reg = /^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+(\.[a-zA-Z0-9_-])+/
      if (!reg.test(value)) {
        callback(new Error('请输入正确的邮箱'))
      } else {
        callback()
      }
    }
    return {
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        nickname: '',
        phone: '',
        email: ''
      },
      registerRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { validator: validatePassword, trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ],
        nickname: [
          { required: true, message: '请输入昵称', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { validator: validatePhone, trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { validator: validateEmail, trigger: 'blur' }
        ]
      },
      loading: false
    }
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
    
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (valid) {
          this.loading = true
          const { username, password, nickname, phone, email } = this.registerForm
          register({ username, password, nickname, phone, email })
            .then(() => {
              this.$message.success('注册成功，请登录')
              this.$router.push('/login')
            })
            .catch(error => {
              this.$message.error(error.message || '注册失败')
            })
            .finally(() => {
              this.loading = false
            })
        }
      })
    }
  }
}
</script>

<style scoped>
.register-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #0a0e17;
  overflow: hidden;
  color: #fff;
  padding: 20px 0;
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

.register-card {
  width: 450px;
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

.register-card :deep(.el-card__header) {
  background: transparent;
  border-bottom: 1px solid rgba(118, 185, 0, 0.3);
}

.register-card :deep(.el-card__body) {
  background: transparent;
}

h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 400;
  color: #ffffff;
  text-shadow: 0 0 10px #76b900, 0 0 20px #76b900;
  letter-spacing: 1px;
}

.register-card :deep(.el-form-item__label) {
  color: #ffffff !important;
}

.register-card :deep(.el-input__inner) {
  background: rgba(10, 14, 23, 0.8) !important;
  border: 1px solid rgba(118, 185, 0, 0.4) !important;
  color: #ffffff !important;
}

.register-card :deep(.el-input__inner:focus) {
  border-color: #76b900 !important;
  box-shadow: 0 0 0 2px rgba(118, 185, 0, 0.2) !important;
}

.register-card :deep(.el-input__inner::placeholder) {
  color: #8892b0 !important;
}

.register-card :deep(.el-button--primary) {
  background: linear-gradient(45deg, #76b900, #9dd33a) !important;
  border: none !important;
  color: #ffffff !important;
  font-weight: 600;
  text-shadow: none;
}

.register-card :deep(.el-button--primary:hover) {
  background: linear-gradient(45deg, #9dd33a, #b8e55c) !important;
  box-shadow: 0 0 15px rgba(118, 185, 0, 0.5) !important;
}

.register-card a {
  color: #76b900;
  text-decoration: none;
  text-shadow: 0 0 4px rgba(118, 185, 0, 0.5);
}

.register-card a:hover {
  color: #9dd33a;
  text-shadow: 0 0 8px rgba(118, 185, 0, 0.8);
}

@media (max-width: 768px) {
  .robot-icon, .ai-icon {
    transform: scale(0.7);
  }
  
  .register-card {
    width: 400px;
    margin: 0 20px;
  }
}
</style>