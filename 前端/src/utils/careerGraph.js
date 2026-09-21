// 稳定 ID 保证同名岗位、技能不会合并或吞掉连线。
export function buildCareerGraph(graph) {
  const nodes=[{id:'center',name:graph.center,x:0,y:0,symbolSize:68,itemStyle:{color:'#5485ff',borderColor:'#a9c4ff',borderWidth:3},label:{fontSize:14,fontWeight:700}}],links=[]
  const branches=Array.isArray(graph.branches)?graph.branches:[]
  const colors={target:'#79a4ff',promotion:'#63c9bb',transfer:'#e6bd78'}
  branches.forEach((b,i)=>{
    const angle=-Math.PI/2+i*Math.PI*2/branches.length,id=`branch-${i}`
    nodes.push({id,name:b.name,branchIndex:i,x:Math.cos(angle)*220,y:Math.sin(angle)*220,symbolSize:36,itemStyle:{color:colors[b.kind]||colors.target}})
    links.push({source:'center',target:id})
    ;(Array.isArray(b.skills)?b.skills:[]).slice(0,3).forEach((s,j)=>{
      const theta=angle+(j-(Math.min(b.skills.length,3)-1)/2)*.24,skillId=`skill-${i}-${j}`
      nodes.push({id:skillId,name:s,branchIndex:i,x:Math.cos(theta)*365,y:Math.sin(theta)*365,symbolSize:9,label:{fontSize:10,color:'#91a6c6',width:85},itemStyle:{color:colors[b.kind]||colors.target}})
      links.push({source:id,target:skillId,lineStyle:{type:'dashed',opacity:.4}})
    })
  })
  return {nodes,links}
}
