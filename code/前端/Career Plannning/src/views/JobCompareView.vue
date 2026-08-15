<template>
  <div class="compare-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="gradient-text">🤖 AI 岗位定位分析报告</h2>
        <p>基于全量职业图谱，测算新增岗位在“宇宙星图”中的坐标与能力模型</p>
      </div>
      <button class="back-btn" @click="$router.back()">← 返回列表</button>
    </div>

    <div class="compare-container" v-if="!isLoading">
      
      <div class="analysis-section glass-panel">
        <div class="section-title">
          <span class="icon">📝</span> AI 深度解读报告
        </div>
        <div class="analysis-content">
          <div class="text-block">
            <p v-html="formatAnalysis(reportData.analysis)"></p>
          </div>
          <div class="analysis-footer">
            <span>✨ 报告由底层大语言模型实时测算生成</span>
          </div>
        </div>
      </div>

      <div class="matching-section">
        
        <div class="radar-card glass-panel" v-if="profileDetail">
          <div class="section-title">
            <span class="icon">🎯</span> 最高契合岗位画像模型
          </div>
          
          <div class="top-match-info">
            <div class="match-score">
              <span class="score-num">{{ (reportData.matchedJobs[0].similarity * 100).toFixed(0) }}</span>
              <span class="score-unit">%</span>
              <div class="score-label">契合度</div>
            </div>
            <div class="match-detail">
              <h3 class="match-name">{{ reportData.matchedJobs[0].positionName }}</h3>
              <p class="match-desc" v-if="profileDetail.jobRequirementProfile">
                {{ profileDetail.jobRequirementProfile.description || '暂无描述' }}
              </p>
            </div>
          </div>

          <div ref="radarChartRef" class="radar-box"></div>

          <div class="hard-requirements" v-if="profileDetail.hardRequirement">
            <div class="req-tag" v-if="profileDetail.hardRequirement.educationRequirement">
              🎓 {{ profileDetail.hardRequirement.educationRequirement }}
            </div>
            <div class="req-tag" v-if="profileDetail.hardRequirement.experienceRequirement">
              💼 {{ profileDetail.hardRequirement.experienceRequirement }}
            </div>
            <div class="req-tag" v-if="profileDetail.hardRequirement.ageRequirement">
              ⏳ {{ profileDetail.hardRequirement.ageRequirement }}
            </div>
            <div class="req-tag" v-if="profileDetail.hardRequirement.certificateRequirement">
              📜 {{ profileDetail.hardRequirement.certificateRequirement }}
            </div>
          </div>

          <button class="go-graph-btn" @click="goToGraph(reportData.matchedJobs[0].profileId)">
            🌌 载入该岗位的 3D 演化星图 →
          </button>
        </div>

        <div class="similar-list glass-panel" v-if="reportData.matchedJobs && reportData.matchedJobs.length > 1">
          <div class="section-title">其它高关联岗位序列</div>
          <div class="job-items">
            <div v-for="(job, index) in reportData.matchedJobs.slice(1)" :key="index" class="job-item-card">
              <div class="job-main">
                <span class="job-rank">#{{ index + 2 }}</span>
                <span class="job-name">{{ job.positionName }}</span>
              </div>
              <div class="job-score">
                <div class="score-bar-bg">
                  <div class="score-bar" :style="{ width: job.similarity * 100 + '%' }"></div>
                </div>
                <span class="score-num">{{ (job.similarity * 100).toFixed(0) }}%</span>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <div v-else class="loading-screen">
      <div class="loader-core"></div>
      <p class="loading-text">AI 正在深度扫描全量图谱，重构岗位多维模型...</p>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, shallowRef, nextTick, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import * as echarts from 'echarts'
import API_CONFIG from '../config/api'

const router = useRouter()
const route = useRoute()

const isLoading = ref(true)
const reportData = ref({})
const profileDetail = ref(null) // 存储“全家桶大接口”返回的数据

const chartRef = shallowRef(null)
const radarChartRef = ref(null)

const baseURL = API_CONFIG.BASE_URL
const getHeaders = () => {
  const token = localStorage.getItem('token') || ''
  return { 'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}

// 🌟 文本解析器 (保持你优秀的逻辑)
const formatAnalysis = (text) => {
  if (!text) return ''
  let parsed = text
  parsed = parsed.replace(/['‘](.*?)['’]/g, '<span class="keyword-tag">$1</span>')
  parsed = parsed.replace(/ID:(\d+)/g, '<span class="id-tag">ID:$1</span>')
  parsed = parsed.replace(/(①|②|③|④|⑤|\(1\)|\(2\)|\(3\)|\(4\)|\(5\))/g, '<br><span class="list-badge">$1</span>')
  parsed = parsed.replace(/(综上.*?|综合判断.*?|建议.*?)[：:,，]/g, '<br><br><span class="summary-badge">💡 $1</span>')
  parsed = parsed.replace(/；/g, '；<br><br>')
  return parsed
}

// ================= 核心数据流 =================
const fetchDataSequence = async () => {
  const jobId = route.query.jobId || '232728665803640000' // 你测试用的 job_info ID
  
  try {
    isLoading.value = true
    
    // 1. 调用 AI 分析接口 (拿到文本和匹配列表)
    const aiRes = await axios.post(`${baseURL}/api/job-compare/analyze-new-job`, 
      { newJobId: jobId },
      { headers: getHeaders() }
    )

    if (aiRes.data.code === 200) {
      reportData.value = aiRes.data.data
      
      // 2. 如果有匹配到的岗位，立刻拿排名第一的去查“全家桶详情”！
      if (reportData.value.matchedJobs && reportData.value.matchedJobs.length > 0) {
        const topProfileId = reportData.value.matchedJobs[0].profileId
        await fetchProfileDetail(topProfileId)
      } else {
        isLoading.value = false // 没有匹配项直接结束
      }
    }
  } catch (error) {
    console.error('获取报告序列失败:', error)
    isLoading.value = false
  }
}

// ================= 调用新全家桶接口 =================
const fetchProfileDetail = async (profileId) => {
  try {
    const detailRes = await axios.get(`${baseURL}/api/job-detail/by-profile/${profileId}`, { headers: getHeaders() })
    if (detailRes.data.code === 200) {
      profileDetail.value = detailRes.data.data
      isLoading.value = false
      // 数据准备完毕，DOM 更新后渲染雷达图
      await nextTick()
      initRadarChart()
    }
  } catch (error) {
    console.error('获取画像详情全家桶失败:', error)
    isLoading.value = false
  }
}

// ================= 渲染六边形雷达图 =================
const initRadarChart = () => {
  if (!radarChartRef.value) return
  if (chartRef.value) chartRef.value.dispose()
  chartRef.value = echarts.init(radarChartRef.value)

  // 从全家桶里安全提取权重数据
  const profile = profileDetail.value?.jobRequirementProfile || {}
  const hw = profile.hardWeight || 30
  const skw = profile.skillWeight || 40
  const sow = profile.softWeight || 20
  
  // 从门槛和其他数据里衍生 3 个维度，凑成六边形
  const hasEdu = profileDetail.value?.hardRequirement?.educationRequirement ? 85 : 50
  const hasExp = profileDetail.value?.hardRequirement?.experienceRequirement ? 90 : 60
  // 潜力值通过晋升和换岗的路线数量来决定
  const transferCount = (profileDetail.value?.transferGraphs?.length || 0) + (profileDetail.value?.promotionGraphs?.length || 0)
  const potential = transferCount > 0 ? Math.min(100, 60 + transferCount * 10) : 60

  const option = {
    radar: {
      indicator: [
        { name: '硬性门槛 (Hard)', max: 100 },
        { name: '专业技能 (Skill)', max: 100 },
        { name: '软性素质 (Soft)', max: 100 },
        { name: '学历要求 (Edu)', max: 100 },
        { name: '经验沉淀 (Exp)', max: 100 },
        { name: '跨界潜力 (Poten)', max: 100 }
      ],
      shape: 'polygon',
      radius: '65%',
      splitNumber: 4,
      axisName: { color: '#64748B', fontSize: 11, fontWeight: 'bold' },
      splitArea: { areaStyle: { color: ['rgba(74,144,226,0.02)', 'rgba(74,144,226,0.05)', 'rgba(74,144,226,0.08)', 'rgba(74,144,226,0.11)'] } },
      axisLine: { lineStyle: { color: 'rgba(74,144,226,0.2)' } },
      splitLine: { lineStyle: { color: 'rgba(74,144,226,0.2)' } }
    },
    series: [{
      type: 'radar',
      data: [{
        // 将后端的权重百分比映射到100分制雷达图中 (为了图表饱满，稍微放大比例)
        value: [hw * 2, skw * 1.8, sow * 2.5, hasEdu, hasExp, potential],
        name: '岗位能力模型',
        itemStyle: { color: '#4A90E2' },
        areaStyle: { color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [{ offset: 0, color: 'rgba(74, 144, 226, 0.1)' }, { offset: 1, color: 'rgba(74, 144, 226, 0.5)' }]) },
        lineStyle: { width: 2, color: '#4A90E2' },
        symbolSize: 6
      }]
    }]
  }

  chartRef.value.setOption(option)
}

const goToGraph = (profileId) => {
  router.push({ path: '/graph', query: { id: profileId } })
}

const handleResize = () => { if (chartRef.value) chartRef.value.resize() }

onMounted(() => {
  fetchDataSequence()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartRef.value) chartRef.value.dispose()
})
</script>

<style scoped>
/* 🌟 全局页面底色 (继承你喜欢的清爽风格) */
.compare-page { width: 100%; height: 100vh; padding: 30px 50px; box-sizing: border-box; background: radial-gradient(circle at top left, #F8FAFC 0%, #E2E8F0 100%); overflow-y: auto; font-family: 'Inter', sans-serif; }

/* 头部 */
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.gradient-text { font-size: 2rem; margin: 0 0 8px 0; font-weight: 900; background: linear-gradient(135deg, #0F172A 0%, #3B82F6 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.header-left p { color: #64748B; margin: 0; font-size: 0.95rem; font-weight: 500; letter-spacing: 1px;}
.back-btn { padding: 10px 24px; border-radius: 12px; border: 1px solid #CBD5E1; background: rgba(255,255,255,0.8); cursor: pointer; font-weight: 800; color: #475569; transition: 0.2s; backdrop-filter: blur(10px); }
.back-btn:hover { background: #FFFFFF; color: #0F172A; border-color: #94A3B8; box-shadow: 0 4px 12px rgba(0,0,0,0.05);}

/* 容器布局：左右分栏 */
.compare-container { display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 30px; align-items: start; max-width: 1600px; margin: 0 auto;}

/* 极致通透玻璃卡片 */
.glass-panel { background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(25px); border: 1px solid rgba(255,255,255,0.9); border-radius: 24px; padding: 30px; box-shadow: 0 20px 40px rgba(0,0,0,0.04), inset 0 0 0 1px rgba(255,255,255,0.5); }
.section-title { font-size: 1.15rem; font-weight: 900; color: #1E293B; margin-bottom: 25px; display: flex; align-items: center; gap: 10px; }

/* 左侧：报告区域 */
.analysis-content { line-height: 1.9; color: #334155; font-size: 1.05rem; }
.text-block { background: rgba(255,255,255,0.7); padding: 30px; border-radius: 16px; border: 1px solid #F1F5F9; white-space: pre-wrap; box-shadow: inset 0 2px 10px rgba(0,0,0,0.02);}
.analysis-footer { margin-top: 20px; font-size: 0.85rem; color: #94A3B8; text-align: right; font-weight: 600;}

/* 🌟 右侧：雷达大屏 */
.matching-section { display: flex; flex-direction: column; gap: 30px; }
.radar-card { display: flex; flex-direction: column; position: relative; overflow: hidden; }

/* 头部岗位信息 */
.top-match-info { display: flex; align-items: center; gap: 20px; padding-bottom: 20px; border-bottom: 1px dashed #CBD5E1; }
.match-score { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 80px; height: 80px; border-radius: 20px; background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%); color: #1D4ED8; border: 1px solid #BFDBFE; box-shadow: 0 10px 20px rgba(37, 99, 235, 0.15);}
.score-num { font-size: 2.2rem; font-weight: 900; line-height: 1; }
.score-unit { font-size: 1rem; font-weight: 800; margin-top: -4px;}
.score-label { font-size: 0.75rem; font-weight: 700; opacity: 0.8; margin-top: 2px;}

.match-detail { flex: 1; }
.match-name { font-size: 1.5rem; font-weight: 900; color: #0F172A; margin: 0 0 8px 0; }
.match-desc { font-size: 0.9rem; color: #64748B; margin: 0; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;}

/* 雷达图容器 */
.radar-box { width: 100%; height: 280px; margin: 10px 0; }

/* 🌟 硬门槛玻璃态胶囊标签 */
.hard-requirements { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 25px; padding: 15px; background: rgba(248, 250, 252, 0.5); border-radius: 12px; border: 1px solid #E2E8F0;}
.req-tag { padding: 6px 14px; background: white; border: 1px solid #CBD5E1; border-radius: 8px; font-size: 0.85rem; font-weight: 700; color: #334155; box-shadow: 0 2px 6px rgba(0,0,0,0.02);}

/* 进入星图按钮 */
.go-graph-btn { width: 100%; background: linear-gradient(135deg, #1E3A8A 0%, #3B82F6 100%); color: white; border: none; padding: 16px; border-radius: 14px; font-size: 1.05rem; font-weight: 800; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 10px 20px rgba(37, 99, 235, 0.2); }
.go-graph-btn:hover { transform: translateY(-4px); box-shadow: 0 15px 30px rgba(37, 99, 235, 0.3); background: #1D4ED8;}

/* 底部备选岗位列表 */
.job-items { display: flex; flex-direction: column; gap: 12px; }
.job-item-card { background: rgba(255,255,255,0.8); padding: 16px; border-radius: 12px; border: 1px solid #E2E8F0; display: flex; align-items: center; justify-content: space-between; transition: 0.2s;}
.job-item-card:hover { transform: translateX(5px); border-color: #93C5FD; box-shadow: 0 4px 12px rgba(0,0,0,0.05);}
.job-main { display: flex; align-items: center; gap: 12px; }
.job-rank { font-size: 0.85rem; color: #94A3B8; font-weight: 900; background: #F1F5F9; padding: 4px 8px; border-radius: 6px;}
.job-name { font-size: 1rem; font-weight: 800; color: #1E293B; }
.job-score { display: flex; align-items: center; gap: 12px; width: 120px;}
.score-bar-bg { flex: 1; height: 8px; background: #E2E8F0; border-radius: 4px; overflow: hidden; }
.score-bar { height: 100%; background: linear-gradient(90deg, #93C5FD 0%, #3B82F6 100%); border-radius: 4px; }
.score-num { font-size: 0.9rem; font-weight: 800; color: #3B82F6; width: 40px; text-align: right;}

/* 加载动画 */
.loading-screen { height: 70vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 20px; }
.loader-core { width: 60px; height: 60px; border-radius: 50%; border: 4px solid #E2E8F0; border-top-color: #3B82F6; animation: spin 1s cubic-bezier(0.6, 0.2, 0.4, 0.8) infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { color: #1E293B; font-weight: 800; font-size: 1.2rem; letter-spacing: 1px;}

/* 文本高亮细节 (保持之前的高级质感) */
:deep(.keyword-tag) { color: #3B82F6; font-weight: 700; background-image: linear-gradient(transparent 60%, rgba(59, 130, 246, 0.15) 60%); padding: 0 2px; margin: 0 2px; }
:deep(.id-tag) { color: #8B5CF6; font-weight: bold; background: #F5F3FF; padding: 2px 8px; border-radius: 6px; font-family: monospace; border: 1px solid #DDD6FE; }
:deep(.list-badge) { display: inline-block; color: white; background: #3B82F6; border-radius: 50%; width: 22px; height: 22px; line-height: 22px; text-align: center; font-size: 0.85rem; font-weight: bold; margin-right: 8px; margin-top: 10px; }
:deep(.summary-badge) { display: block; font-size: 1.1rem; font-weight: 800; color: #0F172A; background: linear-gradient(90deg, #FEF08A 0%, transparent 100%); padding: 6px 12px; border-left: 4px solid #F59E0B; border-radius: 4px; margin-bottom: 8px; }
</style>
