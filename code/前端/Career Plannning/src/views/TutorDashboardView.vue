<template>
  <div class="tutor-dashboard">
    <div class="ambient-glow glow-blue"></div>
    <div class="ambient-glow glow-purple"></div>
    <div class="ambient-glow glow-cyan"></div>

    <div class="dashboard-wrapper">
      <div class="dashboard-header fade-in-up" style="animation-delay: 0.1s">
        <div class="header-info">
          <h2 class="gradient-text">👨‍🏫 导师决策中枢大屏</h2>
          <p>实时全息监控 2,400 名在校生职业演化路径与心理动态</p>
        </div>
        
        <div class="stat-cards">
          <div class="stat-card glass-panel card-hover">
            <div class="card-icon blue-icon">👥</div>
            <div class="card-text">
              <span class="label">全局覆盖学生</span>
              <span class="value">328</span>
            </div>
          </div>
          <div class="stat-card glass-panel warning card-hover">
            <div class="card-icon red-icon">🚨</div>
            <div class="card-text">
              <span class="label">高危焦虑预警</span>
              <span class="value">58</span>
            </div>
          </div>
          <div class="stat-card glass-panel success card-hover">
            <div class="card-icon green-icon">✨</div>
            <div class="card-text">
              <span class="label">AI 深度干预修复</span>
              <span class="value">89%</span>
            </div>
          </div>
        </div>
      </div>

      <div class="grid-layout">
        <div class="glass-panel chart-box fade-in-up panel-hover" style="animation-delay: 0.2s">
          <h3 class="panel-title">🎯 学生职业赛道意愿分布</h3>
          <div ref="pieChartRef" class="chart-container"></div>
        </div>

        <div class="glass-panel chart-box fade-in-up panel-hover" style="animation-delay: 0.3s">
          <h3 class="panel-title">🔥 职业核心痛点阻碍 Top 5</h3>
          <div ref="barChartRef" class="chart-container"></div>
        </div>

        <div class="glass-panel full-width fade-in-up panel-hover" style="animation-delay: 0.4s">
          <h3 class="panel-title">📈 校园人才算力池 vs 市场岗位刚需对齐曲线</h3>
          <div ref="lineChartRef" class="line-chart"></div>
        </div>
      </div>
      
      <div style="height: 40px;"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const pieChartRef = ref(null)
const barChartRef = ref(null)
const lineChartRef = ref(null)

onMounted(() => {
  nextTick(() => {
    // 1. 🌟 意愿分布图 -> 恢复为标准环形图（均匀大小，按比例占长度）
    const pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { 
        trigger: 'item',
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        borderRadius: 8,
        borderWidth: 0,
        boxShadow: '0 4px 15px rgba(0,0,0,0.1)'
      },
      color: ['#3B82F6', '#8B5CF6', '#10B981', '#F59E0B', '#EC4899'],
      series: [{
        type: 'pie',
        // 适当调整了内外半径，让环形看起来更饱满
        radius: ['45%', '75%'], 
        center: ['50%', '50%'],
        // 🚨 删除了 roseType 属性，现在不再长短不一了！
        itemStyle: { 
          borderRadius: 8, 
          borderColor: '#fff', 
          borderWidth: 2,
          shadowBlur: 10,
          shadowColor: 'rgba(0,0,0,0.1)'
        },
        label: { 
          show: true, 
          formatter: '{b}\n{d}%',
          fontWeight: 'bold',
          color: '#475569'
        },
        data: [
          { value: 1048, name: '后端架构' },
          { value: 735, name: '前端/移动端' },
          { value: 580, name: '人工智能' },
          { value: 484, name: '数据分析' },
          { value: 300, name: '产品/运营' }
        ]
      }]
    })

    // 2. 痛点条形图 
    const barChart = echarts.init(barChartRef.value)
    barChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      grid: { left: '3%', right: '8%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: { 
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed', color: '#E2E8F0' } },
        axisLabel: { color: '#64748B' }
      },
      yAxis: { 
        type: 'category', 
        data: ['面试恐惧', '项目无亮点', '基础八股弱', '专业不对口', '考研纠结'],
        axisLabel: { fontWeight: 'bold', color: '#334155' },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      series: [{
        data: [820, 932, 1201, 734, 1580],
        type: 'bar',
        barWidth: '45%',
        showBackground: true,
        backgroundStyle: { color: 'rgba(241, 245, 249, 0.5)', borderRadius: 10 },
        itemStyle: { 
          borderRadius: [0, 10, 10, 0],
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#8B5CF6' },
            { offset: 1, color: '#3B82F6' }
          ]),
          shadowBlur: 10,
          shadowColor: 'rgba(59, 130, 246, 0.3)'
        }
      }]
    })

    // 3. 对齐曲线 
    const lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: { 
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderWidth: 0,
        boxShadow: '0 4px 20px rgba(0,0,0,0.1)'
      },
      legend: { 
        data: ['学生平均算力', '企业用人刚需'],
        top: 0,
        textStyle: { fontWeight: 'bold', color: '#475569' }
      },
      grid: { left: '2%', right: '4%', bottom: '5%', top: '15%', containLabel: true },
      xAxis: { 
        type: 'category', 
        boundaryGap: false,
        data: ['Java底层', '数据库调优', '高并发压测', '微服务链路', 'DevOps部署'],
        axisLabel: { color: '#64748B', fontWeight: '600' }
      },
      yAxis: { 
        type: 'value',
        splitLine: { lineStyle: { color: '#F1F5F9' } }
      },
      series: [
        { 
          name: '学生平均算力', 
          type: 'line', 
          smooth: true, 
          symbolSize: 8,
          data: [85, 40, 20, 35, 15], 
          lineStyle: { 
            width: 4, 
            shadowColor: 'rgba(59, 130, 246, 0.5)', 
            shadowBlur: 15,
            shadowOffsetY: 5
          },
          itemStyle: { color: '#3B82F6' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59, 130, 246, 0.4)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0.0)' }
            ])
          }
        },
        { 
          name: '企业用人刚需', 
          type: 'line', 
          smooth: true, 
          symbolSize: 8,
          data: [80, 75, 70, 85, 90], 
          lineStyle: { 
            type: 'dashed', 
            width: 3,
            color: '#10B981'
          },
          itemStyle: { color: '#10B981' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(16, 185, 129, 0.2)' },
              { offset: 1, color: 'rgba(16, 185, 129, 0.0)' }
            ])
          }
        }
      ]
    })

    window.addEventListener('resize', () => {
      pieChart.resize()
      barChart.resize()
      lineChart.resize()
    })
  })
})
</script>

<style scoped>
/* 🌟 核心修复区：彻底解决无法滚动的问题 */
/* 🌟 核心修复：移除 absolute 绝对定位，完美贴合右侧区域 */
.tutor-dashboard { 
  width: 100%;
  height: 100%; 
  /* 适当减小左右 padding，避免小屏幕下太挤 */
  padding: 10px 20px 40px 20px; 
  background: transparent; 
  /* 依然保留内部丝滑滚动 */
  overflow-y: auto; 
  overflow-x: hidden;
  box-sizing: border-box;
  font-family: 'Inter', sans-serif;
  /* 防止在 App.vue 的 flex 容器中被居中压缩 */
  align-self: flex-start; 
}

/* 🌟 美化滚动条，保持科技感 */
.tutor-dashboard::-webkit-scrollbar { width: 8px; }
.tutor-dashboard::-webkit-scrollbar-track { background: transparent; }
.tutor-dashboard::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 4px; }
.tutor-dashboard::-webkit-scrollbar-thumb:hover { background: #94A3B8; }

/* 全息炫彩光晕背景 */
.ambient-glow { position: absolute; border-radius: 50%; filter: blur(120px); pointer-events: none; z-index: 0; }
.glow-blue { width: 600px; height: 600px; background: #3B82F6; top: -150px; left: -100px; opacity: 0.25; }
.glow-purple { width: 500px; height: 500px; background: #8B5CF6; bottom: -50px; right: -150px; opacity: 0.2; }
.glow-cyan { width: 400px; height: 400px; background: #06B6D4; top: 40%; left: 30%; opacity: 0.15; }

/* 限制最大宽度，桌面端完美居中 */
.dashboard-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* 头部排版优化 */
.dashboard-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 40px; 
  flex-wrap: wrap;
  gap: 20px;
}
.header-info { flex: 1; min-width: 300px;}
.gradient-text { 
  font-size: 2.2rem; 
  font-weight: 900; 
  background: linear-gradient(135deg, #1E293B, #3B82F6); 
  -webkit-background-clip: text; 
  -webkit-text-fill-color: transparent; 
  margin: 0 0 10px 0;
}
.header-info p { color: #64748B; margin: 0; font-size: 1.1rem; font-weight: 500;}

/* 玻璃拟态卡片与交互 */
.glass-panel { 
  background: rgba(255, 255, 255, 0.75); 
  backdrop-filter: blur(20px); 
  border: 1px solid rgba(255, 255, 255, 1); 
  border-radius: 20px; 
  box-shadow: 0 10px 30px rgba(0,0,0,0.04); 
}
.panel-hover { transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1); }
.panel-hover:hover { transform: translateY(-6px); box-shadow: 0 20px 40px rgba(0,0,0,0.08); border-color: rgba(59, 130, 246, 0.3);}

/* 顶部三联数据卡片 */
.stat-cards { display: flex; gap: 20px; flex: 2; justify-content: flex-end;}
.stat-card { 
  padding: 20px 25px; 
  display: flex; 
  align-items: center; 
  gap: 18px; 
  min-width: 220px;
}
.card-hover { transition: 0.3s; cursor: default; }
.card-hover:hover { transform: translateY(-4px) scale(1.02); box-shadow: 0 15px 30px rgba(0,0,0,0.08); }

/* 卡片图标 */
.card-icon { width: 54px; height: 54px; border-radius: 16px; display: flex; justify-content: center; align-items: center; font-size: 1.6rem; }
.blue-icon { background: #EFF6FF; color: #3B82F6; }
.red-icon { background: #FEF2F2; color: #EF4444; }
.green-icon { background: #ECFDF5; color: #10B981; }

.card-text { display: flex; flex-direction: column; }
.label { font-size: 0.9rem; color: #64748B; font-weight: 700; margin-bottom: 4px; }
.value { font-size: 1.8rem; font-weight: 900; color: #1E293B; line-height: 1;}
.stat-card.warning .value { color: #EF4444; }
.stat-card.success .value { color: #10B981; }

/* 网格大屏布局 */
.grid-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 28px; }
.chart-box { padding: 30px; }
.full-width { grid-column: span 2; padding: 35px; }
.panel-title { 
  margin: 0 0 25px 0; 
  color: #1E293B; 
  font-size: 1.2rem; 
  font-weight: 800;
  display: flex;
  align-items: center;
}
.panel-title::before {
  content: '';
  display: inline-block;
  width: 5px;
  height: 20px;
  background: linear-gradient(to bottom, #3B82F6, #8B5CF6);
  border-radius: 4px;
  margin-right: 12px;
}
.chart-container { height: 360px; width: 100%;}
.line-chart { height: 420px; width: 100%;}

/* 丝滑级联入场动画 */
.fade-in-up { animation: fadeInUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
@keyframes fadeInUp {
  0% { opacity: 0; transform: translateY(40px); }
  100% { opacity: 1; transform: translateY(0); }
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .dashboard-header { flex-direction: column; align-items: stretch; }
  .stat-cards { justify-content: space-between; width: 100%; }
}
@media (max-width: 900px) {
  .grid-layout { grid-template-columns: 1fr; }
  .full-width { grid-column: span 1; }
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 calc(50% - 10px); }
}
</style>