<template>
  <div class="assess-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">职业旅程 / 能力补充 · <b class="build-tag">构建 {{ BUILD_TAG }}</b></p>
        <h1>能力补充测评</h1>
        <p class="sub">
          AI 出题 → 你作答 → 按回答追问 → 六维评分。硬实力四项来自「基本情况」，与本页软实力六维合成
          <strong>10 维能力画像</strong>。
        </p>
      </div>
      <router-link class="link" to="/profile">返回个人中心</router-link>
    </header>

    <p v-if="error" class="notice error" role="alert">
      {{ error }}
      <button class="btn ghost sm" style="margin-left:10px" @click="retry">重试</button>
    </p>

    <!-- ① 加载中 -->
    <p v-if="loading" class="notice">
      正在读取测评状态…（已等待 {{ loadingSeconds }}s，超过 15s 会自动报错）
      <span class="js-spinner" aria-hidden="true"></span>
      <button class="btn ghost sm" style="margin-left:10px" @click="retry">手动重试</button>
      <button class="btn ghost sm" @click="reloadPage">强制重载页面</button>
    </p>

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
        </div>
        <p class="muted small">同一个测评会继续更新（不会新增记录）；中途退出不计入测评记录。</p>
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
                    @input="onDraftInput"></textarea>
          <p v-if="recognizing" class="muted small voice-line">正在听写，边说边转文字…{{ voiceInterim ? '（' + voiceInterim + '）' : '' }}</p>
          <p v-else-if="voiceHint" class="muted small voice-line">{{ voiceHint }}</p>
          <p v-else-if="!speechSupported" class="muted small voice-line">{{ speechUnsupportedHint }}</p>
          <div class="q-foot">
            <div class="answer-tools">
              <button v-if="speechSupported" type="button" class="btn ghost sm" :class="{ recording: recognizing }"
                      :disabled="busy" @click="toggleVoice">{{ recognizing ? '■ 停止语音' : '🎤 语音输入' }}</button>
              <span class="muted small">草稿自动保存 · {{ draft.length }}/2000</span>
            </div>
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
          <button v-if="session.evaluation?.status === 'review_required'" class="btn primary" :disabled="busy"
                  @click="retryEvaluate">{{ busy ? '重试中…' : '重试评分' }}</button>
          <button class="btn ghost" @click="start">再测一次（新增记录）</button>
          <router-link class="btn ghost" to="/profile">查看个人画像</router-link>
        </div>
      </section>

      <!-- ⑧ 能力雷达（10 维）：有测评结果后默认展示；无结果时显示引导态 -->
      <section v-if="!session || session.status !== 'active'" class="radar-block">
        <AbilityRadar :scores="radarScores" :total="radarTotal" />
      </section>

      <!-- 历史 -->
      <section class="card" v-if="history.length">
        <!-- 只展示已完成的测评；中途退出的不计入 -->
        <div class="row between">
          <h2>测评记录</h2>
          <button class="btn ghost sm" @click="loadHistory">刷新</button>
        </div>
        <div v-for="item in history" :key="item.sessionId" class="history-row" @click="openSession(item.sessionId)">
          <span>{{ formatTime(item.createdAt) }}</span>
          <span class="muted">
            {{ statusText(item.status) }} · 已答 {{ answeredQuestionsOf(item) }}/{{ item.questionTotal }} 题
            <template v-if="item.followUpTurns">（含 {{ item.followUpTurns }} 轮追问）</template>
          </span>
          <span class="link">查看</span>
        </div>
      </section>
    </template>

    <AbilityBasicModal v-model:visible="showBasic" :user-id="null" @saved="onBasicSaved" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import API_CONFIG from '../config/api'
import AbilityBasicModal from '../components/AbilityBasicModal.vue'
import AbilityRadar from '../components/AbilityRadar.vue'
import { stripMarkdown } from '../utils/text'
import { notifyProfileChange } from '../utils/profileChange'

const router = useRouter()

const DIMENSIONS = [
  ['education', '学历背景', 'hard'], ['internship', '实习经历', 'hard'],
  ['professional', '专业技能', 'hard'], ['certificate', '证书资质', 'hard'],
  ['communication', '沟通能力', 'soft'], ['teamwork', '团队协作', 'soft'],
  ['problem_solving', '问题解决', 'soft'], ['innovation', '创新能力', 'soft'],
  ['learning', '学习能力', 'soft'], ['pressure', '抗压能力', 'soft']
]

/** 构建标记：用来确认浏览器跑的是不是最新代码（每次改动递增即可） */
const BUILD_TAG = '2026-10-07-7'
const loadingSeconds = ref(0)
let loadingTick = null

const state = ref({}), session = ref(null), history = ref([])
const loading = ref(true), busy = ref(false), error = ref('')
const draft = ref(''), showBasic = ref(false), remaining = ref(0)
let tick = null, draftTimer = null, lastAutoAttempt = 0

// ===== 语音输入（浏览器原生 Web Speech API；与训练工作台同一套行为）=====
const SpeechRecognitionImpl = typeof window === 'undefined' ? null : (window.SpeechRecognition || window.webkitSpeechRecognition)
const speechApiAvailable = typeof SpeechRecognitionImpl === 'function'
const speechSecureContext = typeof window === 'undefined' || window.isSecureContext !== false
const speechSupported = speechApiAvailable && speechSecureContext
const recognizing = ref(false), voiceInterim = ref(''), voiceHint = ref('')
let recognition = null, voiceBase = '', voiceWriting = false, voiceIntent = false, voiceRestarts = 0
const speechUnsupportedHint = speechApiAvailable
  ? `语音输入需要 HTTPS 或 localhost（当前是 ${typeof location === 'undefined' ? '非安全来源' : location.origin}），可直接打字`
  : '当前浏览器不支持语音输入（建议 Chrome / Edge）；直接打字同样可以完成测评' 

const headers = () => {
  const token = localStorage.getItem('token') || ''
  return { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}
// 最终评分要调平台 AI（最长 120s，后端还会有一次重试）→ 超时给足 300s
const api = axios.create({ baseURL: `${API_CONFIG.BASE_URL}/api/assessment`, timeout: 300000, headers: { 'Content-Type': 'application/json' } })
api.interceptors.request.use(config => { config.headers = { ...config.headers, ...headers() }; return config })

/** 非安全上下文（http://IP）下 crypto.randomUUID 不存在，做兜底，避免点击后静默失败 */
const uuid = () => (globalThis.crypto?.randomUUID ? crypto.randomUUID() : `req-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`)

/**
 * 统一调用：后端统一返回 {code,message,data}，且**异常可能被全局处理器包成 HTTP 200**，
 * 所以必须显式校验 code —— 否则就是"点了没反应、控制台不红"。
 */
async function call(config) {
  const res = await api.request(config)
  const body = res.data || {}
  const ok = body.code === 10001 || body.code === 200 || body.code === 0 || body.success === true
  if (!ok) {
    const failure = new Error(body.message || '测评请求失败，请稍后重试')
    failure.code = body.data?.errorCode
    console.error('[能力补充测评] 接口返回失败:', res.status, body)
    throw failure
  }
  return body.data
}

const profileReady = computed(() => !!state.value.profileReady)
const hardText = computed(() => state.value.hardText || {})
const scores = computed(() => state.value.scores || {})
const total = computed(() => (state.value.objectiveTarget || 10) + (state.value.subjectiveTarget || 4))
// 进行中的测评由 /state 下发（记录列表只含已完成，不含进行中）
const activeSessionId = computed(() => state.value.activeSessionId || '')
const inProgressAnswered = computed(() => Number(state.value.activeAnswered || 0))
const dimensionRows = computed(() => DIMENSIONS.map(([key, label, source]) => ({
  key, label, source, value: Number((session.value?.scores || {})[key] || 0)
})))
const objectiveText = computed(() => Object.entries(session.value?.evaluation?.objective || {})
  .map(([key, value]) => `${dimensionText(key)} ${value}`).join(' · '))

const dimensionText = key => (DIMENSIONS.find(item => item[0] === key) || [, key])[1]

// 能力雷达数据：优先用本次/当前画像的 10 维（组件在拿不到时自行读取当前画像）
const radarScores = computed(() => {
  const has10 = source => source && DIMENSIONS.every(([key]) => Number.isFinite(Number(source[key])))
  if (has10(session.value?.scores)) return session.value.scores
  if (has10(state.value?.scores)) return state.value.scores
  return null
})
const radarTotal = computed(() => session.value?.scores?.total ?? state.value?.scores?.total ?? null)
const kindText = kind => (kind === 'objective' ? '客观题' : '主观题')
const statusText = status => ({ active: '进行中', completed: '已完成', review_required: '评分待复核' }[status] || status)
const format = seconds => `${Math.floor(seconds / 60)}:${String(Math.max(0, seconds % 60)).padStart(2, '0')}`
const formatTime = value => (value ? String(value).replace('T', ' ').slice(0, 16) : '')

/** 已答题数：优先用「已作答轮次 − 追问轮」推算，避免旧数据里 answered_count 被追问轮撑大 */
function answeredQuestionsOf(item) {
  const turns = Number(item?.answeredTurns || 0)
  const followUps = Number(item?.followUpTurns || 0)
  return turns > 0 ? Math.max(0, turns - followUps) : Number(item?.answeredCount || 0)
}

async function loadState() {
  try {
    // 状态查询是轻量接口：短超时，避免后端没起/网络不通时页面一直转
    state.value = await call({ url: '/state', timeout: 20000 }) || {}
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '读取测评状态失败，请稍后重试'
    console.error('[能力补充测评] 读取状态失败:', e)
  }
}

async function loadHistory() {
  try {
    const data = await call({ url: '/sessions', params: { offset: 0, limit: 10 }, timeout: 20000 })
    history.value = data?.items || []
  } catch (e) { /* 历史读取失败不影响主流程 */ }
}

async function start() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    const data = await call({ url: '/sessions', method: 'post', data: { clientRequestId: uuid() } })
    applySession(data)
  } catch (e) {
    const body = e.response?.data || {}
    error.value = body.message || e.message || '开始测评失败，请稍后重试'
    console.error('[能力补充测评] 开始测评失败:', e)
    if (body.data?.errorCode === 'PROFILE_REQUIRED' || e.code === 'PROFILE_REQUIRED') {
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
    applySession(await call({ url: `/sessions/${activeSessionId.value}` }))
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '载入上次测评失败'
    console.error('[能力补充测评] 载入上次测评失败:', e)
  } finally {
    busy.value = false
  }
}

async function openSession(sessionId) {
  busy.value = true
  try {
    applySession(await call({ url: `/sessions/${sessionId}` }))
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '载入测评记录失败'
    console.error('[能力补充测评] 载入测评记录失败:', e)
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
    // 测评完成后：提示画像已自动更新（无变化则不提示）
    notifyProfileChange(router)
  }
}

function startTimer() {
  stopTimer()
  remaining.value = Number(session.value?.current?.remainingSeconds || 0)
  lastAutoAttempt = 0
  if (session.value?.status !== 'active' || !session.value?.current) return
  tick = setInterval(() => {
    if (remaining.value > 0) {
      remaining.value = Math.max(0, remaining.value - 1)
      return
    }
    // 到点：自动提交（服务端按超时判定）。**不因一次失败就停**：每 3 秒重试，直到推进到下一题。
    // 另外后端在拉快照时也会自愈超时题，所以即使浏览器休眠过也能追上进度。
    if (!session.value?.current?.turnId || busy.value) return
    if (Date.now() - lastAutoAttempt < 3000) return
    lastAutoAttempt = Date.now()
    // 主观题：把已输入的草稿一并提交（不丢已写内容，服务端仍标记为「超时」）；客观题：空提交=超时跳过
    const autoAnswer = session.value?.current?.kind === 'subjective' && draft.value.trim() ? draft.value : null
    submit(null, autoAnswer, true)
  }, 1000)
}

function stopTimer() {
  if (tick) clearInterval(tick)
  tick = null
}

async function submit(chosen, answer, auto = false) {
  if (busy.value || !session.value?.current) return
  busy.value = true
  if (!auto) error.value = ''
  try {
    const data = await call({
      url: `/sessions/${session.value.sessionId}/turns`, method: 'post',
      data: { chosen, answer, expectedVersion: session.value.version }
    })
    applySession(data)
  } catch (e) {
    const code = e.response?.data?.data?.errorCode || e.code
    const retriable = ['OPTION_REQUIRED', 'ANSWER_REQUIRED', 'VERSION_CONFLICT', 'NO_ACTIVE_TURN'].includes(code)
    if (auto && retriable) {
      // 自动提交时还没到服务端宽限/版本刚好过期 → 静默等待下一次重试，不打扰用户
      console.warn('[能力补充测评] 超时自动提交暂未成功，将重试:', code, e.message)
    } else {
      error.value = e.response?.data?.message || e.message || '提交失败，请重试'
      console.error('[能力补充测评] 提交作答失败:', e)
    }
  } finally {
    busy.value = false
  }
}

function toggleVoice() { return recognizing.value ? stopVoice() : startVoice() }

function startVoice() {
  if (!speechSupported || recognizing.value || busy.value) return
  voiceHint.value = ''
  voiceIntent = true
  voiceRestarts = 0
  launchRecognition()
}

function launchRecognition() {
  try { recognition = new SpeechRecognitionImpl() } catch (e) { voiceIntent = false; voiceHint.value = '语音输入初始化失败，请改用打字输入。'; return }
  recognition.lang = 'zh-CN'
  recognition.continuous = true
  recognition.interimResults = true
  voiceBase = draft.value
  recognition.onresult = event => {
    let interim = ''
    for (let index = event.resultIndex; index < event.results.length; index += 1) {
      const result = event.results[index]
      const text = result[0]?.transcript || ''
      if (result.isFinal) voiceBase += text
      else interim += text
    }
    voiceWriting = true
    draft.value = voiceBase + interim
    voiceWriting = false
    voiceInterim.value = interim
    scheduleDraft()
  }
  recognition.onerror = event => {
    voiceInterim.value = ''
    if (event.error === 'no-speech' || event.error === 'aborted') return
    voiceIntent = false; recognizing.value = false
    voiceHint.value = ['not-allowed', 'service-not-allowed'].includes(event.error)
      ? '麦克风被拒绝或被服务器策略禁用：请检查地址栏麦克风权限；若通过 nginx 访问，需允许 Permissions-Policy microphone=(self)。'
      : event.error === 'network'
        ? '浏览器的语音识别服务连接失败（Chrome 需访问其云端识别）：请检查系统代理/网络，或改用打字输入。'
        : event.error === 'audio-capture' ? '未检测到可用麦克风设备。'
          : `语音识别中断（${event.error}），可以继续打字输入。`
  }
  recognition.onend = () => {
    voiceInterim.value = ''
    if (!voiceIntent || busy.value) { voiceIntent = false; recognizing.value = false; return }
    if (voiceRestarts >= 30) { voiceIntent = false; recognizing.value = false; voiceHint.value = '语音识别多次中断，已停止；可以重新点击按钮或直接打字。'; return }
    voiceRestarts += 1
    setTimeout(() => { if (voiceIntent) launchRecognition() }, 250)
  }
  try { recognition.start(); recognizing.value = true } catch (e) { voiceIntent = false; recognizing.value = false; voiceHint.value = '语音识别无法启动，请改用打字输入。' }
}

function stopVoice() {
  voiceIntent = false
  try { recognition?.stop() } catch (e) { /* 停止时的异常可忽略 */ }
  recognition = null; recognizing.value = false; voiceInterim.value = ''
}

function onDraftInput() { if (recognizing.value && !voiceWriting) stopVoice(); scheduleDraft() }

/** 评分失败（review_required）后重试：重新调用平台评分并应用结果 */
async function retryEvaluate() {
  if (busy.value || !session.value) return
  busy.value = true
  error.value = ''
  try {
    applySession(await call({ url: `/sessions/${session.value.sessionId}/evaluate`, method: 'post' }))
    await Promise.all([loadState(), loadHistory()])
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '重新评分失败，请稍后再试'
    console.error('[能力补充测评] 重新评分失败:', e)
  } finally {
    busy.value = false
  }
}

function scheduleDraft() {
  clearTimeout(draftTimer)
  draftTimer = setTimeout(async () => {
    try {
      await call({
        url: `/sessions/${session.value.sessionId}/draft`, method: 'put',
        data: { content: draft.value, expectedVersion: session.value.draftVersion }
      })
      session.value.draftVersion += 1
    } catch (e) { /* 草稿冲突：下次快照会带回最新值 */ }
  }, 700)
}

/** 强制重载页面：带时间戳绕过 HTML 缓存（模板里不能直接用 location，需走方法） */
function reloadPage() {
  const url = new URL(window.location.href)
  url.searchParams.set('_r', String(Date.now()))
  window.location.replace(url.toString())
}

/** 手动重试：重新拉状态与历史（失败时页面不再卡在 loading） */
async function retry() {
  loading.value = true
  loadingSeconds.value = 0
  error.value = ''
  const tick = setInterval(() => { loadingSeconds.value += 1 }, 1000)
  try {
    await Promise.allSettled([loadState(), loadHistory()])
  } finally {
    clearInterval(tick)
    loading.value = false
  }
}

async function onBasicSaved() {
  await Promise.all([loadState(), loadHistory()])
}

onMounted(async () => {
  // 无论成功/失败/超时，都必须退出 loading —— 曾因 Promise.all 抛错导致页面一直显示"正在读取测评状态…"
  // 再加一道保险：即使请求永不返回（代理/网络挂住），15 秒后也强制结束 loading 并给出可重试的提示
  loadingSeconds.value = 0
  loadingTick = setInterval(() => { loadingSeconds.value += 1 }, 1000)
  const guard = setTimeout(() => {
    if (loading.value) {
      loading.value = false
      if (!error.value) error.value = '加载超时（15 秒内没有收到后端响应）：请确认后端已启动（8080）、代理/网络正常，然后点「重试」。'
      console.warn('[能力补充测评] 初始化超时，已强制退出 loading')
    }
  }, 15000)
  try {
    await Promise.allSettled([loadState(), loadHistory()])
  } catch (e) {
    console.error('[能力补充测评] 初始化失败:', e)
  } finally {
    clearTimeout(guard)
    clearInterval(loadingTick)
    loading.value = false
  }
})

onUnmounted(() => { stopTimer(); stopVoice() })
</script>

<style scoped>
.assess-page { max-width: 1080px; margin: 0 auto; padding: 28px 24px 60px; color: #1E293B; }
.page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.eyebrow { margin: 0 0 6px; font-size: 12px; letter-spacing: 2px; color: #64748B; }
.build-tag { color: #1D4ED8; font-weight: 700; }
/* 纯 CSS 动画：JS 主线程被冻结时它仍会转 —— 用来区分"代码没更新"和"页面卡死" */
.js-spinner { display: inline-block; width: 12px; height: 12px; margin-left: 8px; vertical-align: -1px;
  border: 2px solid #BFDBFE; border-top-color: #4A90E2; border-radius: 50%; animation: js-spin .8s linear infinite; }
@keyframes js-spin { to { transform: rotate(360deg); } }
h1 { margin: 0 0 8px; font-size: 26px; }
.sub { margin: 0; color: #64748B; font-size: 0.9rem; line-height: 1.7; max-width: 720px; }
.link { color: #4A90E2; text-decoration: none; font-size: 0.9rem; }
.card { background: #fff; border: 1px solid #E2E8F0; border-radius: 16px; padding: 22px; margin-bottom: 18px; box-shadow: 0 4px 18px rgba(51,65,85,.04); }
.card h2 { margin: 0 0 12px; font-size: 1.05rem; }
.muted { color: #64748B; }
.small { font-size: 12px; }
.notice { background: #EFF6FF; border: 1px solid #DBEAFE; border-radius: 10px; padding: 12px; font-size: 13px; line-height: 1.7; }
.notice.error { background: #FFF4F2; border-color: #FBD2C8; color: #9F3020; }
.btn { font: inherit; border: 0; border-radius: 9px; padding: 10px 18px; cursor: pointer; text-decoration: none; display: inline-block; }
.btn.primary { background: #4A90E2; color: #fff; }
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
.option b { color: #4A90E2; }
textarea { font: inherit; width: 100%; box-sizing: border-box; padding: 12px; border: 1px solid #CBD5E1; border-radius: 10px; line-height: 1.7; }
.answer-tools { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.voice-line { margin: 8px 0 0; }
.btn.ghost.recording { background: #FEE2E2; color: #B91C1C; }
.q-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 10px; }
.score-total { display: flex; align-items: baseline; gap: 10px; margin-bottom: 16px; }
.score-total strong { font-size: 44px; color: #4A90E2; }
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
/* 能力雷达区块（组件自身样式保持不变，仅控制外边距） */
.radar-block { margin-top: 18px; }
.radar-block :deep(.radar-glass-panel) { min-height: 460px; }
</style>
