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
          <button v-if="running" class="btn danger" @click="stopGenerating">停止生成</button>
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

      <p v-if="running" class="progress-hint">平台正在生成… 服务端 {{ progressChars }} 字 · 已接收 {{ receivedTotal }} 字（共 6 个智能体，预计 3~5 分钟）</p>

      <section class="agents">
        <article
          v-for="(agent, i) in agents"
          :key="agent.key"
          class="agent-card"
          :class="[agent.status, { 'is-expanded': agent.expanded }]"
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
            <button v-if="agent.content" class="expand-btn" @click="toggleExpand(i)">{{ agent.expanded ? '收起' : '展开' }}</button>
          </div>
          <div class="agent-body" v-if="agent.content">
            <div
              class="markdown"
              :ref="el => setBodyRef(i, el)"
              @scroll="onBodyScroll(i, $event)"
              v-html="renderMarkdown(agent.content)"
            ></div>
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

      <div v-if="partialMsg" class="notice partial">{{ partialMsg }}</div>
      <div v-if="errorMsg" class="notice error">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
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
// 本地已接收字数（与服务端 progressChars 对比，可快速判断是轮询还是渲染卡住）
const receivedTotal = computed(() => agents.value.reduce((n, c) => n + ((c.received || '').length), 0))
const reportName = ref('')
const reportId = ref('')
const errorMsg = ref('')
// 容错提示：报告整合未完成但已整理已完成部分（中性提示，不报红）
const partialMsg = ref('')
const agents = ref(AGENT_DEFS.map(a => ({ ...a, status: 'waiting', content: '', received: '', expanded: false, autoScroll: true })))

// 卡片正文 DOM（收起态/展开态都滚到底；用户上滑后暂停自动滚动）
const bodyRefs = []
const setBodyRef = (i, el) => { if (el) bodyRefs[i] = el }
const toggleExpand = (i) => {
  const card = agents.value[i]
  if (!card) return
  card.expanded = !card.expanded
  // 展开/收起都回到"跟最新"（收起态必须始终显示最新三行）
  card.autoScroll = true
  nextTick(() => {
    const el = bodyRefs[i]
    if (el) el.scrollTop = el.scrollHeight
  })
}
// 用户上滑 → 暂停自动滚动（并冻结该卡片输出）；回到最底 → 恢复跟随
const onBodyScroll = (i, e) => {
  const el = e.target
  const card = agents.value[i]
  if (!card) return
  card.autoScroll = el.scrollTop + el.clientHeight >= el.scrollHeight - 16
}
// 开始生成时只把状态置为 running，**默认保持收起**（不自动展开；要展开由用户点）
const markRunning = (i) => {
  const card = agents.value[i]
  if (!card) return
  if (card.status === 'waiting') card.status = 'running'
}

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

// ===== 轮询 / 打字机状态（刷新后可用 localStorage 里的 jobId 续跑） =====
let pollTimer = null          // setInterval id（固定节拍；不靠递归 setTimeout，避免一处异常就永远停）
let pollInFlight = false      // 在途保护：同一时刻只允许一个请求
let currentPollTick = null    // 供可见性回调/看门狗立即补拉
let pollFailures = 0
let pollToken = 0
let typeTimer = null
let currentJobId = null       // 当前报告任务ID（供「停止生成」）
let doneReceived = false
const POLL_MS = 1500
const POLL_TIMEOUT_MS = 12000

const stopPolling = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  currentPollTick = null
}
const stopTypewriter = () => { if (typeTimer) { clearInterval(typeTimer); typeTimer = null } }

// 本地均匀打字机：把 card.received 平滑播放到 card.content（平台 delta 是 1~2s 一批）
const startTypewriter = () => {
  if (typeTimer) return
  typeTimer = setInterval(() => {
    let pending = false
    agents.value.forEach((card, i) => {
      const recv = card.received || ''
      const shown = card.content || ''
      if (shown.length > recv.length) {
        // 服务端内容被整体替换/回退 → 直接对齐，避免错位
        card.content = recv
        return
      }
      if (shown.length < recv.length) {
        pending = true
        // 展开态且用户已上滑 → **冻结该卡片输出**（高度不再变化，否则拖到底永远是“移动靶”），
        // 等用户拖回底部（autoScroll=true）再继续追赶；收起态始终跟随。
        if (card.expanded && !card.autoScroll) return
        const backlog = recv.length - shown.length
        const step = Math.max(2, Math.ceil(backlog / 30))
        card.content = recv.slice(0, shown.length + step)
        if (bodyRefs[i]) {
          const el = bodyRefs[i]
          el.scrollTop = el.scrollHeight
        }
      }
    })
    // 全部播完 且 已收到 done → 收尾（若用户暂停则等其恢复）
    if (!pending && doneReceived) {
      stopTypewriter()
      agents.value.forEach(c => { if (c.status !== 'error') c.status = 'done' })
      finished.value = true
      running.value = false
    }
  }, 30)
}

const reset = () => {
  stopPolling()
  stopTypewriter()
  doneReceived = false
  agents.value = AGENT_DEFS.map(a => ({ ...a, status: 'waiting', content: '', received: '', expanded: false, autoScroll: true }))
  finished.value = false
  errorMsg.value = ''
  partialMsg.value = ''
  savedHint.value = ''
  reportName.value = ''
  reportId.value = ''
}

const markPreviousDone = (idx) => {
  for (let i = 0; i < idx; i++) {
    if (agents.value[i].status === 'running') agents.value[i].status = 'done'
  }
}

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
    currentJobId = jobId
    // 持久化 jobId：任务在平台侧独立运行，刷新页面后仍可续跑
    localStorage.setItem('reportJobId', jobId)
    startPolling(jobId)
  } catch (e) {
    errorMsg.value = `生成失败：${e.message}（请确认后端已启动、平台报告服务已就绪）`
    running.value = false
  }
}

// 轮询平台任务：每次全量拉取各 agent 正文（幂等替换） → 本地打字机实时呈现
// 健壮性：请求超时 + 失败重试（不中断、不清 jobId） + 页面可见时立即补拉
const startPolling = (jobId) => {
  stopPolling()
  currentJobId = jobId
  pollFailures = 0
  const myToken = ++pollToken
  const tick = async () => {
    if (myToken !== pollToken || pollInFlight) return
    pollInFlight = true
    try {
      const token = localStorage.getItem('token') || ''
      const res = await axios.get(`/api/career-report/jobs/${jobId}`, {
        // 全量模式：每次传 offsets={} → 平台返回各 agent 从 0 开始的完整片段。
        // （之前按递增 offsets 追加会因位置漂移导致服务端持续返回空 delta → 表现为“几秒后卡死，刷新又好”）
        params: { offsets: '{}' },
        timeout: POLL_TIMEOUT_MS,
        headers: token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {}
      })
      // 已被新一轮轮询取代 → 丢弃旧响应，避免重复追加
      if (myToken !== pollToken) return
      const body = res.data || {}
      // 后端业务错误（Result.error）不能默默继续轮询
      if (body.code !== undefined && body.code !== 10001 && body.code !== 200 && body.code !== 0) {
        throw new Error(body.message || '查询报告任务失败')
      }
      const d = body.data || {}
      if (!d.status && !d.error) {
        throw new Error('报告任务响应异常（无 status）')
      }
      pollFailures = 0
      if (d.error) {
        errorMsg.value = String(d.error)
        const ra = agents.value.find(a => a.status === 'running')
        if (ra) ra.status = 'error'
        localStorage.removeItem('reportJobId')
        running.value = false
        stopPolling()
        stopTypewriter()
        return
      }
      // 平台侧已取消（终态）：停止本地轮询并清理
      if (d.status === 'canceled') {
        stopPolling()
        stopTypewriter()
        pollToken++
        currentJobId = null
        localStorage.removeItem('reportJobId')
        running.value = false
        errorMsg.value = '任务已取消'
        return
      }
      progressChars.value = d.progressChars || progressChars.value

      // 全量替换（幂等）：deltas 里是该 agent 从 0 开始的完整正文
      if (Array.isArray(d.deltas)) {
        d.deltas.forEach(dl => {
          const j = agents.value.findIndex(a => a.key === dl.agent)
          if (j < 0) return
          const text = String(dl.data || '')
          if (!text) return
          const card = agents.value[j]
          card.received = text          // 替换，而非追加 —— 不会重复、不会漂移
          markRunning(j)
        })
      }
      // 2) 推进卡片状态（currentAgent 高亮 / agentsDone 标完成）
      const idx = agents.value.findIndex(a => a.key === d.currentAgent)
      if (idx >= 0) {
        markPreviousDone(idx)
        markRunning(idx)
      }
      if (Array.isArray(d.agentsDone)) {
        d.agentsDone.forEach(k => {
          const j = agents.value.findIndex(a => a.key === k)
          if (j >= 0) agents.value[j].status = 'done'
        })
      }
      startTypewriter()

      // 3) 完成：用最终全文校准，打字机继续播完剩余
      if (d.status === 'done') {
        doneReceived = true
        reportName.value = d.reportName || ''
        reportId.value = d.reportId ? String(d.reportId) : ''
        savedHint.value = d.saved === false ? '（但落库失败，详见后端日志）' : '并已保存'
        // 容错：后端用已有内容整理了“部分完成”报告 → 中性提示，不报红
        partialMsg.value = d.partial
          ? '报告整合环节未完成（平台超时或异常），已为你整理并保存已完成的分析部分，可在个人中心查看/导出。'
          : ''
        localStorage.removeItem('reportJobId')
        stopPolling()
        const list = d.content && Array.isArray(d.content.agents) ? d.content.agents : []
        list.forEach(seg => {
          const j = agents.value.findIndex(a => a.key === (seg.key || seg.name))
          if (j >= 0) agents.value[j].received = String(seg.content || '')
        })
        startTypewriter()
        return
      }
      // 诊断日志（浏览器 Console 可看到每一拍）
      console.debug('[report] poll(full)', d.status, d.currentAgent || '-',
        'deltas=', (d.deltas || []).map(x => `${x.agent}:${(x.data || '').length}`).join(','),
        'chars=', d.progressChars)
    } catch (e) {
      if (myToken !== pollToken) return
      pollFailures++
      console.warn('[report] 轮询失败', pollFailures, e && e.message)
      if (pollFailures >= 6) {
        errorMsg.value = `轮询失败（已重试 ${pollFailures} 次）：${e && e.message}`
        localStorage.removeItem('reportJobId')
        running.value = false
        stopPolling()
        stopTypewriter()
        return
      }
    } finally {
      pollInFlight = false
    }
  }
  currentPollTick = tick
  tick()
  // 固定节拍驱动：即使某一拍异常/请求卡住，后续拍仍会继续
  pollTimer = setInterval(tick, POLL_MS)
}

// 随时停止：通知后端取消平台任务 → 丢弃本次内容（不落库）、清空界面
const stopGenerating = async () => {
  const jobId = currentJobId || localStorage.getItem('reportJobId')
  if (jobId) {
    try {
      const token = localStorage.getItem('token') || ''
      await axios.post(`/api/career-report/jobs/${jobId}/cancel`, {}, {
        timeout: 15000,
        headers: token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {}
      })
    } catch (e) {
      console.warn('[report] 取消平台任务失败（仍会停止本地生成）', e && e.message)
    }
  }
  stopPolling()
  stopTypewriter()
  pollToken++                 // 令在途轮询响应作废
  currentJobId = null
  localStorage.removeItem('reportJobId')
  running.value = false
  reset()
  errorMsg.value = '已停止生成（本次内容已丢弃）'
}

const escapeHtml = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

// 第 6 段末尾的结构化目标块（仅后端用）；流式 deltas 也会带上，渲染前剔除，避免穿帮
const stripGoalsBlock = (text) => {
  if (!text) return ''
  let t = String(text)
  const i = t.indexOf('<<<GOALS_JSON')
  if (i >= 0) t = t.slice(0, i)
  return t.replace(/<<<END_GOALS_JSON>>>/g, '')
}

const renderMarkdown = (text) => {
  if (!text) return ''
  let html = escapeHtml(stripGoalsBlock(text))
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

// 无进行中任务时：拉取该用户最近一份报告并直接展示（刷新后也能看到最终报告）
const loadLatestReport = async () => {
  if (!userId.value) return
  try {
    const token = localStorage.getItem('token') || ''
    const res = await axios.get(`/api/career-report/user/${userId.value}`, {
      timeout: 12000,
      headers: token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {}
    })
    const body = res.data || {}
    if (body.code !== 10001 && body.code !== 200 && body.code !== 0) return
    const list = Array.isArray(body.data) ? body.data : []
    if (list.length === 0) return
    const latest = list[0]
    let content = null
    try {
      content = typeof latest.reportContent === 'string' ? JSON.parse(latest.reportContent) : latest.reportContent
    } catch (e) { content = null }
    const list2 = content && Array.isArray(content.agents) ? content.agents : []
    if (list2.length === 0) return
    reportName.value = latest.reportName || ''
    reportId.value = latest.id ? String(latest.id) : ''
    savedHint.value = '（最近一次）'
    list2.forEach(seg => {
      const j = agents.value.findIndex(a => a.key === (seg.key || seg.name))
      if (j >= 0) {
        const card = agents.value[j]
        card.received = String(seg.content || '')
        card.content = card.received
        card.status = 'done'
      }
    })
    agents.value.forEach(c => { if (c.status !== 'error' && !c.received) c.status = 'done' })
    finished.value = true
    running.value = false
  } catch (e) { /* 无历史/未登录时忽略 */ }
}

// 页面可见/聚焦时立即补拉一拍（后台标签页定时器被节流，是“看着卡住”的常见原因）
const catchUp = () => {
  if (document.visibilityState !== 'visible') return
  if (!running.value) return
  const jobId = localStorage.getItem('reportJobId')
  if (jobId) {
    if (currentPollTick) {
      currentPollTick()
    } else {
      console.warn('[report] 看门狗：轮询已停止，自动重启')
      startPolling(jobId)
    }
  }
  if (!typeTimer) startTypewriter()
}
document.addEventListener('visibilitychange', catchUp)
window.addEventListener('focus', catchUp)

// 看门狗：运行中但轮询已停止 → 自动重启（防任何意外导致永久卡住）
setInterval(() => {
  if (!running.value) return
  const jobId = localStorage.getItem('reportJobId')
  if (jobId && !pollTimer) {
    console.warn('[report] 看门狗：轮询已停止，自动重启')
    startPolling(jobId)
  }
}, 5000)

onMounted(async () => {
  await getUserInfo()
  const jobId = localStorage.getItem('reportJobId')
  if (jobId && userId.value) {
    // 刷新续跑：任务在平台侧独立运行，用持久化的 jobId 继续轮询
    running.value = true
    startPolling(jobId)
  } else {
    await loadLatestReport()
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
.btn.danger { background: #FFFFFF; color: #DC2626; border-color: #FECACA; }
.btn.danger:hover { background: #FEF2F2; }

.input-panel { background: #FFFFFF; border: 1px solid #E4EAF2; border-radius: 12px; padding: 12px 15px; margin-bottom: 14px; }
.input-label { display: block; font-size: 0.78rem; font-weight: 600; color: #64748B; margin-bottom: 8px; }
.input-area { width: 100%; border: 1px solid #DFE6EF; border-radius: 8px; padding: 10px 12px; font-size: 0.9rem;
              color: #1E293B; font-family: inherit; resize: vertical; outline: none; box-sizing: border-box; }
.input-area:focus { border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74,144,226,0.10); }

.agents { display: flex; flex-direction: column; gap: 10px; }
/* 卡片尺寸：未开始=更矮更窄（70px / 88% 宽，首屏不溢出）；轮到生成=收起 200px；展开=520px */
.agent-card { background: #FFFFFF; border: 1px solid #E4EAF2; border-radius: 12px; padding: 0 15px;
              height: 200px; max-width: 100%; box-sizing: border-box;
              display: flex; flex-direction: column; overflow: hidden;
              transition: border-color 0.16s ease, height 0.18s ease, width 0.18s ease; }
.agent-card.waiting { height: 84px; }
.agent-card.is-expanded { height: 520px; width: 100%; margin: 0; }
.agent-card.running { border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74,144,226,0.08); }
.agent-card.done { border-color: #CDE7D6; }
.agent-card.error { border-color: #FECACA; }

.agent-head { display: flex; align-items: center; gap: 12px; padding: 11px 0; flex: 0 0 auto; }
.agent-index { width: 26px; height: 26px; border-radius: 7px; background: #EEF4FB; color: #2563EB;
               display: flex; align-items: center; justify-content: center; font-size: 0.8rem; font-weight: 700; }
.agent-meta { flex: 1; min-width: 0; }
.agent-meta h3 { margin: 0; font-size: 0.95rem; font-weight: 650; color: #1E293B; }
.agent-desc { font-size: 0.76rem; color: #94A3B8; }

.tag { font-size: 0.72rem; font-weight: 600; padding: 3px 9px; border-radius: 6px; display: inline-flex; align-items: center; gap: 5px; }
.tag.waiting { background: #F1F5F9; color: #94A3B8; }
.tag.running { background: #EFF6FF; color: #2563EB; }
.tag.done { background: #ECFDF5; color: #059669; }
.tag.error { background: #FEF2F2; color: #DC2626; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; animation: pulse 1.2s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.25; } }

.agent-body { flex: 1 1 auto; min-height: 0; border-top: 1px solid #F1F5F9; display: flex; flex-direction: column; }
/* 收起态：固定 3 行高，隐藏滚动条，JS 滚到最新（最新三行） */
.agent-body .markdown { flex: 1 1 auto; min-height: 0; overflow-y: hidden; overflow-x: hidden; padding: 6px 0; word-break: break-word; }
/* 展开态：固定高可滚动，JS 自动跟到最新；用户上滑后暂停 */
.agent-card.is-expanded .agent-body .markdown { overflow-y: auto; padding-right: 6px; }
.expand-btn { flex: 0 0 auto; background: none; border: none; color: #2563EB; font-size: 0.78rem; font-weight: 600; cursor: pointer; padding: 2px 4px; }
.expand-btn:hover { text-decoration: underline; }
.agent-placeholder { flex: 1 1 auto; display: flex; align-items: center; font-size: 0.82rem; color: #B6C2D2; border-top: 1px solid #F1F5F9; }

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
/* 容错提示：中性（白底/灰边），不报红，也不似警告 */
.notice.partial { background: #FFFFFF; color: #475569; border: 1px solid #E2E8F0; margin-top: 12px; }
</style>
