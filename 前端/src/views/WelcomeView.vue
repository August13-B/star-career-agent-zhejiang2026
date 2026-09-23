<template>
  <div class="atlas-page">
    <aside class="atlas-sidebar" :class="{ 'is-open': mobileMenuOpen }">
      <router-link class="atlas-brand" to="/" @click="mobileMenuOpen = false">
        <span class="atlas-brand-mark"><AppIcon name="compass" :size="25" /></span>
        <span><strong>星职</strong><small>STAR CAREER</small></span>
      </router-link>
      <p class="atlas-side-label">职业导航</p>
      <nav class="atlas-side-nav" aria-label="主要功能">
        <router-link to="/" class="is-current" @click="mobileMenuOpen = false"><AppIcon name="compass" :size="18" /> 我的航图</router-link>
        <router-link to="/multi-agent" @click="mobileMenuOpen = false"><AppIcon name="cpu" :size="18" /> 联合测评</router-link>
        <router-link to="/graph" @click="mobileMenuOpen = false"><AppIcon name="graph" :size="18" /> 职业星图</router-link>
        <router-link to="/ai-score" @click="mobileMenuOpen = false"><AppIcon name="radar" :size="18" /> 能力测评</router-link>
        <router-link to="/growth" @click="mobileMenuOpen = false"><AppIcon name="trendUp" :size="18" /> 成长计划</router-link>
        <router-link to="/assistant" @click="mobileMenuOpen = false"><AppIcon name="chat" :size="18" /> AI 伙伴</router-link>
        <router-link to="/profile" @click="mobileMenuOpen = false"><AppIcon name="user" :size="18" /> 个人画像</router-link>
      </nav>
      <div class="atlas-side-bottom"><p>每一个认真生活的人，<br />都值得被看见。</p><span>星职 · 与你同行</span></div>
    </aside>

    <div class="atlas-body">
      <header class="atlas-topbar">
        <button class="atlas-menu-button" type="button" :aria-expanded="mobileMenuOpen" aria-label="打开导航菜单" @click="mobileMenuOpen = !mobileMenuOpen"><AppIcon name="compass" :size="20" /> 功能导航</button>
        <span class="atlas-topbar-title">面向未来工作的 AI 职业导航与终身学习伙伴</span>
        <router-link class="atlas-account" :to="isLoggedIn ? '/profile' : '/login'">{{ isLoggedIn ? '进入个人中心' : '登录 / 注册' }} <span aria-hidden="true">↗</span></router-link>
      </header>

      <main>
        <section class="atlas-hero" aria-labelledby="atlas-title">
          <div class="atlas-hero-copy">
            <p class="atlas-kicker">人生如海 · 职业如星</p>
            <h1 id="atlas-title">让职业选择，<br />更有方向<span class="atlas-title-point">。</span></h1>
            <h2>从多智能体测评，到 3–5 年成长路径</h2>
            <p class="atlas-hero-description">基于你的兴趣、能力与经历，结合职业趋势与岗位要求，<br class="atlas-desktop-only" />把职业探索绘成一张可以持续调整的成长航图。</p>
            <div class="atlas-hero-actions"><button class="atlas-primary-button" type="button" @click="goToAssessment">{{ isLoggedIn ? '继续联合测评' : '开启联合测评' }} <span aria-hidden="true">→</span></button><a href="#atlas-workflow" class="atlas-text-link">了解完整路径 <span aria-hidden="true">↓</span></a></div>
          </div>
          <p class="atlas-hero-caption">探索更多可能，<br />让每一步都有方向。</p>
        </section>

        <section id="atlas-workflow" class="atlas-workflow" aria-label="职业成长流程">
          <div v-for="(step, index) in workflow" :key="step.title" class="atlas-workflow-step"><span class="atlas-workflow-index">{{ String(index + 1).padStart(2, '0') }}</span><div><strong>{{ step.title }}</strong><small>{{ step.description }}</small></div></div>
        </section>

        <div class="atlas-content-grid">
          <section class="atlas-panel atlas-map-panel" aria-labelledby="atlas-map-title">
            <div class="atlas-panel-head"><div><p class="atlas-section-index">EXPLORE / 01</p><h2 id="atlas-map-title">我的职业星图</h2><p>从测评结果出发，探索方向之间的联系</p></div><div class="atlas-tab-group" aria-label="星图视图"><button type="button" :class="{ active: viewMode === 'graph' }" @click="viewMode = 'graph'">图谱视图</button><button type="button" :class="{ active: viewMode === 'path' }" @click="viewMode = 'path'">路径视图</button></div></div>
            <template v-if="viewMode === 'graph'">
              <div class="atlas-demo-label">方向示意 · 完成联合测评后生成你的专属星图</div>
              <div class="atlas-map-stage">
                <div ref="graphEl" class="atlas-graph" role="img" aria-label="示意职业星图，四个方向由密集星点相互连接"></div>
                <button class="atlas-map-center" type="button" title="开启联合测评，绘制专属星图" @click="goToAssessment"><strong>以你为中心</strong><small>探索职业的更多可能</small></button>
                <button v-for="(branch, index) in branches" :key="branch.id" class="atlas-role-node" :class="[`atlas-role-node--${branch.id}`, { 'is-active': activeBranchIndex === index }]" type="button" :aria-pressed="activeBranchIndex === index" @click="activeBranchIndex = index"><span class="atlas-role-orb"><AppIcon :name="branch.icon" :size="23" /></span><span class="atlas-role-copy"><strong>{{ branch.name }}</strong><small>{{ branch.tagline }}</small></span></button>
                <button v-for="(branch, index) in branches" :key="`${branch.id}-card`" class="atlas-role-card" :class="[`atlas-role-card--${branch.id}`, { 'is-active': activeBranchIndex === index }]" type="button" :aria-label="`探索${branch.name}方向`" @click="activeBranchIndex = index"><span class="atlas-role-card-kicker">探索方向 / 0{{ index + 1 }}</span><span v-for="skill in branch.skills" :key="skill" class="atlas-role-skill"><i aria-hidden="true"></i>{{ skill }}</span><span class="atlas-role-card-link">查看方向 <span aria-hidden="true">→</span></span></button>
                <span v-for="label in constellationLabels" :key="label.text" class="atlas-constellation-label" :style="{ left: label.x, top: label.y }" aria-hidden="true">{{ label.text }}</span>
                <div class="atlas-map-ornament" aria-hidden="true"><AppIcon name="compass" :size="39" /></div>
                <span class="atlas-map-motto">Where Talents Meet a Bigger Tomorrow</span>
              </div>
              <div class="atlas-branch-detail"><span class="atlas-branch-symbol"><AppIcon :name="selectedBranch.icon" :size="22" /></span><div><small>当前探索方向</small><strong>{{ selectedBranch.name }}</strong><p>{{ selectedBranch.summary }}</p></div><router-link to="/graph" class="atlas-inline-link">查看真实星图 →</router-link></div>
            </template>
            <div v-else class="atlas-path-view"><p>职业路径会在联合测评后，根据你的画像与目标岗位生成，并随成长反馈调整。</p><div v-for="(phase, index) in pathPreview" :key="phase.year" class="atlas-path-step"><span>{{ String(index + 1).padStart(2, '0') }}</span><div><strong>{{ phase.year }}</strong><p>{{ phase.action }}</p></div></div><router-link to="/multi-agent" class="atlas-primary-button atlas-path-cta">生成我的成长路径 <span>→</span></router-link></div>
          </section>

          <div class="atlas-insights">
            <section class="atlas-panel atlas-ability-panel" aria-labelledby="atlas-ability-title"><div class="atlas-panel-head"><div><p class="atlas-section-index">PROFILE / 02</p><h2 id="atlas-ability-title">能力画像</h2></div><span class="atlas-demo-pill">演示数据</span></div><div class="atlas-ability-body"><div ref="radarEl" class="atlas-radar" role="img" aria-label="演示能力雷达图"></div><div class="atlas-ability-note"><strong>看见优势，也看见差距</strong><p>测评会结合经历、目标与岗位要求，生成动态能力画像。</p><router-link to="/multi-agent">获得我的画像 →</router-link></div></div></section>
            <section class="atlas-panel atlas-growth-panel" aria-labelledby="atlas-growth-title"><div class="atlas-panel-head"><div><p class="atlas-section-index">JOURNEY / 03</p><h2 id="atlas-growth-title">3–5 年成长路径</h2></div><router-link to="/growth" class="atlas-inline-link">查看计划 →</router-link></div><div class="atlas-timeline"><div v-for="phase in pathPreview" :key="phase.year"><span></span><strong>{{ phase.year }}</strong><small>{{ phase.title }}</small></div></div><p class="atlas-panel-footnote">路径可细化、可调整，学习任务会随反馈更新。</p></section>
            <section class="atlas-panel atlas-action-panel" aria-labelledby="atlas-action-title"><div class="atlas-panel-head"><div><p class="atlas-section-index">ACTION / 04</p><h2 id="atlas-action-title">把计划变成行动</h2></div><router-link to="/growth" class="atlas-inline-link">查看成长任务 →</router-link></div><div class="atlas-action-list"><div><AppIcon name="graduation" :size="17" /><span><strong>阶段学习任务</strong><small>围绕能力差距安排学习与实践</small></span></div><div><AppIcon name="briefcase" :size="17" /><span><strong>职场场景训练</strong><small>在模拟对话中练习真实工作能力</small></span></div></div></section>
          </div>
        </div>

        <footer class="atlas-footer"><span>星职 StarCareer · 面向未来工作的职业导航</span><span>职业建议仅供探索参考，个人信息由你掌控。</span></footer>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import AppIcon from '../components/AppIcon.vue'
import '../styles/atlasLanding.css'

const router = useRouter()
const isLoggedIn = ref(Boolean(localStorage.getItem('token')))
const mobileMenuOpen = ref(false)
const viewMode = ref('graph')
const activeBranchIndex = ref(0)
const graphEl = ref(null)
const radarEl = ref(null)
let graphChart
let radarChart
let resizeObserver

const workflow = [
  { title: '感知与测评', description: '兴趣 · 能力 · 经历' },
  { title: '画像与认知', description: '多智能体 · 职业趋势' },
  { title: '星图与决策', description: '岗位匹配 · 路径选择' },
  { title: '学习与执行', description: '阶段任务 · 场景训练' },
  { title: '反馈与成长', description: '跟踪效果 · 动态调整' }
]
const branches = [
  { id: 'frontend', name: '前端开发', tagline: '创造可感知的数字世界', summary: '面向可感知的数字体验，连接技术实现与用户需求。', icon: 'cpu', skills: ['界面实现', '工程能力'] },
  { id: 'product', name: '产品设计', tagline: '连接用户与未来', summary: '洞察用户与业务问题，把想法变成可验证的方案。', icon: 'briefcase', skills: ['需求分析', '用户研究'] },
  { id: 'data', name: '数据分析', tagline: '用数据看见真实的世界', summary: '从数据中发现规律，为业务决策提供可靠依据。', icon: 'trendUp', skills: ['数据处理', '业务洞察'] },
  { id: 'ai', name: 'AI 应用', tagline: '与智能共创新可能', summary: '把 AI 能力融入真实场景，设计有效的人机协作。', icon: 'sparkle', skills: ['AI 工具', '场景落地'] }
]
const constellationLabels = [
  { text: '用户体验', x: '43%', y: '6%' }, { text: '商业思维', x: '46%', y: '11%' }, { text: '创意表达', x: '49%', y: '16%' },
  { text: '编程能力', x: '3%', y: '43%' }, { text: '工程能力', x: '7%', y: '48%' }, { text: '项目经验', x: '11%', y: '53%' },
  { text: '产品思维', x: '84%', y: '43%' }, { text: '用户洞察', x: '82%', y: '48%' }, { text: '设计能力', x: '86%', y: '53%' },
  { text: '统计分析', x: '35%', y: '84%' }, { text: '业务理解', x: '39%', y: '89%' },
  { text: '提示工程', x: '63%', y: '84%' }, { text: '跨学科融合', x: '66%', y: '89%' }
]
const constellationPoints = [
  [85, 92], [137, 76], [188, 99], [229, 128], [270, 157], [313, 188], [359, 250], [177, 174],
  [243, 210], [109, 239], [167, 276], [221, 307], [273, 350], [330, 304], [78, 349], [146, 374],
  [213, 405], [293, 425], [390, 405], [447, 354], [523, 350], [572, 399], [641, 371], [474, 289],
  [543, 260], [616, 283], [467, 185], [522, 91], [606, 102], [429, 115], [384, 157], [356, 84],
  [280, 73], [310, 261], [419, 251], [413, 325], [195, 221], [471, 75], [645, 179], [78, 185],
  [255, 275], [492, 220], [575, 171], [352, 361], [580, 319], [125, 151], [226, 67], [496, 410]
]
const constellationLinks = [
  [0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [0, 7], [7, 8], [8, 5], [7, 36], [36, 8],
  [39, 45], [45, 7], [9, 10], [10, 36], [10, 11], [11, 12], [12, 13], [13, 6], [14, 15], [15, 11],
  [15, 16], [16, 17], [17, 12], [17, 43], [43, 18], [18, 19], [19, 20], [20, 21], [21, 22],
  [20, 44], [44, 25], [25, 38], [23, 24], [24, 25], [23, 19], [23, 34], [34, 6], [34, 35],
  [35, 19], [26, 41], [41, 24], [26, 30], [30, 6], [27, 28], [28, 42], [42, 26], [29, 30],
  [29, 37], [37, 27], [31, 32], [32, 4], [31, 30], [40, 11], [40, 33], [33, 6], [46, 2],
  [46, 32], [47, 18], [47, 21], [4, 33], [8, 40], [12, 43], [13, 35], [5, 30], [30, 41]
]
const selectedBranch = computed(() => branches[activeBranchIndex.value])
const pathPreview = [
  { year: '第 1 年', title: '夯实基础', action: '确认方向，建立核心技能与可展示的项目作品。' },
  { year: '第 2 年', title: '能力跃迁', action: '进入真实场景，训练协作、表达与问题解决能力。' },
  { year: '第 3 年', title: '拓展边界', action: '结合反馈调整路径，探索跨岗位与行业机会。' },
  { year: '第 4–5 年', title: '持续成长', action: '形成长期优势，让学习与职业选择持续迭代。' }
]

function goToAssessment() { router.push(isLoggedIn.value ? '/multi-agent' : { name: 'login', query: { redirect: '/multi-agent' } }) }

function renderGraph() {
  if (!graphEl.value || viewMode.value !== 'graph') return
  graphChart?.dispose()
  graphChart = echarts.init(graphEl.value, null, { renderer: 'canvas' })
  const nodes = constellationPoints.map(([x, y], index) => ({ id: `star-${index}`, x, y, symbolSize: [6, 13, 21, 33, 34, 43].includes(index) ? 12 : index % 5 === 0 ? 8 : 5, itemStyle: { color: index % 4 === 0 ? '#fffdf0' : '#ffe4aa', borderColor: '#fffef2', borderWidth: 1, shadowBlur: index % 5 === 0 ? 21 : 13, shadowColor: '#ffe0a2' } }))
  nodes.push({ id: 'bound-start', x: 0, y: 0, symbolSize: 0, itemStyle: { opacity: 0 } }, { id: 'bound-end', x: 720, y: 500, symbolSize: 0, itemStyle: { opacity: 0 } })
  const links = constellationLinks.map(([from, to], index) => ({ source: `star-${from}`, target: `star-${to}`, lineStyle: { opacity: index % 5 === 0 ? .98 : .77, width: index % 5 === 0 ? 1.9 : 1.35 } }))
  graphChart.setOption({ animationDuration: 1100, animationEasing: 'cubicOut', series: [{ type: 'graph', layout: 'none', roam: false, silent: true, left: 0, top: 0, right: 0, bottom: 0, data: nodes, links, lineStyle: { color: '#fff0c6', width: 1.3, opacity: .8, shadowBlur: 5, shadowColor: '#ffe4b2' }, label: { show: false }, edgeSymbol: ['none', 'none'] }] })
}

function renderRadar() {
  if (!radarEl.value) return
  radarChart?.dispose()
  radarChart = echarts.init(radarEl.value, null, { renderer: 'canvas' })
  radarChart.setOption({ radar: { center: ['50%', '52%'], radius: '57%', splitNumber: 3, shape: 'polygon', axisName: { color: '#4a5f73', fontSize: 10 }, splitLine: { lineStyle: { color: '#d7d8d4' } }, splitArea: { areaStyle: { color: ['#fbfaf5', '#f4f2eb'] } }, axisLine: { lineStyle: { color: '#c7bda8' } }, indicator: [{ name: '学习力', max: 100 }, { name: '表达力', max: 100 }, { name: '协作力', max: 100 }, { name: '分析力', max: 100 }, { name: '执行力', max: 100 }, { name: '创新力', max: 100 }] }, series: [{ type: 'radar', data: [{ value: [76, 66, 72, 70, 82, 74], name: '演示画像', areaStyle: { color: 'rgba(65,103,132,.2)' }, lineStyle: { color: '#345d7a', width: 2 }, itemStyle: { color: '#345d7a' }, symbolSize: 4 }] }] })
}

watch(viewMode, async mode => { if (mode === 'graph') { await nextTick(); renderGraph(); if (graphEl.value) resizeObserver?.observe(graphEl.value) } })
onMounted(() => { renderGraph(); renderRadar(); resizeObserver = new ResizeObserver(() => { graphChart?.resize(); radarChart?.resize() }); if (graphEl.value) resizeObserver.observe(graphEl.value); if (radarEl.value) resizeObserver.observe(radarEl.value) })
onBeforeUnmount(() => { resizeObserver?.disconnect(); graphChart?.dispose(); radarChart?.dispose() })
</script>
