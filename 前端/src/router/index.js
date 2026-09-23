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
import WelcomeView from '../views/WelcomeView.vue'
import DashboardView from '../views/DashboardView.vue'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'welcome', component: WelcomeView, meta: { entryFlow: true } },
    { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { entryFlow: true, requiresAuth: true } },
    { path: '/assistant', name: 'agent', component: AgentView, meta: { requiresAuth: true } },
    { path: '/graph', name: 'graph', component: GraphView, meta: { requiresAuth: true } },
    { path: '/login', name: 'login', component: LoginView, meta: { entryFlow: true } },
    { path: '/profile', name: 'profile', component: UserCenterView, meta: { requiresAuth: true } },

    // 🌟 这里保留一个图谱管理后台即可
    {
      path: '/admin/job-info',
      name: 'JobInfoAdmin',
      component: JobInfoAdmin,
      meta: { requiresAuth: true }
    },

    {
      path: '/compare',
      name: 'JobCompare',
      component: JobCompareView,
      meta: { requiresAuth: true }
    },
    {
      path: '/job-detail',
      name: 'JobDetail',
      component: JobDetailView,
      meta: { requiresAuth: true }
    },
    {
      path: '/ai-score',
      name: 'AiScore',
      component: () => import('../views/AiScoreView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/multi-agent',
      name: 'MultiAgent',
      component: () => import('../views/MultiAgentView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/growth',
      name: 'Growth',
      component: () => import('../views/GrowthView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/tutor-dashboard',
      name: 'TutorDashboard',
      component: TutorDashboardView,
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
