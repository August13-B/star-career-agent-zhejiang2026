import test from 'node:test'
import assert from 'node:assert/strict'
import { nextDraftAction } from './draftSync.js'

const base = { force: false, hasSession: true, untouched: true, serverVersion: 4, localVersion: 4 }

test('旧快照（版本不高于本地）不得写回输入框', () => {
  // 提交回答后：本地 draft/savedDraft 均为空（untouched=true），旧快照版本相同
  assert.equal(nextDraftAction(base), 'keep')
  assert.equal(nextDraftAction({ ...base, serverVersion: 3 }), 'keep')
})

test('首次载入与强制读取一律采用服务端草稿', () => {
  assert.equal(nextDraftAction({ ...base, force: true, serverVersion: 1 }), 'use')
  assert.equal(nextDraftAction({ ...base, hasSession: false, serverVersion: 1 }), 'use')
})

test('服务端版本更高且本地未修改时采用服务端草稿', () => {
  assert.equal(nextDraftAction({ ...base, serverVersion: 5 }), 'use')
})

test('服务端版本更高但本地有未保存修改时报冲突而不是覆盖', () => {
  assert.equal(nextDraftAction({ ...base, untouched: false, serverVersion: 5 }), 'conflict')
})

test('版本比较按数值而非字符串', () => {
  assert.equal(nextDraftAction({ ...base, serverVersion: 10, localVersion: 9 }), 'use')
  assert.equal(nextDraftAction({ ...base, serverVersion: '10', localVersion: '9' }), 'use')
})
