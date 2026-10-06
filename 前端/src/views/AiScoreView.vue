<template>
  <div class="assess-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">职业旅程 / 能力补充</p>
        <h1>能力补充测评</h1>
        <p class="sub">
          AI 出题 → 你作答 → 按回答追问 → 六维评分。硬实力四项来自「基本情况」，与本页软实力六维合成
          <strong>10 维能力画像</strong>。
        </p>
      </div>
      <router-link class="link" to="/profile">返回个人中心</router-link>
    </header>

    <p v-if="error" class="notice error" role="alert">{{ error }}</p>

    <!-- ① 加载中 -->
    <p v-if="loading" class="notice">正在读取测评状态…</p>

    <template v-else>
      <!-- ② 前置：未填基本情况 -->
      <section v-if="!profileReady" class="card">
        <h2>先填「基本情况」</h2>
        <p class="muted">
          测评需要先有硬实力基础（学历 / 实习 / 专业技能 / 证书）。填完立刻开始，六维软素质由本页 AI 问答产出。
        </p>
        <div class="hard-preview" v-if="hardText && hardText.education">
          <span v-for="(value, key) in hardText" :key="key">{{ value }}</span>
        </div>
        <button class="btn primary" @click="showBasic = true">填写基本情况</button>
      </section>

      <!-- ③ 有进行中的会话：断点续答 -->
      <section v-else-if="!session && activeSessionId" class="card">
        <h2>上次测评还没做完</h2>
        <p class="muted">已作答 {{ inProgressAnswered }} 题，可以继续；草稿已保存。</p>
        <div class="row">
          <button class="btn primary" :disabled="busy" @click="resume">{{ busy ? '载入中…' : '继续上次测评' }}</button>
          <button class="btn ghost" :disabled="busy" @click="start">重新开始一份</button>
        </div>
      </section>

      <!-- ④ 开始 -->
      <section v-else-if="!session" class="card">
        <h2>开始测评</h2>
        <ul class="facts">
          <li>客观题 <b>{{ state.objectiveTarget }}</b> 道（每道 <b>{{ state.objectiveSeconds }}</b> 秒）</li>
          <li>主观题 <b>{{ state.subjectiveTarget }}</b> 道（每道 <b>{{ Math.round(state.subjectiveSeconds / 60) }}</b> 分钟，回答笼统时会被追问，同题最多 2 轮）</li>
          <li>超时会自动跳到下一题；中途可关闭页面，下次继续（草稿与作答已保存）</li>
        </ul>
        <div class="scores-mini" v-if="scores.total">
          <span>当前画像总分 <b>{{ scores.total }}</b></span>
          <span class="muted">（硬实力 30% + 软实力 70%；软实力六维将由本次测评更新）</span>
        </div>
        <button class="btn primary" :disabled="busy" @click="start">{{ busy ? '正在出题…' : '开始测评' }}</button>
      </section>

      <!-- ⑤ 答题中 -->
      <section v-else-if="session.status === 'active' && session.current" class="card">
        <div class="q-head">
          <span class="pill">第 {{ session.current.questionNo }} / {{ total }} 题</span>
          <span class="pill neutral">{{ kindText(session.current.kind) }}</span>
          <span class="pill neutral">{{ dimensionText(session.current.dimension) }}</span>
          <span v-if="session.current.followUp" class="pill warn">追问</span>
          <span class="countdown" :class="{ danger: remaining <= 10 }">剩余 {{ format(remaining) }}</span>
        </div>
        <progress :value="session.current.questionNo - 1" :max="total"></progress>
        <p class="question">{{ stripMarkdown(session.current.question) }}</p>

        <div v-if="session.current.kind === 'objective'" class="options">
          <button v-for="option in session.current.options" :key="option.index" class="option"
                  :disabled="busy" @click="submit(option.index, null)">
            <b>{{ String.fromCharCode(65 + option.index) }}</b>
            <span>{{ option.text }}</span>
          </button>
        </div>
        <template v-else>
          <textarea v-model="draft" rows="5" maxlength="2000" :disabled="busy"
                    placeholder="请给出具体做法、依据和结果（越具体越容易被评分）"
                    @input="scheduleDraft"></textarea>
          <div class="q-foot">
            <span class="muted small">草稿自动保存 · {{ draft.length }}/2000</span>
            <button class="btn primary" :disabled="busy || !draft.trim()" @click="submit(null, draft)">
              {{ busy ? '提交中…' : '提交回答' }}
            </button>
          </div>
        </template>
        <p v-if="busy" class="notice" role="status">AI 正在出下一题 / 评分，请稍候…</p>
      </section>

      <!-- ⑥ 结果 -->
      <section v-else class="card">
        <h2>{{ session.evaluation?.status === 'review_required' ? '测评已完成（评分待复核）' : '测评结果' }}</h2>
        <p v-if="session.evaluation?.status === 'review_required'" class="notice error">
          {{ session.evaluation.message }}；作答已保存，可稍后重试。
        </p>
        <template v-if="session.evaluation?.result">
          <div class="score-total">
            <strong>{{ Number(session.scores?.total || 0).toFixed(0) }}</strong>
            <span class="muted">/ 100 画像总分（硬 30% + 软 70%）</span>
          </div>
          <div class="dim-grid">
            <div v-for="row in dimensionRows" :key="row.key" class="dim">
              <div class="dim-head">
                <span>{{ row.label }}</span>
                <b>{{ row.value }}</b>
                <em v-if="row.source === 'hard'">来自基本情况</em>
                <em v-else>本次测评</em>
              </div>
              <progress :value="row.value" max="100"></progress>
            </div>
          </div>
          <p class="comment">{{ stripMarkdown(session.evaluation.result.comment || '') }}</p>
          <template v-if="session.evaluation.result.evidence?.length">
            <h3>评分依据（可核对原话）</h3>
            <ul class="evidence">
              <li v-for="(item, index) in session.evaluation.result.evidence" :key="index">
                <b>{{ dimensionText(item.dimension) }}</b>：{{ stripMarkdown(item.quote || '') }}
                <span class="muted small">（证据 {{ item.evidenceId }}）</span>
              </li>
            </ul>
          </template>
          <template v-if="session.evaluation.result.suggestions?.length">
            <h3>下一步怎么练</h3>
            <ol class="suggestions">
              <li v-for="(tip, index) in session.evaluation.result.suggestions" :key="index">{{ stripMarkdown(tip) }}</li>
            </ol>
          </template>
          <p v-if="session.evaluation.objective" class="muted small">
            客观题维度分（后端按选项分值复算，供核对）：{{ objectiveText }}
          </p>
        </template>
        <div class="row">
          <button class="btn primary" @click="start">再测一次</button>
          <router-link class="btn ghost" to="/profile">查看个人画像</router-link>
        </div>
      </section>

      <!-- 历史 -->
      <section class="card" v-if="history.length">
        <div class="row between">
          <h2>测评记录</h2>
          <button class="btn ghost sm" @click="loadHistory">刷新</button>
        </div>
        <div v-for="item in history" :key="item.sessionId" class="history-row" @click="openSession(item.sessionId)">
          <span>{{ formatTime(item.createdAt) }}</span>
          <span class="muted">{{ statusText(item.status) }} · 已答 {{ item.answeredCount }}/{{ item.questionTotal }}</span>
          <span class="link">查看</span>
        </div>
      </section>
    </template>

    <AbilityBasicModal v-model:visible="showBasic" :user-id="null" @saved="onBasicSaved" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import axios from 'axios'
import API_CONFIG from '../config/api'
import AbilityBasicModal from '../components/AbilityBasicModal.vue'
import { stripMarkdown } from '../utils/text'

const DIMENSIONS = [
  ['education', '学历背景', 'hard'], ['internship', '实习经历', 'hard'],
  ['professional', '专业技能', 'hard'], ['certificate', '证书资质', 'hard'],
  ['communication', '沟通能力', 'soft'], ['teamwork', '团队协作', 'soft'],
  ['problem_solving', '问题解决', 'soft'], ['innovation', '创新能力', 'soft'],
  ['learning', '学习能力', 'soft'], ['pressure', '抗压能力', 'soft']
]

const state = ref({}), session = ref(null), history = ref([])
const loading = ref(true), busy = ref(false), error = ref('')
const draft = ref(''), showBasic = ref(false), remaining = ref(0)
let tick = null, draftTimer = null

const headers = () => {
  const token = localStorage.getItem('token') || ''
  return { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}
const api = axios.create({ baseURL: `${API_CONFIG.BASE_URL}/api/assessment`, timeout: 180000 })
api.interceptors.request.use(config => { config.headers = { ...config.headers, ...headers() }; return config })

const profileReady = computed(() => !!state.value.profileReady)
const hardText = computed(() => state.value.hardText || {})
const scores = computed(() => state.value.scores || {})
const total = computed(() => (state.value.objectiveTarget || 10) + (state.value.subjectiveTarget || 4))
const activeSessionId = computed(() => (history.value.find(item => item.status === 'active') || {}).sessionId || '')
const inProgressAnswered = computed(() => (history.value.find(item => item.status === 'active') || {}).answeredCount || 0)
const dimensionRows = computed(() => DIMENSIONS.map(([key, label, source]) => ({
  key, label, source, value: Number((session.value?.scores || {})[key] || 0)
})))
const objectiveText = computed(() => Object.entries(session.value?.evaluation?.objective || {})
  .map(([key, value]) => `${dimensionText(key)} ${value}`).join(' · '))

const dimensionText = key => (DIMENSIONS.find(item => item[0] === key) || [, key])[1]
const kindText = kind => (kind === 'objective' ? '客观题' : '主观题')
const statusText = status => ({ active: '进行中', completed: '已完成', review_required: '评分待复核' }[status] || status)
const format = seconds => `${Math.floor(seconds / 60)}:${String(Math.max(0, seconds % 60)).padStart(2, '0')}`
const formatTime = value => (value ? String(value).replace('T', ' ').slice(0, 16) : '')

async function loadState() {
  try {
    const res = await api.get('/state')
    state.value = res.data?.data || {}
  } catch (e) {
    error.value = e.response?.data?.message || '读取测评状态失败，请稍后重试'
  }
}

async function loadHistory() {
  try {
    const res = await api.get('/sessions', { params: { offset: 0, limit: 10 } })
    history.value = res.data?.data?.items || []
  } catch (e) { /* 历史读取失败不影响主流程 */ }
}

async function start() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    const res = await api.post('/sessions', { clientRequestId: crypto.randomUUID() })
    applySession(res.data?.data)
  } catch (e) {
    const body = e.response?.data || {}
    error.value = body.message || '开始测评失败，请稍后重试'
    if (body.data?.errorCode === 'PROFILE_REQUIRED') {
      state.value = { ...state.value, profileReady: false }
    }
  } finally {
    busy.value = false
  }
}

async function resume() {
  if (!activeSessionId.value) return
  busy.value = true
  try {
    const res = await api.get(`/sessions/${activeSessionId.value}`)
    applySession(res.data?.data)
  } catch (e) {
    error.value = e.response?.data?.message || '载入上次测评失败'
  } finally {
    busy.value = false
  }
}

async function openSession(sessionId) {
  busy.value = true
  try {
    const res = await api.get(`/sessions/${sessionId}`)
    applySession(res.data?.data)
  } catch (e) {
    error.value = e.response?.data?.message || '载入测评记录失败'
  } finally {
    busy.value = false
  }
}

function applySession(data) {
  if (!data) return
  session.value = data
  draft.value = data.draft || ''
  startTimer()
  if (data.status !== 'active') {
    stopTimer()
    loadState()
    loadHistory()
  }
}

function startTimer() {
  stopTimer()
  remaining.value = Number(session.value?.current?.remainingSeconds || 0)
  if (session.value?.status !== 'active' || !session.value?.current) return
  tick = setInterval(() => {
    remaining.value = Math.max(0, remaining.value - 1)
    if (remaining.value === 0) {
      clearInterval(tick)
      submit(null, null) // 超时自动跳下一题（服务端已判定超时）
    }
  }, 1000)
}

function stopTimer() {
  if (tick) clearInterval(tick)
  tick = null
}

async function submit(chosen, answer) {
  if (busy.value || !session.value?.current) return
  busy.value = true
  error.value = ''
  try {
    const res = await api.post(`/sessions/${session.value.sessionId}/turns`, {
      chosen, answer, expectedVersion: session.value.version
    })
    applySession(res.data?.data)
  } catch (e) {
    error.value = e.response?.data?.message || '提交失败，请重试'
  } finally {
    busy.value = false
  }
}

function scheduleDraft() {
  clearTimeout(draftTimer)
  draftTimer = setTimeout(async () => {
    try {
      await api.put(`/sessions/${session.value.sessionId}/draft`, {
        content: draft.value, expectedVersion: session.value.draftVersion
      })
      session.value.draftVersion += 1
    } catch (e) { /* 草稿冲突：下次快照会带回最新值 */ }
  }, 700)
}

async function onBasicSaved() {
  await Promise.all([loadState(), loadHistory()])
}

onMounted(async () => {
  await Promise.all([loadState(), loadHistory()])
  loading.value = false
})

onUnmounted(stopTimer)
</script>

<style scoped>
.assess-page { max-width: 1080px; margin: 0 auto; padding: 28px 24px 60px; color: #1E293B; }
.page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.eyebrow { margin: 0 0 6px; font-size: 12px; letter-spacing: 2px; color: #64748B; }
h1 { margin: 0 0 8px; font-size: 26px; }
.sub { margin: 0; color: #64748B; font-size: 0.9rem; line-height: 1.7; max-width: 720px; }
.link { color: #2563EB; text-decoration: none; font-size: 0.9rem; }
.card { background: #fff; border: 1px solid #E2E8F0; border-radius: 16px; padding: 22px; margin-bottom: 18px; box-shadow: 0 4px 18px rgba(51,65,85,.04); }
.card h2 { margin: 0 0 12px; font-size: 1.05rem; }
.muted { color: #64748B; }
.small { font-size: 12px; }
.notice { background: #EFF6FF; border: 1px solid #DBEAFE; border-radius: 10px; padding: 12px; font-size: 13px; line-height: 1.7; }
.notice.error { background: #FFF4F2; border-color: #FBD2C8; color: #9F3020; }
.btn { font: inherit; border: 0; border-radius: 9px; padding: 10px 18px; cursor: pointer; text-decoration: none; display: inline-block; }
.btn.primary { background: #2563EB; color: #fff; }
.btn.primary:disabled { background: #E2E8F0; color: #94A3B8; cursor: not-allowed; }
.btn.ghost { background: #EFF6FF; color: #1D4ED8; }
.btn.sm { padding: 6px 12px; font-size: 0.85rem; }
.row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.row.between { justify-content: space-between; }
.facts { margin: 0 0 14px 18px; color: #475569; font-size: 0.9rem; line-height: 1.9; }
.scores-mini { display: flex; gap: 10px; align-items: baseline; margin-bottom: 14px; font-size: 0.9rem; }
.hard-preview { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0 16px; }
.hard-preview span { background: #F1F5F9; border-radius: 8px; padding: 6px 10px; font-size: 12px; color: #475569; }
.q-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.pill { background: #E9F5F0; color: #157856; border-radius: 20px; padding: 5px 10px; font-size: 12px; }
.pill.neutral { background: #F1F5F9; color: #64748B; }
.pill.warn { background: #FFF7ED; color: #B45309; }
.countdown { margin-left: auto; font-weight: 700; font-variant-numeric: tabular-nums; color: #1D4ED8; }
.countdown.danger { color: #B91C1C; }
progress { display: block; width: 100%; height: 9px; accent-color: #3B82F6; border: 0; margin-bottom: 14px; }
.question { font-size: 1rem; line-height: 1.9; white-space: pre-wrap; }
.options { display: flex; flex-direction: column; gap: 10px; }
.option { display: flex; gap: 10px; align-items: flex-start; text-align: left; background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 14px; font: inherit; cursor: pointer; line-height: 1.7; }
.option:hover { border-color: #93C5FD; background: #F0F7FF; }
.option b { color: #2563EB; }
textarea { font: inherit; width: 100%; box-sizing: border-box; padding: 12px; border: 1px solid #CBD5E1; border-radius: 10px; line-height: 1.7; }
.q-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 10px; }
.score-total { display: flex; align-items: baseline; gap: 10px; margin-bottom: 16px; }
.score-total strong { font-size: 44px; color: #2563EB; }
.dim-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 22px; }
.dim-head { display: flex; align-items: baseline; gap: 8px; font-size: 0.9rem; }
.dim-head b { margin-left: auto; color: #1D4ED8; }
.dim-head em { font-style: normal; font-size: 11px; color: #94A3B8; }
.dim progress { margin: 6px 0 0; }
.comment { margin: 18px 0; line-height: 1.9; color: #334155; }
.evidence, .suggestions { margin: 0 0 16px 18px; padding: 0; line-height: 1.9; color: #334155; font-size: 0.9rem; }
.history-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 0; border-top: 1px solid #F1F5F9; cursor: pointer; font-size: 0.9rem; }
.history-row:hover { color: #1D4ED8; }
@media (max-width: 720px) { .dim-grid { grid-template-columns: 1fr; } .page-head { flex-direction: column; } }
</style>
