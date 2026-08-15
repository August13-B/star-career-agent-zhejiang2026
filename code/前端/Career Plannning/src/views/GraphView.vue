<template>
  <div class="graph-page dark-universe">
    
    <div class="starfield">
      <div 
        v-for="i in 150" 
        :key="i" 
        class="star" 
        :style="getStarStyle()"
      ></div>
    </div>

    <div class="nebula-glow glow-1"></div>
    <div class="nebula-glow glow-2"></div>

    <div class="page-header">
      <h2 class="neon-text">🌌 全息宇宙演化星图</h2>
      <p class="subtitle">AI 神经元引擎 · 维度跃迁链路分析</p>
    </div>

    <div class="graph-workspace">
      <div class="chart-container dark-glass">
        
        <div v-if="isLoading" class="overlay-state">
          <div class="quantum-loader"></div>
          <p class="loading-text">正在扫描深空拓扑数据...</p>
        </div>
        
        <div v-else-if="isDataEmpty" class="overlay-state empty-state">
          <span class="empty-icon">🛰️</span>
          <p>当前星系坐标未建立，空间折叠尚未展开</p>
          <button @click="handleTriggerGeneration" class="cyber-btn" :disabled="isGenerating">
            {{ isGenerating ? '⚡ 星轨引擎充能中...' : '🚀 唤醒 AI 重构星图' }}
          </button>
        </div>

        <div v-show="!isLoading && !isDataEmpty" ref="chartRef" class="echarts-box"></div>
      </div>

      <transition name="panel-fade">
        <div v-if="activeNode && activeNode.category && activeNode.category !== '当前岗位' && activeNode.category !== '星尘'" class="detail-panel dark-glass">
          <div class="panel-deco-line"></div>
          <div class="panel-header">
            <h3>{{ activeNode.name }}</h3>
            <span class="node-tag" :class="activeNode.category === '晋升路线' ? 'tag-up' : 'tag-transfer'">
              {{ activeNode.category === '晋升路线' ? '🔺 维度跃升' : '🌀 平行跃迁' }}
            </span>
          </div>
          <div class="panel-body">
            <div class="info-row">
              <span class="label">🎯 技能跃迁向量</span>
              <span class="value">{{ activeNode.skillDiff || '核心能力平滑过渡' }}</span>
            </div>
            <div class="info-row" v-if="activeNode.education">
              <span class="label">🎓 学历结界</span>
              <span class="value">{{ activeNode.education }}</span>
            </div>
            <div class="info-row" v-if="activeNode.experience">
              <span class="label">💼 经验沉淀要求</span>
              <span class="value">{{ activeNode.experience }}</span>
            </div>
            <div class="info-row">
              <span class="label">⏱️ 蜕变周期估算</span>
              <span class="value highlight">{{ activeNode.learningCycle || '3-6' }} 个月</span>
            </div>
          </div>
          <button class="close-btn" @click="activeNode = null">关闭控制台 ✕</button>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, shallowRef, nextTick } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { useRoute } from 'vue-router'
import API_CONFIG from '../config/api'

const route = useRoute()
const chartRef = ref(null)
const chartInstance = shallowRef(null)
const activeNode = ref(null)

const isLoading = ref(true)
const isDataEmpty = ref(false)
const isGenerating = ref(false)

const realGraphData = ref({ center: null, promotions: [], transfers: [] })

const baseURL = API_CONFIG.BASE_URL
const getHeaders = () => {
  const token = localStorage.getItem('token') || ''
  return { 'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}

// ================= 🌟 生成繁星的随机算法 =================
const getStarStyle = () => {
  const size = Math.random() * 2.5 + 0.5 // 星星大小 0.5px - 3px
  const duration = Math.random() * 3 + 1.5 // 闪烁周期 1.5s - 4.5s
  const delay = Math.random() * 5 // 错开闪烁时间
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 100}%`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`,
    opacity: Math.random() * 0.5 + 0.1 // 初始透明度随机
  }
}

// ================= 🌟 获取星图数据 (带超级防崩盾) =================
const fetchGraphData = async () => {
  const profileId = route.query.id || '232745912058150912'
  try {
    isLoading.value = true; 
    isDataEmpty.value = false;
    
    const res = await axios.get(`${baseURL}/api/analysis/graph/${profileId}`, { headers: getHeaders() })
    
    // 防范后端嵌套多层数据
    let remoteData = res.data.data || res.data;
    
    // 防范后端传回的是个 JSON 字符串
    if (typeof remoteData === 'string') {
        try { remoteData = JSON.parse(remoteData); } catch(e) {}
    }

    if (remoteData && remoteData.center) {
      realGraphData.value = remoteData;
      isLoading.value = false; 
      
      await nextTick(); 
      try {
        initChart()
      } catch(e) {
        console.error("星图渲染引擎报错，已安全拦截:", e);
      }
    } else {
      isDataEmpty.value = true; 
      isLoading.value = false;
    }
  } catch (error) {
    console.error("接口请求失败:", error);
    isDataEmpty.value = true; 
    isLoading.value = false;
  }
}

const handleTriggerGeneration = async () => {
  const jobId = route.query.id || '232745912058150912'
  try {
    isGenerating.value = true
    const res = await axios.post(`${baseURL}/api/analysis/job/${jobId}`, {}, { headers: getHeaders() })
    if (res.data.code === 200 || res.data.code === 0) await fetchGraphData()
  } finally {
    isGenerating.value = false
  }
}

// ================= 🌟 暗黑荧光数据构建 (超强容错版) =================
const buildGraphData = () => {
  const nodes = []
  const links = []
  const data = realGraphData.value
  
  if (!data || !data.center) return { nodes, links }

  // 兜底中心点名称，防止 null
  const centerName = data.center.name || '未知基准岗位';

  // 1. 核心主星 (赛博亮蓝)
  nodes.push({
    name: centerName,
    category: '当前岗位',
    symbolSize: 95,
    itemStyle: { 
      color: new echarts.graphic.RadialGradient(0.3, 0.3, 1, [{ offset: 0, color: '#60A5FA' }, { offset: 1, color: '#1E3A8A' }]), 
      shadowBlur: 50, shadowColor: '#3B82F6', borderColor: '#BFDBFE', borderWidth: 2
    }
  })

  const dustNames = []

  // 2. 晋升行星 (高亮荧光绿) - 强加 Array.isArray 校验！
  if (Array.isArray(data.promotions)) {
    data.promotions.forEach((p, index) => {
      // 容错：如果后端传的是个纯字符串数组，把它变成对象
      let nodeData = typeof p === 'string' ? { name: p } : p;
      // 容错：如果没有名字，给个默认名字防止 Echarts 崩溃
      let nodeName = nodeData.name || `晋升锚点 ${index + 1}`;
      
      const size = 50 + Math.random() * 25
      nodes.push({ 
        ...nodeData, 
        name: nodeName,
        category: '晋升路线', 
        symbolSize: size,
        itemStyle: { 
          color: new echarts.graphic.RadialGradient(0.3, 0.3, 1, [{ offset: 0, color: '#34D399' }, { offset: 1, color: '#064E3B' }]), 
          shadowBlur: 30, shadowColor: '#10B981', borderColor: '#A7F3D0', borderWidth: 1.5
        } 
      })
      links.push({ source: centerName, target: nodeName, lineStyle: { width: 2 } })
    })
  }

  // 3. 换岗行星 (幻影霓虹紫) - 强加 Array.isArray 校验！
  if (Array.isArray(data.transfers)) {
    data.transfers.forEach((t, index) => {
      // 容错机制同上
      let nodeData = typeof t === 'string' ? { name: t } : t;
      let nodeName = nodeData.name || `换岗锚点 ${index + 1}`;

      const size = 45 + Math.random() * 20
      nodes.push({ 
        ...nodeData, 
        name: nodeName,
        category: '换岗路线', 
        symbolSize: size,
        itemStyle: { 
          color: new echarts.graphic.RadialGradient(0.3, 0.3, 1, [{ offset: 0, color: '#C084FC' }, { offset: 1, color: '#6B21A8' }]), 
          shadowBlur: 30, shadowColor: '#A855F7', borderColor: '#E9D5FF', borderWidth: 1.5
        } 
      })
      links.push({ source: centerName, target: nodeName, lineStyle: { width: 2 } })
    })
  }

  // 4. 星座底纹 (填补空白的终极武器)
  const dustColors = ['#38BDF8', '#818CF8', '#34D399', '#A78BFA']
  for (let i = 0; i < 35; i++) {
    const name = `dust_${Math.random()}`
    dustNames.push(name)
    nodes.push({
      name, category: '星尘', symbolSize: Math.random() * 4 + 1.5,
      itemStyle: { color: dustColors[i % dustColors.length], opacity: 0.8, shadowBlur: 10, shadowColor: dustColors[i % dustColors.length] },
      label: { show: false }
    })
    links.push({ source: centerName, target: name, lineStyle: { opacity: 0 } })
  }
  for (let i = 0; i < 40; i++) {
    links.push({
      source: dustNames[Math.floor(Math.random() * dustNames.length)],
      target: dustNames[Math.floor(Math.random() * dustNames.length)],
      lineStyle: { opacity: 0.15, width: 0.5, type: 'dashed', curveness: 0.1, color: '#64748B' }
    })
  }

  return { nodes, links }
}

// ================= 🌟 深空暗黑 ECharts 配置 =================
const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance.value) chartInstance.value.dispose()
  
  chartInstance.value = echarts.init(chartRef.value)
  const { nodes, links } = buildGraphData()

  const option = {
    backgroundColor: 'transparent',
    tooltip: { 
      trigger: 'item', 
      backgroundColor: 'rgba(15, 23, 42, 0.9)',
      borderColor: 'rgba(51, 65, 85, 0.8)',
      textStyle: { color: '#F8FAFC', fontWeight: 'bold' },
      padding: [12, 18], borderRadius: 12, boxShadow: '0 10px 30px rgba(0,0,0,0.5)',
      formatter: (p) => {
        if (!p.data || p.data.category === '星尘') return '';
        return `<div style="display:flex;align-items:center;gap:8px;"><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color}"></span>${p.name}</div>`;
      } 
    },
    color: ['#3B82F6', '#10B981', '#A855F7'],
    legend: { 
      data: ['当前岗位', '晋升路线', '换岗路线'], 
      bottom: '4%', icon: 'circle', itemGap: 40, itemWidth: 12,
      textStyle: { color: '#CBD5E1', fontSize: 13, fontWeight: '700' }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      force: { 
        repulsion: [1500, 4000],
        edgeLength: [150, 400], 
        gravity: 0.05,
        friction: 0.1 
      },
      roam: true,
      categories: [{ name: '当前岗位' }, { name: '晋升路线' }, { name: '换岗路线' }, { name: '星尘' }],
      label: { 
        show: true, 
        position: 'right', 
        distance: 12,
        formatter: (p) => (p.data && p.data.category === '星尘') ? '' : `{title|${p.name}}`,
        rich: {
          title: {
            color: '#F8FAFC',
            fontSize: 13,
            fontWeight: 800,
            backgroundColor: 'rgba(15, 23, 42, 0.7)',
            borderColor: 'rgba(51, 65, 85, 0.8)',
            borderWidth: 1.5,
            padding: [6, 14],
            borderRadius: 20,
            shadowColor: 'rgba(0,0,0,0.5)',
            shadowBlur: 10,
          }
        }
      },
      data: nodes,
      links: links,
      lineStyle: { color: 'source', curveness: 0.25, opacity: 0.4 },
      emphasis: { 
        focus: 'adjacency', 
        lineStyle: { width: 4, opacity: 1, shadowBlur: 15, shadowColor: 'rgba(255,255,255,0.5)' },
        itemStyle: { shadowBlur: 60 }
      }
    }],
    animationEasingUpdate: 'quinticInOut', animationDurationUpdate: 2500
  }
  
  chartInstance.value.setOption(option)
  
  chartInstance.value.on('click', (p) => {
    if (p.dataType === 'node' && p.data && p.data.category !== '星尘') {
      activeNode.value = p.data
    }
  })
  
  chartInstance.value.getZr().on('click', (p) => {
    if (!p.target) activeNode.value = null
  })
}

onMounted(() => {
  fetchGraphData()
  window.addEventListener('resize', () => chartInstance.value?.resize())
})

onUnmounted(() => {
  window.removeEventListener('resize', () => chartInstance.value?.resize())
  if (chartInstance.value) chartInstance.value.dispose()
})
</script>

<style scoped>
/* 🌟 终极暗黑宇宙底层 */
.graph-page.dark-universe { width: 100%; height: 100vh; display: flex; flex-direction: column; background: radial-gradient(ellipse at bottom, #589dec 0%, #394b9c 100%); padding: 30px; overflow: hidden; box-sizing: border-box; font-family: 'Inter', -apple-system, sans-serif; position: relative; }

/* 🌟 满天繁星生成器 */
.starfield { position: absolute; inset: 0; pointer-events: none; z-index: 0; overflow: hidden;}
.star { position: absolute; background: white; border-radius: 50%; box-shadow: 0 0 8px 2px rgba(255, 255, 255, 0.4); animation: twinkle linear infinite alternate; }
@keyframes twinkle { 
  0% { transform: scale(0.8); opacity: 0.1; } 
  100% { transform: scale(1.2); opacity: 1; } 
}
/* 巨大的深空星云光晕 */
.nebula-glow { position: absolute; width: 1200px; height: 1200px; border-radius: 50%; filter: blur(200px); opacity: 0.15; z-index: 0; pointer-events: none;}
.glow-1 { top: -20%; left: -10%; background: #31539d; }
.glow-2 { bottom: -20%; right: -10%; background: #9333EA; }
/* 头部科幻排版 */
.page-header { text-align: center; margin-bottom: 25px; z-index: 2; position: relative; }
.neon-text { font-size: 2.2rem; font-weight: 900; color: #F8FAFC; margin: 0 0 8px 0; letter-spacing: 2px; text-shadow: 0 0 20px rgba(75, 128, 212, 0.8), 0 0 40px rgba(59, 130, 246, 0.4);}
.subtitle { color: #94A3B8; margin: 0; font-size: 1.05rem; font-weight: 500; letter-spacing: 3px; text-transform: uppercase;}
.graph-workspace { flex: 1; position: relative; display: flex; justify-content: center; z-index: 1;}
/* 极客黑晶玻璃态主容器 */
.chart-container.dark-glass { width: 100%; height: 100%; border-radius: 30px; position: relative; background: rgba(17, 47, 122, 0.4); backdrop-filter: blur(15px); overflow: hidden; box-shadow: inset 0 0 0 1px rgba(255,255,255,0.05), 0 20px 50px rgba(0,0,0,0.5);}
.echarts-box { width: 100%; height: 100%; min-height: 500px; }
/* 加载状态与空状态（暗黑版） */
.overlay-state { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 20px; background: rgba(2, 6, 23, 0.7); backdrop-filter: blur(10px); z-index: 5;}
.quantum-loader { width: 60px; height: 60px; border-radius: 50%; border: 3px solid rgba(56, 189, 248, 0.1); border-top-color: #38BDF8; border-right-color: #818CF8; animation: spin 1s linear infinite; box-shadow: 0 0 30px rgba(56, 189, 248, 0.3); }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { color: #38BDF8; font-weight: 800; font-size: 1.1rem; letter-spacing: 2px;}
.empty-state p { color: #94A3B8; font-weight: 600; font-size: 1.1rem;}
.empty-icon { font-size: 5rem; margin-bottom: -10px; filter: drop-shadow(0 0 20px rgba(56, 189, 248, 0.4));}
.cyber-btn { padding: 16px 32px; background: rgba(15, 23, 42, 0.8); color: #38BDF8; border: 1px solid #38BDF8; border-radius: 12px; cursor: pointer; font-size: 1.05rem; font-weight: 800; transition: all 0.3s ease; box-shadow: 0 0 15px rgba(56, 189, 248, 0.2), inset 0 0 15px rgba(56, 189, 248, 0.1);}
.cyber-btn:hover:not(:disabled) { transform: translateY(-3px); background: #38BDF8; color: #020617; box-shadow: 0 0 30px rgba(56, 189, 248, 0.5);}
/* 🌟 深空探测站（暗黑悬浮面板） */
.detail-panel.dark-glass { position: absolute; right: 40px; top: 40px; width: 340px; padding: 28px; border-radius: 24px; background: rgba(15, 23, 42, 0.85); z-index: 100; box-shadow: 0 30px 60px rgba(0,0,0,0.6), inset 0 0 0 1px rgba(255,255,255,0.1); backdrop-filter: blur(25px); display: flex; flex-direction: column; gap: 20px;}
.panel-deco-line { position: absolute; left: 0; top: 30px; bottom: 30px; width: 4px; background: linear-gradient(to bottom, #38BDF8, #818CF8); border-radius: 0 4px 4px 0; box-shadow: 0 0 10px rgba(56, 189, 248, 0.5);}
.panel-header { border-bottom: 1px solid rgba(58, 119, 205, 0.2); padding-bottom: 18px; }
.panel-header h3 { margin: 0 0 12px 0; font-size: 1.3rem; color: #F8FAFC; font-weight: 900; line-height: 1.3; text-shadow: 0 0 10px rgba(255,255,255,0.2);}
.node-tag { padding: 6px 14px; border-radius: 12px; font-size: 0.8rem; font-weight: 900; display: inline-block; border: 1px solid transparent;}
.tag-up { background: rgba(5, 150, 105, 0.2); color: #34D399; border-color: rgba(52, 211, 153, 0.3); }
.tag-transfer { background: rgba(109, 40, 217, 0.2); color: #C084FC; border-color: rgba(192, 132, 252, 0.3); }
.panel-body { display: flex; flex-direction: column; gap: 16px; }
.info-row { display: flex; flex-direction: column; gap: 8px; }
.info-row .label { color: #94A3B8; font-size: 0.85rem; font-weight: 800; letter-spacing: 1px;}
.info-row .value { color: #E2E8F0; font-weight: 700; background: rgba(30, 41, 59, 0.6); padding: 12px 16px; border-radius: 12px; display: block; border: 1px solid rgba(148, 163, 184, 0.1); font-size: 0.95rem; line-height: 1.5;}
.highlight { color: #38BDF8 !important; font-weight: 900 !important; background: rgba(56, 189, 248, 0.1) !important; border-color: rgba(56, 189, 248, 0.3) !important; text-shadow: 0 0 8px rgba(56,189,248,0.4); }
.close-btn { width: 100%; margin-top: 10px; padding: 14px; border: 1px solid rgba(148, 163, 184, 0.2); border-radius: 12px; cursor: pointer; background: transparent; font-weight: 900; color: #94A3B8; transition: 0.3s; font-size: 0.95rem;}
.close-btn:hover { background: rgba(255,255,255,0.05); color: #F8FAFC; border-color: rgba(255,255,255,0.2);}
.panel-fade-enter-active, .panel-fade-leave-active { transition: all 0.5s cubic-bezier(0.2, 1, 0.3, 1); }
.panel-fade-enter-from, .panel-fade-leave-to { opacity: 0; transform: translateX(30px) scale(0.95); filter: blur(5px); }
</style>