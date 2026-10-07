/**
 * 训练对话按「纯文本」展示：把 Markdown 标记去掉，避免界面上直接出现
 * `**加粗**`、`### 标题`、`- 项` 这类原始符号（产品要求：不要直接出 Markdown 格式）。
 *
 * 只做「去标记」，不做富文本渲染；换行等排版结构保留。
 */
export function stripMarkdown(value) {
  if (value === null || value === undefined) return ''
  // 平台可能把换行转义成字面量 \n 下发：先还原，再按纯文本展示
  let text = unescapeNewlines(value)

  // 代码块/行内代码：保留内容，去掉围栏与反引号
  text = text.replace(/```[^\n]*\n?([\s\S]*?)```/g, '$1')
  text = text.replace(/`([^`\n]*)`/g, '$1')
  // 图片/链接：保留可读文案
  text = text.replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
  text = text.replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
  // 标题、引用、分隔线
  text = text.replace(/^[ \t]{0,3}#{1,6}[ \t]*/gm, '')
  text = text.replace(/^[ \t]{0,3}>[ \t]?/gm, '')
  text = text.replace(/^[ \t]{0,3}(?:[-*_][ \t]*){3,}$/gm, '')
  // 无序 / 有序列表标记
  text = text.replace(/^[ \t]{0,3}[-*+][ \t]+/gm, '')
  text = text.replace(/^[ \t]{0,3}\d+[.)][ \t]+/gm, '')
  // 加粗 / 斜体 / 删除线
  text = text.replace(/(\*\*|__)([\s\S]*?)\1/g, '$2')
  text = text.replace(/(^|[^*\w])\*([^*\n]{1,200})\*(?!\*)/g, '$1$2')
  text = text.replace(/(^|[^_\w])_([^_\n]{1,200})_(?!_)/g, '$1$2')
  text = text.replace(/~~([\s\S]*?)~~/g, '$1')
  // 表格：分隔行整行去掉，竖线视作空格；连续空白压缩为单个空格
  text = text.replace(/^[ \t]{0,3}\|?[ \t]*:?-{2,}:?[ \t]*(\|[ \t]*:?-{2,}:?[ \t]*)*\|?[ \t]*\n?/gm, '')
  text = text.replace(/\|/g, ' ')
  text = text.replace(/[ \t]{2,}/g, ' ')

  // 收尾：行首尾空白、连续空行
  text = text.replace(/^[ \t]+|[ \t]+$/gm, '')
  text = text.replace(/\n{3,}/g, '\n\n')
  return text.trim()
}

/**
 * 平台偶尔把换行“转义”后下发：正文里出现的是**字面量** `\n`（两个字符），
 * 于是聊天窗口直接显示「……行情。\n\n### 一、……」而不是换行。
 *
 * 这里把常见转义序列还原成真实字符：
 *   `\n` / `\r\n` / `\r` → 换行    `\t` → 制表符    `\"` → 双引号    `\\` → 反斜杠
 * 其它未知转义**原样保留**（如 C# 的 `\d`、正则 `\w`、Windows 路径 `C:\Users` 不受影响）。
 */
export function unescapeNewlines(value) {
  if (value === null || value === undefined) return ''
  let text = String(value)
  if (!text.includes('\\')) return text
  // 平台偶尔下发两层转义（\\n）→ 反复还原到稳定，上限 3 轮
  for (let pass = 0; pass < 3; pass++) {
    const next = unescapeOnce(text)
    if (next === text) return next
    text = next
  }
  return text
}

function unescapeOnce(text) {
  let out = ''
  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    if (ch !== '\\' || i + 1 >= text.length) { out += ch; continue }
    const next = text[++i]
    if (next === 'n' || next === 'r') {
      // \r\n 视作一个换行
      if (next === 'r' && text[i + 1] === '\\' && text[i + 2] === 'n') i += 2
      out += '\n'
    } else if (next === 't') {
      out += '\t'
    } else if (next === '"') {
      out += '"'
    } else if (next === '\\') {
      out += '\\'
    } else {
      out += '\\' + next
    }
  }
  return out
}
