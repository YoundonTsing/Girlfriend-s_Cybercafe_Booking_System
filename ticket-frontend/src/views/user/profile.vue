<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <div slot="header">
        <span>个人信息</span>
      </div>
      <el-form :model="userForm" :rules="rules" ref="userForm" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" disabled></el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updateProfile" :loading="loading">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="password-card">
      <div slot="header">
        <span>修改密码</span>
      </div>
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordForm" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="changePassword" :loading="passwordLoading">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="order-card">
      <div slot="header">
        <span>我的订单</span>
        <router-link to="/order/list" class="more">查看全部</router-link>
      </div>
      <el-table :data="recentOrders" style="width: 100%">
        <el-table-column prop="orderNo" label="订单编号" width="180"></el-table-column>
        <el-table-column label="演出信息">
          <template slot-scope="scope">
            {{ scope.row.show.title }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template slot-scope="scope">
            ¥{{ scope.row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getOrderStatusType(scope.row.status)">
              {{ getOrderStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <el-button size="mini" @click="goToOrderDetail(scope.row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { useUserStore } from '@/stores/user'
import { updateUserInfo, changePassword } from '@/api/user'
import { getOrderList } from '@/api/order'

export default {
  name: 'UserProfile',
  setup() {
    const userStore = useUserStore()
    return {
      userStore
    }
  },
  data() {
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
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }
    return {
      userForm: {
        username: '',
        nickname: '',
        phone: '',
        email: ''
      },
      rules: {
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
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      },
      loading: false,
      passwordLoading: false,
      recentOrders: []
    }
  },
  computed: {
    userId() {
      return this.userStore.userId
    },
    username() {
      return this.userStore.username
    },
    nickname() {
      return this.userStore.nickname
    },
    phone() {
      return this.userStore.phone
    },
    email() {
      return this.userStore.email
    }
  },
  created() {
    this.initUserForm()
    this.fetchRecentOrders()
  },
  methods: {
    initUserForm() {
      this.userForm = {
        username: this.username,
        nickname: this.nickname,
        phone: this.phone,
        email: this.email
      }
    },
    fetchRecentOrders() {
      getOrderList({ page: 1, size: 5 })
        .then(response => {
          this.recentOrders = response.data.records
        })
        .catch(error => {
          console.error('获取最近订单失败', error)
        })
    },
    updateProfile() {
      this.$refs.userForm.validate(valid => {
        if (valid) {
          this.loading = true
          updateUserInfo(this.userForm)
            .then(() => {
              this.$message.success('个人信息更新成功')
              // 更新Pinia中的用户信息
              this.userStore.getInfo()
            })
            .catch(error => {
              this.$message.error(error.message || '更新失败')
            })
            .finally(() => {
              this.loading = false
            })
        }
      })
    },
    changePassword() {
      this.$refs.passwordForm.validate(valid => {
        if (valid) {
          this.passwordLoading = true
          changePassword(this.passwordForm)
            .then(() => {
              this.$message.success('密码修改成功，请重新登录')
              this.userStore.logout().then(() => {
                this.$router.push('/login')
              })
            })
            .catch(error => {
              this.$message.error(error.message || '密码修改失败')
            })
            .finally(() => {
              this.passwordLoading = false
            })
        }
      })
    },
    goToOrderDetail(id) {
      this.$router.push(`/order/${id}`)
    },
    getOrderStatusType(status) {
      switch (status) {
        case 'CREATED':
          return 'warning'
        case 'PAID':
          return 'success'
        case 'CANCELED':
          return 'info'
        case 'TIMEOUT':
          return 'danger'
        default:
          return 'info'
      }
    },
    getOrderStatusText(status) {
      switch (status) {
        case 'CREATED':
          return '待支付'
        case 'PAID':
          return '已支付'
        case 'CANCELED':
          return '已取消'
        case 'TIMEOUT':
          return '已超时'
        default:
          return '未知状态'
      }
    }
  }
}
</script>

<style scoped>
.profile-container {
  padding: 20px;
}

.profile-card, .password-card, .order-card {
  margin-bottom: 20px;
}

.more {
  float: right;
  color: #409EFF;
  text-decoration: none;
}
</style>