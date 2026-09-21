// 网络分片与 SSE 事件不一一对应；缓存完整事件后再交给业务层解析。
export async function* readSseData(body) {
  if (!body) throw new Error('服务器未返回流式响应')
  const reader = body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let data = []
  const consumeLine = (line) => {
    if (line === '') {
      const event = data.length ? data.join('\n') : null
      data = []
      return event
    }
    if (line === 'data') data.push('')
    else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
    // 心跳注释及 event/id/retry 元数据不进入正文。
    return null
  }
  try {
    while (true) {
      const { done, value } = await reader.read()
      buffer += done ? decoder.decode() : decoder.decode(value, { stream: true })
      let end
      while ((end = buffer.search(/[\r\n]/)) !== -1) {
        // CRLF 可能恰好分在两次读取中，先保留末尾 CR。
        if (!done && buffer[end] === '\r' && end === buffer.length - 1) break
        const separatorLength = buffer[end] === '\r' && buffer[end + 1] === '\n' ? 2 : 1
        const event = consumeLine(buffer.slice(0, end))
        buffer = buffer.slice(end + separatorLength)
        if (event !== null) yield event
      }
      if (done) {
        // 兼容最后一帧后直接关闭连接、未补空行的上游。
        if (buffer) consumeLine(buffer)
        const event = consumeLine('')
        if (event !== null) yield event
        return
      }
    }
  } finally {
    await reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}
