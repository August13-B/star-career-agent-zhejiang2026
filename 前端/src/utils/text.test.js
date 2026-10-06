import test from 'node:test'
import assert from 'node:assert/strict'
import { stripMarkdown } from './text.js'

test('去掉加粗与斜体标记', () => {
  assert.equal(stripMarkdown('**先核对需求**再实现'), '先核对需求再实现')
  assert.equal(stripMarkdown('重点：*并发*与__回退__'), '重点：并发与回退')
})

test('去掉标题、引用、列表与分隔线标记', () => {
  assert.equal(stripMarkdown('### 追问\n你的排查步骤？'), '追问\n你的排查步骤？')
  assert.equal(stripMarkdown('> 提示：先说结论'), '提示：先说结论')
  assert.equal(stripMarkdown('- 明确范围\n- 明确负责人'), '明确范围\n明确负责人')
  assert.equal(stripMarkdown('1. 澄清诉求\n2. 资源与范围'), '澄清诉求\n资源与范围')
  assert.equal(stripMarkdown('上一段\n\n---\n\n下一段'), '上一段\n\n下一段')
})

test('去掉代码围栏与链接图片标记，保留可读内容', () => {
  assert.equal(stripMarkdown('```json\n{"a":1}\n```'), '{"a":1}')
  assert.equal(stripMarkdown('见 `grow_task` 字段'), '见 grow_task 字段')
  assert.equal(stripMarkdown('[评审材料](https://example.com)已上传'), '评审材料已上传')
  assert.equal(stripMarkdown('![示意图](a.png)'), '示意图')
})

test('表格与删除线', () => {
  assert.equal(stripMarkdown('| 维度 | 分数 |\n| --- | --- |\n| 沟通 | 80 |'), '维度 分数\n沟通 80')
  assert.equal(stripMarkdown('~~无效~~有效'), '无效有效')
})

test('保留换行结构且不误伤正常文本', () => {
  assert.equal(stripMarkdown('第一行\n第二行'), '第一行\n第二行')
  assert.equal(stripMarkdown('转化率 报名/意向 = 21.3%'), '转化率 报名/意向 = 21.3%')
  assert.equal(stripMarkdown('时间 3 * 4 = 12 秒'), '时间 3 * 4 = 12 秒')
  assert.equal(stripMarkdown('这是虚构练习_不要当成真实履历'), '这是虚构练习_不要当成真实履历')
})

test('空值与无标记文本', () => {
  assert.equal(stripMarkdown(null), '')
  assert.equal(stripMarkdown(undefined), '')
  assert.equal(stripMarkdown(''), '')
  assert.equal(stripMarkdown('普通回答，没有标记。'), '普通回答，没有标记。')
})
