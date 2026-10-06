export function trainingHeaders(token = localStorage.getItem('token')) {
  if (!token) return { 'Content-Type': 'application/json' }
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token.replace(/^Bearer\s+/i, '')}` }
}

export async function trainingRequest(path, options = {}) {
  const response = await fetch(`/api/training${path}`, {
    ...options,
    headers: trainingHeaders(),
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })
  const result = await response.json().catch(() => null)
  if (!response.ok || result?.code !== 10001) {
    const error = new Error(response.status === 401 ? '登录已失效，请重新登录' : (result?.message || '训练请求失败，请稍后重试'))
    error.status = response.status
    error.code = result?.data?.errorCode
    throw error
  }
  return result.data
}

export function newTrainingRequestId() {
  return crypto.randomUUID()
}

export function trainingBusy(run) {
  return run?.status === 'queued' || run?.status === 'running'
}

export function trainingStatus(status) {
  return ({ active: '进行中', scoring: '正在评价', completed: '已完成', review_required: '评分待复核', canceled: '已取消' })[status] || status
}
