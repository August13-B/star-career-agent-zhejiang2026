import test from 'node:test'
import assert from 'node:assert/strict'
import {buildCareerGraph} from './careerGraph.js'
test('同名岗位仍生成独立分支及技能连线',()=>{
 const graph={center:'前端',branches:[{name:'前端',kind:'target',skills:['Vue']},{name:'前端',kind:'promotion',skills:['Vue','架构']}]}
 const {nodes,links}=buildCareerGraph(graph)
 assert.equal(nodes.length,6);assert.equal(links.length,5)
 assert.equal(new Set(nodes.map(n=>n.id)).size,6)
 links.forEach(l=>{assert.ok(nodes.some(n=>n.id===l.source));assert.ok(nodes.some(n=>n.id===l.target))})
 assert.deepEqual(buildCareerGraph(graph),{nodes,links})
})
test('不为无数据岗位伪造职业分支',()=>{const {nodes,links}=buildCareerGraph({center:'前端',branches:[]});assert.equal(nodes.length,1);assert.equal(links.length,0)})
