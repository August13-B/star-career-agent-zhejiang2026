"""Retry failed real-AI paths using one disposable local account; always clean up.

Tests chat, full report/PDF/growth generation, ability analysis, job comparison,
and the first generated reply for each training scenario. Run training_smoke.py
after recovery to verify complete training, scoring and profile application.
"""
import json
import time
import uuid

from project_acceptance import Acceptance, FIXTURES, ROOT

OUTPUT = ROOT / 'logs/failed-ai-retest-result.json'


class AIRetry(Acceptance):
    def save(self):
        OUTPUT.write_text(json.dumps(self.results, ensure_ascii=False, indent=2), encoding='utf-8')

    def check(self, name, test):
        entry = {'name': name}
        try:
            entry.update(status='PASS', detail=test())
        except Exception as error:
            entry.update(status='FAIL', detail=str(error)[:220] if isinstance(error, AssertionError) else type(error).__name__)
        self.results['checks'].append(entry)
        self.save()
        print(name + ': ' + entry['status'], flush=True)

    def compare(self):
        with self.db.cursor() as cursor:
            cursor.execute('SELECT id FROM job_info WHERE job_id IS NOT NULL ORDER BY id LIMIT 1')
            row = cursor.fetchone()
        assert row, 'No seeded job has a profile'
        result = self.call(self.a, 'POST', '/job-compare/analyze-new-job', {'newJobId': str(row[0])}, timeout=220)
        assert result and result.get('analysis') and result.get('matchedJobs'), 'No usable AI comparison'

    def training_first_reply(self, template):
        created = self.call(self.a, 'POST', '/training/sessions',
                            {'templateId': template, 'clientRequestId': uuid.uuid4().hex, 'useForProfile': False})
        session_id = created['sessionId']
        try:
            deadline = time.monotonic() + 220
            while time.monotonic() < deadline:
                snapshot = self.call(self.a, 'GET', '/training/sessions/' + session_id)
                run = snapshot['run']
                if run['status'] not in ('queued', 'running'):
                    assert run['status'] == 'succeeded', 'Training first reply failed: ' + str(run.get('errorCode'))
                    return {'initialRun': 'succeeded', 'scope': 'first reply only; full training requires training_smoke.py'}
                time.sleep(.7)
            raise AssertionError('Training first reply exceeded test deadline')
        finally:
            self.call(self.a, 'POST', '/training/sessions/' + session_id + '/cancel')

    def run(self):
        self.u, self.a = self.create()
        self.current_user()
        self.profile()
        self.quiz()
        self.results['fixturesReady'] = True
        self.check('普通对话真实 AI 流式与历史', self.chat)
        self.check('六智能体报告、自动成长目标及 PDF', self.report)
        self.check('AI 能力分析', self.ai_score)
        self.check('岗位 AI 对比', self.compare)
        for template in ('interview_backend_intern.v1', 'communication_release.v1', 'office_review.v1'):
            self.check(template + '首轮真实生成', lambda item=template: self.training_first_reply(item))


def main():
    if FIXTURES.exists():
        raise SystemExit('A prior acceptance fixture exists. Clean it with project_acceptance.py --cleanup first.')
    run = AIRetry()
    run.results['scope'] = 'real AI retry; three training first replies, not full training acceptance'
    try:
        run.run()
    finally:
        try:
            run.cleanup()
            run.results['fixturesCleaned'] = True
        finally:
            run.results['finishedAt'] = time.strftime('%Y-%m-%d %H:%M:%S')
            run.save()
    return int(any(entry['status'] != 'PASS' for entry in run.results['checks']))


if __name__ == '__main__':
    raise SystemExit(main())
