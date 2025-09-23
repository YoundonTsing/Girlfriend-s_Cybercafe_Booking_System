import { defineConfig } from 'vite'
import { resolve } from 'path'
import { createRequire } from 'module'
const require = createRequire(import.meta.url)
const uni = require('@dcloudio/vite-plugin-uni').default

export default defineConfig({
  plugins: [
    uni()
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5200,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        // 保留 /api 前缀给网关匹配
      }
    }
  },
  build: {
    target: 'es2015'
  },
  define: {
    __UNI_FEATURE_RPX__: true,
    __UNI_FEATURE_LONGPRESS__: true
  }
})