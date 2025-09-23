<template>
  <el-dialog
    title="帮助中心"
    v-model="visible"
    width="1400px"
  >
    <div class="help-content">
      <el-collapse v-model="activeNames">
        <el-collapse-item title="快速入门" name="1">
          <div>
            <h4>1. 注册账户</h4>
            <p>点击页面右上角"注册"按钮，填写手机号、邮箱等信息完成注册。</p>
            
            <h4>2. 浏览演出</h4>
            <p>在首页或演出列表页面浏览感兴趣的演出信息。</p>
            
            <h4>3. 下单支付</h4>
            <p>确认订单信息后，选择支付方式完成付款。</p>
          </div>
        </el-collapse-item>
        
        <el-collapse-item title="购票指南" name="2">
          <div>
            <h4>如何选择座位？</h4>
            <p>点击座位图中的可选座位，系统会显示该座位的详细信息和价格。</p>
            
            <h4>购票数量限制</h4>
            <p>每个用户单场演出最多可购买6张票。</p>
            
            <h4>取票方式</h4>
            <p>支持电子票和现场取票两种方式。</p>
          </div>
        </el-collapse-item>
        
        <el-collapse-item title="支付帮助" name="3">
          <div>
            <h4>支持的支付方式</h4>
            <ul>
              <li>微信支付</li>
              <li>支付宝</li>
              <li>银联卡支付</li>
            </ul>
            
            <h4>支付安全</h4>
            <p>我们采用SSL加密技术保护您的支付信息。</p>
          </div>
        </el-collapse-item>
        
        <el-collapse-item title="联系我们" name="4">
          <div>
            <p>客服电话：400-123-4567</p>
            <p>工作时间：9:00-18:00</p>
            <p>客服邮箱：support@tickets.com</p>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleClose">我知道了</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: 'HelpDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      activeNames: ['1']
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
    }
  }
}
</script>

<style scoped>
.help-content {
  max-height: 400px;
  overflow-y: auto;
}

.help-content h4 {
  color: #409eff;
  margin: 16px 0 8px 0;
}

.help-content p {
  color: #606266;
  margin: 8px 0;
  line-height: 1.6;
}

.help-content ul {
  margin: 8px 0;
  padding-left: 20px;
}

.help-content li {
  color: #606266;
  margin: 4px 0;
}

.dialog-footer {
  text-align: center;
}
</style>