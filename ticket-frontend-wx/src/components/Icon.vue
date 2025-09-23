<template>
  <text 
    class="iconfont" 
    :class="iconClass"
    :style="iconStyle"
    @click="handleClick"
  ></text>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  name: string // 图标名称，如 'home', 'user' 等
  size?: number | string // 图标大小
  color?: string // 图标颜色
  prefix?: string // 图标前缀，默认为 'icon-'
}

interface Emits {
  click: [event: Event]
}

const props = withDefaults(defineProps<Props>(), {
  size: 16,
  color: '#333',
  prefix: 'icon-'
})

const emit = defineEmits<Emits>()

// 计算图标类名
const iconClass = computed(() => {
  return `${props.prefix}${props.name}`
})

// 计算图标样式
const iconStyle = computed(() => {
  const size = typeof props.size === 'number' ? `${props.size}px` : props.size
  return {
    fontSize: size,
    color: props.color
  }
})

// 处理点击事件
const handleClick = (event: Event) => {
  emit('click', event)
}
</script>

<style scoped>
.iconfont {
  display: inline-block;
  font-style: normal;
  vertical-align: baseline;
  text-align: center;
  text-transform: none;
  line-height: 1;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  cursor: pointer;
  transition: color 0.3s ease;
}

.iconfont:hover {
  opacity: 0.8;
}
</style>