import axios from 'axios'

/**
 * 画像「自动更新」相关的前端工具（Stage 3/4）：
 * - fetchLatestChange()      拉取最近一次画像变更（个人中心展示 + 操作后提示都用它）
 * - syncReportToProfile(id)  把一份联合测评报告同步到能力画像（幂等，可重复调用）
 * - notifyProfileChange()    操作完成后调用：有新变化就弹一条轻提示
 *
 * 注意：提示文案只描述「发生了什么」（来源 + 各维度变化），不暴露评分机制。
 */

export const DIM_NAMES = {
  education: '学历背景',
  internship: '实习经历',
  professional: '专业技能',
  certificate: '证书资质',
  innovation: '创新能力',
  learning: '学习能力',
  pressure: '抗压能力',
  communication: '沟通能力',
  problem_solving: '问题解决',
  teamwork: '团队协作'
}

const authHeaders = () => {
  const token = localStorage.getItem('token') || ''
  return { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}

/** 已提示过的变更时间戳（避免重复提示） */
const NOTIFIED_KEY = 'profileChangeNotifiedAt'

/** 各维度变化摘要：如「专业技能 60→72，沟通能力 66→70」。 */
export const describeChange = (change) => {
  const deltas = change?.deltas || {}
  return Object.entries(deltas)
    .map(([key, value]) => `${DIM_NAMES[key] || key} ${value.before}→${value.after}`)
    .join('，')
}

export async function fetchLatestChange() {
  try {
    const res = await axios.get('/api/profile/change/latest', { headers: authHeaders() })
    return res.data?.code === 200 ? res.data.data : null
  } catch (e) {
    return null
  }
}

export async function syncReportToProfile(reportId) {
  if (!reportId) return null
  try {
    const res = await axios.post(`/api/profile/score/report/${reportId}`, {}, {
      headers: authHeaders(),
      timeout: 180000
    })
    return res.data?.code === 200 ? res.data.data : null
  } catch (e) {
    return null
  }
}

/** 底部轻提示（内联样式，不依赖全局 CSS；3.5 秒后自动消失）。 */
export function toast(message, { actionText, onAction } = {}) {
  if (!message || typeof document === 'undefined') return
  const el = document.createElement('div')
  el.setAttribute('role', 'status')
  el.style.cssText = [
    'position:fixed', 'right:22px', 'bottom:22px', 'z-index:9999',
    'display:flex', 'align-items:center', 'gap:10px',
    'max-width:min(420px,86vw)', 'padding:12px 16px',
    'background:#FFFFFF', 'border:1px solid #DBEAFE', 'border-radius:12px',
    'box-shadow:0 8px 26px rgba(15,23,42,.14)',
    'font-size:13px', 'line-height:1.6', 'color:#334155',
    'font-family:-apple-system,BlinkMacSystemFont,"Microsoft YaHei",sans-serif'
  ].join(';')
  const dot = document.createElement('i')
  dot.style.cssText = 'width:7px;height:7px;border-radius:50%;background:#4A90E2;flex:0 0 auto'
  const text = document.createElement('span')
  text.textContent = message
  el.append(dot, text)
  if (actionText) {
    const action = document.createElement('button')
    action.textContent = actionText
    action.style.cssText = 'border:0;background:none;color:#1D4ED8;font-weight:600;cursor:pointer;font-size:13px;padding:0 2px'
    action.onclick = () => { if (onAction) onAction(); el.remove() }
    el.appendChild(action)
  }
  document.body.appendChild(el)
  setTimeout(() => el.remove(), 3500)
}

/**
 * 操作完成后调用：如果画像最近有变化就提示一条，并跳到个人中心。
 * @returns 变更对象（没有变化或未登录时为 null）
 */
export async function notifyProfileChange(router) {
  const change = await fetchLatestChange()
  if (change?.available && change.source) {
    // 同一份变更只提示一次（避免切页/重载重复弹）
    const stamp = String(change.updatedAt || '')
    try {
      if (stamp && sessionStorage.getItem(NOTIFIED_KEY) === stamp) return change
      if (stamp) sessionStorage.setItem(NOTIFIED_KEY, stamp)
    } catch (e) { /* 隐私模式下 sessionStorage 不可用时忽略 */ }
    const summary = describeChange(change)
    toast(
      `画像已更新 · 来源：${change.sourceLabel || change.source}${summary ? `（${summary}）` : ''}`,
      router ? { actionText: '查看画像 →', onAction: () => router.push('/profile') } : undefined
    )
  }
  return change
}
