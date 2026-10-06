"""Local end-to-end acceptance using synthetic accounts and the configured platform.

Requires requests, pymysql, cryptography. No email is sent. --keep-fixtures retains
only this run's accounts for browser checks; --cleanup removes those exact accounts.
Results and temporary credentials are written to ignored logs/, never printed.
ACCEPTANCE_ENV_FILE selects an isolated configuration; reports identify its mode.
"""
import argparse
import base64
import hashlib
import io
import json
import secrets
import sys
import time
import uuid
import zipfile
from pathlib import Path
from urllib.parse import urlparse

import pymysql
import requests
from cryptography.hazmat.primitives import padding, serialization
from cryptography.hazmat.primitives.asymmetric import padding as rsa_padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import manage
from acceptance_config import load_test_env, result_dir

FIXTURES = result_dir() / 'project-acceptance-fixtures.json'
OUTPUT = result_dir() / 'project-acceptance-result.json'


class Acceptance:
    def __init__(self):
        self.env = load_test_env()
        url = urlparse(self.env['DB_URL'].removeprefix('jdbc:'))
        if url.hostname not in ('localhost', '127.0.0.1', '::1'):
            raise RuntimeError('Only a local database may be tested')
        self.db = pymysql.connect(host=url.hostname, port=url.port or 3306,
            user=self.env['DB_USERNAME'], password=self.env['DB_PASSWORD'],
            database=url.path.lstrip('/'), charset='utf8mb4', autocommit=True)
        self.public = serialization.load_der_public_key(base64.b64decode(self.env['RSA_PUBLIC_KEY']))
        self.base = 'http://127.0.0.1:' + self.env.get('SERVER_PORT', '8080') + '/api'
        self.users = []
        self.results = {'startedAt': time.strftime('%Y-%m-%d %H:%M:%S'), 'checks': [], 'platformMode': self.env.get('TEST_PLATFORM_MODE', 'real')}

    def login(self, user):
        key, iv = secrets.token_hex(16).encode(), secrets.token_hex(8).encode()
        def encrypt(value):
            pad = padding.PKCS7(128).padder()
            raw = pad.update(value.encode()) + pad.finalize()
            cipher = Cipher(algorithms.AES(key), modes.CBC(iv)).encryptor()
            return base64.b64encode(cipher.update(raw) + cipher.finalize()).decode()
        def rsa(value):
            return base64.b64encode(self.public.encrypt(value, rsa_padding.PKCS1v15())).decode()
        client = requests.Session()
        data = self.call(client, 'POST', '/user/login', {
            'login_way': 'auto', 'encryptedLoginValue': encrypt(user['account']),
            'encryptedPassword': encrypt(user['password']), 'AES': rsa(key), 'IV': rsa(iv)})
        client.headers['Authorization'] = data['token']
        return client

    def create(self):
        user = {'id': secrets.randbelow(2**52) + 2**40,
                'account': '__acceptance_' + uuid.uuid4().hex[:12],
                'password': 'LocalAcceptance!234'}
        with self.db.cursor() as c:
            c.execute('INSERT INTO user(id,user_account,user_password,nickname,user_role,user_status,register_type,is_deleted) VALUES(%s,%s,%s,%s,1,1,5,0)',
                (user['id'], user['account'], hashlib.sha256(user['password'].encode()).hexdigest(), '验收' + user['account'][-12:]))
        self.users.append(user)
        FIXTURES.write_text(json.dumps(self.users), encoding='utf-8')
        return user, self.login(user)

    def call(self, client, method, path, body=None, timeout=30):
        r = client.request(method, self.base + path, json=body, timeout=timeout)
        assert r.status_code in (200, 201, 202), f'HTTP {r.status_code}: {method} {path}'
        result = r.json()
        assert result.get('code') in (0, 200, 10001, '200'), f'Business code {result.get("code")}: {method} {path}'
        return result.get('data')

    def check(self, name, test):
        item = {'name': name}
        try:
            detail = test()
            item.update(status='PASS', detail=detail)
        except Exception as error:
            item.update(status='FAIL', detail=str(error)[:220])
        self.results['checks'].append(item)
        OUTPUT.write_text(json.dumps(self.results, ensure_ascii=False, indent=2), encoding='utf-8')
        print(name + ': ' + item['status'], flush=True)

    def cleanup(self):
        for u in self.users:
            assert u['account'].startswith('__acceptance_')
            with self.db.cursor() as c:
                c.execute('SELECT id FROM user WHERE id=%s AND user_account=%s', (u['id'], u['account']))
                if c.fetchone() is None:
                    continue
            # Delete only this synthetic user's generated platform reports through the app.
            client = self.login(u)
            with self.db.cursor() as c:
                c.execute('SELECT id FROM career_report WHERE user_id=%s', (u['id'],))
                ids = [str(row[0]) for row in c.fetchall()]
            if ids:
                self.call(client, 'POST', '/career-report/batch-delete', {'ids': ids}, timeout=60)
            with self.db.cursor() as c:
                c.execute("UPDATE training_run r JOIN training_session s ON s.id=r.session_id SET r.status='canceled' WHERE s.user_id=%s AND r.status IN ('queued','running')", (u['id'],))
                for table in ('training_growth_link', 'training_profile_application', 'training_artifact', 'training_session_config', 'training_evaluation', 'training_turn', 'training_run'):
                    c.execute(f'DELETE FROM {table} WHERE session_id IN (SELECT id FROM training_session WHERE user_id=%s)', (u['id'],))
                c.execute('DELETE FROM training_session WHERE user_id=%s', (u['id'],))
                c.execute('DELETE FROM grow_task_record WHERE user_id=%s', (u['id'],))
                for table in ('student_ability_score_history', 'student_profile_history',
                              'student_ability_score', 'student_ability', 'student_profile',
                              'ai_conversation', 'grow_plan', 'career_report'):
                    c.execute(f'DELETE FROM {table} WHERE user_id=%s', (u['id'],))
                c.execute('DELETE FROM user WHERE id=%s AND user_account=%s', (u['id'], u['account']))
            # The export service creates local files; only remove this fixture's artifacts.
            for directory in (ROOT / 'resumes', ROOT / '后端/resumes'):
                for artifact in directory.glob('resume_' + str(u['id']) + '_*.docx'):
                    if artifact.resolve().parent == directory.resolve():
                        artifact.unlink()
        FIXTURES.unlink(missing_ok=True)
        self.db.close()

    def run(self):
        self.u, self.a = self.create()
        self.v, self.b = self.create()
        self.check('RSA/AES 登录与当前用户', lambda: self.current_user())
        self.check('个人画像新增、读取和修改', self.profile)
        self.check('六维问卷、十维分数持久化', self.quiz)
        self.check('岗位列表、详情、晋升换岗图谱', self.jobs)
        self.check('成长计划任务与完成记录', self.growth)
        self.check('普通对话平台流式与历史', self.chat)
        self.check('多智能体报告、自动成长目标及 PDF', self.report)
        self.check('AI 能力分析平台接口', self.ai_score)
        self.check('Word 简历导出', self.resume)
        self.check('个人画像跨账号隔离', self.profile_isolation)
        self.check('成长计划跨账号隔离', self.growth_isolation)
        self.check('训练匿名访问限制', self.training_auth)
        self.results['finishedAt'] = time.strftime('%Y-%m-%d %H:%M:%S')
        OUTPUT.write_text(json.dumps(self.results, ensure_ascii=False, indent=2), encoding='utf-8')

    def current_user(self):
        data = self.call(self.a, 'GET', '/user/getUserInfo')
        assert int(data['id']) == self.u['id']

    def profile(self):
        self.call(self.a, 'POST', '/student/insert', {
            'userId': self.u['id'], 'userName': '验收虚构同学', 'nickname': '验收',
            'college': '虚构测试大学', 'major': '软件工程', 'grade': '大三',
            'education': '本科', 'careerIntentions': 'Java 后端工程师', 'targetCity': '杭州',
            'projectExperience': '虚构校园报名项目，使用 Spring Boot 和 MySQL 实现报名与重复提交校验。',
            'skill': 'Java、SQL、Git', 'version': 1})
        p = self.call(self.a, 'POST', '/student/condition', {'userId': self.u['id']})[0]
        self.profile_id = p['id']
        assert p['major'] == '软件工程'
        self.call(self.a, 'PUT', '/student/update', {'id': p['id'], 'userId': self.u['id'], 'targetCity': '宁波'})
        p2 = self.call(self.a, 'GET', '/student/' + str(p['id']))[0]
        assert p2['targetCity'] == '宁波' and int(p2['version']) > int(p['version'])

    def quiz(self):
        questions = self.call(self.a, 'GET', '/ability/quiz')['questions']
        assert 6 <= len(questions) <= 12
        assert all('score' not in option for q in questions for option in q['options'])
        scored = self.call(self.a, 'POST', '/ability/quiz/submit', {
            'basic': {'education': '本科', 'internshipMonths': '0', 'skillLevel': '熟练', 'certCount': '1'},
            'answers': [{'id': q['id'], 'k': q['options'][0]['k']} for q in questions]})
        assert len(scored['scores']) == 11
        assert self.call(self.a, 'GET', '/ability/score/user/' + str(self.u['id']))
        return {'questionCount': len(questions), 'dimensions': 10}

    def jobs(self):
        page = self.call(self.a, 'GET', '/job-info/page?current=1&size=5')
        assert int(page['total']) >= 9958 and len(page['records']) == 5
        with self.db.cursor() as c:
            c.execute('SELECT id,job_id FROM job_info WHERE job_id IS NOT NULL LIMIT 1')
            job, profile = c.fetchone()
        detail = self.call(self.a, 'GET', f'/job-detail/by-job-info/{job}')
        graph = self.call(self.a, 'GET', f'/analysis/graph/{profile}')
        assert detail and graph['center']
        self.results['jobExample'] = {'jobId': str(job), 'profileId': str(profile)}
        return {'jobRows': page['total'], 'promotions': len(graph['promotions']), 'transfers': len(graph['transfers'])}

    def growth(self):
        self.plan = self.call(self.a, 'POST', '/grow/plans', {'planName': '验收虚构成长计划', 'planType': 2})['planId']
        self.task = self.call(self.a, 'POST', '/grow/tasks', {'planId': self.plan, 'taskName': '验证事务和接口'})['taskId']
        self.call(self.a, 'PATCH', '/grow/tasks/' + str(self.task), {'status': 2})
        self.call(self.a, 'POST', f'/grow/tasks/{self.task}/records', {'content': '仅验收临时记录，已完成接口练习。'})
        plans = self.call(self.a, 'GET', '/grow/plans?userId=' + str(self.u['id']))
        own = next(p for p in plans if str(p['plan']['id']) == str(self.plan))
        assert own['tasks'][0]['task']['status'] == 2 and own['tasks'][0]['records']
        assert float(own['plan']['progress']) == 100

    def chat(self):
        created = self.call(self.a, 'POST', '/ai-conversation/create', {
            'user_id': self.u['id'], 'conversation_type': 1, 'title': '验收临时对话'})
        conversation_id = created.get('id') or created.get('conversationId') or created.get('conversation_id')
        assert conversation_id, 'Create response lacks conversation ID'
        r = self.a.post(self.base + '/ai-conversation/send-stream', json={
            'user_id': self.u['id'], 'conversation_id': conversation_id, 'conversation_type': 1,
            'content': '这是虚构测试。请用不超过80字说明 Java 实习生应如何练习 SQL。'}, timeout=220)
        assert r.status_code == 200 and 'text/event-stream' in r.headers.get('Content-Type', '')
        r.encoding = 'utf-8'
        assert 'data:' in r.text, 'SSE response contained no events'
        # A 200 SSE containing an upstream outage message is not a successful AI reply.
        chunks = []
        for line in r.text.splitlines():
            if line.startswith('data:'):
                payload = line[5:].strip()
                try:
                    frame = json.loads(payload)
                    assert not isinstance(frame, dict) or not frame.get('error'), 'SSE returned an error envelope'
                    chunks.append(json.dumps(frame, ensure_ascii=False))
                except ValueError:
                    chunks.append(payload)
        response_text = '\n'.join(chunks)
        assert not any(marker in response_text for marker in (
            'AI 服务调用失败', 'AI 服务暂不可用', 'Service Unavailable',
            'AI 服务响应超时', 'AI 服务连接失败')), 'SSE returned an upstream service error, not an AI answer'
        history = self.call(self.a, 'GET', '/ai-conversation/history/' + str(conversation_id))
        assert history
        self.call(self.a, 'PUT', '/ai-conversation/update-title', {
            'user_id': self.u['id'], 'conversation_id': conversation_id, 'newTitle': '验收已重命名'})
        with self.db.cursor() as c:
            c.execute('SELECT COUNT(*) FROM ai_message WHERE conversation_id=%s AND message_type=2', (conversation_id,))
            assert c.fetchone()[0] >= 1, 'No persisted assistant message'
        return {'streamBytes': len(r.content)}

    def report(self):
        started = self.call(self.a, 'POST', '/career-report/start', {
            'user_id': self.u['id'], 'target_job': 'Java 后端工程师',
            'content': '请依据我的虚构测试画像生成职业报告，提出1/3/5年目标，不补造院校或实习经历。'}, timeout=60)
        job = started['jobId']
        deadline = time.monotonic() + 540
        while time.monotonic() < deadline:
            data = self.call(self.a, 'GET', '/career-report/jobs/' + job, timeout=60)
            if data.get('status') == 'done':
                break
            assert data.get('status') != 'error', 'Platform report failed'
            time.sleep(4)
        else:
            raise AssertionError('Report deadline exceeded')
        assert data.get('saved'), 'Report not saved in local database'
        report_id = data['reportId']
        assert self.call(self.a, 'GET', '/career-report/user/' + str(self.u['id']))
        for mode in ('report', 'full'):
            r = self.a.get(self.base + f'/career-report/{report_id}/export/pdf?mode={mode}', timeout=60)
            assert r.status_code == 200 and r.content.startswith(b'%PDF'), 'PDF export failed: ' + mode
            (result_dir() / f'acceptance-report-{mode}.pdf').write_bytes(r.content)
        with self.db.cursor() as c:
            c.execute('SELECT COUNT(*) FROM grow_plan WHERE user_id=%s AND report_id=%s', (self.u['id'], report_id))
            generated = c.fetchone()[0]
        self.results['reportDetail'] = {'partial': bool(data.get('partial')), 'agentCount': len(data.get('agentsDone', [])), 'generatedPlans': generated}
        assert not data.get('partial'), 'Only a partial report was produced'
        assert generated >= 3, 'Missing automatically generated 1/3/5 year plans'
        return self.results['reportDetail']

    def ai_score(self):
        data = self.call(self.a, 'POST', '/ai/analysis/ability/score', {
            'userId': self.u['id'], 'message': '仅基于已有虚构资料分析，不补造工作经验。', 'temperature': 0.1}, timeout=220)
        assert data and data[0].get('totalScore') is not None

    def resume(self):
        r = self.a.get(self.base + '/resume/export/' + str(self.u['id']), timeout=60)
        assert r.status_code == 200 and r.content.startswith(b'PK'), 'Word export did not return DOCX'
        with zipfile.ZipFile(io.BytesIO(r.content)) as archive:
            assert 'word/document.xml' in archive.namelist()

    def profile_isolation(self):
        r = self.b.get(self.base + '/student/' + str(self.profile_id), timeout=20)
        assert r.status_code in (403, 404) or r.json().get('code') in (403, 404), 'Other fixture account can read this fixture profile'

    def growth_isolation(self):
        r = self.b.get(self.base + '/grow/plans?userId=' + str(self.u['id']), timeout=20)
        assert r.status_code in (403, 404) or r.json().get('code') in (403, 404), 'Other fixture account can read this fixture growth plans'

    def training_auth(self):
        assert requests.get(self.base + '/training/sessions', timeout=15).status_code == 401


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--keep-fixtures', action='store_true')
    parser.add_argument('--cleanup', action='store_true')
    args = parser.parse_args()
    run = Acceptance()
    if args.cleanup:
        run.users = json.loads(FIXTURES.read_text(encoding='utf-8'))
        run.cleanup()
        print('Exact synthetic fixtures removed.')
        return
    if FIXTURES.exists():
        raise RuntimeError('Clean up previous fixture run first')
    try:
        run.run()
    finally:
        if not args.keep_fixtures:
            run.cleanup()
    raise SystemExit(any(c['status'] == 'FAIL' for c in run.results['checks']))


if __name__ == '__main__':
    main()
