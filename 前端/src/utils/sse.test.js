import test from 'node:test'
import assert from 'node:assert/strict'
import { readSseData } from './sse.js'

const encode = (text) => new TextEncoder().encode(text)
async function collect(body) {
  const events = []
  for await (const event of readSseData(body)) events.push(event)
  return events
}

test('中文、Emoji、JSON 和 CRLF 逐字节分片后仍完整', async () => {
  const expected = ['{"data":"你好🙂"}', '{"done":true}']
  const bytes = encode(': heartbeat\r\n\r\ndata: ' + expected[0] + '\r\n\r\ndata: ' + expected[1] + '\n\n')
  let index = 0
  const body = new ReadableStream({
    pull(controller) {
      if (index < bytes.length) controller.enqueue(bytes.slice(index, ++index))
      else controller.close()
    }
  })
  assert.deepEqual(await collect(body), expected)
})

test('完整事件立即返回，不等待连接关闭；多行数据正确合并', { timeout: 2000 }, async () => {
  let controller
  const body = new ReadableStream({ start(value) { controller = value } })
  const iterator = readSseData(body)
  const first = iterator.next()
  controller.enqueue(encode('event: message\ndata: first\ndata: second\n\n'))
  assert.deepEqual(await first, { value: 'first\nsecond', done: false })
  controller.enqueue(encode('data: tail'))
  controller.close()
  assert.deepEqual(await iterator.next(), { value: 'tail', done: false })
  assert.equal((await iterator.next()).done, true)
  assert.equal(body.locked, false)
})

test('兼容 CR、空数据和同一分片内多个事件，忽略心跳及元数据', async () => {
  const body = new ReadableStream({ start(controller) {
    controller.enqueue(encode(': ping\revent: message\rid: 1\rretry: 1000\r\rdata\r\rdata:  空格\r\rdata: [DONE]\r\r'))
    controller.close()
  } })
  assert.deepEqual(await collect(body), ['', ' 空格', '[DONE]'])
})

test('消费者提前退出时取消读取并释放锁', async () => {
  let cancelled = false
  const body = new ReadableStream({
    start(controller) { controller.enqueue(encode('data: first\n\n')) },
    cancel() { cancelled = true }
  })
  for await (const event of readSseData(body)) {
    assert.equal(event, 'first')
    break
  }
  assert.equal(cancelled, true)
  assert.equal(body.locked, false)
})

test('读取失败会向上传递错误并释放锁，空响应给出明确错误', async () => {
  const body = new ReadableStream({ start(controller) { controller.error(new Error('连接中断')) } })
  await assert.rejects(collect(body), /连接中断/)
  assert.equal(body.locked, false)
  await assert.rejects(collect(null), /未返回流式响应/)
})
