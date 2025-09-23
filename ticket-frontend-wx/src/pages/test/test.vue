<template>
  <view class="test-container">
    <text class="test-title">测试页面</text>
    <text class="test-content">如果你能看到这个页面，说明基础配置正常</text>
    <button @click="showAlert" class="test-button">点击测试</button>
  </view>
</template>

<script>
export default {
  name: 'TestPage',
  data() {
    return {
      message: '测试成功'
    }
  },
  methods: {
    showAlert() {
      uni.showToast({
        title: this.message,
        icon: 'success'
      })
    }
  }
}
</script>

<style scoped>
.test-container {
  padding: 40rpx;
  text-align: center;
}

.test-title {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 40rpx;
}

.test-content {
  display: block;
  font-size: 32rpx;
  color: #666;
  margin-bottom: 60rpx;
}

.test-button {
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 8rpx;
  padding: 20rpx 40rpx;
  font-size: 32rpx;
}
</style>