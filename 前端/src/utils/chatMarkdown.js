import { unescapeNewlines } from './text.js'

const escapeHtml = (text) => text.replace(/&/g, '&amp;').replace(/</g, '&lt;')
  .replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;')

function inline(text) {
  // 代码片段不再参与加粗等替换；所有模型输出先转义，禁止注入 HTML。
  return text.split(/(`[^`]+`)/g).map(part => {
    if (part.startsWith('`') && part.endsWith('`')) {
      return `<code class="md-inline-code">${escapeHtml(part.slice(1, -1))}</code>`
    }
    return escapeHtml(part).replace(/\*\*(.+?)\*\*/g, '<strong class="md-bold">$1</strong>')
      .replace(/__(.+?)__/g, '<strong class="md-bold">$1</strong>')
  }).join('')
}

export function renderChatMarkdown(text) {
  if (!text) return ''
  // 平台偶尔把换行转义成字面量 `\n` 下发 → 先还原（围栏代码块内的字面量保留，那是代码内容）
  const restored = String(text).split(/(```[\s\S]*?(?:```|$))/g)
    .map((part, index) => (index % 2 === 1 ? part : unescapeNewlines(part))).join('')
  const html = []
  let paragraph = []
  let list = null
  let code = null
  const flushParagraph = () => {
    if (paragraph.length) html.push(`<p class="md-paragraph">${paragraph.map(inline).join('<br>')}</p>`)
    paragraph = []
  }
  const closeList = () => { if (list) html.push(`</${list}>`); list = null }
  const flushCode = () => html.push(`<pre class="md-pre"><code class="md-code-block">${escapeHtml(code.join('\n'))}</code></pre>`)
  for (const original of restored.replace(/\r\n?/g, '\n').split('\n')) {
    if (/^\s*```/.test(original)) {
      flushParagraph(); closeList()
      if (code === null) code = []
      else { flushCode(); code = null }
      continue
    }
    if (code !== null) { code.push(original); continue }
    // 兼容旧记录里丢失换行后的“正文###中文标题-**要点**”。不改动 C#、URL 锚点或代码。
    const legacy = !original.includes('`') && /[^#\n]#{3,6}[\u3400-\u9fff]/.test(original)
    const lines = legacy ? original.replace(/([^#\n])(?=#{3,6}[\u3400-\u9fff])/g, '$1\n')
      .replace(/([^\n])(?=-\*\*[^*\n]+\*\*)/g, '$1\n').split('\n') : [original]
    for (const line of lines) {
      if (!line.trim()) { flushParagraph(); closeList(); continue }
      const heading = line.match(/^ {0,3}(#{1,6})[ \t]+(.+?)\s*#*$/)
        || line.match(/^ {0,3}(#{2,6})([\u3400-\u9fff].*)$/)
      if (heading) {
        flushParagraph(); closeList()
        const level = heading[1].length
        html.push(`<h${level} class="md-h${level}">${inline(heading[2])}</h${level}>`)
        continue
      }
      const item = line.match(/^\s*([-*+]|\d+[.)])\s+(.+)$/)
        || (legacy && line.match(/^\s*(-)(\*\*.+)$/))
      if (item) {
        flushParagraph()
        const kind = /^\d/.test(item[1]) ? 'ol' : 'ul'
        if (list !== kind) { closeList(); html.push(`<${kind} class="md-list">`); list = kind }
        html.push(`<li>${inline(item[2])}</li>`)
      } else {
        closeList(); paragraph.push(line)
      }
    }
  }
  flushParagraph(); closeList()
  if (code !== null) flushCode()
  return html.join('')
}
