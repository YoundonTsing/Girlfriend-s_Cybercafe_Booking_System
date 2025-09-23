<template>
  <el-dialog
    title="系统设置"
    v-model="visible"
    width="1200px"
  >
    <el-row :gutter="40">
      <el-col :span="12">
        <h3>基础设置</h3>
        <el-form :model="settings" label-width="120px">
          <el-form-item label="语言设置">
            <el-select v-model="settings.language" placeholder="请选择语言" style="width: 100%">
              <el-option label="简体中文" value="zh-CN"></el-option>
              <el-option label="English" value="en-US"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="主题设置">
            <el-radio-group v-model="settings.theme">
              <el-radio label="light">浅色主题</el-radio>
              <el-radio label="dark">深色主题</el-radio>
              <el-radio label="auto">跟随系统</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="时间格式">
            <el-radio-group v-model="settings.timeFormat">
              <el-radio label="12">12小时制</el-radio>
              <el-radio label="24">24小时制</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="页面大小">
            <el-select v-model="settings.pageSize" style="width: 100%">
              <el-option label="10条/页" :value="10"></el-option>
              <el-option label="20条/页" :value="20"></el-option>
              <el-option label="50条/页" :value="50"></el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </el-col>
      
      <el-col :span="12">
        <h3>通知设置</h3>
        <el-form :model="settings" label-width="120px">
          <el-form-item label="邮件通知">
            <el-switch v-model="settings.emailNotification"></el-switch>
            <div class="setting-desc">接收订单状态变更邮件通知</div>
          </el-form-item>
          
          <el-form-item label="短信通知">
            <el-switch v-model="settings.smsNotification"></el-switch>
            <div class="setting-desc">接收重要订单信息短信通知</div>
          </el-form-item>
          
          <el-form-item label="浏览器通知">
            <el-switch v-model="settings.browserNotification"></el-switch>
            <div class="setting-desc">允许浏览器推送通知</div>
          </el-form-item>
          
          <el-form-item label="营销信息">
            <el-switch v-model="settings.marketingNotification"></el-switch>
            <div class="setting-desc">接收演出推荐和优惠信息</div>
          </el-form-item>
          
          <el-form-item label="自动登出">
            <el-select v-model="settings.autoLogout" style="width: 100%">
              <el-option label="30分钟" value="30"></el-option>
              <el-option label="1小时" value="60"></el-option>
              <el-option label="2小时" value="120"></el-option>
              <el-option label="不自动登出" value="0"></el-option>
            </el-select>
            <div class="setting-desc">无操作后自动退出登录</div>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave">保存设置</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: 'SettingsDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      settings: {
        language: 'zh-CN',
        theme: 'light',
        timeFormat: '24',
        pageSize: 20,
        emailNotification: true,
        smsNotification: true,
        browserNotification: false,
        marketingNotification: false,
        autoLogout: '120'
      }
    }
  },
  computed: {
    visible: {
      get() {
        return this.modelValue
      },
      set(value) {
        this.$emit('update:modelValue', value)
      }
    }
  },
  methods: {
    handleClose() {
      this.visible = false
    },
    handleSave() {
      this.$message.success('设置保存成功')
      this.visible = false
    }
  }
}
</script>

<style scoped>
.dialog-footer {
  text-align: right;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e6e6e6;
}

h3 {
  color: #409eff;
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.setting-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-col {
  padding: 0 20px;
}

.el-col:first-child {
  border-right: 1px solid #e6e6e6;
}
</style>