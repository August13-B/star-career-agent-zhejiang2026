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
  // 🌟 核心：配置多重跨域代理
  server: {
    proxy: {
      // 🚦 朋友 A 的服务器（管用户、管 AI 对话）
      '/api/user': { target: 'http://57c42474b0ea.ofalias.net:57332', changeOrigin: true },
      '/api/ai-conversation': { target: 'http://57c42474b0ea.ofalias.net:57332', changeOrigin: true },
      '/api/llm': { target: 'http://57c42474b0ea.ofalias.net:57332', changeOrigin: true },
      
      // 🚦 新朋友的服务器（岗位基础信息和硬门槛需求） - 更具体的路径放在前面
      '/api/job-info': { 
        target: 'http://7e526c6c1d80.ofalias.net:53880', 
        changeOrigin: true 
      },
      '/api/job-hard-requirement': { 
        target: 'http://7e526c6c1d80.ofalias.net:53880', 
        changeOrigin: true 
      },
      
      // 🚦 朋友 B 的服务器（管岗位业务，目前可能 502） - 通用路径放在后面
      '/api/job': { target: 'http://cfc8522bc8db.ofalias.net:64679', changeOrigin: true }
    }
  }
})
