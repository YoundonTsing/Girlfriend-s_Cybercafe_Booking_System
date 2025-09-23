<template>
  <el-dialog
    title="登录即将过期"
    v-model="visible"
    width="400px"
    :before-close="handleClose"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div class="warning-content">
      <el-icon class="warning-icon"><Warning /></el-icon>
      <p>您的登录状态即将在 <strong>{{ countdown }}</strong> 秒后过期</p>
      <p>请选择继续使用或重新登录</p>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleLogout">重新登录</el-button>
        <el-button type="primary" @click="handleExtend">继续使用</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { defineComponent, ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

export default defineComponent({
  name: 'TokenExpiryWarning',
  components: {
    Warning
  },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    remainingTime: {
      type: Number,
      default: 300 // 5分钟
    }
  },
  emits: ['update:modelValue', 'extend', 'logout'],
  setup(props, { emit }) {
    const userStore = useUserStore()
    const router = useRouter()
    const countdown = ref(props.remainingTime)
    let timer = null

    const visible = computed({
      get: () => props.modelValue,
      set: (value) => emit('update:modelValue', value)
    })

    const startCountdown = () => {
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          handleTimeout()
        }
      }, 1000)
    }

    const stopCountdown = () => {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    }

    const handleClose = () => {
      // 防止用户关闭对话框
      return false
    }

    const handleExtend = () => {
      stopCountdown()
      emit('extend')
      visible.value = false
      ElMessage.success('登录状态已延长')
    }

    const handleLogout = () => {
      stopCountdown()
      emit('logout')
      visible.value = false
      userStore.resetToken()
      router.push('/login')
      ElMessage.info('请重新登录')
    }

    const handleTimeout = () => {
      stopCountdown()
      visible.value = false
      userStore.resetToken()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    }

    onMounted(() => {
      if (visible.value) {
        startCountdown()
      }
    })

    onUnmounted(() => {
      stopCountdown()
    })

    // 监听visible变化
    watch(() => props.modelValue, (newVal) => {
      if (newVal) {
        countdown.value = props.remainingTime
        startCountdown()
      } else {
        stopCountdown()
      }
    })

    return {
      visible,
      countdown,
      handleClose,
      handleExtend,
      handleLogout
    }
  }
})
</script>

<style scoped>
.warning-content {
  text-align: center;
  padding: 20px 0;
}

.warning-icon {
  font-size: 48px;
  color: #f56c6c;
  margin-bottom: 16px;
}

.warning-content p {
  margin: 8px 0;
  color: #606266;
}

.warning-content strong {
  color: #f56c6c;
  font-size: 18px;
}

.dialog-footer {
  text-align: center;
}

.dialog-footer .el-button {
  margin: 0 8px;
}
</style>