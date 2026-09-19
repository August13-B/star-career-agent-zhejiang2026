import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 开发环境：所有 /api 请求代理到合并后的后端
      // （后端 context-path = /api，默认端口 8080，见 后端/.env SERVER_PORT）
      // ⚠️ 必须用 127.0.0.1 而非 localhost：
      //    在 WSL/Windows 混合环境下 localhost 可能解析到 IPv6 ::1，
      //    而后端只监听 IPv4 → 会出现 ECONNREFUSED
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
