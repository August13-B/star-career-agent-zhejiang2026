<template>
  <section class="outcome">
    <h3>能力画像更新</h3>
    <p>{{ session.profileApplication?.message || '本次尚未执行画像更新。' }}</p>
    <button v-if="['pending', 'failed'].includes(session.profileApplication?.status)" :disabled="pending" @click="apply">重试画像更新</button>
    <template v-if="session.profileApplication?.after">
      <p class="muted">版本 {{ session.profileApplication.version }} · 使用最近一次合格训练观察值更新覆盖维度。分数可能上升或下降，不代表正式职业认证。</p>
      <table><caption>画像更新前后（10维）</caption><thead><tr><th>能力维度</th><th>更新前</th><th>更新后</th></tr></thead><tbody><tr v-for="(label,key) in labels" :key="key"><th>{{ label }}</th><td>{{ session.profileApplication.before[key] }}</td><td>{{ session.profileApplication.after[key] }}</td></tr></tbody></table>
      <router-link to="/profile">查看个人能力画像</router-link>
    </template>
    <template v-if="session.previousEvaluation?.result?.dimensions && session.evaluation.status === 'valid'">
      <h3>与上一次完整训练比较</h3><p class="muted">只比较相同模板、难度的有效反馈；训练分与个人画像总分是不同指标。</p>
      <p v-for="(score,key) in session.evaluation.result.dimensions" :key="key">{{ session.template.dimensions[key] }}：{{ session.previousEvaluation.result.dimensions[key] }} → {{ score }}</p>
    </template>
    <template v-if="session.evaluation.result?.suggestions">
      <h3>把一条建议加入成长任务</h3>
      <p v-if="session.growthTask">已关联成长任务。每次训练只创建一条，重复操作不会增加任务。<router-link to="/growth">前往成长轨迹</router-link></p>
      <template v-else>
        <label>选择练习建议<select v-model="suggestion"><option v-for="(item,index) in session.evaluation.result.suggestions" :key="index" :value="index">{{ item }}</option></select></label>
        <label>加入已有成长计划<select v-model="planId"><option value="">请选择计划</option><option v-for="plan in plans" :key="plan.id" :value="plan.id">{{ plan.name }}</option></select></label>
        <p v-if="!plans.length">还没有可用计划，请先在<router-link to="/growth">成长轨迹</router-link>创建计划，然后返回本次训练。</p>
        <button :disabled="pending || !planId" @click="link">加入成长任务</button><button class="secondary" :disabled="pending" @click="loadPlans">刷新计划</button>
      </template>
    </template>
    <p v-if="error" role="alert" class="error">{{ error }}</p>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { trainingRequest } from '../utils/trainingApi'
const props = defineProps({ session: { type: Object, required: true } }), emit = defineEmits(['updated'])
const labels = { education: '学历背景', internship: '实习经历', professional: '专业技能', certificate: '证书资质', innovation: '创新能力', learning: '学习能力', pressure: '抗压能力', communication: '沟通能力', problem_solving: '问题解决', teamwork: '团队协作' }
const plans = ref([]), planId = ref(''), suggestion = ref(0), pending = ref(false), error = ref('')
async function loadPlans() { try { plans.value = await trainingRequest('/growth/plans') } catch (e) { error.value = e.message } }
async function mutate(path, body) { if (pending.value) return; pending.value = true; error.value = ''; try { await trainingRequest(`/sessions/${props.session.id}${path}`, { method: 'POST', body }); emit('updated') } catch (e) { error.value = e.message } finally { pending.value = false } }
function link() { return mutate('/growth-task', { planId: planId.value, suggestionIndex: suggestion.value }) }
function apply() { return mutate('/profile/retry') }
onMounted(loadPlans)
</script>
<style scoped>
.outcome{border-top:1px solid #e2e8f0;margin-top:24px;padding-top:10px}p{line-height:1.7}.muted{font-size:12px;color:#64748b}table{width:100%;max-width:650px;border-collapse:collapse;margin:16px 0}th,td{padding:8px;border-bottom:1px solid #e2e8f0;text-align:left}caption{text-align:left;font-weight:600}label{display:block;margin:15px 0}select{display:block;width:100%;max-width:720px;font:inherit;padding:10px;margin-top:7px;border:1px solid #cbd5e1;border-radius:8px}button{background:#2563eb;color:#fff;padding:10px 16px;border:0;border-radius:9px;margin-right:10px;cursor:pointer}button:disabled{background:#e2e8f0;color:#64748b}.secondary{background:#eff6ff;color:#2563eb}.error{color:#b91c1c}
</style>
