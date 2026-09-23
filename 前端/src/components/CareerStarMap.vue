<template>
  <div class="star-map-stage" role="group" :aria-label="ariaLabel">
    <div ref="constellationEl" class="star-map-constellation" aria-hidden="true"></div>
    <button class="star-map-center" type="button" :title="centerAction" @click="emit('center-click')"><strong>{{ centerLabel }}</strong><small>{{ centerSubtitle }}</small></button>
    <template v-for="(branch, index) in visibleBranches" :key="`${branch.id || branch.name}-${index}`">
      <button class="star-map-node" :class="[`star-map-node--${positions[index]}`, { 'is-active': selectedIndex === index }]" type="button" :aria-pressed="selectedIndex === index" :title="branch.name" @click="emit('select', index)">
        <span class="star-map-orb"><AppIcon :name="branch.icon || icons[index]" :size="23" /></span>
        <span class="star-map-node-copy"><strong>{{ branch.name }}</strong><small>{{ branch.tagline || branch.kindLabel }}</small></span>
      </button>
      <button class="star-map-card" :class="[`star-map-card--${positions[index]}`, { 'is-active': selectedIndex === index }]" type="button" :aria-label="`查看${branch.name}方向`" @click="emit('select', index)">
        <span class="star-map-card-kicker">{{ preview ? '探索方向' : '测评方向' }} / 0{{ index + 1 }}</span>
        <span v-for="skill in (branch.skills || []).slice(0, 2)" :key="skill" class="star-map-skill"><i aria-hidden="true"></i>{{ skill }}</span>
        <span class="star-map-card-link">查看方向 <span aria-hidden="true">→</span></span>
      </button>
    </template>
    <span v-for="(label, index) in labels" :key="`${label.text}-${index}`" class="star-map-label" :style="{ left: label.x, top: label.y }" aria-hidden="true">{{ label.text }}</span>
    <div class="star-map-ornament" aria-hidden="true"><AppIcon name="compass" :size="39" /></div>
    <span class="star-map-motto">Where Talents Meet a Bigger Tomorrow</span>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'
import AppIcon from './AppIcon.vue'
import '../styles/careerStarMap.css'

const props = defineProps({
  branches: { type: Array, default: () => [] },
  selectedIndex: { type: Number, default: -1 },
  centerLabel: { type: String, default: '以你为中心' },
  centerSubtitle: { type: String, default: '探索职业的更多可能' },
  centerAction: { type: String, default: '返回测评画像' },
  ariaLabel: { type: String, default: '职业星图' },
  preview: { type: Boolean, default: false }
})
const emit = defineEmits(['select', 'center-click'])
const constellationEl = ref(null)
const visibleBranches = computed(() => props.branches.slice(0, 4))
const positions = ['northwest', 'northeast', 'southwest', 'southeast']
const icons = ['cpu', 'briefcase', 'trendUp', 'sparkle']
const labelPositions = [
  ['43%', '6%'], ['46%', '11%'], ['49%', '16%'], ['3%', '43%'], ['7%', '48%'], ['11%', '53%'],
  ['84%', '43%'], ['82%', '48%'], ['86%', '53%'], ['35%', '84%'], ['39%', '89%'], ['63%', '84%'], ['66%', '89%']
]
const previewWords = ['用户体验', '商业思维', '创意表达', '编程能力', '工程能力', '项目经验', '产品思维', '用户洞察', '设计能力', '统计分析', '业务理解', '提示工程', '跨学科融合']
const labels = computed(() => {
  const words = props.preview ? previewWords : props.branches.flatMap(branch => branch.skills || []).filter((word, index, all) => all.indexOf(word) === index)
  return words.slice(0, labelPositions.length).map((text, index) => ({ text, x: labelPositions[index][0], y: labelPositions[index][1] }))
})
const points = [
  [85,92],[137,76],[188,99],[229,128],[270,157],[313,188],[359,250],[177,174],
  [243,210],[109,239],[167,276],[221,307],[273,350],[330,304],[78,349],[146,374],
  [213,405],[293,425],[390,405],[447,354],[523,350],[572,399],[641,371],[474,289],
  [543,260],[616,283],[467,185],[522,91],[606,102],[429,115],[384,157],[356,84],
  [280,73],[310,261],[419,251],[413,325],[195,221],[471,75],[645,179],[78,185],
  [255,275],[492,220],[575,171],[352,361],[580,319],[125,151],[226,67],[496,410]
]
const edges = [
  [0,1],[1,2],[2,3],[3,4],[4,5],[5,6],[0,7],[7,8],[8,5],[7,36],[36,8],
  [39,45],[45,7],[9,10],[10,36],[10,11],[11,12],[12,13],[13,6],[14,15],[15,11],
  [15,16],[16,17],[17,12],[17,43],[43,18],[18,19],[19,20],[20,21],[21,22],
  [20,44],[44,25],[25,38],[23,24],[24,25],[23,19],[23,34],[34,6],[34,35],
  [35,19],[26,41],[41,24],[26,30],[30,6],[27,28],[28,42],[42,26],[29,30],
  [29,37],[37,27],[31,32],[32,4],[31,30],[40,11],[40,33],[33,6],[46,2],
  [46,32],[47,18],[47,21],[4,33],[8,40],[12,43],[13,35],[5,30],[30,41]
]
let chart
let observer
let twinkleTimer
let stopMotionWatch = () => {}
onMounted(() => {
  chart = echarts.init(constellationEl.value, null, { renderer: 'canvas' })
  const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  const nodes = points.map(([x,y], index) => ({ id: `star-${index}`, x, y, symbolSize: [6,13,21,33,34,43].includes(index) ? 12 : index % 5 === 0 ? 8 : 5, itemStyle: { color: index % 4 === 0 ? '#f4dfb1' : '#c2985f', borderColor: '#fffaf0', borderWidth: 1, shadowBlur: index % 5 === 0 ? 11 : 6, shadowColor: '#a6783b' } }))
  nodes.push({ id:'bound-start',x:0,y:0,symbolSize:0,itemStyle:{opacity:0} },{ id:'bound-end',x:720,y:500,symbolSize:0,itemStyle:{opacity:0} })
  const links = edges.map(([from,to], index) => ({ source:`star-${from}`, target:`star-${to}`, lineStyle:{opacity:index%5===0?.72:.46,width:index%5===0?1.6:1.15} }))
  chart.setOption({ animation:!motionQuery.matches, animationDuration:1100, animationDurationUpdate:900, animationEasing:'cubicOut', animationEasingUpdate:'cubicInOut', series:[{ id:'star-map',type:'graph',layout:'none',roam:false,silent:true,left:0,top:0,right:0,bottom:0,data:nodes,links,lineStyle:{color:'#987b56',width:1.15,opacity:.5},label:{show:false} }] })
  const twinklePoints = [6, 18, 27, 33, 43]
  let twinkleFrame = 0
  const twinkle = () => {
    if (document.hidden || motionQuery.matches) return
    const litPoints = new Set([0, 2, 4].map(offset => twinklePoints[(twinkleFrame + offset) % twinklePoints.length]))
    twinkleFrame++
    chart.setOption({series:[{id:'star-map',data:nodes.map((node,index) => litPoints.has(index) ? { ...node, symbolSize:node.symbolSize + 5, itemStyle:{...node.itemStyle,color:'#fff2c5',shadowBlur:28,shadowColor:'#eebd67'} } : node)}]})
  }
  const onMotionChange = () => {
    chart.setOption({animation:!motionQuery.matches,series:[{id:'star-map',data:nodes}]})
    if (!motionQuery.matches) twinkle()
  }
  motionQuery.addEventListener('change', onMotionChange)
  stopMotionWatch = () => motionQuery.removeEventListener('change', onMotionChange)
  twinkleTimer = window.setInterval(twinkle, 1200)
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(constellationEl.value)
})
onBeforeUnmount(() => { window.clearInterval(twinkleTimer); stopMotionWatch(); observer?.disconnect(); chart?.dispose() })
</script>
