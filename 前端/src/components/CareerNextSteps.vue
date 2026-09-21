<template>
  <nav class="career-next" aria-label="职业探索下一步">
    <span>{{ target ? `探索目标：${target}` : '职业探索' }}</span>
    <router-link to="/profile">完善个人画像</router-link>
    <router-link :to="{ path: '/ai-score', query: { target } }">测评能力差距</router-link>
    <router-link :to="{ path: '/', query: { prompt } }">咨询智能体</router-link>
    <router-link :to="{ path: '/multi-agent', query: { target } }">生成职业报告</router-link>
    <router-link to="/growth">跟进成长计划</router-link>
  </nav>
</template>
<script setup>
import { computed } from 'vue'
const props = defineProps({ target: { type: String, default: '' } })
const prompt = computed(() => `请结合我最新的个人画像、能力测评和岗位要求，分析${props.target || '我的职业意向'}的能力差距，并给出可执行的学习与项目建议。资料不足时请说明，不要编造。`)
</script>
<style scoped>
.career-next { display:flex; flex-wrap:wrap; gap:12px; align-items:center; padding:12px; border-radius:12px; background:#eef5ff; color:#334155; font-size:14px; }
.career-next a { color:#2563eb; text-decoration:underline; }
</style>
