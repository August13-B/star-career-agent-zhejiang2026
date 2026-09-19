<template>
  <div class="report-page">
    <div class="workspace">
      <header class="page-header">
        <div class="title-block">
          <span class="badge"><AppIcon name="cpu" :size="13" /> 多智能体协同</span>
          <h1>职业规划报告生成</h1>
          <p class="subtitle">6 个专职智能体按顺序协作，逐段实时输出</p>
        </div>
        <div class="header-actions">
          <button class="btn ghost" @click="reset" :disabled="running">清空</button>
          <button class="btn primary" @click="generate" :disabled="running || !userId">
            <AppIcon name="sparkle" :size="15" />
            {{ running ? '生成中…' : '生成职业报告' }}
          </button>
        </div>
      </header>

      <div v-if="!userId" class="notice warn">请先登录后再生成报告。</div>

      <section class="input-panel">
        <label class="input-label">补充说明（可选）</label>
        <textarea
          v-model="extraInput"
          class="input-area"
          rows="2"
          placeholder="例如：目标岗位是后端开发；可投入学习时间每周 10 小时；希望留在杭州…"
          :disabled="running"
        ></textarea>
      </section>

      <p v-if="running" class="progress-hint">平台正在生成… 已输出 {{ progressChars }} 字（共 6 个智能体，预计 3~4 分钟）</p>

      <section class="agents">
        <article
          v-for="(agent, i) in agents"
          :key="agent.key"
          class="agent-card"
          :class="agent.status"
        >
          <div class="agent-head">
            <div class="agent-index">{{ i + 1 }}</div>
            <div class="agent-meta">
              <h3>{{ agent.name }}</h3>
              <span class="agent-desc">{{ agent.desc }}</span>
            </div>
            <div class="agent-status">
              <span v-if="agent.status === 'waiting'" class="tag waiting">等待</span>
              <span v-else-if="agent.status === 'running'" class="tag running">
                <i class="dot"></i> 生成中
              </span>
              <span v-else-if="agent.status === 'done'" class="tag done">完成</span>
              <span v-else-if="agent.status === 'error'" class="tag error">失败</span>
            </div>
          </div>
          <div class="agent-body" v-if="agent.content">
            <div class="markdown" v-html="renderMarkdown(agent.content)"></div>
          </div>
          <div class="agent-placeholder" v-else>
            {{ agent.status === 'running' ? '正在检索知识库并生成…' : '等待前序智能体完成' }}
          </div>
        </article>
      </section>

      <footer class="result-bar" v-if="finished">
        <div class="result-info">
          <AppIcon name="check" :size="15" />
          <span>报告已生成{{ savedHint }}<template v-if="reportName">：{{ reportName }}</template></span>
          <span v-if="reportId" class="report-id">ID {{ reportId }}</span>
        </div>
        <router-link class="btn ghost" to="/profile">前往个人中心查看</router-link>
      </footer>

      <div v-if="errorMsg" class="notice error">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import AppIcon from '../components/AppIcon.vue'

// 组件名：供 App.vue 的 <keep-alive :include="['MultiAgentView']"> 命中，
// 保证生成报告期间切页/返回不丢进度（后台 fetch 仍在累积）
defineOptions({ name: 'MultiAgentView' })

const router = useRouter()

const AGENT_DEFS = [
  { key: 'profile_analysis', name: '画像分析', desc: '能力现状与优劣势诊断' },
  { key: 'career_exploration', name: '职业探索', desc: '岗位要求·薪资·行业趋势' },
  { key: 'goal_setting', name: '目标设定', desc: '1 / 3 / 5 年职业目标' },
  { key: 'path_planning', name: '路径规划', desc: '晋升链路与关键跃迁节点' },
  { key: 'action_planning', name: '行动计划', desc: '分阶段任务与验收标准' },
  { key: 'report_composition', name: '报告整合', desc: '一致性校验与最终建议' }
]

const userId = ref(localStorage.getItem('userId') || '')
const extraInput = ref('')
const running = ref(false)
const finished = ref(false)
const savedHint = ref('')
const progressChars = ref(0)
const reportName = ref('')
const reportId = ref('')
const errorMsg = ref('')
const agents = ref(AGENT_DEFS.map(a => ({ ...a, status: 'waiting', content: '' })))

const getUserInfo = async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    userId.value = ''
    return
  }
  try {
    const headers = { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` }
    const res = await axios.get('/api/user/getUserInfo', { headers })
    if (res.data?.data?.id) {
      // 64 位雪花 ID 保持字符串，切勿 Number()
      userId.value = String(res.data.data.id)
      localStorage.setItem('userId', userId.value)
    } else {
      // token 失效 / 用户已被清理：旧的 localStorage.userId 会直接导致报告落库外键失败
      localStorage.removeItem('userId')
      userId.value = ''
      alert('登录状态已失效，请重新登录后再生成报告')
      router.push('/login')
    }
  } catch (e) {
    localStorage.removeItem('userId')
    userId.value = ''
  }
}

const reset = () => {
  agents.value = AGENT_DEFS.map(a => ({ ...a, status: 'waiting', content: '' }))
  finished.value = false
  errorMsg.value = ''
  savedHint.value = ''
  reportName.value = ''
  reportId.value = ''
}

const markPreviousDone = (idx) => {
  for (let i = 0; i < idx; i++) {
    if (agents.value[i].status === 'running') agents.value[i].status = 'done'
  }
}

// 平台 done 帧携带完整分段内容 → 前端按段打字机渲染
const revealSegments = (list) => {
  if (!Array.isArray(list) || list.length === 0) {
    agents.value.forEach(a => { if (a.status !== 'error') a.status = 'done' })
    finished.value = true
    running.value = false
    return
  }
  let i = 0
  const typeNext = () => {
    if (i >= list.length) {
      agents.value.forEach(a => { if (a.status !== 'error') a.status = 'done' })
      finished.value = true
      running.value = false
      return
    }
    const seg = list[i]
    const idx = agents.value.findIndex(a => a.key === (seg.key || seg.name))
    if (idx < 0) { i++; typeNext(); return }
    for (let j = 0; j < idx; j++) {
      if (agents.value[j].status !== 'done') agents.value[j].status = 'done'
    }
    const card = agents.value[idx]
    card.status = 'running'
    card.content = ''
    const text = String(seg.content || '')
    let pos = 0
    // 每段约 1.5s 左右打完（步长自适应）
    const step = Math.max(12, Math.ceil(text.length / 90))
    const timer = setInterval(() => {
      pos += step
      card.content = text.slice(0, pos)
      if (pos >= text.length) {
        clearInterval(timer)
        card.content = text
        card.status = 'done'
        i++
        typeNext()
      }
    }, 16)
  }
  typeNext()
}

// 轮询句柄（刷新后可用 localStorage 里的 jobId 续跑）
let pollTimer = null
const stopPolling = () => { if (pollTimer) { clearTimeout(pollTimer); pollTimer = null } }

const generate = async () => {
  if (running.value) return
  stopPolling()
  reset()
  running.value = true
  progressChars.value = 0
  const token = localStorage.getItem('token') || ''
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {})
  }
  try {
    const resp = await fetch('/api/career-report/start', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        user_id: userId.value,
        content: extraInput.value.trim() ||
          '请为我生成一份完整的职业规划报告。若缺少我的画像信息，请基于岗位知识库与通用情况给出，并说明假设。'
      })
    })
    const data = await resp.json()
    if (data.code !== 10001 && data.code !== 200 && data.code !== 0) {
      throw new Error(data.message || '创建报告任务失败')
    }
    const jobId = data.data && data.data.jobId
    if (!jobId) throw new Error('未获取到 jobId')
    // 持久化 jobId：任务在平台侧独立运行，刷新页面后仍可续跑
    localStorage.setItem('reportJobId', jobId)
    startPolling(jobId)
  } catch (e) {
    errorMsg.value = `生成失败：${e.message}（请确认后端已启动、平台报告服务已就绪）`
    running.value = false
  }
}

// 轮询平台任务：running 期间推进卡片；done 后按段打字机渲染
const startPolling = (jobId) => {
  stopPolling()
  const poll = async () => {
    try {
      const token = localStorage.getItem('token') || ''
      const res = await axios.get(`/api/career-report/jobs/${jobId}`, {
        headers: token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {}
      })
      const d = (res.data && res.data.data) ? res.data.data : {}
      if (d.error) {
        errorMsg.value = String(d.error)
        const ra = agents.value.find(a => a.status === 'running')
        if (ra) ra.status = 'error'
        localStorage.removeItem('reportJobId')
        running.value = false
        stopPolling()
        return
      }
      progressChars.value = d.progressChars || progressChars.value
      const idx = agents.value.findIndex(a => a.key === d.currentAgent)
      if (idx >= 0) {
        markPreviousDone(idx)
        if (agents.value[idx].status === 'waiting') agents.value[idx].status = 'running'
      }
      if (Array.isArray(d.agentsDone)) {
        d.agentsDone.forEach(k => {
          const j = agents.value.findIndex(a => a.key === k)
          if (j >= 0) agents.value[j].status = 'done'
        })
      }
      if (d.status === 'done') {
        reportName.value = d.reportName || ''
        reportId.value = d.reportId ? String(d.reportId) : ''
        savedHint.value = d.saved === false ? '（但落库失败，详见后端日志）' : '并已保存'
        localStorage.removeItem('reportJobId')
        stopPolling()
        const list = d.content && Array.isArray(d.content.agents) ? d.content.agents : []
        revealSegments(list)   // 内部会把 running 置 false
        return
      }
      pollTimer = setTimeout(poll, 4000)
    } catch (e) {
      errorMsg.value = `轮询失败：${e.message}`
      localStorage.removeItem('reportJobId')
      running.value = false
      stopPolling()
    }
  }
  poll()
}

const escapeHtml = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

const renderMarkdown = (text) => {
  if (!text) return ''
  let html = escapeHtml(text)
  html = html.replace(/^#### (.*)$/gm, '<h4>$1</h4>')
  html = html.replace(/^### (.*)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.*)$/gm, '<h2>$1</h2>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/^- (.*)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>')
  html = html.replace(/\n{2,}/g, '</p><p>')
  html = html.replace(/\n/g, '<br/>')
  return `<p>${html}</p>`
}

onMounted(async () => {
  await getUserInfo()
  // 刷新续跑：任务在平台侧独立运行，用持久化的 jobId 继续轮询
  const jobId = localStorage.getItem('reportJobId')
  if (jobId && userId.value) {
    running.value = true
    startPolling(jobId)
  }
})
</script>

<style scoped>
.report-page { width: 100%; height: 100%; overflow-y: auto; background: #F6F8FC; padding: 20px; box-sizing: border-box; }
.workspace { max-width: 980px; margin: 0 auto; }

.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 14px; }
.title-block h1 { margin: 6px 0 3px; font-size: 1.4rem; font-weight: 700; color: #1E293B; }
.badge { display: inline-flex; align-items: center; gap: 6px; font-size: 0.74rem; font-weight: 600;
         color: #2563EB; background: #EFF6FF; padding: 4px 10px; border-radius: 6px; }
.subtitle { margin: 0; color: #64748B; font-size: 0.9rem; }
.progress-hint { margin: 0 0 12px; padding: 8px 14px; background: #EFF6FF; border: 1px solid #DBEAFE; border-radius: 8px;
                 color: #1D4ED8; font-size: 0.84rem; }
.header-actions { display: flex; gap: 8px; }

.btn { display: inline-flex; align-items: center; gap: 6px; border: 1px solid transparent; border-radius: 8px;
       padding: 9px 16px; font-size: 0.88rem; font-weight: 600; cursor: pointer; text-decoration: none;
       transition: background 0.16s ease, border-color 0.16s ease; }
.btn.primary { background: #4A90E2; color: #FFFFFF; }
.btn.primary:hover:not(:disabled) { background: #357ABD; }
.btn.primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn.ghost { background: #FFFFFF; color: #475569; border-color: #DFE6EF; }
.btn.ghost:hover { background: #F1F5F9; }

.input-panel { background: #FFFFFF; border: 1px solid #E4EAF2; border-radius: 12px; padding: 12px 15px; margin-bottom: 14px; }
.input-label { display: block; font-size: 0.78rem; font-weight: 600; color: #64748B; margin-bottom: 8px; }
.input-area { width: 100%; border: 1px solid #DFE6EF; border-radius: 8px; padding: 10px 12px; font-size: 0.9rem;
              color: #1E293B; font-family: inherit; resize: vertical; outline: none; box-sizing: border-box; }
.input-area:focus { border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74,144,226,0.10); }

.agents { display: flex; flex-direction: column; gap: 10px; }
.agent-card { background: #FFFFFF; border: 1px solid #E4EAF2; border-radius: 12px; padding: 14px 16px; transition: border-color 0.16s ease; }
/* 等待态压扁：保证首屏（页头+输入区+6 张卡片）尽量装得下，不提前出现滚动条；
   一旦 AI 内容到达卡片撑开，超出视口后再自然出现滚动条 */
.agent-card.waiting { padding: 9px 15px; }
.agent-card.waiting .agent-placeholder { display: none; }
.agent-card.running { border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74,144,226,0.08); }
.agent-card.done { border-color: #CDE7D6; }
.agent-card.error { border-color: #FECACA; }

.agent-head { display: flex; align-items: center; gap: 12px; }
.agent-index { width: 26px; height: 26px; border-radius: 7px; background: #EEF4FB; color: #2563EB;
               display: flex; align-items: center; justify-content: center; font-size: 0.8rem; font-weight: 700; }
.agent-meta { flex: 1; }
.agent-meta h3 { margin: 0; font-size: 0.95rem; font-weight: 650; color: #1E293B; }
.agent-desc { font-size: 0.76rem; color: #94A3B8; }

.tag { font-size: 0.72rem; font-weight: 600; padding: 3px 9px; border-radius: 6px; display: inline-flex; align-items: center; gap: 5px; }
.tag.waiting { background: #F1F5F9; color: #94A3B8; }
.tag.running { background: #EFF6FF; color: #2563EB; }
.tag.done { background: #ECFDF5; color: #059669; }
.tag.error { background: #FEF2F2; color: #DC2626; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; animation: pulse 1.2s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.25; } }

.agent-body { margin-top: 12px; border-top: 1px solid #F1F5F9; padding-top: 10px; }
.agent-placeholder { margin-top: 10px; font-size: 0.82rem; color: #B6C2D2; }

.markdown :deep(h2), .markdown :deep(h3), .markdown :deep(h4) { color: #1E293B; margin: 12px 0 8px; font-weight: 650; }
.markdown :deep(h3) { font-size: 1.02rem; }
.markdown :deep(h4) { font-size: 0.95rem; color: #2563EB; }
.markdown :deep(p) { margin: 6px 0; line-height: 1.75; color: #334155; font-size: 0.9rem; }
.markdown :deep(ul) { margin: 6px 0 6px 18px; padding: 0; }
.markdown :deep(li) { line-height: 1.7; color: #334155; font-size: 0.9rem; }
.markdown :deep(strong) { color: #1E293B; }

.result-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px;
              background: #FFFFFF; border: 1px solid #CDE7D6; border-radius: 12px; padding: 12px 16px; }
.result-info { display: flex; align-items: center; gap: 8px; color: #059669; font-size: 0.88rem; font-weight: 600; }
.report-id { font-size: 0.76rem; color: #94A3B8; font-family: Consolas, monospace; }
.warn-text { color: #D97706; font-weight: 500; font-size: 0.8rem; }

.notice { border-radius: 8px; padding: 10px 14px; font-size: 0.86rem; margin-bottom: 14px; }
.notice.warn { background: #FFFBEB; color: #B45309; border: 1px solid #FDE68A; }
.notice.error { background: #FEF2F2; color: #B91C1C; border: 1px solid #FECACA; margin-top: 12px; }
</style>
