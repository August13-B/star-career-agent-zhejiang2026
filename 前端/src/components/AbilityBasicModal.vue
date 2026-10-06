<template>
  <transition name="modal-fade">
    <div v-if="visible" class="ab-overlay" @click.self="close">
      <div class="ab-card">
        <div class="ab-head">
          <h3>基本情况（专业与硬实力）</h3>
          <button class="ab-close" @click="close">✕</button>
        </div>
        <div class="ab-body">
          <p class="ab-tip">
            这里只填<strong>专业与硬实力</strong>（学历 / 实习 / 专业技能 / 证书），按规则表换算成绩；
            <strong>六维软素质</strong>请到「能力补充测评」由 AI 问答产出。
          </p>
          <div class="ab-grid">
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
            <label>实习/项目说明
              <input v-model="basic.internshipDesc" type="text" placeholder="如：某公司后端实习，负责接口开发" />
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
            <label>主要证书
              <input v-model="basic.certDesc" type="text" placeholder="如：CET-6 / 软考中级" />
            </label>
          </div>
          <p v-if="error" class="ab-error" role="alert">{{ error }}</p>
          <p v-if="savedHint" class="ab-hint">{{ savedHint }}</p>
        </div>
        <div class="ab-foot">
          <button class="ab-btn ghost" @click="close">取消</button>
          <button class="ab-btn primary" :disabled="saving || !complete" @click="save">
            {{ saving ? '保存中…' : '保存基本情况' }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import axios from 'axios'
import API_CONFIG from '../config/api'

const props = defineProps({ visible: { type: Boolean, default: false }, userId: { type: [String, Number], default: null } })
const emit = defineEmits(['update:visible', 'saved'])

const empty = { education: '', major: '', skillLevel: '', skillDesc: '', internshipMonths: '', internshipDesc: '', certCount: '', certDesc: '' }
const basic = ref({ ...empty })
const saving = ref(false)
const error = ref('')
const savedHint = ref('')
const headers = () => {
  const token = localStorage.getItem('token') || ''
  return { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}
const complete = computed(() => !!(basic.value.education && basic.value.skillLevel && basic.value.internshipMonths && basic.value.certCount))

/** 打开时预填：原始选项来自 /api/assessment/state 的 basics（缺省则表单为空） */
watch(() => props.visible, async (open) => {
  if (!open) return
  error.value = ''
  savedHint.value = ''
  try {
    const res = await axios.get(`${API_CONFIG.BASE_URL}/api/assessment/state`, { headers: headers() })
    const data = res.data?.data || {}
    const saved = data.basics || {}
    basic.value = { ...empty, ...Object.fromEntries(Object.entries(saved).map(([k, v]) => [k, v == null ? '' : String(v)])) }
  } catch (e) {
    // 未登录/接口异常时保持空表单，用户仍可填写
  }
})

const close = () => emit('update:visible', false)

async function save() {
  if (!complete.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    const res = await axios.post(`${API_CONFIG.BASE_URL}/api/ability/quiz/basic`, basic.value, { headers: headers() })
    const body = res.data || {}
    if (body.code !== 10001 && body.code !== 200 && body.code !== 0) {
      error.value = body.message || '保存失败，请稍后重试'
      return
    }
    savedHint.value = '✓ 已保存：硬实力四项已更新，六维软素质保留（可去「能力补充测评」更新）'
    emit('saved', body.data || {})
    setTimeout(() => emit('update:visible', false), 900)
  } catch (e) {
    error.value = e.response?.data?.message || '保存出错，请稍后重试'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.ab-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, .5); display: flex; align-items: center; justify-content: center; z-index: 60; padding: 24px; }
.ab-card { width: min(760px, 100%); max-height: 88vh; overflow: auto; background: #fff; border-radius: 16px; box-shadow: 0 20px 60px rgba(15, 23, 42, .25); }
.ab-head { display: flex; align-items: center; justify-content: space-between; padding: 18px 22px; border-bottom: 1px solid #EAECEF; }
.ab-head h3 { margin: 0; font-size: 1.05rem; color: #1E293B; }
.ab-close { background: none; border: none; font-size: 1.1rem; color: #94A3B8; cursor: pointer; }
.ab-body { padding: 18px 22px; }
.ab-tip { margin: 0 0 14px; font-size: .85rem; line-height: 1.7; color: #64748B; background: #F0F7FF; border: 1px dashed #BFDBFE; border-radius: 10px; padding: 10px 12px; }
.ab-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.ab-grid label { display: flex; flex-direction: column; gap: 6px; font-size: .85rem; color: #334155; }
.ab-grid input, .ab-grid select { font: inherit; padding: 9px 10px; border: 1px solid #CBD5E1; border-radius: 8px; }
.ab-grid i { color: #EF4444; font-style: normal; }
.ab-error { margin: 14px 0 0; color: #B91C1C; font-size: .85rem; }
.ab-hint { margin: 14px 0 0; color: #157856; font-size: .85rem; }
.ab-foot { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 22px; border-top: 1px solid #EAECEF; background: #F8FAFC; }
.ab-btn { font: inherit; border: 0; border-radius: 9px; padding: 9px 16px; cursor: pointer; }
.ab-btn.primary { background: #2563EB; color: #fff; }
.ab-btn.primary:disabled { background: #E2E8F0; color: #94A3B8; cursor: not-allowed; }
.ab-btn.ghost { background: #EFF6FF; color: #1D4ED8; }
@media (max-width: 640px) { .ab-grid { grid-template-columns: 1fr; } }
</style>
