<template>
  <transition name="quiz-fade">
    <div v-if="visible" class="quiz-overlay" @click.self="close">
      <div class="quiz-modal">
        <div class="quiz-header">
          <div class="quiz-title">
            <span class="quiz-badge">核心能力模型</span>
            <h3>{{ phaseTitle }}</h3>
          </div>
          <button class="quiz-close" @click="close">✕</button>
        </div>

        <!-- 进度 -->
        <div class="quiz-steps">
          <span :class="{ active: phase === 'basic', done: phase !== 'basic' }">1 基本情况</span>
          <i></i>
          <span :class="{ active: phase === 'quiz', done: phase === 'result' }">2 六维测评</span>
          <i></i>
          <span :class="{ active: phase === 'result' }">3 结果</span>
        </div>

        <!-- 第 1 步：基本情况 -->
        <div v-if="phase === 'basic'" class="quiz-body">
          <p class="quiz-tip">先填写你的基本专业情况（客观项），下一步将用约 10 道情境题评估 6 项软实力。</p>
          <div class="basic-grid">
            <label>学历 <i>*</i>
              <select v-model="basic.education">
                <option value="">请选择</option>
                <option value="high_school">高中/中专</option>
                <option value="college">专科</option>
                <option value="bachelor">本科</option>
                <option value="master">硕士</option>
                <option value="phd">博士</option>
              </select>
            </label>
            <label>专业
              <input v-model="basic.major" type="text" placeholder="如：信息管理与信息系统" />
            </label>
            <label>专业技能掌握度 <i>*</i>
              <select v-model="basic.skillLevel">
                <option value="">请选择</option>
                <option value="beginner">入门</option>
                <option value="basic">了解</option>
                <option value="proficient">熟练</option>
                <option value="expert">精通</option>
              </select>
            </label>
            <label>主要技能
              <input v-model="basic.skillDesc" type="text" placeholder="如：SQL / Python / Excel" />
            </label>
            <label>实习/项目时长 <i>*</i>
              <select v-model="basic.internshipMonths">
                <option value="">请选择</option>
                <option value="0">无</option>
                <option value="1_3">1-3 个月</option>
                <option value="4_6">4-6 个月</option>
                <option value="7_12">7-12 个月</option>
                <option value="12_plus">12 个月以上</option>
              </select>
            </label>
            <label>实习/项目简述
              <input v-model="basic.internshipDesc" type="text" placeholder="如：电商数据分析实习" />
            </label>
            <label>证书数量 <i>*</i>
              <select v-model="basic.certCount">
                <option value="">请选择</option>
                <option value="0">暂无</option>
                <option value="1">1 项</option>
                <option value="2">2 项</option>
                <option value="3_plus">3 项及以上</option>
              </select>
            </label>
            <label>证书名称
              <input v-model="basic.certDesc" type="text" placeholder="如：CET-6 / 软件设计师" />
            </label>
          </div>
        </div>

        <!-- 第 2 步：六维测评 -->
        <div v-else-if="phase === 'quiz'" class="quiz-body">
          <div class="quiz-progress-text">
            第 {{ qIndex + 1 }} / {{ questions.length }} 题 · 已答 {{ answeredCount }} 题
          </div>
          <div class="quiz-bar"><span :style="{ width: ((qIndex + 1) / questions.length * 100) + '%' }"></span></div>

          <div v-if="currentQuestion" class="question-block">
            <p class="question-text">{{ currentQuestion.text }}</p>
            <div class="option-list">
              <button
                v-for="opt in currentQuestion.options"
                :key="opt.k"
                class="option-btn"
                :class="{ chosen: answers[currentQuestion.id] === opt.k }"
                @click="choose(opt.k)"
              >{{ opt.t }}</button>
            </div>
          </div>
        </div>

        <!-- 第 3 步：结果 -->
        <div v-else class="quiz-body">
          <div class="result-head">
            <div class="result-total">{{ result?.scores?.total ?? '—' }}<small>综合得分</small></div>
            <p class="result-comment">{{ result?.comment }}</p>
          </div>
          <div class="score-grid">
            <div v-for="d in (result?.dimensions || [])" :key="d.key" class="score-item">
              <div class="score-item-head"><span>{{ d.name }}</span><b>{{ d.score }}</b></div>
              <div class="score-track"><i :style="{ width: d.score + '%' }"></i></div>
            </div>
          </div>
        </div>

        <p v-if="error" class="quiz-error">{{ error }}</p>

        <div class="quiz-footer">
          <template v-if="phase === 'basic'">
            <button class="qbtn ghost" @click="close">取消</button>
            <button class="qbtn primary" :disabled="loading" @click="startQuiz">{{ loading ? '出题中…' : '下一步：开始测评' }}</button>
          </template>
          <template v-else-if="phase === 'quiz'">
            <button class="qbtn ghost" @click="prev" :disabled="qIndex === 0">上一题</button>
            <button v-if="qIndex < questions.length - 1" class="qbtn ghost" @click="qIndex++">跳过</button>
            <button class="qbtn primary" :disabled="loading || !allAnswered" @click="submit">
              {{ loading ? '评分中…' : '提交并生成评分' }}
            </button>
          </template>
          <template v-else>
            <button class="qbtn ghost" @click="restart">重新测评</button>
            <button class="qbtn primary" @click="finish">完成</button>
          </template>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import axios from 'axios'

const props = defineProps({
  visible: { type: Boolean, default: false },
  userId: { type: [String, Number], default: '' }
})
const emit = defineEmits(['update:visible', 'saved'])

const phase = ref('basic')
const loading = ref(false)
const error = ref('')
const basic = reactive({
  education: '', major: '', skillLevel: '', skillDesc: '',
  internshipMonths: '', internshipDesc: '', certCount: '', certDesc: ''
})
const questions = ref([])
const qIndex = ref(0)
const answers = reactive({})
const result = ref(null)

const phaseTitle = computed(() => ({
  basic: '填写基本情况',
  quiz: '六维能力测评',
  result: '你的能力画像'
}[phase.value]))

const currentQuestion = computed(() => questions.value[qIndex.value] || null)
const answeredCount = computed(() => questions.value.filter(q => answers[q.id] !== undefined).length)
const allAnswered = computed(() => questions.value.length > 0 && answeredCount.value === questions.value.length)

const authHeaders = () => {
  const t = localStorage.getItem('token') || ''
  return t ? { Authorization: t.startsWith('Bearer ') ? t : `Bearer ${t}` } : {}
}
const close = () => emit('update:visible', false)

watch(() => props.visible, (v) => {
  if (v) {
    phase.value = 'basic'
    result.value = null
    error.value = ''
  }
})

const startQuiz = async () => {
  error.value = ''
  if (!basic.education || !basic.skillLevel || !basic.internshipMonths || !basic.certCount) {
    error.value = '请先完成带 * 的基本情况'
    return
  }
  loading.value = true
  try {
    const res = await axios.get('/api/ability/quiz', { headers: authHeaders() })
    if (res.data.code === 10001 || res.data.code === 200) {
      questions.value = res.data.data?.questions || []
      if (questions.value.length === 0) { error.value = '题库为空，请稍后重试'; return }
      qIndex.value = 0
      Object.keys(answers).forEach(k => delete answers[k])
      phase.value = 'quiz'
    } else {
      error.value = res.data.message || '抽题失败'
    }
  } catch (e) {
    error.value = '抽题失败，请确认后端已启动'
  } finally {
    loading.value = false
  }
}

const choose = (k) => {
  if (!currentQuestion.value) return
  answers[currentQuestion.value.id] = k
  if (qIndex.value < questions.value.length - 1) {
    setTimeout(() => { qIndex.value++ }, 120)
  }
}
const prev = () => { if (qIndex.value > 0) qIndex.value-- }

const submit = async () => {
  error.value = ''
  if (!allAnswered.value) { error.value = '还有题目未作答'; return }
  loading.value = true
  try {
    const payload = {
      basic: { ...basic },
      answers: questions.value.map(q => ({ id: q.id, k: answers[q.id] }))
    }
    const res = await axios.post('/api/ability/quiz/submit', payload, {
      headers: { ...authHeaders(), 'Content-Type': 'application/json' }
    })
    if (res.data.code === 10001 || res.data.code === 200) {
      result.value = res.data.data
      phase.value = 'result'
      emit('saved')
    } else {
      error.value = res.data.message || '提交失败'
    }
  } catch (e) {
    error.value = '提交失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const restart = () => { phase.value = 'basic'; result.value = null; error.value = '' }
const finish = () => close()
</script>

<style scoped>
.quiz-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.55); backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center; z-index: 10000; }
.quiz-modal { width: 92%; max-width: 720px; max-height: 90vh; background: #FFFFFF; border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22); display: flex; flex-direction: column; overflow: hidden; }
.quiz-header { padding: 18px 22px; border-bottom: 1px solid #EEF2F7; display: flex; align-items: center; justify-content: space-between; }
.quiz-title h3 { margin: 4px 0 0; font-size: 1.15rem; color: #1E293B; }
.quiz-badge { font-size: 0.72rem; font-weight: 600; color: #2563EB; background: #EFF6FF; padding: 3px 9px; border-radius: 6px; }
.quiz-close { background: none; border: 1px solid #E2E8F0; border-radius: 8px; width: 30px; height: 30px; cursor: pointer; color: #94A3B8; }
.quiz-close:hover { color: #EF4444; border-color: #FECACA; }

.quiz-steps { display: flex; align-items: center; gap: 8px; padding: 12px 22px; font-size: 0.8rem; color: #94A3B8; border-bottom: 1px solid #F4F7FB; }
.quiz-steps i { flex: 1; height: 1px; background: #E2E8F0; }
.quiz-steps span.active { color: #2563EB; font-weight: 700; }
.quiz-steps span.done { color: #10B981; }

.quiz-body { padding: 18px 22px; overflow-y: auto; flex: 1; }
.quiz-tip { margin: 0 0 14px; font-size: 0.86rem; color: #64748B; }
.basic-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 16px; }
.basic-grid label { display: flex; flex-direction: column; gap: 6px; font-size: 0.82rem; color: #475569; font-weight: 600; }
.basic-grid label i { color: #EF4444; font-style: normal; }
.basic-grid select, .basic-grid input { padding: 9px 11px; border: 1px solid #DFE6EF; border-radius: 8px; font-size: 0.88rem;
  color: #1E293B; font-family: inherit; outline: none; box-sizing: border-box; background: #FFFFFF; }
.basic-grid select:focus, .basic-grid input:focus { border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.1); }

.quiz-progress-text { font-size: 0.82rem; color: #64748B; margin-bottom: 8px; }
.quiz-bar { height: 6px; background: #EEF2F7; border-radius: 999px; overflow: hidden; margin-bottom: 16px; }
.quiz-bar span { display: block; height: 100%; background: #4A90E2; transition: width 0.2s ease; }
.question-text { font-size: 1rem; color: #1E293B; font-weight: 650; margin: 0 0 14px; line-height: 1.6; }
.option-list { display: flex; flex-direction: column; gap: 10px; }
.option-btn { text-align: left; padding: 12px 14px; border: 1px solid #E2E8F0; border-radius: 10px; background: #F8FAFC;
  color: #334155; font-size: 0.9rem; cursor: pointer; transition: 0.16s; font-family: inherit; }
.option-btn:hover { border-color: #93C5FD; background: #EFF6FF; }
.option-btn.chosen { border-color: #4A90E2; background: #EFF6FF; color: #1D4ED8; font-weight: 600; }

.result-head { text-align: center; margin-bottom: 16px; }
.result-total { font-size: 2.4rem; font-weight: 800; color: #1D4ED8; line-height: 1; }
.result-total small { display: block; font-size: 0.8rem; font-weight: 500; color: #94A3B8; margin-top: 6px; }
.result-comment { margin: 12px auto 0; max-width: 560px; font-size: 0.86rem; color: #475569; line-height: 1.7; }
.score-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 18px; }
.score-item-head { display: flex; justify-content: space-between; font-size: 0.82rem; color: #475569; margin-bottom: 5px; }
.score-item-head b { color: #1E293B; }
.score-track { height: 7px; background: #EEF2F7; border-radius: 999px; overflow: hidden; }
.score-track i { display: block; height: 100%; background: linear-gradient(90deg, #60A5FA, #4A90E2); }

.quiz-error { margin: 0 22px; padding: 8px 12px; background: #FEF2F2; color: #B91C1C; border: 1px solid #FECACA; border-radius: 8px; font-size: 0.84rem; }
.quiz-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 22px; border-top: 1px solid #EEF2F7; background: #FAFBFD; }
.qbtn { border: 1px solid transparent; border-radius: 9px; padding: 9px 18px; font-size: 0.88rem; font-weight: 600; cursor: pointer; font-family: inherit; }
.qbtn.primary { background: #4A90E2; color: #FFFFFF; }
.qbtn.primary:hover:not(:disabled) { background: #357ABD; }
.qbtn.primary:disabled { opacity: 0.5; cursor: not-allowed; }
.qbtn.ghost { background: #FFFFFF; color: #475569; border-color: #DFE6EF; }
.qbtn.ghost:hover { background: #F1F5F9; }

@media (max-width: 640px) {
  .basic-grid, .score-grid { grid-template-columns: 1fr; }
}
.quiz-fade-enter-active, .quiz-fade-leave-active { transition: opacity 0.2s ease; }
.quiz-fade-enter-from, .quiz-fade-leave-to { opacity: 0; }
</style>
