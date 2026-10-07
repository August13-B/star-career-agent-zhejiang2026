import test from 'node:test'
import assert from 'node:assert/strict'
import { renderChatMarkdown } from './chatMarkdown.js'

test('标题、段落、加粗和列表分别排版，不展示标题井号', () => {
  const html = renderChatMarkdown('### 前端方向\n\n结合你的画像。\n\n- **技能**：Vue\n- 项目经验\n\n1. 学习\n2. 实践')
  assert.match(html, /<h3[^>]*>前端方向<\/h3>/)
  assert.match(html, /<ul[^>]*><li><strong[^>]*>技能/)
  assert.match(html, /<ol[^>]*><li>学习<\/li><li>实践/)
  assert.ok(!html.includes('###'))
})

test('兼容旧消息中的粘连中文标题，不删除普通井号', () => {
  const html = renderChatMarkdown('你好：###我能做什么-**职业定位**：建议。###补充信息-**阶段**：大二')
  assert.ok(!html.includes('###'))
  assert.match(html, /<h3[^>]*>我能做什么/)
  assert.match(renderChatMarkdown('学习 C#，访问 /docs#intro'), /C#，访问 \/docs#intro/)
})

test('模型 HTML 和实体均转义，代码中的 Markdown 保持原样', () => {
  const html = renderChatMarkdown('<img src=x onerror=alert(1)> &lt;script&gt;\n\n`### 标题`\n\n```js\n# 注释\n**not bold**\n<script>bad()</script>\n```')
  assert.ok(!html.includes('<img'))
  assert.ok(!html.includes('<script>'))
  assert.ok(html.includes('&amp;lt;script&amp;gt;'))
  assert.match(html, /<code[^>]*>### 标题<\/code>/)
  assert.ok(html.includes('**not bold**'))
})

test('流式半截代码块与各级标题可安全渲染', () => {
  assert.match(renderChatMarkdown('```js\nconst x = 1'), /<pre[^>]*><code[^>]*>const x = 1<\/code><\/pre>/)
  for (let level = 1; level <= 6; level++) assert.match(renderChatMarkdown('#'.repeat(level) + ' 标题'), new RegExp(`<h${level}`))
})

test('平台下发的字面量 \\n 被还原成真实换行（不再显出一串 \\n）', () => {
  const raw = '张睿你好，先给结论：以你目前的画像。\\n\\n### 一、你的评分说明什么\\n- 能冲的位置：入门级开发。\\n- 卡点：问题解决 50。'
  const html = renderChatMarkdown(raw)
  assert.ok(!html.includes('\\n'), '正文里不应再出现字面量反斜杠 n')
  assert.match(html, /<h3[^>]*>一、你的评分说明什么<\/h3>/)
  assert.match(html, /<ul[^>]*><li>能冲的位置/)
})

test('围栏代码块内的字面量 \\n 保留（那是代码内容）', () => {
  const html = renderChatMarkdown('说明：\\n\\n```js\nconsole.log("a\\nb")\n```')
  // 代码块内容保留字面量 \\n；引号被 HTML 转义为 &quot;
  assert.match(html, /<pre[^>]*><code[^>]*>console\.log\(&quot;a\\nb&quot;\)<\/code><\/pre>/)
  assert.match(html, /<p[^>]*>说明：<\/p>/)
})
