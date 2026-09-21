import test from 'node:test'
import assert from 'node:assert/strict'
import {buildCareerGraph} from './careerGraph.js'
test('同名岗位仍生成独立分支及技能连线',()=>{
 const graph={center:'前端',branches:[{name:'前端',kind:'target',skills:['Vue']},{name:'前端',kind:'promotion',skills:['Vue','架构']}]}
 const {nodes,links}=buildCareerGraph(graph)
 assert.equal(nodes.length,6);assert.equal(links.length,5)
 assert.equal(new Set(nodes.map(n=>n.id)).size,6)
 links.forEach(l=>{assert.ok(nodes.some(n=>n.id===l.source));assert.ok(nodes.some(n=>n.id===l.target))})
 assert.deepEqual(buildCareerGraph(graph),{nodes,links,width:840,height:380})
})
test('不为无数据岗位伪造职业分支',()=>{const {nodes,links}=buildCareerGraph({center:'前端',branches:[]});assert.equal(nodes.length,1);assert.equal(links.length,0)})

test('八分支分层布局不重叠，排序后仍保留原始索引',()=>{
 const graph={center:'职业方向',branches:Array.from({length:8},(_,i)=>({name:'长岗位名称'.repeat(12),kind:['transfer','target','promotion'][i%3],skills:['能力一','能力二','能力三','完整第四项']}))}
 const {nodes,links,height}=buildCareerGraph(graph)
 assert.equal(nodes.length,33);assert.equal(links.length,32)
 const branches=nodes.filter(n=>n.id.startsWith('branch-'))
 assert.equal(branches[0].branchIndex,1)
 for(let i=1;i<branches.length;i++) assert.ok(branches[i].y-branches[i-1].y>=132)
 nodes.forEach(n=>{assert.ok(n.y>=0 && n.y<=height);assert.ok(n.x>=0 && n.x<=840)})
 assert.ok(branches.every(n=>n.label.formatter.includes('…')))
 assert.equal(branches[0].name,graph.branches[1].name)
})

test('无技能分支不产生虚假能力节点',()=>{
 const {nodes,links}=buildCareerGraph({center:'设计',branches:[{name:'产品设计',kind:'target'}]})
 assert.equal(nodes.length,2);assert.equal(links.length,1)
})
