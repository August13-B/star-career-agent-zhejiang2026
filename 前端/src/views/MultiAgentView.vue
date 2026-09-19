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
          <span v-if="!hasMarkers" class="warn-text">（未检测到智能体标记，已按兜底归入「报告整合」）</span>
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
const hasMarkers = ref(true)
const savedHint = ref('')
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

const generate = async () => {
  reset()
  running.value = true
  const token = localStorage.getItem('token') || ''
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {})
  }
  try {
    const resp = await fetch('/api/career-report/generate-stream', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        user_id: userId.value,
        content: extraInput.value.trim() ||
          '请为我生成一份完整的职业规划报告。若缺少我的画像信息，请基于岗位知识库与通用情况给出，并说明假设。'
      })
    })
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)

    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop()

      for (const line of lines) {
        const raw = line.replace(/^data:\s*/, '').trim()
        if (!raw) continue
        let msg
        try { msg = JSON.parse(raw) } catch { continue }

        if (msg.done) {
          // 后端结束帧：{done, agents, hasMarkers, saved, reportId, reportName}
          hasMarkers.value = msg.hasMarkers !== false
          reportName.value = msg.reportName || ''
          reportId.value = msg.reportId ? String(msg.reportId) : ''
          savedHint.value = msg.saved === false ? '（但落库失败，详见后端日志）' : '并已保存'
          continue
        }
        const idx = agents.value.findIndex(a => a.key === msg.agent)
        if (idx < 0) continue
        markPreviousDone(idx)
        const agent = agents.value[idx]
        if (agent.status === 'waiting') agent.status = 'running'
        agent.content += msg.data || ''
      }
    }
    agents.value.forEach(a => { if (a.status === 'running') a.status = 'done' })
    finished.value = true
  } catch (e) {
    errorMsg.value = `生成失败：${e.message}（请确认后端已启动、模型网关已开通）`
    const runningAgent = agents.value.find(a => a.status === 'running')
    if (runningAgent) runningAgent.status = 'error'
  } finally {
    running.value = false
  }
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

onMounted(getUserInfo)
</script>

<style scoped>
.report-page { width: 100%; height: 100%; overflow-y: auto; background: #F6F8FC; padding: 24px; box-sizing: border-box; }
.workspace { max-width: 980px; margin: 0 auto; }

.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 18px; }
.title-block h1 { margin: 8px 0 4px; font-size: 1.5rem; font-weight: 700; color: #1E293B; }
.badge { display: inline-flex; align-items: center; gap: 6px; font-size: 0.74rem; font-weight: 600;
         color: #2563EB; background: #EFF6FF; padding: 4px 10px; border-radius: 6px; }
.subtitle { margin: 0; color: #64748B; font-size: 0.9rem; }
.header-actions { display: flex; gap: 8px; }

.btn { display: inline-flex; align-items: center; gap: 6px; border: 1px solid transparent; border-radius: 8px;
       padding: 9px 16px; font-size: 0.88rem; font-weight: 600; cursor: pointer; text-decoration: none;
       transition: background 0.16s ease, border-color 0.16s ease; }
.btn.primary { background: #4A90E2; color: #FFFFFF; }
.btn.primary:hover:not(:disabled) { background: #357ABD; }
.btn.primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn.ghost { background: #FFFFFF; color: #475569; border-color: #DFE6EF; }
.btn.ghost:hover { background: #F1F5F9; }

.input-panel { background: #FFFFFF; border: 1px solid #E4EAF2; border-radius: 12px; padding: 14px 16px; margin-bottom: 16px; }
.input-label { display: block; font-size: 0.78rem; font-weight: 600; color: #64748B; margin-bottom: 8px; }
.input-area { width: 100%; border: 1px solid #DFE6EF; border-radius: 8px; padding: 10px 12px; font-size: 0.9rem;
              color: #1E293B; font-family: inherit; resize: vertical; outline: none; box-sizing: border-box; }
.input-area:focus { border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74,144,226,0.10); }

.agents { display: flex; flex-direction: column; gap: 12px; }
.agent-card { background: #FFFFFF; border: 1px solid #E4EAF2; border-radius: 12px; padding: 14px 16px; transition: border-color 0.16s ease; }
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
