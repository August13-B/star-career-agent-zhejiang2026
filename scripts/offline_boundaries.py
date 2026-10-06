"""Fault injection and durable training outcomes through real localhost HTTP/WS.

Run after project_acceptance.py --keep-fixtures in the offline sandbox only.
No service or persistence methods are mocked inside the application.
"""
import json
import time
import uuid
import requests
from concurrent.futures import ThreadPoolExecutor
from acceptance_config import result_dir
from project_acceptance import Acceptance, FIXTURES
from training_smoke import workplace_cases

CONTROL = 'http://127.0.0.1:18081/__control'


class Offline(Acceptance):
    def __init__(self):
        super().__init__()
        assert self.env.get('TEST_PLATFORM_MODE') == 'offline'
        self.users = json.loads(FIXTURES.read_text(encoding='utf-8'))
        self.u, self.v = self.users
        self.a, self.b = [self.login(u) for u in self.users]

    def mode(self, mode):
        requests.post(CONTROL, json={'mode': mode}, timeout=3).raise_for_status()

    def scalar(self, sql, *args):
        with self.db.cursor() as cur:
            cur.execute(sql, args)
            return cur.fetchone()[0]

    def check(self, name, test):
        try:
            detail = test()
            item = {'name': name, 'status': 'PASS', 'detail': detail}
        except Exception as error:
            item = {'name': name, 'status': 'FAIL', 'detail': str(error)[:250]}
        finally:
            self.mode('normal')
        self.results['checks'].append(item)
        print(name + ': ' + item['status'], flush=True)
        (result_dir() / 'offline-boundaries-result.json').write_text(json.dumps(self.results, ensure_ascii=False, indent=2), encoding='utf-8')

    def failed_chat(self, mode):
        self.mode(mode)
        created = self.call(self.a, 'POST', '/ai-conversation/create', {'user_id': self.u['id'], 'conversation_type': 1, 'title': '离线故障注入'})
        cid = created.get('id') or created.get('conversationId') or created.get('conversation_id')
        response = self.a.post(self.base + '/ai-conversation/send-stream', json={
            'user_id': self.u['id'], 'conversation_id': cid, 'conversation_type': 1, 'content': '离线故障测试'}, timeout=25)
        # SSE uses UTF-8 even when the response omits an explicit charset.
        frames = [json.loads(line[5:]) for line in response.content.decode('utf-8').splitlines() if line.startswith('data:')]
        errors = [f for f in frames if isinstance(f, dict) and f.get('error')]
        persisted = self.scalar('SELECT COUNT(*) FROM ai_message WHERE conversation_id=%s AND message_type=2', cid)
        assert errors and persisted == 0, f'errorFrames={len(errors)}, incorrectlyPersistedReplies={persisted}'
        return {'errorFrames': len(errors), 'persistedReplies': persisted}

    def wait(self, sid):
        until = time.monotonic() + 35
        while time.monotonic() < until:
            state = self.call(self.a, 'GET', '/training/sessions/' + sid)
            if state['run']['status'] not in ('running', 'queued'):
                return state
            time.sleep(.2)
        raise AssertionError('Training worker timeout')

    def training_retry(self):
        self.mode('run_error')
        created = self.call(self.a, 'POST', '/training/sessions', {'templateId': 'office_review.v1', 'clientRequestId': uuid.uuid4().hex})
        sid = created['sessionId']
        state = self.wait(sid)
        assert state['run']['status'] == 'failed'
        assert not [m for m in state['messages'] if m['role'] == 'assistant' and m['status'] == 'complete']
        self.mode('normal')
        self.call(self.a, 'POST', '/training/runs/' + created['runId'] + '/retry', {'expectedAttempt': state['run']['attempt']})
        state = self.wait(sid)
        assert state['run']['status'] == 'succeeded'
        self.call(self.a, 'POST', '/training/sessions/' + sid + '/cancel')
        return 'Failed run recovers through explicit retry, then cancels durably'

    def complete_office(self, invalid=False):
        self.quiz()  # Create/refresh the baseline through the public questionnaire API.
        _, answers, artifact = workplace_cases()[1]
        created = self.call(self.a, 'POST', '/training/sessions', {'templateId': 'office_review.v1', 'clientRequestId': uuid.uuid4().hex, 'useForProfile': True})
        sid = created['sessionId']
        state = self.wait(sid)
        for answer in answers:
            self.call(self.a, 'POST', '/training/sessions/' + sid + '/turns', {'content': answer, 'clientRequestId': uuid.uuid4().hex, 'expectedVersion': state['version']})
            state = self.wait(sid)
            assert state['run']['status'] == 'succeeded'
        self.call(self.a, 'POST', '/training/sessions/' + sid + '/artifacts', {'content': artifact, 'clientRequestId': uuid.uuid4().hex, 'expectedRevision': 0})
        state = self.call(self.a, 'GET', '/training/sessions/' + sid)
        before = self.scalar('SELECT version FROM student_profile WHERE user_id=%s AND is_deleted=0', self.u['id'])
        self.mode('invalid_evidence' if invalid else 'normal')
        self.call(self.a, 'POST', '/training/sessions/' + sid + '/finish', {'clientRequestId': uuid.uuid4().hex, 'expectedVersion': state['version']})
        state = self.wait(sid)
        if invalid:
            assert state['status'] == 'review_required' and state['evaluation']['status'] == 'review_required'
            assert self.scalar('SELECT version FROM student_profile WHERE user_id=%s AND is_deleted=0', self.u['id']) == before
            return 'Fabricated evidence rejected; no profile version change'
        assert state['status'] == 'completed' and state['evaluation']['result']['total'] == 80
        # The outcome callback may commit just after the run's terminal state.
        until = time.monotonic() + 15
        while time.monotonic() < until and state['profileApplication']['status'] == 'pending':
            time.sleep(.2)
            state = self.call(self.a, 'GET', '/training/sessions/' + sid)
        assert state['profileApplication']['status'] == 'applied', str(state['profileApplication'])
        after = self.scalar('SELECT version FROM student_profile WHERE user_id=%s AND is_deleted=0', self.u['id'])
        assert after == before + 1
        def retry(_):
            return self.call(self.a, 'POST', '/training/sessions/' + sid + '/profile/retry')
        with ThreadPoolExecutor(max_workers=3) as pool:
            list(pool.map(retry, range(3)))
        assert self.scalar('SELECT version FROM student_profile WHERE user_id=%s AND is_deleted=0', self.u['id']) == after
        plans = self.call(self.a, 'GET', '/training/growth/plans')
        body = {'planId': plans[0]['id'], 'suggestionIndex': 0}
        first = self.call(self.a, 'POST', '/training/sessions/' + sid + '/growth-task', body)
        assert self.call(self.a, 'POST', '/training/sessions/' + sid + '/growth-task', body) == first
        assert self.b.post(self.base + '/training/sessions/' + sid + '/growth-task', json=body, timeout=10).status_code == 404
        assert self.scalar('SELECT COUNT(*) FROM training_growth_link WHERE session_id=%s', sid) == 1
        return {'computedTotal': 80, 'mockSuppliedTotal': 1, 'profileVersionDelta': after - before,
                'concurrentRetryDuplicates': 0, 'growthLinks': 1}


if __name__ == '__main__':
    run = Offline()
    try:
        for mode in ('http_error', 'ws_reject', 'run_error', 'disconnect'):
            run.check('chat_' + mode, lambda m=mode: run.failed_chat(m))
        run.check('training_failed_run_retry', run.training_retry)
        run.check('training_profile_and_growth', run.complete_office)
        run.check('training_invalid_evidence', lambda: run.complete_office(invalid=True))
    finally:
        run.mode('normal')
        run.db.close()
    raise SystemExit(any(c['status'] != 'PASS' for c in run.results['checks']))
