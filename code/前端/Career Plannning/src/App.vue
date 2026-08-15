<template>
  <div class="app-layout">
    
    <aside class="sidebar">
      <div class="logo-area">
        <span class="logo-icon">✨</span>
        <span class="logo-text">AI职业规划师</span>
      </div>

      <button class="new-chat-btn" @click="openWizard">
        <span style="font-size: 1.2rem;">+</span> 新建职业规划
      </button>

      <div class="nav-section">
        <p class="section-title">核心功能</p>
        <nav class="nav-menu">
          <router-link to="/" class="nav-item">
            <span class="nav-icon">💬</span>
            <span class="nav-text">智能体对话</span>
          </router-link>
          
          <router-link to="/graph" class="nav-item">
            <span class="nav-icon">🌌</span>
            <span class="nav-text">职业星图</span>
          </router-link>

          <router-link to="/ai-score" class="nav-item">
            <span class="nav-icon">📊</span>
            <span class="nav-text">AI 能力测评</span>
          </router-link>

          <router-link to="/multi-agent" class="nav-item">
            <span class="nav-icon">🧠</span>
            <span class="nav-text">多智能体中枢</span>
          </router-link>

          <router-link v-if="userRole === 4" to="/tutor-dashboard" class="nav-item tutor-item">
            <span class="nav-icon">📈</span>
            <span class="nav-text">学生就业大盘</span>
          </router-link>

          <router-link to="/profile" class="nav-item">
            <span class="nav-icon">👤</span>
            <span class="nav-text">个人中心</span>
          </router-link>

          <router-link v-if="isAdmin" to="/admin/job-info" class="nav-item admin-item">
            <span class="nav-icon">⚙️</span>
            <span class="nav-text">图谱管理中枢</span>
          </router-link>
        </nav>
      </div>

      <div class="recommend-section">
        <p class="section-title">🌟 推荐专属路线</p>
        <div class="recommend-list">
          <div class="recommend-item" @click="openReport('dachang')">
            <span class="item-icon">🚀</span>
            <span class="item-text">互联网大厂晋升通道</span>
          </div>
          <div class="recommend-item" @click="openReport('kaoyan')">
            <span class="item-icon">⚖️</span>
            <span class="item-text">跨专业/考研就业对比</span>
          </div>
          <div class="recommend-item" @click="openReport('pm')">
            <span class="item-icon">💼</span>
            <span class="item-text">零基础转行产品经理</span>
          </div>
        </div>
      </div>

      <div class="user-profile-wrapper">
        <div v-if="showUserMenu" class="user-popover">
          <router-link to="/profile" class="popover-item" @click="showUserMenu = false">
            <span class="popover-icon">⚙️</span>
            <span class="popover-text">个人中心</span>
          </router-link>
          <div class="popover-item logout" @click="handleLogout">
            <span class="popover-icon">🚪</span>
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
              {{ isLogged ? (isAdmin ? '🔴 超级管理员在线' : (userRole === 4 ? '🟣 专属导师在线' : '🟢 在线')) : '点击登录同步数据 →' }}
            </span>
          </div>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" :key="$route.fullPath" />
        </transition>
      </router-view>
    </main>

    <transition name="modal-zoom">
      <div v-if="reportVisible" class="modal-overlay" @click.self="closeReport">
        <div class="custom-modal report-modal">
          <div class="modal-header">
            <div class="header-left">
              <span class="modal-badge">🔥 深度行研数据</span>
              <h3 class="gradient-title">{{ currentReport.title }}</h3>
            </div>
            <button class="close-modal-btn" @click="closeReport">✕</button>
          </div>
          <div class="modal-body" v-html="currentReport.content"></div>
          
          <div class="modal-footer">
            <p class="footer-note">数据更新至 2025 年 Q1 | AI 职业大模型实时推演</p>
            <button class="footer-btn" @click="closeReport">阅毕返回</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-zoom">
      <div v-if="wizardVisible" class="modal-overlay" @click.self="closeWizard">
        <div class="custom-modal wizard-modal">
          <div class="modal-header">
            <div class="header-left">
              <span class="modal-badge wizard-badge">AI 专属定制引擎</span>
              <h3 class="gradient-title">开启您的专属规划蓝图</h3>
            </div>
            <button class="close-modal-btn" @click="closeWizard">✕</button>
          </div>
          <div class="modal-body wizard-body">
            <div class="wizard-step">
              <label>1. 您目前所处的阶段是？</label>
              <div class="options-grid">
                <div class="option-card" :class="{'active': wizardForm.status === '在校学生'}" @click="selectStatus('在校学生')">🎓 在校学生</div>
                <div class="option-card" :class="{'active': wizardForm.status === '应届毕业生'}" @click="selectStatus('应届毕业生')">🎓 应届毕业生</div>
                <div class="option-card" :class="{'active': wizardForm.status === '职场新人(0-3年)'}" @click="selectStatus('职场新人(0-3年)')">💼 职场新人</div>
                <div class="option-card" :class="{'active': wizardForm.status === '资深职场人'}" @click="selectStatus('资深职场人')">🏆 资深职场人</div>
              </div>
            </div>
            <div class="wizard-step">
              <label>2. 您期望的目标行业或岗位？</label>
              <input type="text" v-model="wizardForm.target" placeholder="例如：前端开发、新能源产品经理..." class="wizard-input" />
            </div>
            <div class="wizard-step">
              <label>3. 您当前最迫切的诉求是什么？</label>
              <div class="options-grid">
                <div class="option-card" :class="{'active': wizardForm.need === '简历优化与包装'}" @click="selectNeed('简历优化与包装')">📄 简历优化</div>
                <div class="option-card" :class="{'active': wizardForm.need === '面试技巧与模拟'}" @click="selectNeed('面试技巧与模拟')">🎤 面试技巧</div>
                <div class="option-card" :class="{'active': wizardForm.need === '跨行跳槽路径指引'}" @click="selectNeed('跨行跳槽路径指引')">🛤️ 跨行跳槽</div>
                <div class="option-card" :class="{'active': wizardForm.need === '核心技能快速突破'}" @click="selectNeed('核心技能快速突破')">⚡ 技能突破</div>
              </div>
            </div>
          </div>
          
          <div class="modal-footer">
            <p class="footer-note" style="color: #10B981;">AI 正在收集数据并准备生成...</p>
            <button class="footer-btn start-ai-btn" @click="generatePlan" :disabled="!wizardForm.status || !wizardForm.target || !wizardForm.need">
              ✨ 启动 AI 定制引擎
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

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
          <p>✅ <strong>能力重心：</strong> 熟练使用工具、文档撰写规范、逻辑闭环。</p>
          <p>💰 <strong>市场行情：</strong> 年包 25w-35w (含年终及小额激励)。</p>
        </div>

        <h4>2. P6/T5 骨干层：独立战场指挥官 (晋升最难坎)</h4>
        <p><strong>核心要求：</strong> 负责一个完整业务模块。不仅要“做对”，更要“为什么这么做”。具备跨团队协作和抗压能力。</p>
        <ul class="tag-list">
          <li><span class="tag">Owner意识</span></li>
          <li><span class="tag">复杂场景解决</span></li>
          <li><span class="tag">项目管理</span></li>
        </ul>
        <div class="salary-box">📈 核心红利：此职级开始涉及股票期权奖励，年包可达 45w-70w。</div>

        <h4>3. P7/T8 专家/组长：业务决策与影响力</h4>
        <p><strong>关键动作：</strong> 制定本业务线的技术/产品路径，能够沉淀通用方法论，培养下属。晋升需通过集团评审委员会（评委面试）。</p>
        <div class="report-alert">💡 <strong>AI 洞察：</strong> 大厂目前趋向扁平化，P6 到 P7 的淘汰率约为 40%，核心差异点在于“对业务结果的深度负责度”。</div>
        
        <h4>📊 2025 大厂招聘门槛动态</h4>
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
          <p>❌ <strong>风险预警：</strong> 2025 考研报录比预计达 4.2:1。若失败，错过应届生黄金校招期，空窗期代价极大。</p>
          <p>💰 <strong>隐形亏损：</strong> 备考1年+读研3年，共计 4 年无薪资，隐形成本约 60w-80w。</p>
        </div>

        <h4>B. 直接就业的“实战积累”分析</h4>
        <p><strong>适合场景：</strong> 目标岗位为产品运营、前端开发、销售管理等更看重“作品集”和“项目经验”的领域。</p>
        <ul class="task-list">
          <li><strong>前 1-2 年：</strong> 快速积累真实商业项目，建立职业信用。</li>
          <li><strong>第 3 年：</strong> 凭借实战经验跳槽，薪资涨幅通常超过硕士起薪。</li>
        </ul>

        <h4>📊 核心对比矩阵 (决策公式：Q = (S * I) / C)</h4>
        <table class="report-table">
          <tr><th>决策因素</th><th>考研派</th><th>实战派</th></tr>
          <tr><td>职业上限</td><td>更高，利于管理层跃迁</td><td>由具体项目战绩决定</td></tr>
          <tr><td>抗风险力</td><td>强（学历永久生效）</td><td>中（依赖所在行业景气度）</td></tr>
          <tr><td>首月薪资</td><td>8k - 15k (硕士价)</td><td>5k - 9k (本科价)</td></tr>
        </table>
        
        <div class="report-alert">⚖️ <strong>AI 建议：</strong> 若你现在的专业是文史哲且想进互联网，建议直接自学转行；若你想进国企大行，请务必考研。</div>
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
        <div class="salary-box">🔥 关键：去 Github 或各大社区找一份 PRD 模版，练习写 10 份以上的功能描述。</div>

        <h4>阶段 3：作品集包装与面试黑话 (Day 51-100)</h4>
        <p><strong>核心：</strong> 没有实习经历，就创造项目。挑选一个冷门行业（如智慧养老），从 0 到 1 构思一款产品，画出原型图，写好需求文档。</p>
        <ul class="task-list">
          <li><strong>面试黑话速成：</strong> MVP、用户画像、UV/PV、转化漏斗、埋点采集、敏捷开发。</li>
          <li><strong>项目包装：</strong> 将你的分析报告整理成精美 PDF，放在简历显眼位置。</li>
        </ul>

        <div class="report-alert">🎤 <strong>AI 面试秘籍：</strong> 面试官问“你觉得好的产品是什么？”，不要回答“好用”，要回答“能为公司创造商业价值，同时降低用户成本”。</div>
      </div>
    `
  }
}

const openReport = (type) => { currentReport.value = reportDatabase[type]; reportVisible.value = true }
const closeReport = () => { reportVisible.value = false }

const wizardVisible = ref(false)
const wizardForm = reactive({ status: '', target: '', need: '' })
const selectStatus = (val) => { wizardForm.status = val }
const selectNeed = (val) => { wizardForm.need = val }
const openWizard = () => { wizardForm.status = ''; wizardForm.target = ''; wizardForm.need = ''; wizardVisible.value = true }
const closeWizard = () => { wizardVisible.value = false }
const generatePlan = () => {
  const prompt = `你好，AI 专属领航员！我目前的身份阶段是【${wizardForm.status}】，我期望未来的目标行业或岗位是【${wizardForm.target}】，我现在最迫切的核心诉求是【${wizardForm.need}】。请根据我的具体情况，为我量身定制一份详细、专业且可落地的职业发展蓝图和接下来的行动指南。`
  closeWizard()
  router.push({ path: '/', query: { autoPrompt: prompt, t: Date.now() } })
}
</script>

<style>
body { margin: 0; background-color: #F4F7FC; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; color: #2C3E50; }
.app-layout { display: flex; height: 100vh; width: 100vw; overflow: hidden; position: relative; }
.sidebar { width: 280px; min-width: 280px; flex-shrink: 0; background: linear-gradient(135deg, #f0f9ff, #e0f2fe); border-right: 1px solid #d1e0f0; display: flex; flex-direction: column; padding: 25px 20px; box-sizing: border-box; z-index: 10; box-shadow: 2px 0 15px rgba(0, 0, 0, 0.05); transition: all 0.3s ease;}
.logo-area { display: flex; align-items: center; gap: 12px; font-size: 1.3rem; font-weight: 700; padding: 10px 10px 25px 10px; color: #1e3a8a; background: rgba(255, 255, 255, 0.7); border-radius: 12px; margin-bottom: 15px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);}
.logo-area .logo-icon { font-size: 1.8rem; animation: pulse 2s infinite; }
@keyframes pulse { 0% { transform: scale(1); } 50% { transform: scale(1.1); } 100% { transform: scale(1); } }

.new-chat-btn { background: linear-gradient(135deg, #4A90E2, #357ABD); color: white; border: none; padding: 14px; border-radius: 12px; font-weight: 600; font-size: 1rem; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 10px; transition: all 0.3s ease; margin-bottom: 25px; box-shadow: 0 4px 15px rgba(74, 144, 226, 0.3); position: relative; overflow: hidden;}
.new-chat-btn::before { content: ''; position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent); transition: 0.5s; }
.new-chat-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(74, 144, 226, 0.4); }

.section-title { font-size: 0.85rem; color: #475569; font-weight: 600; padding: 0 10px 8px 10px; margin-bottom: 12px; margin-top: 0; border-bottom: 1px solid rgba(148, 163, 184, 0.2); display: flex; align-items: center; gap: 8px; }
.section-title::before { content: ''; width: 4px; height: 16px; background: linear-gradient(to bottom, #4A90E2, #357ABD); border-radius: 2px; }
.nav-section { margin-bottom: 25px; }
.nav-menu { display: flex; flex-direction: column; gap: 6px; }
.nav-item { text-decoration: none; color: #334155; padding: 12px 14px; border-radius: 10px; font-size: 1rem; font-weight: 500; transition: all 0.3s ease; display: flex; align-items: center; gap: 12px; background: rgba(255, 255, 255, 0.6); border: 1px solid rgba(226, 232, 240, 0.5); box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02); }
.nav-item:hover { background-color: rgba(255, 255, 255, 0.9); transform: translateX(4px); box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05); }
.nav-item.router-link-active { background: linear-gradient(to right, #e8f4ff, #d1e8ff); color: #1e40af; font-weight: 600; border-left: 3px solid #4A90E2; box-shadow: 0 4px 12px rgba(74, 144, 226, 0.15); }

.nav-item.tutor-item { margin-top: 8px; background: linear-gradient(to right, #eff6ff, #e0e7ff); border: 1px solid #c7d2fe; color: #4f46e5; }
.nav-item.admin-item { margin-top: 8px; background: linear-gradient(to right, #fff1f2, #ffe4e6); border: 1px solid #fecdd3; color: #e11d48;}

.recommend-section { flex: 1; overflow-y: auto; padding: 5px 0; }
.recommend-list { display: flex; flex-direction: column; gap: 8px; }
.recommend-item { padding: 14px 12px; font-size: 0.9rem; font-weight: 500; color: #334155; cursor: pointer; border-radius: 10px; background: rgba(255, 255, 255, 0.7); border: 1px solid rgba(226, 232, 240, 0.5); transition: all 0.3s ease; position: relative; overflow: hidden; display: flex; align-items: center; gap: 10px; }
.recommend-item::before { content: ''; position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent); transition: 0.5s; }
.recommend-item:hover::before { left: 100%; }
.recommend-item:hover { background: rgba(255, 255, 255, 0.9); color: #4A90E2; border-color: rgba(74, 144, 226, 0.3); transform: translateY(-2px); box-shadow: 0 6px 15px rgba(0,0,0,0.08); }
.item-icon { font-size: 1.1rem; }

.user-profile-wrapper { position: relative; margin-top: auto; border-top: 1px solid rgba(234, 236, 239, 0.7); padding: 15px 10px 10px 10px; background: rgba(255, 255, 255, 0.5); border-radius: 12px 12px 0 0; }
.user-popover { position: absolute; bottom: 100%; left: 0; width: 100%; background: white; border: 1px solid #EAECEF; border-radius: 12px; box-shadow: 0 -4px 20px rgba(0,0,0,0.08); padding: 8px; margin-bottom: 10px; z-index: 100; animation: popUp 0.2s ease-out; }
@keyframes popUp { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.popover-item { padding: 10px 12px; border-radius: 8px; cursor: pointer; font-size: 0.9rem; color: #475569; display: flex; align-items: center; gap: 8px; text-decoration: none; transition: 0.2s; font-weight: 500; }
.popover-item:hover { background-color: #F1F5F9; }
.popover-item.logout { color: #EF4444; }
.user-profile { display: flex; align-items: center; gap: 12px; cursor: pointer; padding: 5px; border-radius: 8px; transition: 0.2s; }
.user-profile:hover { background-color: #F8FAFC; }
.avatar-small { width: 36px; height: 36px; background-color: #E8F0FE; color: #4A90E2; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-weight: bold; font-size: 1rem; transition: 0.3s; }
.avatar-small.is-logged { background: linear-gradient(135deg, #60A5FA, #4A90E2); color: white; box-shadow: 0 4px 10px rgba(74,144,226,0.3); }
.avatar-small.is-admin-avatar { background: linear-gradient(135deg, #FB7185, #E11D48); }
.avatar-small.is-tutor-avatar { background: linear-gradient(135deg, #A78BFA, #7C3AED); color: white;}
.user-info { display: flex; flex-direction: column; }
.username { font-size: 0.9rem; font-weight: 600; color: #2C3E50; }
.user-status { font-size: 0.75rem; font-weight: 600; margin-top: 2px; }

.main-content { flex: 1; min-width: 0; display: flex; justify-content: center; align-items: center; padding: 20px; box-sizing: border-box; overflow: hidden; }

/* 弹窗核心样式 */
.modal-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(15, 23, 42, 0.55); backdrop-filter: blur(8px); z-index: 9999; display: flex; justify-content: center; align-items: center; }
.custom-modal { background: #FFFFFF; border-radius: 20px; box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25); display: flex; flex-direction: column; overflow: hidden; }
.modal-header { padding: 24px 30px; border-bottom: 1px solid #F1F5F9; display: flex; justify-content: space-between; align-items: center; background: #FFFFFF; }
.header-left { display: flex; flex-direction: column; gap: 6px; }
.modal-badge { align-self: flex-start; font-size: 0.75rem; font-weight: bold; color: #4A90E2; background: rgba(74, 144, 226, 0.1); padding: 4px 10px; border-radius: 20px; letter-spacing: 1px; }
.gradient-title { margin: 0; font-size: 1.4rem; font-weight: 800; background: linear-gradient(135deg, #1E293B, #3B82F6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.close-modal-btn { background: #F8FAFC; border: 1px solid #E2E8F0; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; transition: 0.3s; font-size: 1.2rem; display: flex; align-items: center; justify-content: center; color: #94A3B8;}
.close-modal-btn:hover { background: #FEE2E2; color: #EF4444; border-color: #FECACA; transform: rotate(90deg); }

.modal-body { flex: 1; padding: 30px 40px; overflow-y: auto; background: #FAFAF9; }

/* 🌟 核心补齐：底部区域与按钮的所有样式 */
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

.start-ai-btn { 
  background: linear-gradient(135deg, #10B981, #059669); 
  font-size: 1.05rem; 
  padding: 12px 28px; 
  box-shadow: 0 4px 10px rgba(16, 185, 129, 0.2);
}
.start-ai-btn:disabled { 
  opacity: 0.5; 
  filter: grayscale(100%); 
  cursor: not-allowed; 
  transform: none; 
  box-shadow: none;
}

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
.wizard-modal { width: 92%; max-width: 650px; }
.wizard-badge { background: rgba(16, 185, 129, 0.1); color: #10B981; }
.wizard-body { display: flex; flex-direction: column; gap: 25px; }
.wizard-step label { font-size: 1.05rem; font-weight: bold; color: #1E293B; margin-bottom: 12px; display: block; }
.options-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.option-card { padding: 14px; border: 1px solid #E2E8F0; border-radius: 12px; text-align: center; cursor: pointer; transition: 0.2s; background: #F8FAFC; font-weight: 600; color: #64748B; font-size: 0.95rem; }
.option-card:hover { border-color: #93C5FD; background: #EFF6FF; color: #3B82F6; }
.option-card.active { background: #EFF6FF; border-color: #3B82F6; color: #1D4ED8; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);}
.wizard-input { width: 100%; padding: 14px 16px; border: 2px solid #E2E8F0; border-radius: 12px; font-size: 1rem; outline: none; box-sizing: border-box; background: #F8FAFC; font-family: inherit;}
.wizard-input:focus { border-color: #3B82F6; background: #FFFFFF; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1); }

.page-fade-enter-active, .page-fade-leave-active { transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1); }
.page-fade-enter-from, .page-fade-leave-to { opacity: 0; transform: translateY(15px); }
</style>