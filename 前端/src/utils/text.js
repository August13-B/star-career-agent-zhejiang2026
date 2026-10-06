/**
 * 训练对话按「纯文本」展示：把 Markdown 标记去掉，避免界面上直接出现
 * `**加粗**`、`### 标题`、`- 项` 这类原始符号（产品要求：不要直接出 Markdown 格式）。
 *
 * 只做「去标记」，不做富文本渲染；换行等排版结构保留。
 */
export function stripMarkdown(value) {
  if (value === null || value === undefined) return ''
  let text = String(value)

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
