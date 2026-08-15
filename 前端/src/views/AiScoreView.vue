<template>
  <div class="innovative-score-page">
    <div class="mesh-background">
      <div class="color-blob blob-1"></div>
      <div class="color-blob blob-2"></div>
      <div class="color-blob blob-3"></div>
      <div class="grid-overlay"></div>
    </div>

    <div class="page-container">
      <div class="page-header">
        <div class="title-badge">
          <span class="pulse-dot"></span> AI 综合能力评估矩阵
        </div>
        <h1 class="gradient-text">多维神经元测算系统</h1>
      </div>

      <div class="holographic-layout">
        
        <div class="control-glass-panel">
          <div class="panel-deco-line"></div>
          <h3 class="panel-title">
            <span class="icon">🧠</span> 测算控制台
          </h3>
          
          <div class="input-group">
            <label>分析指令 / 测算重点</label>
            <div class="textarea-wrapper">
              <textarea 
                v-model="analyzeMessage" 
                placeholder="例如：重点分析该学生的编程能力和项目实践能力，偏向互联网后端开发方向..."
              ></textarea>
              <div class="focus-border"></div>
            </div>
          </div>

          <div class="input-group slider-group">
            <label>AI 严谨度 <span class="temp-val">{{ temperature }}</span></label>
            <input type="range" v-model.number="temperature" min="0.1" max="1.0" step="0.1" class="hologram-slider" />
            <div class="slider-labels">
              <span>精准客观</span>
              <span>发散创造</span>
            </div>
          </div>

          <button 
            class="start-btn" 
            @click="startAnalysis" 
            :disabled="isAnalyzing || !analyzeMessage.trim()"
            :class="{ 'is-loading': isAnalyzing }"
          >
            <span v-if="!isAnalyzing" class="btn-text">🚀 启动全息扫描</span>
            <span v-else class="btn-text">
              <div class="cyber-dots"><span></span><span></span><span></span></div>
              深度测算中...
            </span>
            <div class="btn-glow"></div>
          </button>

          <transition name="slide-up">
            <div v-if="scoreData" class="insight-box">
              <div class="insight-header">
                <span class="icon">💡</span> 
                <span>AI 测算综述</span>
              </div>
              <div class="insight-content">
                <p>{{ decryptedComment || scoreData.scoreComment }}</p>
              </div>
            </div>
          </transition>
        </div>

        <div class="radar-glass-panel">
          
          <div v-if="!scoreData && !isAnalyzing" class="empty-state">
            <div class="holo-ring"></div>
            <p>输入指令，唤醒能力雷达矩阵</p>
          </div>

          <div v-if="isAnalyzing" class="scanning-state">
            <div class="radar-scanner">
              <div class="sweep"></div>
              <div class="grid-circle"></div>
              <div class="grid-circle inner"></div>
            </div>
            <p class="scanning-text">正在通过千万级简历库进行多维比对</p>
          </div>

          <div v-show="scoreData && !isAnalyzing" class="chart-wrapper">
            <div class="score-hero">
              <svg viewBox="0 0 100 100" class="score-circle">
                <circle cx="50" cy="50" r="45" class="bg-circle"></circle>
                <circle cx="50" cy="50" r="45" class="progress-circle" :stroke-dasharray="`${(scoreData?.totalScore || 0) * 2.82}, 300`"></circle>
              </svg>
              <div class="score-info">
                <span class="score-label">综合战力</span>
                <span class="score-value">{{ scoreData?.totalScore || 0 }}</span>
              </div>
            </div>
            
            <div ref="radarChartRef" class="radar-chart"></div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, shallowRef, nextTick } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import API_CONFIG from '../config/api'

const baseURL = API_CONFIG.BASE_URL

const analyzeMessage = ref('重点分析该学生的编程能力和项目实践能力，偏向互联网后端开发方向')
const temperature = ref(0.1)
const isAnalyzing = ref(false)
const scoreData = ref(null)
const decryptedComment = ref('')

const radarChartRef = ref(null)
const chartInstance = shallowRef(null)

const getHeaders = () => {
  const token = localStorage.getItem('token') || ''
  return { 'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}

const startAnalysis = async () => {
  if (!analyzeMessage.value.trim()) return
  const userId = localStorage.getItem('userId') || 8 
  
  isAnalyzing.value = true
  scoreData.value = null
  decryptedComment.value = ''

  try {
    const payload = {
      userId: Number(userId),
      message: analyzeMessage.value.trim(),
      temperature: temperature.value
    }
    const res = await axios.post(`${baseURL}/api/ai/analysis/ability/score`, payload, { headers: getHeaders() })

    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      const data = Array.isArray(res.data.data) ? res.data.data[0] : res.data.data
      scoreData.value = data
      
      // ... 前面的代码 ...
      decryptedComment.value = "AI 已完成测算" + data.scoreComment.substring(0, 500)

      await nextTick()
      // 🌟 修复 ECharts 缩骨功：延迟 150 毫秒，等 CSS 和 Flexbox 把盒子完全撑开后再画图！
      setTimeout(() => {
        renderRadarChart()
        // 画完之后强行再触发一次重绘，确保万无一失
        chartInstance.value?.resize()
      }, 150)
      
    } else {
      alert('测算失败：' + (res.data.message || '未知错误'))
    }
  } catch (error) {
    alert('网络或服务异常，请检查后端服务。')
  } finally {
    isAnalyzing.value = false
  }
}

// 🌌 创新点 4：重新定制的高科技风格 ECharts 参数
const renderRadarChart = () => {
  if (!radarChartRef.value || !scoreData.value) return
  if (chartInstance.value) chartInstance.value.dispose()
  
  chartInstance.value = echarts.init(radarChartRef.value)
  const d = scoreData.value

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#E2E8F0',
      textStyle: { color: '#1E293B', fontWeight: 'bold' },
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
      radius: '68%',
      splitNumber: 5,
      axisName: {
        color: '#475569',
        fontSize: 13,
        fontWeight: 800,
        padding: [5, 10]
      },
      splitLine: {
        lineStyle: { color: ['rgba(74, 144, 226, 0.1)', 'rgba(74, 144, 226, 0.2)', 'rgba(74, 144, 226, 0.3)'].reverse() }
      },
      splitArea: { 
        show: true,
        areaStyle: { color: ['rgba(255,255,255,0.4)', 'rgba(240,249,255,0.4)'] }
      },
      axisLine: { lineStyle: { color: 'rgba(74, 144, 226, 0.3)' } }
    },
    series: [
      {
        name: '能力矩阵',
        type: 'radar',
        data: [
          {
            value: [
              d.educationScore, d.internshipScore, d.professionalScore, 
              d.problemSolvingScore, d.learningScore, d.innovationScore, 
              d.pressureScore, d.teamworkScore, d.communicationScore, d.certificateScore
            ],
            name: '当前评估值',
            symbol: 'circle',
            symbolSize: 8,
            itemStyle: {
              color: '#3B82F6',
              borderColor: '#FFFFFF',
              borderWidth: 2,
              shadowBlur: 10,
              shadowColor: '#3B82F6'
            },
            areaStyle: {
              color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                { offset: 0, color: 'rgba(59, 130, 246, 0.2)' },
                { offset: 1, color: 'rgba(59, 130, 246, 0.6)' }
              ])
            },
            lineStyle: { width: 3, color: '#3B82F6', shadowBlur: 10, shadowColor: 'rgba(59,130,246,0.5)' }
          }
        ]
      }
    ],
    animationEasing: 'elasticOut',
    animationDuration: 2000,
  }

  chartInstance.value.setOption(option)
}

onMounted(() => { window.addEventListener('resize', () => chartInstance.value?.resize()) })
onUnmounted(() => {
  window.removeEventListener('resize', () => chartInstance.value?.resize())
  if (chartInstance.value) chartInstance.value.dispose()
})
</script>

<style scoped>
/* ================= 🌌 流体全息拟态风 ================= */
.innovative-score-page { position: relative; width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background: #F8FAFC; overflow: hidden; font-family: 'Inter', -apple-system, sans-serif;}

/* 流体背景 */
.mesh-background { position: absolute; inset: 0; z-index: 0; overflow: hidden; }
.color-blob { position: absolute; filter: blur(120px); border-radius: 50%; animation: float 20s infinite ease-in-out alternate; opacity: 0.5; }
.blob-1 { width: 800px; height: 800px; background: #93C5FD; top: -200px; left: -100px; animation-delay: 0s; }
.blob-2 { width: 700px; height: 700px; background: #C4B5FD; bottom: -100px; right: -100px; animation-delay: -5s; }
.blob-3 { width: 600px; height: 600px; background: #A7F3D0; top: 40%; left: 40%; animation-delay: -10s; }
.grid-overlay { position: absolute; inset: 0; background-image: linear-gradient(rgba(255,255,255,0.4) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.4) 1px, transparent 1px); background-size: 30px 30px; opacity: 0.5; }
@keyframes float { 0% { transform: translate(0, 0) scale(1); } 100% { transform: translate(50px, 50px) scale(1.1); } }

/* 主容器 */
.page-container { position: relative; z-index: 1; width: 100%; max-width: 1300px; height: 90vh; display: flex; flex-direction: column; padding: 0 20px; }

/* 头部 */
.page-header { text-align: center; margin-bottom: 30px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.title-badge { display: inline-flex; align-items: center; gap: 8px; padding: 6px 18px; background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(10px); color: #3B82F6; border-radius: 30px; font-size: 0.9rem; font-weight: 700; border: 1px solid rgba(255, 255, 255, 0.8); box-shadow: 0 4px 15px rgba(0,0,0,0.03); }
.pulse-dot { width: 8px; height: 8px; background: #10B981; border-radius: 50%; box-shadow: 0 0 10px #10B981; animation: pulse 2s infinite; }
@keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); } 70% { box-shadow: 0 0 0 10px rgba(16, 185, 129, 0); } 100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); } }
.gradient-text { font-size: 2.4rem; margin: 0; font-weight: 900; letter-spacing: 1px; background: linear-gradient(135deg, #1E293B 0%, #3B82F6 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; filter: drop-shadow(0 4px 10px rgba(59, 130, 246, 0.2)); }

/* 悬浮错落布局 */
.holographic-layout { display: flex; gap: 30px; flex: 1; min-height: 0; }

/* 玻璃态面板基类 */
.control-glass-panel, .radar-glass-panel { background: rgba(255, 255, 255, 0.55); backdrop-filter: blur(25px); -webkit-backdrop-filter: blur(25px); border: 1px solid rgba(255, 255, 255, 0.8); box-shadow: 0 25px 50px rgba(0,0,0,0.05), inset 0 0 0 1px rgba(255,255,255,0.5); border-radius: 30px; padding: 35px; display: flex; flex-direction: column; position: relative; overflow: hidden; }

/* 左侧控制台 */
.control-glass-panel { width: 400px; flex-shrink: 0; z-index: 2; }
.panel-deco-line { position: absolute; top: 40px; left: 0; width: 4px; height: 40px; background: linear-gradient(to bottom, #3B82F6, #8B5CF6); border-radius: 0 4px 4px 0; }
.panel-title { margin: 0 0 30px 0; font-size: 1.3rem; font-weight: 800; color: #1E293B; display: flex; align-items: center; gap: 10px; }

.input-group { margin-bottom: 25px; display: flex; flex-direction: column; gap: 12px; }
.input-group label { font-size: 0.95rem; font-weight: 700; color: #475569; display: flex; justify-content: space-between; }
.temp-val { color: #3B82F6; font-family: monospace; font-size: 1.1rem; }

.textarea-wrapper { position: relative; background: rgba(255, 255, 255, 0.8); border-radius: 16px; transition: 0.3s; box-shadow: inset 0 2px 5px rgba(0,0,0,0.02); }
.textarea-wrapper textarea { width: 100%; border: none; background: transparent; outline: none; resize: none; font-size: 0.95rem; color: #334155; padding: 18px; line-height: 1.6; height: 130px; box-sizing: border-box; }
.focus-border { position: absolute; inset: 0; border: 2px solid transparent; border-radius: 16px; pointer-events: none; transition: 0.3s; }
.textarea-wrapper:focus-within .focus-border { border-color: #3B82F6; box-shadow: 0 0 15px rgba(59, 130, 246, 0.2); }

.hologram-slider { -webkit-appearance: none; width: 100%; height: 8px; background: rgba(59, 130, 246, 0.15); border-radius: 4px; outline: none; }
.hologram-slider::-webkit-slider-thumb { -webkit-appearance: none; appearance: none; width: 20px; height: 20px; border-radius: 50%; background: #3B82F6; cursor: pointer; box-shadow: 0 0 10px rgba(59, 130, 246, 0.5); border: 2px solid #FFF; }
.slider-labels { display: flex; justify-content: space-between; font-size: 0.8rem; color: #64748B; font-weight: 600; }

/* 炫酷按钮 */
.start-btn { position: relative; padding: 18px; background: #1E293B; color: white; border: none; border-radius: 16px; cursor: pointer; overflow: hidden; transition: 0.3s; box-shadow: 0 10px 20px rgba(30, 41, 59, 0.2); margin-top: 10px;}
.start-btn:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 15px 30px rgba(30, 41, 59, 0.3); }
.start-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.btn-text { position: relative; z-index: 1; font-size: 1.1rem; font-weight: 800; display: flex; align-items: center; justify-content: center; gap: 10px; }
.btn-glow { position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(59,130,246,0.4) 0%, transparent 70%); opacity: 0; transition: 0.3s; }
.start-btn:hover:not(:disabled) .btn-glow { opacity: 1; animation: rotateGlow 3s linear infinite; }
@keyframes rotateGlow { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

/* 洞察报告框 */
.insight-box { margin-top: 25px; padding: 20px; background: linear-gradient(135deg, rgba(255,255,255,0.9), rgba(248,250,252,0.9)); border: 1px solid rgba(255,255,255,1); border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.05); }
.insight-header { display: flex; align-items: center; gap: 8px; color: #3B82F6; font-weight: 800; margin-bottom: 12px; font-size: 1.05rem; }
.insight-content p { margin: 0; color: #475569; font-size: 0.95rem; line-height: 1.7; }

/* 右侧雷达面板 */
.radar-glass-panel { flex: 1; display: flex; justify-content: center; align-items: center; }

/* 空状态波纹 */
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 20px; color: #94A3B8; font-weight: 700; font-size: 1.1rem; }
.holo-ring { width: 120px; height: 120px; border: 2px dashed rgba(148, 163, 184, 0.4); border-radius: 50%; animation: spin 10s linear infinite; position: relative; }
.holo-ring::after { content: ''; position: absolute; inset: 10px; border: 2px solid rgba(148, 163, 184, 0.2); border-radius: 50%; animation: spin 5s linear infinite reverse; }

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
.score-label { font-size: 0.75rem; color: #64748B; font-weight: 800; text-transform: uppercase; margin-bottom: -5px;}
.score-value { font-size: 2.5rem; font-weight: 900; color: #1E293B; }

/* 动画类 */
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }
.cyber-dots span { display: inline-block; width: 6px; height: 6px; background: #3B82F6; border-radius: 50%; margin: 0 2px; animation: bounce 1.4s infinite ease-in-out; }
.cyber-dots span:nth-child(1) { animation-delay: -0.32s; }
.cyber-dots span:nth-child(2) { animation-delay: -0.16s; }
</style>