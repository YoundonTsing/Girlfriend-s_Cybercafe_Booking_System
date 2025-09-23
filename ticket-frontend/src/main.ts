import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import './assets/css/global.css'
import './styles/mobile.scss'  // 移动端样式
import './permission' // 权限控制逻辑
import request from './utils/request' // 导入HTTP客户端

const app = createApp(App)
const pinia = createPinia()

// 将HTTP客户端挂载到全局属性
app.config.globalProperties.$http = request

app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})
app.mount('#app')