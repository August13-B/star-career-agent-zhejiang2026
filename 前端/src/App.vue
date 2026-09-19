<template>
  <div class="app-layout">
    
    <aside class="sidebar">
      <div class="logo-area">
        <span class="logo-mark"><AppIcon name="compass" :size="19" /></span>
        <span class="logo-text">AI职业规划师</span>
      </div>

      <div class="nav-section">
        <p class="section-title">核心功能</p>
        <nav class="nav-menu">
          <router-link to="/" class="nav-item">
            <AppIcon name="chat" class="nav-icon" :size="17" />
            <span class="nav-text">智能体对话</span>
          </router-link>
          
          <router-link to="/graph" class="nav-item">
            <AppIcon name="graph" class="nav-icon" :size="17" />
            <span class="nav-text">职业星图</span>
          </router-link>

          <router-link to="/ai-score" class="nav-item">
            <AppIcon name="radar" class="nav-icon" :size="17" />
            <span class="nav-text">AI 能力测评</span>
          </router-link>

          <router-link to="/multi-agent" class="nav-item">
            <AppIcon name="cpu" class="nav-icon" :size="17" />
            <span class="nav-text">多智能体中枢</span>
          </router-link>

          <router-link v-if="userRole === 4" to="/tutor-dashboard" class="nav-item tutor-item">
            <AppIcon name="dashboard" class="nav-icon" :size="17" />
            <span class="nav-text">学生就业大盘</span>
          </router-link>

          <router-link to="/profile" class="nav-item">
            <AppIcon name="user" class="nav-icon" :size="17" />
            <span class="nav-text">个人中心</span>
          </router-link>

          <router-link v-if="isAdmin" to="/admin/job-info" class="nav-item admin-item">
            <AppIcon name="settings" class="nav-icon" :size="17" />
            <span class="nav-text">图谱管理中枢</span>
          </router-link>
        </nav>
      </div>

      <div class="recommend-section">
        <p class="section-title">推荐专属路线</p>
        <div class="recommend-list">
          <div class="recommend-item" @click="openReport('dachang')">
            <AppIcon name="trendUp" class="item-icon" :size="16" />
            <span class="item-text">互联网大厂晋升通道</span>
          </div>
          <div class="recommend-item" @click="openReport('kaoyan')">
            <AppIcon name="scale" class="item-icon" :size="16" />
            <span class="item-text">跨专业/考研就业对比</span>
          </div>
          <div class="recommend-item" @click="openReport('pm')">
            <AppIcon name="briefcase" class="item-icon" :size="16" />
            <span class="item-text">零基础转行产品经理</span>
          </div>
        </div>
      </div>

      <div class="user-profile-wrapper">
        <div v-if="showUserMenu" class="user-popover">
          <router-link to="/profile" class="popover-item" @click="showUserMenu = false">
            <AppIcon name="settings" class="popover-icon" :size="15" />
            <span class="popover-text">个人中心</span>
          </router-link>
          <div class="popover-item logout" @click="handleLogout">
            <AppIcon name="logout" class="popover-icon" :size="15" />
            <span class="popover-text">退出登录</span>
          </div>
        </div>

        <div class="user-profile" @click="toggleUserMenu">
          <div class="avatar-small" :class="{ 'is-logged': isLogged, 'is-admin-avatar': isAdmin, 'is-tutor-avatar': userRole === 4 }">
            {{ isLogged ? username.charAt(0) : '我' }}
          </div>
          <div class="user-info">
            <span class="username">{{ isLogged ? username : '未登录游客' }}</span>
            <span class="user-status" :style="{ color: isLogged ? (isAdmin ? '#E11D48' : (userRole === 4 ? '#7C3AED' : '#10B981')) : '#4A90E2' }">
              <i v-if="isLogged" class="status-dot"></i>
              {{ isLogged ? (isAdmin ? '超级管理员在线' : (userRole === 4 ? '专属导师在线' : '在线')) : '点击登录同步数据' }}
            </span>
          </div>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <!-- 多智能体页用 keep-alive 缓存：报告生成中切页/返回不会丢失进度（SSE 仍在后台累积） -->
          <keep-alive :include="['MultiAgentView']">
            <component :is="Component" :key="$route.fullPath" />
          </keep-alive>
        </transition>
      </router-view>
    </main>

    <transition name="modal-zoom">
      <div v-if="reportVisible" class="modal-overlay" @click.self="closeReport">
        <div class="custom-modal report-modal">
          <div class="modal-header">
            <div class="header-left">
              <span class="modal-badge"><AppIcon name="file" :size="13" /> 深度行研数据</span>
              <h3 class="gradient-title">{{ currentReport.title }}</h3>
            </div>
            <button class="close-modal-btn" @click="closeReport"><AppIcon name="close" :size="17" /></button>
          </div>
          <div class="modal-body" v-html="currentReport.content"></div>
          
          <div class="modal-footer">
            <p class="footer-note">数据更新至 2025 年 Q1 | AI 职业大模型实时推演</p>
            <button class="footer-btn" @click="closeReport">阅毕返回</button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppIcon from './components/AppIcon.vue'

const router = useRouter()
const route = useRoute()

const isLogged = ref(false)
const username = ref('')
const showUserMenu = ref(false)
const isAdmin = ref(false) 
const userRole = ref(0) 

const checkLoginStatus = () => {
  const token = localStorage.getItem('token')
  if (token) {
    isLogged.value = true
    username.value = localStorage.getItem('userName') || '用户' 
    const localRole = localStorage.getItem('userRole')
    isAdmin.value = (String(localRole) === '2')
    userRole.value = Number(localRole) || 0
  } else {
    isLogged.value = false; username.value = '未登录游客'; isAdmin.value = false; userRole.value = 0
  }
}

onMounted(() => { checkLoginStatus() })
watch(() => route.path, () => { checkLoginStatus(); showUserMenu.value = false })

const toggleUserMenu = () => {
  if (isLogged.value) showUserMenu.value = !showUserMenu.value
  else router.push('/login')
}

const handleLogout = () => {
  localStorage.clear(); isLogged.value = false; isAdmin.value = false; userRole.value = 0; router.push('/login')
}

const reportVisible = ref(false)
const currentReport = ref({ title: '', content: '' })

const reportDatabase = {
  dachang: {
    title: '互联网大厂岗位职级与晋升逻辑全解析',
    content: `
      <div class="report-content">
        <div class="report-banner blue-banner">基于 2025 腾讯(T)、阿里(P)、字节(L)、美团等一线大厂职级对标体系</div>
        
        <h4>1. P5/T4 初级执行层：从学生到职场人的跃迁</h4>
        <p><strong>核心定义：</strong> 能够独立负责一个明确的需求点，保质保量完成导师分配的任务。</p>
        <div class="vs-card">
          <p><strong>能力重心：</strong> 熟练使用工具、文档撰写规范、逻辑闭环。</p>
          <p><strong>市场行情：</strong> 年包 25w-35w (含年终及小额激励)。</p>
        </div>

        <h4>2. P6/T5 骨干层：独立战场指挥官 (晋升最难坎)</h4>
        <p><strong>核心要求：</strong> 负责一个完整业务模块。不仅要“做对”，更要“为什么这么做”。具备跨团队协作和抗压能力。</p>
        <ul class="tag-list">
          <li><span class="tag">Owner意识</span></li>
          <li><span class="tag">复杂场景解决</span></li>
          <li><span class="tag">项目管理</span></li>
        </ul>
        <div class="salary-box">核心红利：此职级开始涉及股票期权奖励，年包可达 45w-70w。</div>

        <h4>3. P7/T8 专家/组长：业务决策与影响力</h4>
        <p><strong>关键动作：</strong> 制定本业务线的技术/产品路径，能够沉淀通用方法论，培养下属。晋升需通过集团评审委员会（评委面试）。</p>
        <div class="report-alert"><strong>AI 洞察：</strong> 大厂目前趋向扁平化，P6 到 P7 的淘汰率约为 40%，核心差异点在于“对业务结果的深度负责度”。</div>
        
        <h4>2025 大厂招聘门槛动态</h4>
        <table class="report-table">
          <tr><th>维度</th><th>2022年</th><th>2025年展望</th></tr>
          <tr><td>学历要求</td><td>本科/211</td><td>重点院校/硕士为主</td></tr>
          <tr><td>项目深度</td><td>参与过项目</td><td>完整落地并有核心指标突破</td></tr>
          <tr><td>软素质</td><td>学习力</td><td>商业洞察力+韧性</td></tr>
        </table>
      </div>
    `
  },
  kaoyan: {
    title: '跨专业考研 vs 直接就业：2025 多维决策模型',
    content: `
      <div class="report-content">
        <div class="report-banner purple-banner">针对非名校背景、非天坑专业、寻求阶层跨越的决策建议</div>
        
        <h4>A. 跨专业考研的“投入产出比”分析</h4>
        <p><strong>适合场景：</strong> 目标进入金融券商、核心算法、体制内等“唯学历论”行业。考研是唯一洗白背景的通道。</p>
        <div class="vs-card">
          <p><strong>风险预警：</strong> 2025 考研报录比预计达 4.2:1。若失败，错过应届生黄金校招期，空窗期代价极大。</p>
          <p><strong>隐形亏损：</strong> 备考1年+读研3年，共计 4 年无薪资，隐形成本约 60w-80w。</p>
        </div>

        <h4>B. 直接就业的“实战积累”分析</h4>
        <p><strong>适合场景：</strong> 目标岗位为产品运营、前端开发、销售管理等更看重“作品集”和“项目经验”的领域。</p>
        <ul class="task-list">
          <li><strong>前 1-2 年：</strong> 快速积累真实商业项目，建立职业信用。</li>
          <li><strong>第 3 年：</strong> 凭借实战经验跳槽，薪资涨幅通常超过硕士起薪。</li>
        </ul>

        <h4>核心对比矩阵 (决策公式：Q = (S * I) / C)</h4>
        <table class="report-table">
          <tr><th>决策因素</th><th>考研派</th><th>实战派</th></tr>
          <tr><td>职业上限</td><td>更高，利于管理层跃迁</td><td>由具体项目战绩决定</td></tr>
          <tr><td>抗风险力</td><td>强（学历永久生效）</td><td>中（依赖所在行业景气度）</td></tr>
          <tr><td>首月薪资</td><td>8k - 15k (硕士价)</td><td>5k - 9k (本科价)</td></tr>
        </table>
        
        <div class="report-alert"><strong>AI 建议：</strong> 若你现在的专业是文史哲且想进互联网，建议直接自学转行；若你想进国企大行，请务必考研。</div>
      </div>
    `
  },
  pm: {
    title: '零基础转行产品经理 (PM) 2025 极速冲刺路线',
    content: `
      <div class="report-content">
        <div class="report-banner green-banner">本路线旨在帮助非互联网背景、零经验小白在 100 天内拿到 Offer</div>
        
        <h4>阶段 1：思维拆解与工具扫盲 (Day 1-20)</h4>
        <p><strong>重点：</strong> 不要纠结画图美感，要纠结逻辑链路。学习如何把一个 App 的功能点拆解成逻辑流程图。</p>
        <div class="tags-group">
          <span class="tag">Axure RP 10</span>
          <span class="tag">Figma</span>
          <span class="tag">Visio 逻辑流</span>
          <span class="tag">脑图思维</span>
        </div>

        <h4>阶段 2：输出一份“能打”的竞品分析 (Day 21-50)</h4>
        <p><strong>避坑指南：</strong> 不要写成功能对比表！要从商业模式、用户分层、体验地图、核心壁垒四个维度深度剖析。</p>
        <div class="salary-box">关键：去 Github 或各大社区找一份 PRD 模版，练习写 10 份以上的功能描述。</div>

        <h4>阶段 3：作品集包装与面试黑话 (Day 51-100)</h4>
        <p><strong>核心：</strong> 没有实习经历，就创造项目。挑选一个冷门行业（如智慧养老），从 0 到 1 构思一款产品，画出原型图，写好需求文档。</p>
        <ul class="task-list">
          <li><strong>面试黑话速成：</strong> MVP、用户画像、UV/PV、转化漏斗、埋点采集、敏捷开发。</li>
          <li><strong>项目包装：</strong> 将你的分析报告整理成精美 PDF，放在简历显眼位置。</li>
        </ul>

        <div class="report-alert"><strong>AI 面试秘籍：</strong> 面试官问“你觉得好的产品是什么？”，不要回答“好用”，要回答“能为公司创造商业价值，同时降低用户成本”。</div>
      </div>
    `
  }
}

const openReport = (type) => { currentReport.value = reportDatabase[type]; reportVisible.value = true }
const closeReport = () => { reportVisible.value = false }
</script>

<style>
body { margin: 0; background-color: #F4F7FC; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; color: #2C3E50; }
.app-layout { display: flex; height: 100vh; width: 100vw; overflow: hidden; position: relative; }
.sidebar { width: 268px; min-width: 268px; flex-shrink: 0; background: #FBFDFF; border-right: 1px solid #E4EAF2; display: flex; flex-direction: column; padding: 20px 16px; box-sizing: border-box; z-index: 10; }
.logo-area { display: flex; align-items: center; gap: 10px; font-size: 1.05rem; font-weight: 700; letter-spacing: 0.3px; padding: 2px 4px 18px 4px; color: #1E3A8A; }
.logo-mark { display: inline-flex; align-items: center; justify-content: center; width: 30px; height: 30px; border-radius: 8px; background: #4A90E2; color: #FFFFFF; }

.section-title { font-size: 0.74rem; color: #64748B; font-weight: 600; letter-spacing: 0.7px; padding: 0 4px; margin: 0 0 10px 0; display: flex; align-items: center; gap: 7px; }
.section-title::before { content: ''; width: 3px; height: 12px; background: #4A90E2; border-radius: 2px; }
.nav-section { margin-bottom: 22px; }
.nav-menu { display: flex; flex-direction: column; gap: 2px; }
.nav-item { text-decoration: none; color: #475569; padding: 9px 11px; border-radius: 7px; font-size: 0.9rem; font-weight: 500; transition: background 0.16s ease, color 0.16s ease; display: flex; align-items: center; gap: 10px; }
.nav-item:hover { background: #EEF4FB; color: #1E3A8A; }
.nav-item.router-link-active { background: #E8F1FC; color: #1D4ED8; font-weight: 600; }

.nav-item.tutor-item { color: #4F46E5; }
.nav-item.admin-item { color: #BE123C; }

.recommend-section { flex: 1; overflow-y: auto; padding: 5px 0; }
.recommend-list { display: flex; flex-direction: column; gap: 8px; }
.recommend-item { padding: 10px 11px; font-size: 0.85rem; font-weight: 500; color: #475569; cursor: pointer; border-radius: 7px; transition: background 0.16s ease, color 0.16s ease; display: flex; align-items: center; gap: 9px; }
.recommend-item:hover { background: #EEF4FB; color: #1D4ED8; }
.item-icon { color: #64748B; }
.recommend-item:hover .item-icon { color: #4A90E2; }

.user-profile-wrapper { position: relative; margin-top: auto; border-top: 1px solid #E9EEF5; padding: 12px 4px 4px 4px; }
.user-popover { position: absolute; bottom: 100%; left: 0; width: 100%; background: #FFFFFF; border: 1px solid #E4EAF2; border-radius: 9px; box-shadow: 0 8px 24px rgba(15, 23, 42, 0.10); padding: 6px; margin-bottom: 8px; z-index: 100; }
@keyframes popUp { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.popover-item { padding: 10px 12px; border-radius: 8px; cursor: pointer; font-size: 0.9rem; color: #475569; display: flex; align-items: center; gap: 8px; text-decoration: none; transition: 0.2s; font-weight: 500; }
.popover-item:hover { background-color: #F1F5F9; }
.popover-item.logout { color: #EF4444; }
.user-profile { display: flex; align-items: center; gap: 12px; cursor: pointer; padding: 5px; border-radius: 8px; transition: 0.2s; }
.user-profile:hover { background-color: #F8FAFC; }
.avatar-small { width: 36px; height: 36px; background-color: #E8F0FE; color: #4A90E2; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-weight: bold; font-size: 1rem; transition: 0.3s; }
.avatar-small.is-logged { background: #4A90E2; color: white; }
.avatar-small.is-admin-avatar { background: #E11D48; }
.avatar-small.is-tutor-avatar { background: #7C3AED; color: white;}
.user-info { display: flex; flex-direction: column; }
.username { font-size: 0.88rem; font-weight: 600; color: #1E293B; }
.user-status { font-size: 0.72rem; font-weight: 500; margin-top: 2px; display: flex; align-items: center; gap: 5px; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; display: inline-block; }

.main-content { flex: 1; min-width: 0; display: flex; justify-content: center; align-items: center; padding: 20px; box-sizing: border-box; overflow: hidden; }

/* 弹窗核心样式 */
.modal-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(15, 23, 42, 0.55); backdrop-filter: blur(8px); z-index: 9999; display: flex; justify-content: center; align-items: center; }
.custom-modal { background: #FFFFFF; border-radius: 12px; box-shadow: 0 16px 40px rgba(15, 23, 42, 0.18); display: flex; flex-direction: column; overflow: hidden; }
.modal-header { padding: 24px 30px; border-bottom: 1px solid #F1F5F9; display: flex; justify-content: space-between; align-items: center; background: #FFFFFF; }
.header-left { display: flex; flex-direction: column; gap: 6px; }
.modal-badge { align-self: flex-start; display: inline-flex; align-items: center; gap: 5px; font-size: 0.72rem; font-weight: 600; color: #2563EB; background: #EFF6FF; padding: 3px 9px; border-radius: 6px; letter-spacing: 0.3px; }
.gradient-title { margin: 0; font-size: 1.2rem; font-weight: 700; color: #1E293B; }
.close-modal-btn { background: #FFFFFF; border: 1px solid #E2E8F0; width: 30px; height: 30px; border-radius: 8px; cursor: pointer; transition: 0.16s; display: flex; align-items: center; justify-content: center; color: #94A3B8; }
.close-modal-btn:hover { background: #F8FAFC; color: #475569; border-color: #CBD5E1; }

.modal-body { flex: 1; padding: 30px 40px; overflow-y: auto; background: #FAFAF9; }

/* 核心补齐：底部区域与按钮的所有样式 */
.modal-footer { padding: 16px 30px; background: #FFFFFF; border-top: 1px solid #F1F5F9; display: flex; justify-content: space-between; align-items: center; }
.footer-note { margin: 0; font-size: 0.85rem; color: #94A3B8; font-weight: 500; }

.footer-btn { 
  background: linear-gradient(135deg, #60A5FA, #4A90E2); 
  color: white; 
  border: none; 
  padding: 10px 24px; 
  border-radius: 10px; 
  font-weight: bold; 
  cursor: pointer; 
  transition: all 0.2s; 
  box-shadow: 0 4px 10px rgba(74, 144, 226, 0.2); 
}
.footer-btn:hover:not(:disabled) { 
  transform: translateY(-2px); 
  box-shadow: 0 6px 15px rgba(74, 144, 226, 0.3); 
}

/* .start-ai-btn 已随向导弹窗下线 */

/* 报告面板样式 */
.report-modal { width: 92%; max-width: 820px; max-height: 85vh; }
.report-content h4 { font-size: 1.15rem; color: #0F172A; margin: 25px 0 10px 0; border-left: 4px solid #4A90E2; padding-left: 12px; }
.report-banner { padding: 15px 25px; border-radius: 10px; font-weight: bold; margin-bottom: 25px; }
.blue-banner { background: #EFF6FF; color: #1D4ED8; border-left: 5px solid #3B82F6; }
.purple-banner { background: #F5F3FF; color: #6D28D9; border-left: 5px solid #8B5CF6; }
.green-banner { background: #F0FDF4; color: #15803D; border-left: 5px solid #22C55E; }
.salary-box { background: #FFFBEB; border: 1px solid #FEF3C7; border-left: 4px solid #F59E0B; padding: 15px; border-radius: 8px; color: #B45309; font-weight: 600; margin-bottom: 20px; display: block; }
.vs-card { background: #FFFFFF; border: 1px solid #E2E8F0; padding: 20px; border-radius: 12px; margin-bottom: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.03); }
.report-table { width: 100%; border-collapse: collapse; margin: 25px 0; font-size: 0.95rem; background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.02); }
.report-table th, .report-table td { border: 1px solid #F1F5F9; padding: 16px; text-align: left; }
.report-table th { background: #F8FAFC; color: #1E293B; font-weight: bold; }

/* 向导弹窗样式 */
/* 向导弹窗样式已移除（“新建职业规划”入口下线） */

.page-fade-enter-active, .page-fade-leave-active { transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1); }
.page-fade-enter-from, .page-fade-leave-to { opacity: 0; transform: translateY(15px); }
</style>