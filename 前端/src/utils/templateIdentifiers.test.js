import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

/**
 * 模板引用检查：模板里**直接调用**的函数必须在 `<script setup>` 里定义（或为 import/全局）。
 *
 * 回归背景：曾出现模板写了 `answeredQuestionsOf(item)` 但脚本里没定义 →
 * 渲染函数每次抛 `TypeError: _ctx.xxx is not a function` → 组件反复崩溃重渲染、
 * 页面卡在 loading（表现为"一直转圈、但秒数不动"），排查成本极高。
 */
const SRC = path.resolve(import.meta.dirname, '..')
// JS 关键字：v-for="x in y" / v-if="a in b" 之类会被误判为“函数调用”
const KEYWORDS = new Set([
  'in', 'of', 'if', 'else', 'return', 'typeof', 'instanceof', 'new', 'void', 'delete', 'await', 'yield',
  'function', 'case', 'switch', 'do', 'while', 'for', 'catch', 'throw', 'with', 'class', 'super', 'this'
])
const GLOBALS = new Set([
  'Math', 'Date', 'Number', 'String', 'Boolean', 'Array', 'Object', 'JSON', 'parseInt', 'parseFloat', 'isNaN',
  'encodeURIComponent', 'decodeURIComponent', 'console', 'window', 'document', 'location', 'crypto', 'URL', 'Set', 'Map', 'Promise'
])

function vueFiles(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) return vueFiles(full)
    return entry.name.endsWith('.vue') ? [full] : []
  })
}

/** 模板中「前面不是 . 或标识符字符」的调用名（排除 obj.method(...) / a?.b(...)） */
function calledInTemplate(template) {
  const names = new Set()
  for (const match of template.matchAll(/(^|[^.\w$])([A-Za-z_$][\w$]*)\s*\(/g)) names.add(match[2])
  return names
}

function definedInScript(script) {
  const names = new Set()
  for (const match of script.matchAll(/(?:const|let|var|function|async\s+function)\s+([A-Za-z_$][\w$]*)/g)) names.add(match[1])
  for (const match of script.matchAll(/(?:const|let|var)\s+([^\n]+)/g)) {
    for (const decl of match[1].matchAll(/(?:^|,)\s*([A-Za-z_$][\w$]*)\s*=/g)) names.add(decl[1])
  }
  for (const match of script.matchAll(/import\s*\{([^}]*)\}/g)) {
    for (const part of match[1].split(',')) {
      const name = part.trim().split(/\s+as\s+/).pop()
      if (name) names.add(name)
    }
  }
  for (const match of script.matchAll(/import\s+([A-Za-z_$][\w$]*)\s+from/g)) names.add(match[1])
  return names
}

test('所有 .vue 模板调用的函数都在脚本里定义或已导入', () => {
  const problems = []
  for (const file of vueFiles(SRC)) {
    const src = fs.readFileSync(file, 'utf8')
    const template = src.slice(src.indexOf('<template>'), src.indexOf('</template>'))
    const script = src.slice(src.indexOf('<script'), src.indexOf('</script>'))
    if (!template || !script) continue
    const defined = definedInScript(script)
    const missing = [...calledInTemplate(template)].filter((name) => !defined.has(name) && !GLOBALS.has(name) && !KEYWORDS.has(name))
    if (missing.length) problems.push(`${path.relative(SRC, file)} → 模板调用但未定义: ${missing.join(', ')}`)
  }
  assert.deepEqual(problems, [], `发现 ${problems.length} 个模板引用错误:\n${problems.join('\n')}`)
})
