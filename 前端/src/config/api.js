// API 配置文件
const API_CONFIG = {
  // 后端基地址：
  //   - 开发环境：留空，走 Vite 代理（见 vite.config.js，转发 /api → http://localhost:8080）
  //   - 生产环境：留空，走 Nginx 同源反代（见 nginx/conf/nginx.conf）
  //   - 如需直连其他地址，在此填写，例如 'http://localhost:8080'
  BASE_URL: '',

  // 其他可能的后端服务地址（备用）
  // STUDENT_PROFILE_URL: 'http://localhost:8080',

  // API 超时时间（毫秒）
  TIMEOUT: 10000,

  // API 版本
  API_VERSION: '/api'
};

export default API_CONFIG;
