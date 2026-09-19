<template>
  <div class="growth-page">
    <div class="workspace">
      <header class="page-header">
        <div class="title-block">
          <span class="badge"><AppIcon name="trendUp" :size="13" /> 个人成长</span>
          <h1>我的 1 / 3 / 5 年规划</h1>
          <p class="subtitle">来自职业报告的结构化目标；展开可添待办、记录完成情况、打勾完成</p>
        </div>
        <button class="btn ghost" @click="load" :disabled="loading">刷新</button>
      </header>

      <div v-if="!userId" class="notice warn">请先登录后再查看成长规划。</div>
      <div v-else-if="loading" class="notice">加载中…</div>

      <div v-else-if="plans.length === 0" class="empty-plan">
        <p>还没有成长规划</p>
        <p class="empty-sub">去「多智能体中枢」生成一份职业报告，这里会自动出现 1/3/5 年目标与待办。</p>
        <router-link class="btn primary" to="/multi-agent">前往生成报告</router-link>
      </div>

      <section v-else class="plan-list">
        <article v-for="(p, pi) in plans" :key="p.plan.id" class="plan-card">
          <div class="plan-head" @click="togglePlan(pi)">
            <span class="plan-badge" :class="'h' + (p.plan.planType || 1)">{{ horizonLabel(p.plan.planType) }}</span>
            <div class="plan-info">
              <h3>{{ p.plan.planName }}</h3>
              <span class="plan-meta">目标岗位：{{ p.plan.targetJob }} · {{ dateRange(p.plan) }}</span>
            </div>
            <div class="plan-progress">
              <b>{{ Number(p.plan.progress || 0).toFixed(0) }}%</b>
              <div class="bar"><i :style="{ width: Number(p.plan.progress || 0) + '%' }"></i></div>
            </div>
            <button class="expand-btn">{{ expanded[pi] ? '收起 ▲' : '展开 ▼' }}</button>
          </div>

          <div v-if="expanded[pi]" class="plan-body">
            <div class="task-table">
              <div class="task-head-row">
                <span class="col-check"></span>
                <span class="col-name">任务</span>
                <span class="col-outcome">预期成果</span>
                <span class="col-status">状态</span>
              </div>

              <template v-if="p.tasks.length === 0">
                <div class="task-empty">暂无任务，可在下方「+ 添加代办任务」创建</div>
              </template>

              <div v-for="item in p.tasks" :key="item.task.id" class="task-block">
                <div class="task-row">
                  <span class="col-check">
                    <input type="checkbox" :checked="item.task.status === 2" @change="toggleDone(item.task)" title="打勾完成" />
                  </span>
                  <span class="col-name" :class="{ done: item.task.status === 2 }">{{ item.task.taskName }}</span>
                  <span class="col-outcome">{{ item.task.expectedOutcome || '—' }}</span>
                  <span class="col-status">
                    <i class="status-tag" :class="statusClass(item.task.status)">{{ statusText(item.task.status) }}</i>
                    <button class="link" @click="toggleRecords(item.task.id)">
                      {{ openRecords[item.task.id] ? '收起记录' : `记录(${item.records.length})` }}
                    </button>
                  </span>
                </div>

                <div v-if="openRecords[item.task.id]" class="timeline">
                  <div v-for="r in item.records" :key="r.id" class="tl-item">
                    <span class="tl-dot"></span>
                    <div class="tl-body">
                      <div class="tl-time">{{ r.recordTime }}</div>
                      <div class="tl-text">{{ r.content }}</div>
                    </div>
                  </div>
                  <div v-if="item.records.length === 0" class="tl-empty">还没有完成记录</div>
                  <div class="record-input">
                    <input v-model="recordDraft[item.task.id]" placeholder="记录一次完成情况…（Enter 提交）"
                           @keyup.enter="addRecord(item.task.id)" />
                    <button class="btn primary sm" :disabled="!String(recordDraft[item.task.id] || '').trim()"
                            @click="addRecord(item.task.id)">添加记录</button>
                  </div>
                </div>
              </div>
            </div>

            <div class="add-task">
              <input v-model="taskDraft[p.plan.id]" placeholder="新增待办任务名称…（Enter 创建）"
                     @keyup.enter="addTask(p.plan.id)" />
              <button class="btn primary sm" :disabled="!String(taskDraft[p.plan.id] || '').trim()"
                      @click="addTask(p.plan.id)">＋ 添加任务</button>
            </div>
          </div>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import AppIcon from '../components/AppIcon.vue'

const userId = ref(localStorage.getItem('userId') || '')
const plans = ref([])
const loading = ref(true)
const expanded = ref({})
const openRecords = ref({})
const recordDraft = ref({})
const taskDraft = ref({})

const headers = () => {
  const t = localStorage.getItem('token') || ''
  return t ? { Authorization: t.startsWith('Bearer ') ? t : `Bearer ${t}` } : {}
}

const load = async () => {
  if (!userId.value) { loading.value = false; return }
  loading.value = true
  try {
    const res = await axios.get('/api/grow/plans', { params: { userId: userId.value }, headers: headers() })
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      plans.value = Array.isArray(res.data.data) ? res.data.data : []
      if (plans.value.length > 0 && Object.keys(expanded.value).length === 0) {
        expanded.value[0] = true
      }
    }
  } catch (e) {
    console.error('获取成长规划失败', e)
  } finally {
    loading.value = false
  }
}

const togglePlan = (i) => { expanded.value[i] = !expanded.value[i] }
const toggleRecords = (taskId) => { openRecords.value[taskId] = !openRecords.value[taskId] }

const toggleDone = async (task) => {
  const next = task.status === 2 ? 0 : 2
  try {
    await axios.patch(`/api/grow/tasks/${task.id}`, { status: next }, { headers: headers() })
    await load()
  } catch (e) {
    alert('更新失败，请重试')
  }
}

const addRecord = async (taskId) => {
  const content = String(recordDraft.value[taskId] || '').trim()
  if (!content) return
  try {
    await axios.post(`/api/grow/tasks/${taskId}/records`, { content },
      { headers: { ...headers(), 'Content-Type': 'application/json' } })
    recordDraft.value[taskId] = ''
    openRecords.value[taskId] = true
    await load()
  } catch (e) {
    alert('记录失败，请重试')
  }
}

const addTask = async (planId) => {
  const taskName = String(taskDraft.value[planId] || '').trim()
  if (!taskName) return
  try {
    await axios.post('/api/grow/tasks', { planId, taskName },
      { headers: { ...headers(), 'Content-Type': 'application/json' } })
    taskDraft.value[planId] = ''
    await load()
  } catch (e) {
    alert(e?.response?.data?.message || '创建失败，请重试')
  }
}

const horizonLabel = (t) => ({ 1: '1 年', 2: '3 年', 3: '5 年' }[t] || '目标')
const statusText = (s) => ({ 0: '未开始', 1: '进行中', 2: '已完成', 3: '已暂停', 4: '已延期' }[s] || '未开始')
const statusClass = (s) => ({ 0: 'todo', 1: 'doing', 2: 'done', 3: 'paused', 4: 'delay' }[s] || 'todo')
const dateRange = (p) => {
  const f = (d) => (d ? String(d).slice(0, 10) : '—')
  return `${f(p.startDate)} ~ ${f(p.endDate)}`
}

onMounted(load)
</script>

<style scoped>
.growth-page { width: 100%; height: 100%; overflow-y: auto; background: #F6F8FC; padding: 20px; box-sizing: border-box; }
.workspace { max-width: 1040px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 16px; }
.title-block h1 { margin: 8px 0 4px; font-size: 1.45rem; font-weight: 700; color: #1E293B; }
.badge { display: inline-flex; align-items: center; gap: 6px; font-size: 0.74rem; font-weight: 600; color: #2563EB; background: #EFF6FF; padding: 4px 10px; border-radius: 6px; }
.subtitle { margin: 0; color: #64748B; font-size: 0.88rem; }
.btn { display: inline-flex; align-items: center; gap: 6px; border: 1px solid transparent; border-radius: 8px; padding: 9px 16px; font-size: 0.86rem; font-weight: 600; cursor: pointer; text-decoration: none; font-family: inherit; }
.btn.sm { padding: 7px 13px; font-size: 0.82rem; }
.btn.primary { background: #4A90E2; color: #FFF; }
.btn.primary:hover:not(:disabled) { background: #357ABD; }
.btn.primary:disabled { opacity: .5; cursor: not-allowed; }
.btn.ghost { background: #FFF; color: #475569; border-color: #DFE6EF; }

.notice { border-radius: 10px; padding: 14px 16px; font-size: 0.88rem; background: #FFF; border: 1px solid #E4EAF2; color: #64748B; }
.notice.warn { background: #FFFBEB; border-color: #FDE68A; color: #B45309; }
.empty-plan { background: #FFF; border: 1px dashed #CBD5E1; border-radius: 14px; padding: 40px 24px; text-align: center; color: #64748B; }
.empty-plan p { margin: 0 0 8px; }
.empty-sub { font-size: 0.85rem; color: #94A3B8; margin-bottom: 18px !important; }

.plan-list { display: flex; flex-direction: column; gap: 14px; }
.plan-card { background: #FFF; border: 1px solid #E4EAF2; border-radius: 14px; overflow: hidden; }
.plan-head { display: flex; align-items: center; gap: 14px; padding: 16px 18px; cursor: pointer; }
.plan-head:hover { background: #FAFCFE; }
.plan-badge { flex-shrink: 0; font-size: 0.78rem; font-weight: 700; color: #1D4ED8; background: #EFF6FF; padding: 4px 10px; border-radius: 8px; }
.plan-badge.h2 { color: #7C3AED; background: #F5F3FF; }
.plan-badge.h3 { color: #059669; background: #ECFDF5; }
.plan-info { flex: 1; min-width: 0; }
.plan-info h3 { margin: 0 0 4px; font-size: 1rem; color: #1E293B; }
.plan-meta { font-size: 0.8rem; color: #94A3B8; }
.plan-progress { display: flex; flex-direction: column; align-items: flex-end; gap: 5px; min-width: 96px; }
.plan-progress b { font-size: 0.86rem; color: #1D4ED8; }
.plan-progress .bar { width: 90px; height: 6px; background: #EEF2F7; border-radius: 999px; overflow: hidden; }
.plan-progress .bar i { display: block; height: 100%; background: linear-gradient(90deg, #60A5FA, #4A90E2); }
.expand-btn { flex-shrink: 0; background: none; border: 1px solid #DFE6EF; border-radius: 8px; padding: 6px 12px; font-size: 0.8rem; color: #475569; cursor: pointer; font-family: inherit; }
.expand-btn:hover { background: #F1F5F9; }

.plan-body { border-top: 1px solid #F1F5F9; padding: 6px 18px 18px; }
.task-table { width: 100%; }
.task-head-row, .task-row { display: grid; grid-template-columns: 40px 1.6fr 1.4fr 200px; align-items: center; gap: 10px; }
.task-head-row { padding: 10px 0; font-size: 0.76rem; font-weight: 700; color: #94A3B8; border-bottom: 1px solid #F1F5F9; }
.task-block { border-bottom: 1px dashed #F1F5F9; }
.task-row { padding: 12px 0; font-size: 0.88rem; color: #334155; }
.col-check { display: flex; justify-content: center; }
.col-check input { width: 17px; height: 17px; accent-color: #10B981; cursor: pointer; }
.col-name.done { color: #94A3B8; text-decoration: line-through; }
.col-outcome { font-size: 0.82rem; color: #64748B; }
.col-status { display: flex; align-items: center; gap: 8px; justify-content: flex-end; }
.status-tag { font-style: normal; font-size: 0.72rem; font-weight: 700; padding: 2px 8px; border-radius: 999px; background: #F1F5F9; color: #64748B; }
.status-tag.done { background: #ECFDF5; color: #059669; }
.status-tag.doing { background: #EFF6FF; color: #2563EB; }
.status-tag.delay { background: #FEF2F2; color: #DC2626; }
.status-tag.paused { background: #FFFBEB; color: #B45309; }
.link { background: none; border: none; color: #4A90E2; font-size: 0.8rem; font-weight: 600; cursor: pointer; font-family: inherit; padding: 2px 4px; border-radius: 4px; }
.link:hover { background: #EFF6FF; }
.task-empty { padding: 16px 0; font-size: 0.85rem; color: #94A3B8; }

.timeline { margin: 2px 0 12px 40px; padding-left: 14px; border-left: 2px solid #E2E8F0; }
.tl-item { display: flex; gap: 8px; padding: 5px 0; position: relative; }
.tl-dot { position: absolute; left: -21px; top: 10px; width: 8px; height: 8px; border-radius: 50%; background: #4A90E2; }
.tl-time { font-size: 0.76rem; color: #94A3B8; }
.tl-text { font-size: 0.86rem; color: #334155; }
.tl-empty { font-size: 0.82rem; color: #B6C2D2; padding: 6px 0; }
.record-input { display: flex; gap: 8px; margin-top: 8px; }
.record-input input { flex: 1; padding: 8px 11px; border: 1px solid #DFE6EF; border-radius: 8px; font-size: 0.85rem; outline: none; font-family: inherit; }
.record-input input:focus { border-color: #4A90E2; }

.add-task { display: flex; gap: 10px; margin-top: 14px; }
.add-task input { flex: 1; padding: 10px 12px; border: 1px dashed #CBD5E1; border-radius: 9px; font-size: 0.86rem; outline: none; font-family: inherit; }
.add-task input:focus { border-color: #4A90E2; border-style: solid; }
</style>
