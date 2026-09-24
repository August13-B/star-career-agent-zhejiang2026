<template>
  <section class="training-page">
    <header class="training-header">
      <div><p class="eyebrow">练习 · 反馈 · 再出发</p><h1>{{ sessionId ? '职场训练工作台' : '职场训练' }}</h1></div>
      <router-link v-if="sessionId" class="button secondary" to="/training">返回训练大厅</router-link>
    </header>

    <div v-if="!loggedIn" class="panel empty">
      <h2>登录后开始练习</h2><p>训练记录会保存在你的账户中，离开页面后可以继续。</p>
      <router-link class="button" to="/login">前往登录</router-link>
    </div>
    <template v-else>
      <div v-if="error" class="notice error" role="alert">
        <span>{{ error }}</span><button class="text-button" @click="refresh(true)">重新加载</button>
        <router-link v-if="authExpired" to="/login">重新登录</router-link>
      </div>
      <p v-if="loading && !session && !templates.length" class="empty" role="status">正在读取训练记录…</p>

      <template v-if="!sessionId">
        <div class="panel preparation">
          <h2>开始前的设置</h2>
          <label>练习难度 <select v-model="difficulty"><option value="standard">标准：独立分析</option><option value="entry">入门：适当结构提示</option></select></label>
          <label><input v-model="useForProfile" type="checkbox"> 完整训练通过证据核验后，更新已有能力画像</label>
          <p class="muted small">无完整能力基线时只保存反馈。更新仅影响本次覆盖的维度，可能升降；阶段性反馈不更新画像。训练材料均为虚构，不要填写敏感信息。</p>
        </div>
        <div class="scenario-grid">
          <article v-for="item in templates" :key="item.id" class="panel scenario-card ready">
            <span class="pill">可以开始</span><h2>{{ item.title }}</h2><p>{{ item.description }}</p>
            <p class="muted">{{ item.rounds }} 个阶段 · {{ item.duration }} · 文字对话</p>
            <div class="tags"><span v-for="label in item.dimensions" :key="label">{{ label }}</span></div>
            <p class="muted small">会话、草稿与训练成果保存在当前账户，可随时返回。</p>
            <button :disabled="pending" @click="createSession(item.id)">{{ pending ? '正在创建…' : '开始训练' }}</button>
          </article>
        </div>
        <section class="panel history-panel">
          <div class="section-heading"><h2>我的训练记录</h2><button class="text-button" @click="loadHistory(false)">刷新记录</button></div>
          <div class="history-filters"><label>训练场景<select v-model="historyTemplate" @change="loadHistory(false)"><option value="">全部场景</option><option v-for="item in templates" :key="item.id" :value="item.id">{{ item.title }}</option></select></label><label>训练状态<select v-model="historyStatus" @change="loadHistory(false)"><option value="">全部状态</option><option value="active">进行中</option><option value="completed">已完成</option><option value="review_required">待复核</option><option value="canceled">已取消</option></select></label></div>
          <p v-if="!history.length" class="muted">还没有训练记录，完成第一场练习后再回顾自己的进步。</p>
          <router-link v-for="item in history" :key="item.id" class="history-row" :to="`/training/${item.id}`">
            <div><strong>{{ item.title }}</strong><span class="muted">{{ formatTime(item.createdAt) }} · 已回答 {{ item.answeredCount }}/{{ item.rounds }} 阶段</span></div>
            <span class="pill" :class="{ neutral: item.status === 'canceled' }">{{ trainingStatus(item.status) }}</span>
          </router-link>
          <button v-if="hasMore" class="secondary" :disabled="loadingHistory" @click="loadHistory(true)">加载更多</button>
        </section>
      </template>

      <template v-else-if="session">
        <div class="workspace">
          <aside class="panel brief">
            <span class="pill">{{ trainingStatus(session.status) }}</span><h2>{{ session.template.title }}</h2>
            <p>{{ session.template.description }}</p>
            <p class="muted small">{{ session.difficulty === 'entry' ? '入门难度' : '标准难度' }} · {{ session.useForProfile ? '允许更新已有能力画像' : '仅保存练习反馈' }}</p>
            <details v-if="session.template.materials" open><summary>任务材料</summary><p class="material-text">{{ materialText(session.template.materials) }}</p></details>
            <details v-if="session.template.practiceDraft"><summary>用于核验练习的预设错误草稿</summary><p class="material-text">{{ materialText(session.template.practiceDraft) }}</p><small>这是预设练习材料，并非本轮 AI 实际生成结果。</small></details>
            <p v-if="session.template.stageLabels" class="muted small">当前阶段：{{ session.template.stageLabels[Math.min(session.answeredCount, session.template.rounds - 1)] }}</p>
            <div class="progress-heading"><strong>回答进度</strong><span>{{ session.answeredCount }}/{{ session.template.rounds }}</span></div>
            <progress :value="session.answeredCount" :max="session.template.rounds" aria-label="回答进度"></progress>
            <ul class="dimension-list"><li v-for="(label, key) in session.template.dimensions" :key="key">{{ label }} <span>{{ session.template.weights[key] }}%</span></li></ul>
            <p class="muted small">可提前结束获得阶段性反馈。评分用于练习，不代表实际录用判断。</p>
            <p v-if="busy" class="notice" role="status">{{ session.run.operation === 'evaluate' ? '正在整理评分与回答证据…' : '训练角色正在组织回复…' }}<br>你可以切换页面，返回后继续查看。</p>
            <button v-if="canFinish" class="secondary" :disabled="pending || saving" @click="finishSession">结束并评分</button>
            <button v-if="canCancel" class="text-button danger" :disabled="pending" @click="cancelSession">取消本次训练</button>
          </aside>

          <section class="panel conversation">
            <div class="section-heading"><h2>训练对话</h2><span class="muted small">已提交的回答自动保存</span></div>
            <div ref="messagesElement" class="messages" @scroll="trackScroll">
              <article v-for="message in session.messages" :id="`training-message-${message.id}`" :key="message.id" class="message" :class="message.role">
                <strong>{{ message.speaker }}</strong>
                <p>{{ message.content || (message.status === 'queued' || message.status === 'generating' ? '正在准备问题…' : '本次未生成完整回复') }}</p>
                <span v-if="message.status === 'failed' || message.status === 'canceled'" class="muted small">未完成的回复 · {{ message.status === 'failed' ? '可重试' : '已取消' }}</span>
              </article>
            </div>
            <button v-if="hasNewMessages" class="text-button new-messages" @click="scrollToBottom">有新内容，回到底部</button>
            <div v-if="session.run?.status === 'failed'" class="notice error" role="alert">
              <span>{{ session.run.errorMessage }}</span>
              <button v-if="session.run.attempt < 3" class="secondary" :disabled="pending" @click="retryRun">重试本次任务</button>
              <p v-else class="small">请确认平台状态后开始新训练；此前回答已经保存。</p>
            </div>
            <form v-if="session.status === 'active' && session.answeredCount < session.template.rounds" class="composer" @submit.prevent="sendAnswer">
              <label for="training-answer">我的回答</label>
              <textarea id="training-answer" v-model="draft" maxlength="4000" rows="4" :disabled="pending" placeholder="说清你的做法、理由和验证方式。支持换行；点击按钮提交。" @input="scheduleDraft" @blur="saveDraft().catch(() => {})"></textarea>
              <div v-if="draftConflict" class="notice error">草稿已在其他页面修改。当前输入保留，你可以复制后再读取最新草稿。<button type="button" class="text-button" @click="reloadDraft">读取最新草稿</button></div>
              <div class="composer-actions"><button type="button" class="text-button" :disabled="!canAnswer || pending" @click="skipStage">跳过本阶段</button><span class="muted small">{{ saving ? '保存草稿中…' : draft === savedDraft ? '草稿已保存' : '草稿尚未保存' }} · {{ draft.length }}/4000</span><button type="submit" :disabled="!canAnswer || !draft.trim() || pending || draftConflict">{{ pending ? '提交中…' : '提交回答' }}</button></div>
            </form>
            <p v-else-if="session.status === 'active'" class="notice">所有阶段已回答完毕，等待回复完成，保存最终作品后点击“结束并评分”。</p>
          </section>
        </div>

        <TrainingArtifact v-if="session.template.artifactFields?.length" ref="artifactEditor" :session="session" :locked="pending" @saved="loadSession().catch(reportError)" />
        <section v-if="session.evaluation" class="panel evaluation">
          <div class="section-heading"><h2>{{ session.evaluation.status === 'partial' ? '阶段性训练反馈' : '本次训练反馈' }}</h2><span class="pill">{{ session.evaluation.status === 'review_required' ? '待复核' : '证据已核对' }}</span></div>
          <p class="notice">{{ session.evaluation.message }}。模拟评分用于练习反馈，不代表实际录用判断。</p>
          <template v-if="session.evaluation.result?.dimensions">
            <div class="score-summary"><strong>{{ session.evaluation.result.total }}<small>/100</small></strong><p>{{ session.evaluation.result.comment }}</p></div>
            <div class="scores"><div v-for="(value, key) in session.evaluation.result.dimensions" :key="key"><div class="progress-heading"><span>{{ session.template.dimensions[key] }}</span><strong>{{ value }}</strong></div><progress :value="value" max="100"></progress></div></div>
            <div v-if="session.evaluation.result.factChecks?.length" class="notice">
              <strong>可复核的事实检查</strong><p v-for="fact in session.evaluation.result.factChecks" :key="fact.name">{{ fact.passed ? '✓' : '✗' }} {{ fact.name }}：{{ fact.explanation }}</p><p>{{ session.evaluation.result.factCheckPolicy }}</p>
            </div>
            <h3>评分依据</h3><button v-for="(evidence, index) in session.evaluation.result.evidence" :key="index" class="evidence" @click="showEvidence(evidence)"><strong>{{ session.template.dimensions[evidence.dimension] }}</strong><span>“{{ evidence.quote }}”</span><small>{{ evidence.sourceType === 'artifact' ? '定位到最终作品 ↑' : '定位到原始回答 ↑' }}</small></button>
            <h3>下一次可以这样练</h3><ol><li v-for="suggestion in session.evaluation.result.suggestions" :key="suggestion">{{ suggestion }}</li></ol>
          </template>
          <p v-else class="muted">平台评价未满足结构或证据要求，因此没有展示未经验证的分数。你的训练问答和作品仍可回看。</p>
          <TrainingOutcome :session="session" @updated="loadSession().catch(reportError)" />
          <router-link class="button" to="/training">返回大厅，再练一次</router-link>
        </section>
      </template>
    </template>
    <dialog ref="confirmationElement" class="confirmation-dialog" aria-labelledby="training-confirm-title" @cancel.prevent="resolveConfirmation(false)">
      <h2 id="training-confirm-title">确认本次操作</h2>
      <p>{{ confirmationMessage }}</p>
      <div class="confirmation-actions"><button class="secondary" autofocus @click="resolveConfirmation(false)">返回检查</button><button @click="resolveConfirmation(true)">确认继续</button></div>
    </dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import TrainingArtifact from '../components/TrainingArtifact.vue'
import TrainingOutcome from '../components/TrainingOutcome.vue'
import { newTrainingRequestId, trainingBusy, trainingRequest, trainingStatus } from '../utils/trainingApi'

const difficulty = ref('standard'), useForProfile = ref(true), artifactEditor = ref(null)
const route = useRoute()
const router = useRouter()
const sessionId = computed(() => route.params.sessionId ? String(route.params.sessionId) : '')
const loggedIn = !!localStorage.getItem('token')
const historyTemplate = ref(''), historyStatus = ref('')
const templates = ref([]), history = ref([]), hasMore = ref(false), historyOffset = ref(0)
const session = ref(null), loading = ref(false), loadingHistory = ref(false), pending = ref(false)
const error = ref(''), authExpired = ref(false), messagesElement = ref(null), hasNewMessages = ref(false)
const confirmationElement = ref(null), confirmationMessage = ref('')
const draft = ref(''), savedDraft = ref(''), draftVersion = ref(0), saving = ref(false), draftConflict = ref(false)
const busy = computed(() => trainingBusy(session.value?.run))
const canAnswer = computed(() => session.value?.status === 'active' && !busy.value && session.value?.run?.status === 'succeeded')
const canFinish = computed(() => canAnswer.value && session.value?.answeredCount > 0)
const canCancel = computed(() => session.value && ['active', 'scoring'].includes(session.value.status))
let createRequest = null
const finishRequestId = newTrainingRequestId()
let answerRequest = null, disposed = false, pollTimer, draftTimer, savePromise, readSequence = 0
let followingBottom = true
let confirmationResolve
const pendingReads = new Set()

async function confirmAction(message) {
  confirmationMessage.value = message
  await nextTick()
  return new Promise(resolve => { confirmationResolve = resolve; confirmationElement.value.showModal() })
}
function resolveConfirmation(answer) { confirmationElement.value?.close(); confirmationResolve?.(answer); confirmationResolve = null }
function protectUnsavedDraft(event) {
  if (draft.value !== savedDraft.value || artifactEditor.value?.hasUnsaved()) { event.preventDefault(); event.returnValue = '' }
}

function reportError(reason) { if (!disposed && reason.name !== 'AbortError') { error.value = reason.message; authExpired.value = reason.status === 401 } }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '' }
function trackScroll() { const el = messagesElement.value; if (el) followingBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80 }
function scrollToBottom() { const el = messagesElement.value; if (el) el.scrollTop = el.scrollHeight; followingBottom = true; hasNewMessages.value = false }
function materialText(value) { return typeof value === 'string' ? value : Array.isArray(value) ? value.map(materialText).join('\n\n') : JSON.stringify(value, null, 2) }
function showEvidence(evidence) { document.getElementById(evidence.sourceType === 'artifact' ? 'training-artifact' : `training-message-${evidence.sourceId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' }) }

async function read(path) {
  const controller = new AbortController(); pendingReads.add(controller)
  try { return await trainingRequest(path, { signal: controller.signal }) }
  finally { pendingReads.delete(controller) }
}

async function loadSession(forceDraft = false) {
  const sequence = ++readSequence
  const data = await read(`/sessions/${sessionId.value}`)
  if (disposed || sequence !== readSequence) return
  const previousText = session.value?.messages.map(item => item.content).join('') || ''
  // A poll started before a draft write may arrive after it; never restore an older draft version.
  if (forceDraft || !session.value || (!saving.value && draft.value === savedDraft.value && data.draftVersion >= draftVersion.value)) {
    draft.value = data.draft; savedDraft.value = data.draft; draftVersion.value = data.draftVersion; draftConflict.value = false
  } else if (!saving.value && data.draftVersion > draftVersion.value) draftConflict.value = true
  session.value = data
  await nextTick()
  if (data.messages.map(item => item.content).join('') !== previousText) {
    if (followingBottom) scrollToBottom()
    else hasNewMessages.value = true
  }
}

async function loadHistory(append) {
  loadingHistory.value = true
  try {
    const page = await read(`/sessions?offset=${append ? historyOffset.value : 0}&limit=20${historyTemplate.value ? `&templateId=${encodeURIComponent(historyTemplate.value)}` : ''}${historyStatus.value ? `&status=${encodeURIComponent(historyStatus.value)}` : ''}`)
    if (disposed) return
    history.value = append ? [...history.value, ...page.items] : page.items
    hasMore.value = page.hasMore; historyOffset.value = page.nextOffset
  } catch (reason) { reportError(reason) }
  finally { loadingHistory.value = false }
}

async function refresh(clearError = false) {
  if (!loggedIn || disposed) return
  if (clearError === true) { error.value = ''; authExpired.value = false }
  clearTimeout(pollTimer); loading.value = true
  try {
    if (sessionId.value) await loadSession()
    else { templates.value = await read('/templates'); await loadHistory(false) }
  } catch (reason) { reportError(reason) }
  finally {
    loading.value = false
    if (sessionId.value && !disposed && !authExpired.value) pollTimer = setTimeout(refresh, busy.value ? 1200 : 5000)
  }
}

function scheduleDraft() { clearTimeout(draftTimer); draftTimer = setTimeout(() => saveDraft().catch(() => {}), 700) }
async function saveDraft() {
  clearTimeout(draftTimer)
  if (savePromise) { await savePromise; return saveDraft() }
  if (disposed || !session.value || session.value.status !== 'active' || draft.value === savedDraft.value) return
  if (draftConflict.value) throw new Error('草稿版本冲突，请先复制当前输入并读取最新草稿')
  const content = draft.value, version = draftVersion.value
  saving.value = true
  savePromise = trainingRequest(`/sessions/${sessionId.value}/draft`, { method: 'PUT', body: { content, expectedVersion: version } })
    .then(data => { savedDraft.value = content; draftVersion.value = data.draftVersion })
    .catch(reason => { if (reason.status === 409) draftConflict.value = true; reportError(reason); throw reason })
    .finally(() => { saving.value = false; savePromise = null })
  return savePromise
}

async function reloadDraft() {
  if (draft.value !== savedDraft.value && !await confirmAction('读取最新草稿将替换当前输入，请先复制需要保留的内容。继续？')) return
  clearTimeout(draftTimer)
  try { await loadSession(true); error.value = '' } catch (reason) { reportError(reason) }
}

async function createSession(templateId) {
  if (pending.value) return
  pending.value = true; error.value = ''
  try {
    const options = { templateId, difficulty: difficulty.value, useForProfile: useForProfile.value }
    const key = JSON.stringify(options)
    if (createRequest?.key !== key) createRequest = { key, id: newTrainingRequestId() }
    const created = await trainingRequest('/sessions', { method: 'POST', body: { ...options, clientRequestId: createRequest.id } })
    await router.push(`/training/${created.sessionId}`)
  } catch (reason) { reportError(reason); await loadHistory(false) }
  finally { pending.value = false }
}

async function mutate(action) {
  if (pending.value) return
  pending.value = true; error.value = ''
  try { await action(); await loadSession(true) }
  catch (reason) { reportError(reason); await loadSession().catch(reportError) }
  finally { pending.value = false; clearTimeout(pollTimer); if (!disposed) pollTimer = setTimeout(refresh, 1000) }
}

function sendAnswer() {
  if (!canAnswer.value || !draft.value.trim() || draftConflict.value) return
  return mutate(async () => {
    await saveDraft()
    const content = draft.value
    if (!answerRequest || answerRequest.content !== content) answerRequest = { content, clientRequestId: newTrainingRequestId() }
    await trainingRequest(`/sessions/${sessionId.value}/turns`, { method: 'POST', body: { ...answerRequest, expectedVersion: session.value.version } })
    answerRequest = null; draft.value = ''; savedDraft.value = ''; followingBottom = true
  })
}

async function skipStage() {
  if (draft.value.trim()) { error.value = '请先提交或清空当前回答，再跳过本阶段'; return }
  if (!await confirmAction('跳过会记录为缺少证据，本次只提供阶段性反馈且不更新能力画像。继续？')) return
  return mutate(async () => {
    await saveDraft()
    await trainingRequest(`/sessions/${sessionId.value}/turns`, { method: 'POST', body: { content: '跳过', skip: true, clientRequestId: newTrainingRequestId(), expectedVersion: session.value.version } })
  })
}

async function finishSession() {
  if (!await confirmAction('提交后不能修改本次回答。系统将根据已提交内容给出训练反馈，是否继续？')) return
  return mutate(async () => {
    await saveDraft()
    if (draft.value.trim()) throw new Error('还有未提交的回答，请先提交或清空草稿，再结束训练')
    await artifactEditor.value?.saveVersion()
    await loadSession()
    await trainingRequest(`/sessions/${sessionId.value}/finish`, { method: 'POST', body: { clientRequestId: finishRequestId, expectedVersion: session.value.version } })
  })
}
function retryRun() { return mutate(async () => { await saveDraft(); return trainingRequest(`/runs/${session.value.run.id}/retry`, { method: 'POST', body: { expectedAttempt: session.value.run.attempt } }) }) }
async function cancelSession() {
  if (!await confirmAction('取消后无法继续本次训练，已经保存的回答仍可回看。确定取消？')) return
  return mutate(async () => { await saveDraft(); await trainingRequest(`/sessions/${sessionId.value}/cancel`, { method: 'POST' }) })
}

onBeforeRouteLeave(async () => {
  try { await saveDraft(); await artifactEditor.value?.flush() }
  catch { return confirmAction('草稿未能保存。仍要离开此页面吗？') }
})
onMounted(() => { window.addEventListener('beforeunload', protectUnsavedDraft); refresh() })
onUnmounted(() => { disposed = true; confirmationResolve?.(false); window.removeEventListener('beforeunload', protectUnsavedDraft); clearTimeout(pollTimer); clearTimeout(draftTimer); pendingReads.forEach(controller => controller.abort()) })
</script>

<style scoped>
.history-filters{display:flex;flex-wrap:wrap;gap:16px;margin-bottom:16px}.history-filters select{font:inherit;margin-left:8px;padding:6px;border:1px solid #cbd5e1;border-radius:6px;max-width:230px}.preparation{margin-bottom:22px}.preparation label{display:block;margin:12px 0}.preparation select{font:inherit;border:1px solid #cbd5e1;border-radius:6px;padding:6px}.material-text{white-space:pre-wrap;overflow-wrap:anywhere;font-size:13px;max-height:340px;overflow-y:auto}summary{cursor:pointer;color:#2563eb}
.confirmation-dialog{border:1px solid #dbe4f0;border-radius:16px;padding:24px;max-width:420px;width:calc(100% - 64px);color:#1e293b;box-shadow:0 16px 60px #0f172a30}.confirmation-dialog::backdrop{background:#0f172a66}.confirmation-actions{display:flex;justify-content:flex-end;gap:12px;margin-top:22px}
.training-page{width:100%;min-width:0;height:100%;min-height:0;overflow-y:auto;padding:28px 32px 48px;box-sizing:border-box;color:#1e293b;background:linear-gradient(145deg,#f4f8ff,#f8fafc 65%)}
.training-header,.section-heading,.progress-heading,.composer-actions{display:flex;align-items:center;justify-content:space-between;gap:14px}.training-header{margin-bottom:24px}.eyebrow{font-size:12px;color:#64748b;letter-spacing:2px;margin:0 0 6px}h1{font-size:27px;margin:0}h2{font-size:18px;margin:0 0 12px}h3{font-size:16px;margin:22px 0 12px}p{line-height:1.7}.panel{background:#fff;border:1px solid #e2e8f0;border-radius:18px;padding:22px;box-shadow:0 5px 20px #33415505}.scenario-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}.scenario-card{display:flex;flex-direction:column;gap:8px}.scenario-card h2{margin:8px 0 0}.scenario-card p{margin:5px 0}.scenario-card button{margin-top:auto}.upcoming{background:#fbfcfe}.tags{display:flex;flex-wrap:wrap;gap:6px}.tags span{background:#eff6ff;color:#486386;font-size:12px;padding:5px 8px;border-radius:6px}.pill{display:inline-block;align-self:flex-start;width:fit-content;padding:5px 10px;border-radius:20px;background:#e9f5f0;color:#157856;font-size:12px;white-space:nowrap}.pill.neutral{background:#f1f5f9;color:#64748b}.muted{color:#64748b}.small{font-size:12px}.empty{padding:40px;text-align:center}button,.button{font:inherit;border:0;border-radius:9px;background:#2563eb;color:white;padding:10px 16px;cursor:pointer;text-decoration:none;display:inline-block;text-align:center}button:disabled{cursor:not-allowed;background:#e2e8f0;color:#64748b}button.secondary,.button.secondary{background:#eff6ff;color:#1d4ed8}button.text-button{background:transparent;color:#2563eb;padding:6px 0;text-align:left}button.danger{color:#b91c1c}button:focus-visible,.button:focus-visible,textarea:focus-visible{outline:3px solid #93c5fd;outline-offset:2px}.history-panel{margin-top:24px}.section-heading h2{margin:0}.section-heading{margin-bottom:16px}.history-row{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:17px 0;border-top:1px solid #edf2f7;text-decoration:none;color:inherit}.history-row div{display:flex;flex-direction:column;gap:6px}.history-row .muted{font-size:12px}.workspace{display:grid;grid-template-columns:270px minmax(0,1fr);align-items:start;gap:20px}.brief{display:flex;flex-direction:column;gap:14px}.brief p,.brief h2{margin:0}.brief .progress-heading{font-size:13px}.dimension-list{padding:0;margin:0;list-style:none}.dimension-list li{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f1f5f9;font-size:13px}progress{display:block;width:100%;height:9px;accent-color:#3b82f6;border:0}.conversation{min-width:0;min-height:0;display:flex;flex-direction:column}.messages{overflow-y:auto;min-height:180px;max-height:52vh;overscroll-behavior:contain;padding-right:8px}.message{background:#f6f8fc;border-radius:12px;padding:16px;margin:0 0 14px;scroll-margin:20px}.message.user{background:#edf5ff;margin-left:30px}.message strong{font-size:12px;color:#527195}.message p{white-space:pre-wrap;overflow-wrap:anywhere;margin:8px 0 0;font-size:14px}.composer{border-top:1px solid #e2e8f0;padding-top:16px;margin-top:10px}.composer label{display:block;font-size:13px;font-weight:600;margin-bottom:8px}textarea{font:inherit;font-size:14px;width:100%;min-height:110px;max-height:300px;resize:vertical;box-sizing:border-box;padding:12px;border:1px solid #cbd5e1;border-radius:9px;line-height:1.6}.composer-actions{margin-top:10px}.notice{background:#eff6ff;border:1px solid #dbeafe;border-radius:10px;padding:12px;font-size:13px;line-height:1.7;margin:0 0 14px}.notice.error{background:#fff4f2;color:#9f3020;border-color:#fbd2c8}.notice button{margin-left:8px}.evaluation{margin-top:22px}.score-summary{display:flex;align-items:center;gap:25px}.score-summary>strong{font-size:48px;color:#2563eb;white-space:nowrap}.score-summary small{font-size:17px;color:#64748b}.scores{display:grid;grid-template-columns:repeat(2,1fr);gap:20px;margin:18px 0}.scores progress{margin-top:8px}.evidence{display:flex;width:100%;flex-direction:column;gap:8px;background:#f8fafc!important;color:#334155!important;text-align:left;margin-bottom:10px;line-height:1.6;overflow-wrap:anywhere}.evidence small{color:#2563eb}.evaluation li{margin:9px 0;line-height:1.7}.evaluation>.button{margin-top:15px}.new-messages{align-self:center}
@media(max-width:1150px){.scenario-grid{grid-template-columns:1fr}.workspace{grid-template-columns:220px minmax(0,1fr)}.training-page{padding:22px}.scenario-card button{align-self:flex-start}}
@media(max-width:800px){.workspace{grid-template-columns:1fr}.training-page{padding:18px 12px 40px}.training-header{align-items:flex-start}h1{font-size:22px}.brief{padding:16px}.dimension-list{display:none}.messages{max-height:55vh;min-height:250px}.panel{padding:16px}.scores{grid-template-columns:1fr}.history-row,.score-summary{align-items:flex-start}.score-summary{flex-direction:column;gap:5px}.composer-actions{flex-wrap:wrap}.training-header .button{font-size:12px;padding:8px}}
</style>
