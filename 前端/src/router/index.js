import { createRouter, createWebHistory } from 'vue-router'
import AgentView from '../views/AgentView.vue'
import GraphView from '../views/GraphView.vue'
import LoginView from '../views/LoginView.vue'
import JobDetailView from '../views/JobDetailView.vue'
import UserCenterView from '../views/UserCenterView.vue'
// 🌟 1. 引入岗位管理后台页面
import JobInfoAdmin from '../views/JobInfoAdmin.vue' 
// 🌟 2. 引入新增的 AI 岗位定位深度分析页面
import JobCompareView from '../views/JobCompareView.vue'
import TutorDashboardView from '../views/TutorDashboardView.vue'
import API_CONFIG from '../config/api'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'agent', component: AgentView },
    { path: '/graph', name: 'graph', component: GraphView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/profile', name: 'profile', component: UserCenterView },
    
    // 🌟 这里保留一个图谱管理后台即可
    { 
      path: '/admin/job-info', 
      name: 'JobInfoAdmin', 
      component: JobInfoAdmin 
    },
    
    { 
      path: '/compare', 
      name: 'JobCompare', 
      component: JobCompareView 
    },
    { 
      path: '/job-detail', 
      name: 'JobDetail', 
      component: JobDetailView 
    },
    {
      path: '/ai-score',
      name: 'AiScore',
      component: () => import('../views/AiScoreView.vue')
    },
    {
      path: '/multi-agent',
      name: 'MultiAgent',
      component: () => import('../views/MultiAgentView.vue')
    },
    {
      path: '/growth',
      name: 'Growth',
      component: () => import('../views/GrowthView.vue')
    },
    {
      path: '/training',
      name: 'TrainingLobby',
      component: () => import('../views/TrainingView.vue')
    },
    {
      path: '/training/:sessionId',
      name: 'TrainingSession',
      component: () => import('../views/TrainingView.vue')
    },
    { 
      path: '/tutor-dashboard', 
      name: 'TutorDashboard', 
      component: TutorDashboardView 
    }
  ]
})

router.beforeEach(async (to) => {
  const allowed = to.path === '/admin/job-info' ? [2] : to.path === '/tutor-dashboard' ? [2, 4] : null
  if (!allowed) return true
  const token = localStorage.getItem('token')
  if (!token) return { path: '/login' }
  try {
    const response = await fetch(`${API_CONFIG.BASE_URL}/api/user/getUserInfo`, { headers: { Authorization: token } })
    if (!response.ok) return { path: '/login' }
    const result = await response.json()
    return allowed.includes(Number(result.data?.userRole)) ? true : { path: '/profile' }
  } catch {
    return { path: '/login' }
  }
})

export default router
