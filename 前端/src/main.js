// src/main.js (如果你项目是用 vite 建的，大概长这样)
import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // 引入路由

const app = createApp(App)

app.use(router) // 挂载路由
app.mount('#app')