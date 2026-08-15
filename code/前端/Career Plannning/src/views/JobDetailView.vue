<template>
  <div class="job-detail-page">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">← 返回大厅</button>
      <h2 class="title">岗位多维能力解析模型</h2>
    </div>

    <div class="detail-container" v-if="!isLoading && jobData">
      
      <div class="info-card glass-panel">
        <div class="job-title-wrapper">
          <h1 class="job-name">{{ jobData.jobRequirementProfile?.positionName || '未知岗位' }}</h1>
          <span class="industry-tag">{{ jobData.jobRequirementProfile?.industry || '行业未定' }}</span>
        </div>
        <p class="job-desc">{{ jobData.jobRequirementProfile?.description || '暂无详细描述信息' }}</p>
        
        <div class="quick-tags" v-if="jobData.hardRequirement">
          <span class="q-tag">🎓 {{ jobData.hardRequirement.educationRequirement || '学历不限' }}</span>
          <span class="q-tag">💼 {{ jobData.hardRequirement.experienceRequirement || '经验不限' }}</span>
        </div>
      </div>

      <div class="content-grid">
        
        <div class="radar-section glass-panel">
          <div class="section-title">🎯 核心能力模型雷达</div>
          <div ref="radarChartRef" class="radar-box"></div>
        </div>

        <div class="requirement-section">
          
          <div class="req-card glass-panel" v-if="jobData.skillRequirement">
            <h3 class="req-title">💻 核心专业技能</h3>
            <div class="content-text">{{ jobData.skillRequirement.skillRequirement }}</div>
          </div>

          <div class="req-card glass-panel" v-if="jobData.softRequirement">
            <h3 class="req-title">🤝 软性素质要求</h3>
            <div class="content-text">{{ jobData.softRequirement.communicationSkill }}</div>
          </div>

          <div class="req-card glass-panel action-card">
            <h3 class="req-title">🌌 职业演化空间</h3>
            <p class="hint-text">该岗位共有 <strong>{{ (jobData.promotionGraphs?.length || 0) + (jobData.transferGraphs?.length || 0) }}</strong> 条演化路径</p>
            <button class="go-graph-btn" @click="goToGraph">进入 3D 演化星图 →</button>
          </div>

        </div>
      </div>

    </div>

    <div v-else class="loading-screen">
      <div class="loader-core"></div>
      <p class="loading-text">正在调取岗位底层图谱数据...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import * as echarts from 'echarts'
import API_CONFIG from '../config/api'

const route = useRoute()
const router = useRouter()

const isLoading = ref(true)
const jobData = ref(null)
const radarChartRef = ref(null)
const chartInstance = shallowRef(null)

const baseURL = API_CONFIG.BASE_URL
const getHeaders = () => {
  const token = localStorage.getItem('token') || ''
  return { 'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}

const fetchJobDetail = async () => {
  // 从 URL 获取 jobInfoId，如果没有就用你给的测试 ID
  const jobInfoId = route.query.id || '232728665078419008' 
  
  try {
    isLoading.value = true
    // 🌟 直接调用你后端朋友写的 by-job-info 接口！一步到位！
    const res = await axios.get(`${baseURL}/api/job-detail/by-job-info/${jobInfoId}`, { headers: getHeaders() })
    
    if (res.data.code === 200) {
      jobData.value = res.data.data
      isLoading.value = false
      await nextTick()
      initRadarChart()
    }
  } catch (error) {
    console.error('获取岗位详情失败:', error)
    isLoading.value = false
  }
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  if (chartInstance.value) chartInstance.value.dispose()
  chartInstance.value = echarts.init(radarChartRef.value)

  const profile = jobData.value.jobRequirementProfile || {}
  const hw = profile.hardWeight || 30
  const skw = profile.skillWeight || 40
  const sow = profile.softWeight || 20
  
  const hasEdu = jobData.value.hardRequirement?.educationRequirement ? 85 : 50
  const hasExp = jobData.value.hardRequirement?.experienceRequirement ? 90 : 60
  const transferCount = (jobData.value.transferGraphs?.length || 0) + (jobData.value.promotionGraphs?.length || 0)
  const potential = transferCount > 0 ? Math.min(100, 60 + transferCount * 10) : 60

  const option = {
    radar: {
      indicator: [
        { name: '硬性门槛', max: 100 },
        { name: '专业技能', max: 100 },
        { name: '软性素质', max: 100 },
        { name: '学历要求', max: 100 },
        { name: '经验沉淀', max: 100 },
        { name: '跨界潜力', max: 100 }
      ],
      shape: 'polygon',
      radius: '65%',
      axisName: { color: '#64748B', fontSize: 13, fontWeight: 'bold' },
      splitArea: { areaStyle: { color: ['rgba(74,144,226,0.02)', 'rgba(74,144,226,0.05)', 'rgba(74,144,226,0.08)', 'rgba(74,144,226,0.11)'] } },
      axisLine: { lineStyle: { color: 'rgba(74,144,226,0.2)' } },
      splitLine: { lineStyle: { color: 'rgba(74,144,226,0.2)' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: [hw * 2, skw * 1.8, sow * 2.5, hasEdu, hasExp, potential],
        name: '岗位能力模型',
        itemStyle: { color: '#4A90E2' },
        areaStyle: { color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [{ offset: 0, color: 'rgba(74, 144, 226, 0.1)' }, { offset: 1, color: 'rgba(74, 144, 226, 0.5)' }]) },
        lineStyle: { width: 2, color: '#4A90E2' },
        symbolSize: 6
      }]
    }]
  }

  chartInstance.value.setOption(option)
}

const goToGraph = () => {
  const profileId = jobData.value?.jobRequirementProfile?.id
  if (profileId) {
    router.push({ path: '/graph', query: { id: profileId } })
  }
}

const handleResize = () => { if (chartInstance.value) chartInstance.value.resize() }

onMounted(() => {
  fetchJobDetail()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance.value) chartInstance.value.dispose()
})
</script>

<style scoped>
.job-detail-page { width: 100%; min-height: 100vh; padding: 40px; box-sizing: border-box; background: #F8FAFC; font-family: 'Inter', sans-serif; overflow-y: auto;}

.page-header { display: flex; align-items: center; gap: 30px; margin-bottom: 30px; }
.back-btn { padding: 10px 20px; border-radius: 12px; border: 1px solid #CBD5E1; background: white; cursor: pointer; font-weight: bold; color: #475569; transition: 0.2s; }
.back-btn:hover { background: #F1F5F9; color: #0F172A; }
.title { margin: 0; font-size: 1.5rem; color: #1E293B; font-weight: 900; }

.detail-container { max-width: 1400px; margin: 0 auto; display: flex; flex-direction: column; gap: 25px; }

.glass-panel { background: white; border: 1px solid #E2E8F0; border-radius: 20px; padding: 30px; box-shadow: 0 10px 30px rgba(0,0,0,0.02); }

.info-card { display: flex; flex-direction: column; gap: 15px; }
.job-title-wrapper { display: flex; align-items: center; gap: 15px; }
.job-name { margin: 0; font-size: 2.2rem; color: #0F172A; font-weight: 900; }
.industry-tag { background: #EFF6FF; color: #2563EB; padding: 6px 12px; border-radius: 8px; font-weight: bold; font-size: 0.9rem; border: 1px solid #BFDBFE;}
.job-desc { color: #64748B; font-size: 1.1rem; line-height: 1.6; margin: 0; max-width: 900px;}
.quick-tags { display: flex; gap: 10px; margin-top: 10px; }
.q-tag { background: #F8FAFC; padding: 8px 16px; border-radius: 10px; font-weight: bold; color: #475569; border: 1px solid #E2E8F0;}

.content-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 25px; }

.radar-section { display: flex; flex-direction: column; }
.section-title { font-size: 1.2rem; font-weight: 800; color: #1E293B; margin-bottom: 20px; }
.radar-box { width: 100%; height: 400px; }

.requirement-section { display: flex; flex-direction: column; gap: 20px; }
.req-card { padding: 25px; }
.req-title { margin: 0 0 15px 0; font-size: 1.1rem; font-weight: 800; color: #0F172A; }
.content-text { color: #475569; line-height: 1.8; font-size: 0.95rem; white-space: pre-wrap; background: #F8FAFC; padding: 15px; border-radius: 10px;}

.action-card { display: flex; flex-direction: column; align-items: flex-start; background: linear-gradient(135deg, #EFF6FF 0%, #FFFFFF 100%); border-color: #BFDBFE;}
.hint-text { color: #3B82F6; font-size: 1.05rem; margin-bottom: 20px; }
.hint-text strong { font-size: 1.5rem; font-weight: 900; }
.go-graph-btn { background: #3B82F6; color: white; border: none; padding: 16px 30px; border-radius: 12px; font-size: 1.1rem; font-weight: 800; cursor: pointer; transition: 0.3s; box-shadow: 0 10px 20px rgba(59, 130, 246, 0.2); }
.go-graph-btn:hover { background: #2563EB; transform: translateY(-3px); box-shadow: 0 15px 30px rgba(59, 130, 246, 0.3); }

.loading-screen { height: 60vh; display: flex; flex-direction: column; justify-content: center; align-items: center; gap: 20px;}
.loader-core { width: 50px; height: 50px; border-radius: 50%; border: 4px solid #E2E8F0; border-top-color: #3B82F6; animation: spin 1s infinite linear;}
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { color: #1E293B; font-weight: bold; }
</style>
