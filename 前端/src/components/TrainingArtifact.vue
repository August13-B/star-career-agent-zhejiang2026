<template>
  <section id="training-artifact" class="artifact-panel">
    <h2>{{ session.template.artifactTitle }}</h2>
    <p>草稿自动保存；点击“保存作品版本”留下修订记录。结束训练时冻结最后一个保存版本，评分只引用该版本。</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <form @submit.prevent="saveVersion().catch(report)">
      <label v-for="field in session.template.artifactFields" :key="field.key" :for="`artifact-${field.key}`">
        {{ field.label }}{{ field.required ? ' *' : '' }}
        <select v-if="field.options" :id="`artifact-${field.key}`" v-model="content[field.key]" :disabled="readonly || savingVersion || locked" @change="schedule"><option value="">请选择</option><option v-for="option in field.options" :key="option">{{ option }}</option></select>
        <textarea v-else :id="`artifact-${field.key}`" v-model="content[field.key]" :maxlength="field.maxLength" :rows="field.maxLength < 100 ? 1 : 4" :disabled="readonly || savingVersion || locked" @input="schedule"></textarea>
      </label>
      <p class="muted">{{ saving ? '正在保存草稿…' : dirty ? '草稿尚未保存' : '草稿已保存' }} · 已保存 {{ session.artifact?.revision || 0 }} 个作品版本</p>
      <button v-if="!readonly" type="submit" :disabled="savingVersion || conflict || locked">{{ savingVersion ? '保存中…' : '保存作品版本' }}</button>
      <button v-if="conflict" type="button" @click="loadLatest">保留输入于下方并读取最新草稿</button>
    </form>
    <details v-if="backup"><summary>冲突前的输入（可复制）</summary><pre>{{ backup }}</pre></details>
    <details v-if="session.artifactRevisions?.length">
      <summary>查看历史作品版本</summary>
      <select v-model="selectedRevision" aria-label="历史作品版本" @change="preview"><option value="">请选择版本</option><option v-for="item in session.artifactRevisions" :key="item.id" :value="item.revision">版本 {{ item.revision }}</option></select>
      <div v-if="selected"><p v-for="field in session.template.artifactFields" :key="field.key"><strong>{{ field.label }}：</strong><span>{{ selected.content[field.key] }}</span></p></div>
    </details>
  </section>
</template>
<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { newTrainingRequestId, trainingRequest } from '../utils/trainingApi'
const props = defineProps({ session: { type: Object, required: true }, locked: Boolean })
const emit = defineEmits(['saved'])
const content = ref({}), saved = ref('{}'), version = ref(0), saving = ref(false), savingVersion = ref(false), conflict = ref(false), error = ref(''), backup = ref('')
const selectedRevision = ref(''), selected = ref(null)
const readonly = computed(() => props.session.status !== 'active')
const dirty = computed(() => JSON.stringify(content.value) !== saved.value)
let timer, savingPromise, revisionRequest
function report(e) { error.value = e.message }
function adopt(s) { content.value = { ...(s.status === 'active' ? s.artifactDraft : s.artifact?.content || s.artifactDraft) }; saved.value = JSON.stringify(content.value); version.value = s.artifactDraftVersion; conflict.value = false }
watch(() => props.session, s => {
  if (s.status !== 'active') { if (dirty.value) backup.value = JSON.stringify(content.value, null, 2); adopt(s); return }
  if (!saving.value && !dirty.value && s.artifactDraftVersion >= version.value) adopt(s)
  else if (!saving.value && s.artifactDraftVersion > version.value) conflict.value = true
}, { immediate: true })
function schedule() { clearTimeout(timer); timer = setTimeout(() => flush().catch(report), 700) }
async function flush() {
  clearTimeout(timer)
  if (savingPromise) { await savingPromise; return flush() }
  if (readonly.value || !dirty.value) return
  if (conflict.value) throw new Error('作品草稿版本冲突，请先保留当前输入并读取最新草稿')
  const value = JSON.stringify(content.value)
  saving.value = true
  savingPromise = trainingRequest(`/sessions/${props.session.id}/artifact-draft`, { method: 'PUT', body: { content: JSON.parse(value), expectedVersion: version.value } })
    .then(data => { saved.value = value; version.value = data.artifactDraftVersion })
    .catch(e => { if (e.status === 409) conflict.value = true; report(e); throw e })
    .finally(() => { saving.value = false; savingPromise = null })
  return savingPromise
}
async function saveVersion() {
  if (savingVersion.value) throw new Error('作品版本正在保存，请稍候')
  savingVersion.value = true; error.value = ''
  try {
    await flush()
    const value = JSON.stringify(content.value)
    const latest = props.session.artifact?.content
    if (latest && Object.keys(latest).every(key => latest[key] === (content.value[key] || ''))) return
    if (!revisionRequest || revisionRequest.value !== value) revisionRequest = { value, clientRequestId: newTrainingRequestId(), expectedRevision: props.session.artifact?.revision || 0 }
    await trainingRequest(`/sessions/${props.session.id}/artifacts`, { method: 'POST', body: { content: JSON.parse(value), clientRequestId: revisionRequest.clientRequestId, expectedRevision: revisionRequest.expectedRevision } })
    revisionRequest = null; emit('saved')
  } catch (e) { report(e); throw e } finally { savingVersion.value = false }
}
async function loadLatest() { backup.value = JSON.stringify(content.value, null, 2); try { adopt(await trainingRequest(`/sessions/${props.session.id}`)); error.value = '' } catch (e) { report(e) } }
async function preview() { try { selected.value = selectedRevision.value ? await trainingRequest(`/sessions/${props.session.id}/artifacts/${selectedRevision.value}`) : null } catch (e) { report(e) } }
onUnmounted(() => clearTimeout(timer))
defineExpose({ flush, saveVersion, hasUnsaved: () => dirty.value })
</script>
<style scoped>
.artifact-panel{background:white;border:1px solid #e2e8f0;border-radius:18px;padding:22px;margin-top:22px;color:#334155;scroll-margin:20px}h2{font-size:18px}p{line-height:1.7}label{display:block;margin:18px 0;font-weight:600;font-size:14px}textarea,select{display:block;box-sizing:border-box;width:100%;font:inherit;padding:10px;margin-top:7px;border:1px solid #cbd5e1;border-radius:8px;line-height:1.7;resize:vertical;color:inherit;background:#fff}button{background:#2563eb;color:white;border:0;padding:10px 16px;border-radius:9px;cursor:pointer}button:disabled{background:#e2e8f0;color:#64748b}.muted{font-size:12px;color:#64748b}.error{color:#b91c1c}details{margin-top:18px}summary{cursor:pointer;color:#2563eb}pre,span{white-space:pre-wrap;overflow-wrap:anywhere}textarea:focus-visible,select:focus-visible,button:focus-visible{outline:3px solid #93c5fd}
</style>
