<template>
  <main class="career-workbench">
    <header class="workbench-heading"><div><span class="eyebrow">CAREER ATLAS / 02</span><h1>让下一步，<em>清晰可见。</em></h1><p>从联合测评出发，连接职业方向、能力差距与行动。</p></div><router-link class="wb-button" to="/multi-agent">返回联合测评</router-link></header>
    <nav class="journey" aria-label="职业规划流程"><router-link to="/multi-agent"><b>01</b> 联合测评</router-link><span class="current"><b>02</b> 职业星图</span><router-link to="/growth"><b>03</b> 行动计划</router-link></nav>
    <p v-if="error" class="wb-notice" role="alert">{{ error }} <button @click="loadReports">重新加载</button></p>
    <section v-if="loading" class="wb-empty" aria-live="polite"><h2>正在读取联合测评…</h2></section>
    <section v-else-if="!reports.length && !error" class="wb-empty"><span class="eyebrow">START WITH YOU</span><h2>先认识自己，再展开职业宇宙。</h2><p>请先完成六个智能体的联合测评。职业分支将来自你的测评结论，不会凭空生成。</p><router-link class="wb-button primary" to="/multi-agent">开始联合测评 →</router-link><small>未完整完成的报告不会用作星图依据。</small></section>
    <template v-else-if="reports.length">
      <section class="source-bar"><div><label for="report-select">测评来源</label><select id="report-select" v-model="reportId" :disabled="generating" @change="loadGraph"><option v-for="r in reports" :key="r.id" :value="String(r.id)">{{ r.name }}</option></select></div><span class="source-date">{{ selectedReport?.createdAt }}</span><button v-if="!graph" class="wb-button primary" :disabled="generating || graphLoading" @click="generate">{{ generating ? '正在提取分支…' : '根据测评绘制星图' }}</button><span v-else class="saved-mark">已保存 · 刷新可恢复</span></section>
      <section v-if="!graph" class="wb-empty"><span class="eyebrow">ASSESSMENT READY</span><h2>{{ generating ? '正在连接你的职业坐标…' : '测评已就绪，开始探索可能性。' }}</h2><p>{{ generating ? '正在提取推荐方向、发展阶段和能力差距，请保持当前页面。' : '生成后，点击岗位节点查看测评依据、待补能力与行动建议。' }}</p></section>
      <section v-else class="atlas-layout">
        <div class="atlas-panel"><div class="atlas-toolbar"><div><span class="eyebrow">YOUR CAREER CONSTELLATION</span><h2>{{ graph.center }}</h2></div><button class="chart-reset" @click="render">重置视图</button></div><div class="atlas-legend"><span>蓝 · 目标方向</span><span>青 · 进阶路径</span><span>金 · 迁移方向</span><span>小节点 · 待补能力</span></div><div ref="chartEl" class="atlas-canvas" role="img" :aria-label="`职业星图，${graph.branches.length}条分支，可通过下方按钮选择岗位`"></div><div class="atlas-footer"><span>{{ graph.branches.length }} 条分支 / {{ skillCount }} 个能力节点</span><span>拖拽 · 缩放 · 点击查看</span></div></div>
        <aside class="insight-panel"><span class="eyebrow">PATH INSIGHT</span><h2>{{ active?.name || '你的测评摘要' }}</h2><p>{{ active?.reason || graph.summary }}</p><template v-if="active"><h3>推荐依据 · 测评原文</h3><blockquote>{{ active.evidence }}</blockquote><h3>需要补齐的能力</h3><ul><li v-for="(s,i) in active.skills" :key="i">{{ s }}</li></ul><h3>从这些行动开始</h3><ol><li v-for="(a,i) in active.actions" :key="i">{{ a }}</li></ol><router-link class="wb-button primary" :to="{path:'/',query:{prompt:consultPrompt}}">和智能体深入讨论 →</router-link></template><p v-else>点击星图或下方岗位按钮，查看推荐依据和下一步行动。</p><router-link class="wb-button" to="/growth">查看成长行动计划</router-link><small>AI 整理自联合测评，仅供职业探索参考，不代表录用或晋升承诺。</small></aside>
      </section>
      <section v-if="graph" class="branch-list" aria-label="职业分支列表"><button v-for="(branch,i) in graph.branches" :key="i" :class="{selected:active===branch}" @click="selectBranch(i)"><span>{{ String(i+1).padStart(2,'0') }}</span>{{ branch.name }}<small>{{ kindLabel[branch.kind] }}</small></button></section>
    </template>
  </main>
</template>
<script setup>
import { ref, computed, shallowRef, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import * as echarts from 'echarts'
import { buildCareerGraph } from '../utils/careerGraph'
import '../styles/careerWorkbench.css'
const route=useRoute()
const reports=ref([]),reportId=ref(''),loading=ref(true),graphLoading=ref(false),generating=ref(false),error=ref('')
const graph=ref(null),active=ref(null),chartEl=ref(null),chart=shallowRef(null)
const kindLabel={target:'目标方向',promotion:'进阶路径',transfer:'迁移方向'}
const selectedReport=computed(()=>reports.value.find(r=>String(r.id)===reportId.value))
const skillCount=computed(()=>graph.value?.branches.reduce((n,b)=>n+b.skills.slice(0,3).length,0)||0)
const consultPrompt=computed(()=>`我完成了联合测评，正在探索「${active.value?.name}」。测评依据：${active.value?.evidence}。能力差距：${active.value?.skills.join('、')}。请结合我的画像细化行动：${active.value?.actions.join('；')}。`)
const headers=()=>{const token=localStorage.getItem('token')||'';return {Authorization:token.startsWith('Bearer ')?token:`Bearer ${token}`}}
let request=0,observer
const read=res=>{if(res.data.code!==200)throw new Error(res.data.message||'读取失败');return res.data.data}
const loadReports=async()=>{
  loading.value=true;error.value=''
  try{reports.value=read(await axios.get('/api/report-graph/reports',{headers:headers()}))||[];const wanted=String(route.query.reportId||'');reportId.value=String(reports.value.find(r=>String(r.id)===wanted)?.id||reports.value[0]?.id||'');if(reportId.value)await loadGraph()}
  catch(e){error.value=e.message}finally{loading.value=false;await nextTick();render()}
}
const loadGraph=async()=>{
  const current=++request;graphLoading.value=true;graph.value=null;active.value=null;error.value='';chart.value?.dispose();chart.value=null;observer?.disconnect()
  try{const data=read(await axios.get(`/api/report-graph/${reportId.value}`,{headers:headers()}));if(current===request)graph.value=data}
  catch(e){if(current===request)error.value=e.message}finally{if(current===request){graphLoading.value=false;await nextTick();render()}}
}
const generate=async()=>{
  if(generating.value)return
  const current=++request;generating.value=true;error.value=''
  try{const data=read(await axios.post(`/api/report-graph/${reportId.value}`,{},{headers:headers(),timeout:180000}));if(current!==request)return;graph.value=data;await nextTick();render()}
  catch(e){if(current===request)error.value=e.message||'生成失败，请重试'}finally{if(current===request)generating.value=false}
}
const selectBranch=i=>{active.value=graph.value.branches[i]}
const render=()=>{
  if(!chartEl.value||!graph.value)return
  observer?.disconnect();chart.value?.dispose();chart.value=echarts.init(chartEl.value)
  const {nodes,links}=buildCareerGraph(graph.value)
  chart.value.setOption({animationDuration:450,tooltip:{show:false},series:[{type:'graph',layout:'none',left:'14%',right:'14%',top:'14%',bottom:'18%',roam:true,scaleLimit:{min:.5,max:2.5},data:nodes,links,label:{show:true,color:'#dce7fa',fontSize:12,position:'bottom',distance:9,width:110,overflow:'break'},lineStyle:{color:'#506884',width:1.5,curveness:.08,opacity:.65},emphasis:{focus:'adjacency',lineStyle:{width:3,opacity:1}}}]})
  chart.value.on('click',p=>{if(Number.isInteger(p.data?.branchIndex))selectBranch(p.data.branchIndex)})
  observer=new ResizeObserver(()=>chart.value?.resize());observer.observe(chartEl.value)
}
onMounted(loadReports)
onUnmounted(()=>{++request;observer?.disconnect();chart.value?.dispose()})
</script>
<style scoped>
.source-bar{display:flex;align-items:center;gap:18px;background:#fff;border:1px solid var(--wb-border);padding:18px 22px;border-radius:14px;margin:24px 0}.source-bar>div{flex:1;min-width:0}.source-bar label{display:block;font-size:11px;color:var(--wb-muted);margin-bottom:6px}.source-bar select{width:100%;border:0;background:transparent;font:inherit;color:var(--wb-ink)}.source-date,.saved-mark{font-size:12px;color:var(--wb-muted)}.saved-mark{color:#197065}.atlas-layout{display:grid;grid-template-columns:minmax(0,1fr) 290px;gap:18px}.atlas-panel{background:#111d31;border-radius:18px;color:#e6edfa;min-width:0;background-image:radial-gradient(#26374e 1px,transparent 1px);background-size:24px 24px;overflow:hidden}.atlas-toolbar{padding:24px;display:flex;align-items:center;justify-content:space-between}.atlas-toolbar h2{font-size:22px;margin:10px 0 0}.atlas-toolbar .eyebrow{color:#91a6c6;font-size:10px}.chart-reset{background:#21334c;color:#dce7fa;border:1px solid #40536e;border-radius:8px;padding:10px;cursor:pointer}.atlas-legend{display:flex;gap:14px;flex-wrap:wrap;padding:0 24px;color:#aabbd3;font-size:11px}.atlas-canvas{width:100%;height:530px}.atlas-footer{border-top:1px solid #293b54;display:flex;gap:10px;justify-content:space-between;padding:16px 24px;color:#91a6c6;font-size:11px}.insight-panel{background:white;border:1px solid var(--wb-border);border-radius:18px;padding:24px;min-width:0}.insight-panel h2{font-size:23px;line-height:1.4;margin:14px 0}.insight-panel h3{font-size:13px;margin-top:24px}.insight-panel p,.insight-panel li{font-size:13px;line-height:1.85;color:#59677b}.insight-panel blockquote{font-size:12px;line-height:1.8;color:#50617c;margin:12px 0;border-left:3px solid #5a82e8;padding:4px 12px;background:#f5f8fe}.insight-panel ul,.insight-panel ol{padding-left:18px}.insight-panel .wb-button{margin-top:12px;display:flex;justify-content:center}.insight-panel small{display:block;font-size:11px;line-height:1.8;color:var(--wb-muted);margin-top:22px}.branch-list{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:10px;margin:20px 0}.branch-list button{display:flex;align-items:center;gap:10px;border:1px solid var(--wb-border);padding:16px;border-radius:10px;background:#fff;color:var(--wb-ink);cursor:pointer;text-align:left}.branch-list span{font:12px monospace;color:#6580a2}.branch-list small{margin-left:auto;color:#6a7a91;font-size:10px}.branch-list .selected{border-color:var(--wb-accent);background:#edf3ff}.wb-empty small{display:block;margin-top:22px;color:var(--wb-muted)}
@media(max-width:1250px){.atlas-layout{grid-template-columns:1fr}.source-bar{flex-wrap:wrap}.atlas-canvas{height:480px}}
</style>
