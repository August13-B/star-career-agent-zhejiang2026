import test from 'node:test'
import assert from 'node:assert/strict'
import { trainingHeaders, trainingRequest, trainingBusy } from './trainingApi.js'

test('authorization keeps exactly one Bearer prefix', () => {
  assert.equal(trainingHeaders('Bearer abc').Authorization, 'Bearer abc')
  assert.equal(trainingHeaders('abc').Authorization, 'Bearer abc')
  assert.equal(trainingHeaders(null).Authorization, undefined)
})

test('created and accepted responses retain string IDs and send JSON', async () => {
  const oldFetch = globalThis.fetch, oldStorage = globalThis.localStorage
  globalThis.localStorage = { getItem: () => 'token' }
  try {
    for (const status of [201, 202]) {
      globalThis.fetch = async (path, options) => {
        assert.equal(path, '/api/training/sessions')
        assert.equal(options.headers.Authorization, 'Bearer token')
        assert.deepEqual(JSON.parse(options.body), { clientRequestId: 'request-one' })
        return { ok: true, status, json: async () => ({ code: 10001, data: { sessionId: '9007199254740993' } }) }
      }
      assert.equal((await trainingRequest('/sessions', { method: 'POST', body: { clientRequestId: 'request-one' } })).sessionId, '9007199254740993')
    }
  } finally { globalThis.fetch = oldFetch; globalThis.localStorage = oldStorage }
})

test('ownership and stale version failures surface status without successful data', async () => {
  const oldFetch = globalThis.fetch, oldStorage = globalThis.localStorage
  globalThis.localStorage = { getItem: () => 'token' }
  try {
    for (const status of [401, 404, 409, 429]) {
      globalThis.fetch = async () => ({ ok: false, status, json: async () => ({ code: 10002, message: '不能操作', data: { errorCode: 'TRAINING_CONFLICT' } }) })
      await assert.rejects(trainingRequest('/sessions/1'), error => error.status === status)
    }
  } finally { globalThis.fetch = oldFetch; globalThis.localStorage = oldStorage }
})

test('failed and canceled executions do not display as generating', () => {
  assert.equal(trainingBusy({ status: 'queued' }), true)
  assert.equal(trainingBusy({ status: 'running' }), true)
  for (const status of ['succeeded', 'failed', 'canceled']) assert.equal(trainingBusy({ status }), false)
  assert.equal(trainingBusy(null), false)
})
