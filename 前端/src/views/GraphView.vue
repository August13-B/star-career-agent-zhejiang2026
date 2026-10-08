<template>
  <main class="career-workbench scientific-atlas">
    <header class="workbench-heading"><div><span class="eyebrow">职业探索 / 02</span><h1>我的职业星图</h1><p>以联合测评为依据，梳理适合你的职业方向与能力补齐路径。</p></div><router-link class="wb-button" to="/multi-agent">返回联合测评</router-link></header>
    <nav class="journey" aria-label="职业规划流程"><router-link to="/multi-agent"><b>01</b> 联合测评</router-link><span class="current"><b>02</b> 职业星图</span><router-link to="/growth"><b>03</b> 行动计划</router-link></nav>
    <p v-if="error" class="wb-notice" role="alert">{{ error }} <button @click="loadReports">重新加载</button></p>
    <section v-if="loading" class="wb-empty" aria-live="polite"><h2>正在读取联合测评…</h2></section>
    <section v-else-if="!reports.length" class="wb-empty graph-empty"><span class="eyebrow">从联合测评开始</span><h2>{{ error ? '测评暂时无法读取，先看看星图的样子。' : '完成联合测评，点亮你的职业星图。' }}</h2><p>{{ error ? '下方仅为视觉示意；恢复连接后可重新加载测评，生成你的专属职业分支。' : '请先完成六个智能体的联合测评。职业分支将来自你的测评结论，不会凭空生成。' }}</p><router-link class="wb-button primary" to="/multi-agent">开始联合测评 →</router-link><small>下方为视觉示意，不代表你的测评结果。</small><div class="graph-preview-map"><CareerStarMap :branches="previewBranches" :selected-index="previewSelected" preview center-action="开始联合测评" @select="previewSelected = $event" @center-click="router.push('/multi-agent')" /></div></section>
    <template v-else-if="reports.length">
      <section class="source-bar"><div><label for="report-select">测评来源</label><select id="report-select" v-model="reportId" :disabled="generating" @change="loadGraph"><option v-for="r in reports" :key="r.id" :value="String(r.id)">{{ r.name }}</option></select></div><span class="source-date">{{ selectedReport?.createdAt }}</span><button v-if="!graph" class="wb-button primary" :disabled="generating || graphLoading" @click="generate()">{{ generating ? '正在提取分支…' : '根据测评绘制星图' }}</button><template v-else><span class="saved-mark">已保存 · 刷新可恢复</span><button class="wb-button" :disabled="generating || graphLoading" @click="regenerate">{{ generating ? '正在重新提取…' : '↻ 重新生成星图' }}</button><button class="wb-button ghost" :disabled="generating || graphLoading" @click="clearGraph">清空星图</button></template></section>
      <section v-if="!graph" class="wb-empty"><span class="eyebrow">测评已就绪</span><h2>{{ generating ? '正在整理职业路径…' : '测评已就绪，开始探索可能性。' }}</h2><p>{{ generating ? '正在提取推荐方向、发展阶段和能力差距，请保持当前页面。' : '生成后，点击岗位节点查看测评依据、待补能力与行动建议。' }}</p></section>
      <section v-else class="atlas-layout">
        <div class="atlas-panel graph-star-panel">
          <div class="atlas-toolbar"><div><span class="eyebrow">图 01 · 以测评画像为中心</span><h2>{{ graph.center }}</h2></div><span class="graph-source-mark">联合测评生成</span></div>
          <CareerStarMap :branches="mapBranches" :selected-index="selectedMapIndex" :center-label="graph.center" center-subtitle="你的职业探索起点" :aria-label="`职业星图，优先展示${mapBranches.length}个方向`" @select="selectBranch" @center-click="active = null" />
          <div class="atlas-footer"><span>优先展示 {{ mapBranches.length }} 个方向 · 共 {{ graph.branches.length }} 条职业分支</span><span>点击方向查看依据与行动</span></div>
          <p class="figure-note">星线表达职业探索的连接，不代表晋升先后。{{ graph.branches.length > 4 ? '全部方向可在下方切换。' : '' }}能力详情来自联合测评报告。</p>
        </div>
        <aside class="insight-panel"><span class="eyebrow">分支解读</span><h2>{{ active?.name || '你的测评摘要' }}</h2><p>{{ active?.reason || graph.summary }}</p><template v-if="active"><h3>推荐依据 · 测评原文</h3><blockquote>{{ active.evidence }}</blockquote><template v-if="active.skills?.length"><h3>需要补齐的能力</h3><ul><li v-for="(s,i) in active.skills" :key="i">{{ s }}</li></ul></template><template v-if="active.actions?.length"><h3>从这些行动开始</h3><ol><li v-for="(a,i) in active.actions" :key="i">{{ a }}</li></ol></template><router-link class="wb-button primary" :to="{path:'/assistant',query:{prompt:consultPrompt}}">和智能体深入讨论 →</router-link></template><p v-else>点击星图或下方岗位按钮，查看推荐依据和下一步行动。</p><router-link class="wb-button" to="/growth">查看成长行动计划</router-link><small>AI 整理自联合测评，仅供职业探索参考，不代表录用或晋升承诺。</small></aside>
      </section>
      <section v-if="graph" class="branch-list" aria-label="职业分支列表"><button v-for="(branch,i) in graph.branches" :key="i" :class="[branch.kind,{selected:active===branch}]" :aria-pressed="active===branch" @click="selectBranch(i)"><span>{{ String(i+1).padStart(2,'0') }}</span>{{ branch.name }}<small>{{ kindLabel[branch.kind] }}</small></button></section>
    </template>
  </main>
</template>
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import CareerStarMap from '../components/CareerStarMap.vue'
import '../styles/careerWorkbench.css'
const route=useRoute()
const router=useRouter()
const reports=ref([]),reportId=ref(''),loading=ref(true),graphLoading=ref(false),generating=ref(false),error=ref('')
const graph=ref(null),active=ref(null)
const previewSelected=ref(0)
const previewBranches=[
  {id:'frontend',name:'前端开发',tagline:'创造可感知的数字世界',icon:'cpu',skills:['界面实现','工程能力']},
  {id:'product',name:'产品设计',tagline:'连接用户与未来',icon:'briefcase',skills:['需求分析','用户研究']},
  {id:'data',name:'数据分析',tagline:'用数据发现规律',icon:'trendUp',skills:['数据处理','业务洞察']},
  {id:'ai',name:'AI 应用',tagline:'与智能共创新可能',icon:'sparkle',skills:['AI 工具','场景落地']}
]
const kindLabel={target:'目标方向',promotion:'进阶路径',transfer:'迁移方向'}
const selectedReport=computed(()=>reports.value.find(r=>String(r.id)===reportId.value))
const mapBranches=computed(()=>graph.value?.branches.slice(0,4).map((branch,index)=>({
  ...branch,id:`branch-${index}`,tagline:kindLabel[branch.kind],icon:['cpu','briefcase','trendUp','sparkle'][index]
}))||[])
const selectedMapIndex=computed(()=>graph.value?.branches.indexOf(active.value)??-1)
const consultPrompt=computed(()=>`我完成了联合测评，正在探索「${active.value?.name}」。测评依据：${active.value?.evidence}。能力差距：${active.value?.skills.join('、')}。请结合我的画像细化行动：${active.value?.actions.join('；')}。`)
const headers=()=>{const token=localStorage.getItem('token')||'';return {Authorization:token.startsWith('Bearer ')?token:`Bearer ${token}`}}
let request=0
const read=res=>{if(res.data.code!==200)throw new Error(res.data.message||'读取失败');return res.data.data}
const loadReports=async()=>{
  loading.value=true;error.value=''
  try{reports.value=read(await axios.get('/api/report-graph/reports',{headers:headers()}))||[];const wanted=String(route.query.reportId||'');reportId.value=String(reports.value.find(r=>String(r.id)===wanted)?.id||reports.value[0]?.id||'');if(reportId.value)await loadGraph()}
  catch(e){error.value=e.message}finally{loading.value=false}
}
const loadGraph=async()=>{
  const current=++request;graphLoading.value=true;graph.value=null;active.value=null;error.value=''
  try{const data=read(await axios.get(`/api/report-graph/${reportId.value}`,{headers:headers()}));if(current===request)graph.value=data}
  catch(e){if(current===request)error.value=e.message}finally{if(current===request)graphLoading.value=false}
}
const generate=async(force=false)=>{
  if(generating.value)return
  const current=++request;generating.value=true;error.value=''
  try{const url=`/api/report-graph/${reportId.value}${force?'?force=true':''}`;const data=read(await axios.post(url,{},{headers:headers(),timeout:180000}));if(current!==request)return;graph.value=data}
  catch(e){if(current===request)error.value=e.message||'生成失败，请重试'}finally{if(current===request)generating.value=false}
}
// 清空星图：回到"生成之前"的状态（只删已保存的星图，不动测评本身），便于重新生成
const clearGraph = async () => {
  if (generating.value || graphLoading.value) return
  if (!confirm('清空这份测评已保存的星图？清空后页面会回到生成前的状态，可以重新生成。测评报告本身不受影响。')) return
  graphLoading.value = true; error.value = ''
  try {
    const res = await axios.delete(`/api/report-graph/${reportId.value}`, { headers: headers() })
    if (res.data.code !== 200) throw new Error(res.data.message || '清空失败')
    graph.value = null; active.value = null
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '清空星图失败，请重试'
  } finally {
    graphLoading.value = false
  }
}

// 重新生成：绕过服务端缓存重新提取并覆盖已保存星图（会调用一次 AI，先确认）
const regenerate=()=>{if(generating.value)return;if(!confirm('重新生成会用当前测评重新提取职业分支（约 1~3 分钟，会调用一次 AI），并覆盖已保存的星图。确定继续？'))return;generate(true)}
const selectBranch=i=>{active.value=graph.value.branches[i]}
onMounted(loadReports)
onUnmounted(()=>{++request})
</script>
<style scoped>
.scientific-atlas{--wb-accent:#315d78;--wb-border:#ded5c8;background:#f7f4ed}
.workbench-heading h1{font-size:30px;letter-spacing:0}
.source-bar{display:flex;align-items:center;gap:18px;background:#fff;border:1px solid var(--wb-border);padding:16px 20px;border-radius:10px;margin:22px 0}
.source-bar>div{flex:1;min-width:0}.source-bar label{display:block;font-size:11px;color:var(--wb-muted);margin-bottom:6px}
.source-bar select{width:100%;border:0;background:transparent;font:inherit;color:var(--wb-ink)}
.source-date,.saved-mark{font-size:12px;color:var(--wb-muted)}.saved-mark{color:#557544}
.source-bar>.wb-button,.source-bar .saved-mark{white-space:nowrap}
.atlas-layout{display:grid;grid-template-columns:minmax(0,1fr);gap:18px}
.atlas-panel{background:#fff;border:1px solid var(--wb-border);border-radius:12px;min-width:0;overflow:hidden}
.atlas-toolbar{padding:24px 24px 16px;display:flex;align-items:center;justify-content:space-between;gap:16px}
.atlas-toolbar h2{font-size:21px;margin:10px 0 0;overflow-wrap:anywhere}
.atlas-toolbar .eyebrow{color:#697586;font-size:10px;letter-spacing:1px}
.chart-reset{background:#fff;color:#536176;border:1px solid #d8dfe8;border-radius:7px;padding:9px 13px;cursor:pointer;white-space:nowrap}
.chart-reset:hover{background:#f2f5fa}
.atlas-legend{display:flex;gap:22px;flex-wrap:wrap;padding:0 24px 22px;color:#566174;font-size:12px}
.atlas-legend span{display:flex;align-items:center;gap:7px}
.atlas-legend span::before{content:'';width:11px;height:11px;border-radius:3px;background:#d9e2f4;border:1px solid #4773ca}
.atlas-legend .promotion::before{background:#e2f1d8;border-color:#81975e}.atlas-legend .transfer::before{background:#f9dade;border-color:#b76679}
.atlas-scroll{overflow-x:auto;border-top:1px solid #e9edf1;background:#fff}
.atlas-diagram{min-width:1100px}
.column-labels{display:grid;grid-template-columns:26% 32% 42%;border-bottom:1px solid #edf0f3;padding:14px 0;font-size:12px;color:#667281;text-align:center;background:#fafbfc}
.column-labels b{font-family:Georgia,serif;font-size:17px;color:#263448;margin-right:8px}
.atlas-canvas{width:100%}
.atlas-footer{border-top:1px solid #e9edf1;display:flex;gap:10px;justify-content:space-between;padding:14px 24px 8px;color:#667281;font-size:11px;flex-wrap:wrap}
.figure-note{font-size:11px;line-height:1.8;color:#77808c;margin:0;padding:0 24px 16px}
.insight-panel{background:#fff;border:1px solid var(--wb-border);border-radius:12px;padding:24px;min-width:0}
.insight-panel h2{font-size:21px;line-height:1.4;margin:12px 0;overflow-wrap:anywhere}
.insight-panel h3{font-size:13px;margin-top:22px}
.insight-panel p,.insight-panel li{font-size:13px;line-height:1.85;color:#59677b;overflow-wrap:anywhere}
.insight-panel blockquote{font-size:12px;line-height:1.8;color:#50617c;margin:12px 0;border-left:3px solid #86b9e0;padding:12px 16px;background:#f3f7fc}
.insight-panel ul,.insight-panel ol{padding-left:18px}
.insight-panel .wb-button{margin:12px 10px 0 0}
.insight-panel small{display:block;font-size:11px;line-height:1.8;color:var(--wb-muted);margin-top:20px}
.branch-list{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:10px;margin:18px 0}
.branch-list button{display:flex;align-items:center;gap:10px;border:1px solid var(--wb-border);border-left:3px solid #4773ca;padding:14px;border-radius:8px;background:#fff;color:var(--wb-ink);cursor:pointer;text-align:left;font:inherit;font-size:12px}
.branch-list button.promotion{border-left-color:#81975e}.branch-list button.transfer{border-left-color:#b76679}
.branch-list span{font:12px monospace;color:#6580a2}.branch-list small{margin-left:auto;color:#6a7a91;font-size:10px;white-space:nowrap}
.branch-list button:hover,.branch-list .selected{background:#f0f4fb;border-color:#4773ca}
.wb-empty small{display:block;margin-top:22px;color:var(--wb-muted)}
@media(min-width:1560px){.atlas-layout{grid-template-columns:minmax(840px,1fr) 300px}}
@media(max-width:900px){.source-bar{flex-wrap:wrap}.source-bar>div{flex-basis:100%}.workbench-heading h1{font-size:26px}.atlas-toolbar{padding:20px}.atlas-legend{padding-left:20px}}
.scientific-atlas .workbench-heading h1,.scientific-atlas .atlas-toolbar h2,.scientific-atlas .insight-panel h2{font-family:'Noto Serif SC','Source Han Serif SC',serif;color:#14314b}
.scientific-atlas .source-bar,.scientific-atlas .atlas-panel,.scientific-atlas .insight-panel{border-color:#ded5c8;background:#fffdf8}
.scientific-atlas .atlas-scroll{background:#fffdf8;border-top-color:#e4dac8}
.scientific-atlas .column-labels{background:#faf6ee;border-bottom-color:#e4dac8}
.scientific-atlas .wb-button.primary{border-color:#b89158;background:#f0dbb2;color:#19354c}
.scientific-atlas .insight-panel blockquote{border-left-color:#b89158;background:#faf5e9}
.graph-star-panel .atlas-toolbar{align-items:center;padding:17px 23px 15px;border-bottom:1px solid #e4dac8}
.graph-star-panel .atlas-toolbar h2{margin:4px 0 0;font-size:19px}
.graph-source-mark{flex:0 0 auto;padding:7px 10px;border:1px solid #d8c5a5;border-radius:30px;color:#8c6e45;background:#fbf6ea;font-size:10px}
.graph-empty{padding:42px 24px 30px}.graph-empty>small{margin:17px 0 0}.graph-preview-map{max-width:980px;margin:24px auto 0;border:1px solid #e1d3ba;border-radius:8px;overflow:hidden;text-align:left}
.graph-star-panel .figure-note{padding-top:4px}.graph-star-panel .atlas-footer{border-color:#e4dac8;background:#fffdf8}
@media(min-width:1400px){.atlas-layout{grid-template-columns:minmax(0,1fr) minmax(290px,320px)}}
@media(max-width:900px){.graph-source-mark{display:none}.graph-empty{padding:27px 13px}.graph-preview-map{margin-top:19px}}
</style>
