import { createSSRApp } from 'vue'
import App from './App.vue'
import { createPinia } from 'pinia'
import { createPersistedState } from 'pinia-plugin-persistedstate'

export function createApp() {
  const app = createSSRApp(App)
  const pinia = createPinia()
  
  // 配置持久化插件（仅为有 persist 配置的 store 启用）
  pinia.use(createPersistedState())
  
  app.use(pinia)
  
  return {
    app
  }
}