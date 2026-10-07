<template>
  <div class="radar-glass-panel">
    <div class="radar-panel-label">
      <span>{{ label }}</span>
      <small>{{ hint }}</small>
    </div>

    <div v-if="loading" class="scanning-state">
      <div class="radar-scanner">
        <div class="sweep"></div>
        <div class="grid-circle"></div>
        <div class="grid-circle inner"></div>
      </div>
      <p class="scanning-text">正在读取能力画像，请稍候</p>
    </div>

    <div v-else-if="!hasScores" class="empty-state">
      <div class="holo-ring"></div>
      <p>完成一次测评后，这里会点亮你的能力雷达</p>
    </div>

    <div v-show="hasScores && !loading" class="chart-wrapper">
      <div class="score-hero">
        <svg viewBox="0 0 100 100" class="score-circle">
          <circle cx="50" cy="50" r="45" class="bg-circle"></circle>
          <circle cx="50" cy="50" r="45" class="progress-circle"
                  :stroke-dasharray="`${(currentTotal || 0) * 2.82}, 300`"></circle>
        </svg>
        <div class="score-info">
          <span class="score-label">综合战力</span>
          <span class="score-value">{{ currentTotal || 0 }}</span>
        </div>
      </div>

      <div ref="radarChartRef" class="radar-chart"></div>
    </div>
  </div>
</template>

<script setup>
/**
 * 10 维能力雷达（沿用原「能力补充测评」页的样式与动画，未做视觉改动）。
 *
 * 数据：
 * - 传 scores 时直接使用（键为 10 维 snake_case：education / internship / professional / certificate /
 *   innovation / learning / pressure / communication / problem_solving / teamwork）；
 * - 不传时自动读取当前画像（/api/ability/score/user/{userId}，实体字段为 camelCase，内部自动映射）。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import API_CONFIG from '../config/api'

const props = defineProps({
  scores: { type: Object, default: null },
  total: { type: [Number, String], default: null },
  label: { type: String, default: '图 02 · 能力雷达' },
  hint: { type: String, default: '基于个人能力画像' },
  autoLoad: { type: Boolean, default: true }
})

const RADAR_FIELDS = [
  ['education', 'educationScore'],
  ['internship', 'internshipScore'],
  ['professional', 'professionalScore'],
  ['problem_solving', 'problemSolvingScore'],
  ['learning', 'learningScore'],
  ['innovation', 'innovationScore'],
  ['pressure', 'pressureScore'],
  ['teamwork', 'teamworkScore'],
  ['communication', 'communicationScore'],
  ['certificate', 'certificateScore']
]

const radarChartRef = ref(null)
const loading = ref(props.autoLoad && !props.scores)
const fetched = ref(null)
let chartInstance = null

const source = computed(() => props.scores || fetched.value?.scores || null)
const hasScores = computed(() => !!source.value && RADAR_FIELDS.every(([key]) => Number.isFinite(Number(source.value[key]))))
const currentTotal = computed(() => {
  if (props.total !== null && props.total !== '' && props.total !== undefined) return Number(props.total)
  if (fetched.value?.total != null) return fetched.value.total
  if (!hasScores.value) return null
  // 未提供总分时按后端口径估算：硬实力四项×30% + 软实力六维×70%
  const value = key => Number(source.value[key] || 0)
  const hard = ['education', 'internship', 'professional', 'certificate'].reduce((sum, key) => sum + value(key), 0) / 4
  const soft = ['innovation', 'learning', 'pressure', 'communication', 'problem_solving', 'teamwork'].reduce((sum, key) => sum + value(key), 0) / 6
  return Math.round((hard * 0.3 + soft * 0.7) * 10) / 10
})

const authHeaders = () => {
  const token = localStorage.getItem('token') || ''
  return { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}

async function loadScores() {
  const userId = localStorage.getItem('userId') || ''
  if (!/^\d+$/.test(userId)) { loading.value = false; return }
  try {
    const res = await axios.get(`/api/ability/score/user/${userId}`, {
      baseURL: API_CONFIG.BASE_URL, headers: authHeaders()
    })
    const list = Array.isArray(res.data?.data) ? res.data.data : []
    const latest = [...list].sort((a, b) =>
      String(b.updateTime || b.createTime || '').localeCompare(String(a.updateTime || a.createTime || '')))[0]
    if (latest) {
      const scores = {}
      RADAR_FIELDS.forEach(([key, field]) => { scores[key] = Number(latest[field] ?? 0) })
      fetched.value = { scores, total: Number(latest.totalScore ?? 0) }
    }
  } catch (e) {
    // 读取失败时保持空状态，不打扰答题流程
  } finally {
    loading.value = false
  }
}

function renderRadarChart() {
  if (!radarChartRef.value || !hasScores.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(radarChartRef.value)
  const d = source.value

  const compact = window.innerWidth <= 720
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 252, 244, 0.97)',
      borderColor: '#d4bd91',
      textStyle: { color: '#173a55', fontWeight: 'bold' },
      padding: [15, 20],
      borderRadius: 12,
      boxShadow: '0 10px 30px rgba(0,0,0,0.1)'
    },
    radar: {
      indicator: [
        { name: '教育背景', max: 100 },
        { name: '实习经验', max: 100 },
        { name: '专业技能', max: 100 },
        { name: '解决问题', max: 100 },
        { name: '学习能力', max: 100 },
        { name: '创新能力', max: 100 },
        { name: '抗压能力', max: 100 },
        { name: '团队协作', max: 100 },
        { name: '沟通表达', max: 100 },
        { name: '证书资质', max: 100 }
      ],
      shape: 'polygon',
      radius: compact ? '52%' : '68%',
      splitNumber: 5,
      axisName: {
        color: '#3d5a70',
        fontSize: compact ? 10 : 13,
        fontWeight: 800,
        padding: compact ? [2, 2] : [5, 10]
      },
      splitLine: {
        lineStyle: { color: ['rgba(46, 84, 109, 0.12)', 'rgba(46, 84, 109, 0.2)', 'rgba(46, 84, 109, 0.34)'].reverse() }
      },
      splitArea: {
        show: true,
        areaStyle: { color: ['rgba(255,252,244,0.56)', 'rgba(233,226,211,0.36)'] }
      },
      axisLine: { lineStyle: { color: 'rgba(46, 84, 109, 0.34)' } }
    },
    series: [
      {
        name: '能力矩阵',
        type: 'radar',
        data: [
          {
            value: RADAR_FIELDS.map(([key]) => d[key]),
            name: '当前评估值',
            symbol: 'circle',
            symbolSize: 8,
            itemStyle: {
              color: '#bd945a',
              borderColor: '#FFFFFF',
              borderWidth: 2,
              shadowBlur: 10,
              shadowColor: '#d7b67e'
            },
            areaStyle: {
              color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                { offset: 0, color: 'rgba(206, 165, 98, 0.16)' },
                { offset: 1, color: 'rgba(22, 66, 95, 0.52)' }
              ])
            },
            lineStyle: { width: 3, color: '#b78d51', shadowBlur: 10, shadowColor: 'rgba(183,141,81,0.48)' }
          }
        ]
      }
    ],
    animationEasing: 'elasticOut',
    animationDuration: 2000
  }
  chartInstance.setOption(option)
}

const resizeChart = () => {
  if (!chartInstance) return
  const compact = window.innerWidth <= 720
  chartInstance.setOption({
    radar: {
      radius: compact ? '52%' : '68%',
      axisName: { fontSize: compact ? 10 : 13, padding: compact ? [2, 2] : [5, 10] }
    }
  })
  chartInstance.resize()
}

const redraw = async () => {
  await nextTick()
  if (hasScores.value && radarChartRef.value) renderRadarChart()
}

onMounted(async () => {
  window.addEventListener('resize', resizeChart)
  if (loading.value) await loadScores()
  await redraw()
})

watch(() => [props.scores, fetched.value], redraw, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) chartInstance.dispose()
  chartInstance = null
})
</script>

<style scoped>
/* ===== 以下样式取自原「能力补充测评」页，未做改动 ===== */
.radar-glass-panel {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  border-radius: 30px;
  padding: 35px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  justify-content: center;
  align-items: center;
  min-height: 420px;
}
.radar-panel-label {
  position: absolute;
  top: 22px;
  left: 28px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  z-index: 2;
}
.radar-panel-label span { font-weight: 800; color: #173a55; font-size: 1rem; letter-spacing: 0.5px; }
.radar-panel-label small { font-size: 0.75rem; color: #8b9298; }

/* 空状态波纹 */
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 20px; color: #94A3B8; font-weight: 700; font-size: 1.1rem; text-align: center; }
.holo-ring { width: 120px; height: 120px; border: 2px dashed rgba(148, 163, 184, 0.4); border-radius: 50%; animation: spin 10s linear infinite; position: relative; }
.holo-ring::after { content: ''; position: absolute; inset: 10px; border: 2px solid rgba(148, 163, 184, 0.2); border-radius: 50%; animation: spin 5s linear infinite reverse; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 炫酷神盾局扫描动画 */
.scanning-state { display: flex; flex-direction: column; align-items: center; gap: 30px; }
.radar-scanner { width: 150px; height: 150px; position: relative; border-radius: 50%; background: rgba(59, 130, 246, 0.05); border: 1px solid rgba(59, 130, 246, 0.2); box-shadow: 0 0 30px rgba(59, 130, 246, 0.1); overflow: hidden; }
.grid-circle { position: absolute; inset: 0; border: 1px solid rgba(59, 130, 246, 0.3); border-radius: 50%; }
.grid-circle.inner { inset: 30px; }
.sweep { position: absolute; top: 0; left: 50%; width: 50%; height: 50%; background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.8)); transform-origin: bottom left; animation: radarSweep 2s linear infinite; }
@keyframes radarSweep { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
.scanning-text { color: #3B82F6; font-weight: 800; font-size: 1.2rem; letter-spacing: 2px; text-shadow: 0 0 10px rgba(59, 130, 246, 0.3); }

/* 图表与总分悬浮 */
.chart-wrapper { width: 100%; height: 100%; position: relative; display: flex; align-items: center; justify-content: center; }
.radar-chart { width: 100%; height: 100%; min-height: 550px; }

/* 创新：环形总分展示 */
.score-hero { position: absolute; top: 20px; right: 20px; width: 120px; height: 120px; z-index: 10; display: flex; justify-content: center; align-items: center; background: rgba(255, 255, 255, 0.8); border-radius: 50%; box-shadow: 0 15px 35px rgba(0,0,0,0.08), inset 0 0 0 1px #FFF; backdrop-filter: blur(10px); }
.score-circle { position: absolute; width: 100%; height: 100%; transform: rotate(-90deg); }
.bg-circle { fill: none; stroke: rgba(226, 232, 240, 0.5); stroke-width: 6; }
.progress-circle { fill: none; stroke: #3B82F6; stroke-width: 6; stroke-linecap: round; transition: stroke-dasharray 1.5s ease-out; }
.score-info { text-align: center; display: flex; flex-direction: column; }
.score-label { font-size: 0.75rem; color: #64748B; font-weight: 800; text-transform: uppercase; margin-bottom: -5px; }
.score-value { font-size: 2.5rem; font-weight: 900; color: #1E293B; }

@media (max-width: 720px) {
  .radar-glass-panel { padding: 22px 16px; border-radius: 22px; min-height: 360px; }
  .radar-chart { min-height: 420px; }
  .score-hero { width: 92px; height: 92px; top: 12px; right: 12px; }
  .score-value { font-size: 1.9rem; }
}
</style>
