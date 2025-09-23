<template>
  <el-dialog
    title="意见反馈"
    v-model="visible"
    width="1200px"
  >
    <el-row :gutter="40">
      <el-col :span="12">
        <h3>反馈信息</h3>
        <el-form :model="feedback" :rules="rules" ref="feedbackForm" label-width="100px">
          <el-form-item label="反馈类型" prop="type">
            <el-select v-model="feedback.type" placeholder="请选择反馈类型" style="width: 100%">
              <el-option label="功能建议" value="suggestion"></el-option>
              <el-option label="问题反馈" value="bug"></el-option>
              <el-option label="界面优化" value="ui"></el-option>
              <el-option label="性能问题" value="performance"></el-option>
              <el-option label="安全问题" value="security"></el-option>
              <el-option label="其他" value="other"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="反馈标题" prop="title">
            <el-input 
              v-model="feedback.title" 
              placeholder="请简要描述您的反馈"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item label="优先级">
            <el-radio-group v-model="feedback.priority">
              <el-radio label="low">一般</el-radio>
              <el-radio label="medium">重要</el-radio>
              <el-radio label="high">紧急</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="联系方式">
            <el-input 
              v-model="feedback.contact" 
              placeholder="手机号或邮箱（选填）"
            />
            <div class="form-desc">方便我们及时回复您的反馈</div>
          </el-form-item>
          
          <el-form-item>
            <el-checkbox v-model="feedback.allowContact">
              我同意票务系统就此反馈与我联系
            </el-checkbox>
          </el-form-item>
        </el-form>
      </el-col>
      
      <el-col :span="12">
        <h3>详细描述</h3>
        <el-form :model="feedback" :rules="rules" label-width="80px">
          <el-form-item label="问题描述" prop="content">
            <el-input 
              v-model="feedback.content" 
              type="textarea" 
              :rows="8"
              placeholder="请详细描述您遇到的问题或建议，包括：&#10;1. 具体的操作步骤&#10;2. 期望的结果&#10;3. 实际发生的情况&#10;4. 其他相关信息"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item label="系统环境">
            <el-input 
              v-model="feedback.environment" 
              type="textarea" 
              :rows="3"
              placeholder="请提供您的系统环境信息（可选）：&#10;- 浏览器类型和版本&#10;- 操作系统&#10;- 屏幕分辨率等"
            />
          </el-form-item>
          
          <el-form-item label="相关截图">
            <el-upload
              class="upload-demo"
              action="#"
              :auto-upload="false"
              :file-list="fileList"
              list-type="picture-card"
              :limit="3"
              :on-preview="handlePreview"
              :on-remove="handleRemove"
            >
              <i class="el-icon-plus"></i>
            </el-upload>
            <div class="form-desc">最多上传3张截图，支持jpg/png格式</div>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交反馈</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: 'FeedbackDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      submitting: false,
      fileList: [],
      feedback: {
        type: '',
        title: '',
        content: '',
        contact: '',
        priority: 'medium',
        environment: '',
        allowContact: true
      },
      rules: {
        type: [
          { required: true, message: '请选择反馈类型', trigger: 'change' }
        ],
        title: [
          { required: true, message: '请输入反馈标题', trigger: 'blur' }
        ],
        content: [
          { required: true, message: '请输入详细描述', trigger: 'blur' }
        ]
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
    async handleSubmit() {
      try {
        await this.$refs.feedbackForm.validate()
        this.submitting = true
        
        // 模拟提交
        setTimeout(() => {
          this.$message.success('反馈提交成功，感谢您的建议！')
          this.handleReset()
          this.visible = false
          this.submitting = false
        }, 1000)
      } catch (error) {
        this.$message.error('请检查表单内容')
      }
    },
    handleReset() {
      this.feedback = {
        type: '',
        title: '',
        content: '',
        contact: '',
        priority: 'medium',
        environment: '',
        allowContact: true
      }
      this.fileList = []
      this.$refs.feedbackForm?.resetFields()
    },
    handlePreview(file) {
      console.log('预览文件:', file)
    },
    handleRemove(file, fileList) {
      console.log('移除文件:', file)
      this.fileList = fileList
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

.form-desc {
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

.upload-demo {
  margin-top: 8px;
}

.el-upload--picture-card {
  width: 80px;
  height: 80px;
  line-height: 88px;
}
</style>