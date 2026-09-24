"""Additional local acceptance against the exact synthetic fixtures of project_acceptance.

Run after project_acceptance.py --keep-fixtures. Never sends email or changes seed jobs.
All temporary writes are scoped to fixture IDs; image and job cleanup runs in finally.
"""
import base64
import json
import secrets
import smtplib
import ssl
import struct
import zlib
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor

import requests
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.asymmetric import padding as rsa_padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from project_acceptance import Acceptance, FIXTURES, ROOT

from acceptance_config import result_dir

OUT = result_dir() / 'project-extended-acceptance-result.json'


class Blocked(Exception):
    pass


class Skipped(Exception):
    pass


class Extended(Acceptance):
    def __init__(self):
        super().__init__()
        self.users = json.loads(FIXTURES.read_text(encoding='utf-8'))
        assert len(self.users) == 2
        for u in self.users:
            assert u['account'].startswith('__acceptance_')
            assert self.one('SELECT user_account FROM user WHERE id=%s', u['id'])[0] == u['account']
        self.u, self.v = self.users
        self.a, self.b = (self.login(u) for u in self.users)
        self.images = []
        self.jobs = []
        self.sessions = []

    def one(self, sql, *args):
        with self.db.cursor() as c:
            c.execute(sql, args)
            return c.fetchone()

    def check(self, name, test):
        item = {'name': name}
        try:
            item.update(status='PASS', detail=test())
        except Skipped as e:
            item.update(status='SKIP', detail=str(e))
        except Blocked as e:
            item.update(status='BLOCKED', detail=str(e))
        except Exception as e:
            # Exception text can contain server responses: retain only controlled assertions.
            item.update(status='FAIL', detail=str(e)[:220] if isinstance(e, AssertionError) else type(e).__name__)
        self.results['checks'].append(item)
        OUT.write_text(json.dumps(self.results, ensure_ascii=False, indent=2), encoding='utf-8')
        print(name + ': ' + item['status'], flush=True)

    @staticmethod
    def require(value, message='Unexpected response'):
        assert value, message

    @staticmethod
    def denied(response):
        if response.status_code in (401, 403, 404):
            return
        assert response.headers.get('Content-Type', '').startswith('application/json') and response.json().get('code') in (401, 403, 404), 'Other account or anonymous request was accepted'

    @staticmethod
    def rejected(response):
        if 400 <= response.status_code < 500:
            return
        assert response.status_code == 200 and response.json().get('code') not in (0, 200, 10001, '200'), 'Invalid input was accepted'

    def password_body(self, old, new):
        key, iv = secrets.token_hex(16).encode(), secrets.token_hex(8).encode()
        def enc(value):
            p = padding.PKCS7(128).padder()
            raw = p.update(value.encode()) + p.finalize()
            c = Cipher(algorithms.AES(key), modes.CBC(iv)).encryptor()
            return base64.b64encode(c.update(raw) + c.finalize()).decode()
        cipher = enc(new)
        return {'userId': str(self.v['id']), 'encryptedOldPassword': enc(old),
                'encryptedNewPassword': cipher, 'encryptedNewPassword_again': cipher,
                'AES': base64.b64encode(self.public.encrypt(key, rsa_padding.PKCS1v15())).decode(),
                'IV': base64.b64encode(self.public.encrypt(iv, rsa_padding.PKCS1v15())).decode()}

    def password(self):
        old, new = self.v['password'], 'AcceptanceNew!234'
        for previous, following in [('incorrect-old', new), (old, '123')]:
            self.rejected(self.b.put(self.base + '/user/change_password', json=self.password_body(previous, following), timeout=20))
        changed = False
        try:
            self.call(self.b, 'PUT', '/user/change_password', self.password_body(old, new))
            changed = True
            c = self.login({**self.v, 'password': new})
            assert int(self.call(c, 'GET', '/user/getUserInfo')['id']) == self.v['id']
            try:
                self.login(self.v)
            except AssertionError:
                pass
            else:
                raise AssertionError('Old password remains usable')
        finally:
            if changed:
                self.call(self.b, 'PUT', '/user/change_password', self.password_body(new, old))
                self.login(self.v).close()
        return 'Wrong old password and short new password rejected; change and restore passed'

    def pagination(self):
        p = self.call(self.a, 'GET', '/job-info/page?current=-1&size=1000')
        assert int(p['current']) == 1 and len(p['records']) == 100 and int(p['size']) == 100
        p2 = self.call(self.a, 'GET', '/job-info/page?current=2&size=100')
        assert not {x['id'] for x in p['records']} & {x['id'] for x in p2['records']}
        assert not self.call(self.a, 'GET', '/job-info/page?current=999999&size=5')['records']
        assert not self.call(self.a, 'GET', '/job-info/search?jobName=__acceptance_no_such_job_71b8')

    def concurrent_reads(self):
        def read(index):
            start = time.monotonic()
            r = requests.get(self.base + '/job-info/page', params={'current': index % 3 + 1, 'size': 5}, timeout=20)
            assert r.status_code == 200 and len(r.json()['data']['records']) == 5
            return round((time.monotonic() - start) * 1000, 1)
        with ThreadPoolExecutor(max_workers=5) as pool:
            times = sorted(pool.map(read, range(20)))
        return {'requests': 20, 'concurrency': 5, 'p95Ms': times[18], 'maxMs': times[-1], 'scope': 'read-only smoke, not capacity benchmark'}

    def profile_write(self):
        p = self.call(self.a, 'POST', '/student/condition', {'userId': self.u['id']})[0]
        try:
            r = self.b.put(self.base + '/student/update', json={'id': p['id'], 'userId': self.u['id'], 'targetCity': '验收越权标记'}, timeout=20)
            after = self.call(self.a, 'GET', '/student/' + str(p['id']))[0]
            assert after['targetCity'] == p['targetCity'], 'Other fixture account modified profile city'
            self.denied(r)
        finally:
            self.call(self.a, 'PUT', '/student/update', {'id': p['id'], 'userId': self.u['id'], 'targetCity': p['targetCity']})

    def growth_write(self):
        row = self.one('SELECT t.id,t.status FROM grow_task t JOIN grow_plan p ON p.id=t.plan_id WHERE p.user_id=%s AND p.is_deleted=0 AND t.is_deleted=0 LIMIT 1', self.u['id'])
        assert row
        task, status = row
        try:
            r = self.b.patch(self.base + f'/grow/tasks/{task}', json={'status': 1 if status != 1 else 2}, timeout=20)
            assert self.one('SELECT status FROM grow_task WHERE id=%s', task)[0] == status, 'Other fixture account changed task status'
            self.denied(r)
        finally:
            self.call(self.a, 'PATCH', f'/grow/tasks/{task}', {'status': status})

    def upload(self, filename='test.png', content=None, mime='image/png'):
        def chunk(kind, value):
            return struct.pack('!I', len(value)) + kind + value + struct.pack('!I', zlib.crc32(kind + value))
        if content is None:
            content = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!IIBBBBB', 1, 1, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(b'\x00\xff\x00\x00')) + chunk(b'IEND', b'')
        r = self.a.post(self.base + '/student/image/upload', data={'userId': str(self.u['id']), 'imageType': 'acceptance'}, files={'file': (filename, content, mime)}, timeout=30)
        if r.status_code == 200 and r.json().get('code') == 200:
            self.images.append(str(r.json()['data']['id']))
        return r

    def image_cycle(self):
        r = self.upload()
        assert r.status_code == 200 and r.json().get('code') == 200
        iid = str(r.json()['data']['id'])
        assert any(str(i['id']) == iid for i in self.call(self.a, 'GET', f'/student/image/list/{self.u["id"]}'))
        self.call(self.a, 'DELETE', '/student/image/delete/' + iid)
        assert not any(str(i['id']) == iid for i in self.call(self.a, 'GET', f'/student/image/list/{self.u["id"]}'))

    def image_isolation(self, delete=False):
        iid = self.upload().json()['data']['id']
        if delete:
            self.denied(self.b.delete(self.base + '/student/image/delete/' + str(iid), timeout=20))
        else:
            self.denied(self.b.get(self.base + f'/student/image/list/{self.u["id"]}', timeout=20))

    def job_write(self, method):
        jid, marker = secrets.randbelow(2**50) + 2**40, '__acceptance_job_' + uuid.uuid4().hex[:12]
        with self.db.cursor() as c:
            c.execute('INSERT INTO job_info(id,job_name,company_name,is_deleted) VALUES(%s,%s,%s,0)', (jid, marker, marker))
        self.jobs.append((jid, marker))
        r = requests.request(method, self.base + f'/job-info/{jid}', json={'address': '验收临时修改'} if method == 'PUT' else None, timeout=20)
        self.denied(r)

    def chat_isolation(self):
        row = self.one('SELECT id FROM ai_conversation WHERE user_id=%s LIMIT 1', self.u['id'])
        assert row
        self.denied(self.b.get(self.base + '/ai-conversation/history/' + str(row[0]), timeout=20))

    def compare(self):
        job = self.one('SELECT id FROM job_info WHERE job_id IS NOT NULL LIMIT 1')[0]
        data = self.call(self.a, 'POST', '/job-compare/analyze-new-job', {'newJobId': str(job)}, timeout=220)
        assert data and data.get('analysis') and data.get('matchedJobs'), 'No usable AI comparison'

    def smtp(self):
        if self.env.get('TEST_PLATFORM_MODE') == 'offline':
            raise Skipped('Offline run deliberately excludes external SMTP')
        host, port = self.env.get('MAIL_HOST', 'smtp.qq.com'), int(self.env.get('MAIL_PORT', '587'))
        cls = smtplib.SMTP_SSL if port == 465 else smtplib.SMTP
        with cls(host, port, timeout=20) as smtp:
            smtp.ehlo()
            if port != 465:
                smtp.starttls(context=ssl.create_default_context())
                smtp.ehlo()
            assert smtp.login(self.env['MAIL_USERNAME'], self.env['MAIL_PASSWORD'])[0] == 235
        return 'SMTP TLS and authentication passed; no email sent'

    def training_lifecycle(self, template):
        body = {'templateId': template, 'difficulty': 'entry', 'useForProfile': False, 'clientRequestId': uuid.uuid4().hex}
        snap = self.call(self.a, 'POST', '/training/sessions', body)
        sid = str(snap['sessionId'])
        self.sessions.append(sid)
        again = self.call(self.a, 'POST', '/training/sessions', body)
        assert str(again['sessionId']) == sid
        self.denied(self.b.get(self.base + '/training/sessions/' + sid, timeout=20))
        deadline = time.monotonic() + 45
        while time.monotonic() < deadline:
            snap = self.call(self.a, 'GET', '/training/sessions/' + sid)
            if snap['run']['status'] not in ('queued', 'running'):
                break
            time.sleep(1)
        else:
            raise Blocked('Initial AI turn did not finish within 45 seconds; full run belongs to training_smoke')
        if snap['run']['status'] == 'failed':
            assert not snap.get('evaluation'), 'Failed AI run generated an evaluation'
        version = snap['draftVersion']
        self.call(self.a, 'PUT', '/training/sessions/' + sid + '/draft', {'content': '全量复测恢复草稿', 'expectedVersion': version})
        assert self.call(self.a, 'GET', '/training/sessions/' + sid)['draft'] == '全量复测恢复草稿'
        stale = self.a.put(self.base + '/training/sessions/' + sid + '/draft', json={'content': 'stale', 'expectedVersion': version}, timeout=20)
        assert stale.status_code == 409
        cancelled = self.call(self.a, 'POST', '/training/sessions/' + sid + '/cancel')
        assert cancelled['status'] == 'canceled'
        return {'initialRun': snap['run']['status'], 'errorCode': snap['run'].get('errorCode'), 'checks': ['idempotency', 'ownership', 'draft recovery', 'stale conflict', 'cancel']}

    def run(self):
        self.check('错误登录密码拒绝', self.wrong_password)
        self.check('本人修改密码校验及恢复', self.password)
        for label, headers in [('匿名', {}), ('无效 JWT', {'Authorization': 'invalid.test.token'})]:
            self.check(label + '访问受保护接口', lambda h=headers: self.denied(requests.get(self.base + '/user/getUserInfo', headers=h, timeout=20)))
        self.check('岗位分页边界与空搜索', self.pagination)
        self.check('20 次请求 5 并发读取', self.concurrent_reads)
        self.check('不存在岗位返回失败', lambda: self.rejected(self.a.get(self.base + '/job-info/1', timeout=20)))
        self.check('岗位对比平台链路', self.compare)
        self.check('画像跨账号修改隔离', self.profile_write)
        self.check('成长任务跨账号修改隔离', self.growth_write)
        self.check('能力分数跨账号读取隔离', lambda: self.denied(self.b.get(self.base + f'/ability/score/user/{self.u["id"]}', timeout=20)))
        self.check('对话历史跨账号读取隔离', self.chat_isolation)
        self.check('简历跨账号导出隔离', lambda: self.denied(self.b.get(self.base + f'/resume/export/{self.u["id"]}', timeout=30)))
        self.check('PNG 上传查询删除', self.image_cycle)
        self.check('空图片拒绝', lambda: self.rejected(self.upload(content=b'')))
        self.check('非图片文件拒绝', lambda: self.rejected(self.upload('acceptance.txt', b'harmless synthetic text', 'text/plain')))
        self.check('图片跨账号读取隔离', self.image_isolation)
        self.check('图片跨账号删除隔离', lambda: self.image_isolation(delete=True))
        self.check('匿名岗位修改隔离（临时岗位）', lambda: self.job_write('PUT'))
        self.check('匿名岗位删除隔离（临时岗位）', lambda: self.job_write('DELETE'))
        self.check('SMTP TLS 和认证（不发信）', self.smtp)
        self.check('训练非法分页拒绝', lambda: self.rejected(self.a.get(self.base + '/training/sessions?limit=999', timeout=20)))
        self.check('训练空创建参数拒绝', lambda: self.rejected(self.a.post(self.base + '/training/sessions', json={}, timeout=20)))
        self.check('训练不存在模板拒绝', lambda: self.rejected(self.a.post(self.base + '/training/sessions', json={'templateId': 'missing.v1', 'clientRequestId': uuid.uuid4().hex}, timeout=20)))
        for template in ('interview_backend_intern.v1', 'communication_release.v1', 'office_review.v1'):
            self.check(template + '状态、隔离、草稿与取消', lambda t=template: self.training_lifecycle(t))

    def wrong_password(self):
        try:
            self.login({**self.v, 'password': 'definitely-wrong'})
        except AssertionError:
            return
        raise AssertionError('Invalid password accepted')

    def cleanup_extra(self):
        for sid in self.sessions:
            self.a.post(self.base + '/training/sessions/' + sid + '/cancel', timeout=20)
        for iid in self.images:
            self.a.delete(self.base + '/student/image/delete/' + iid, timeout=20)
            row = self.one('SELECT is_deleted FROM student_image WHERE id=%s AND user_id=%s', iid, self.u['id'])
            assert row is None or row[0] == 1, 'Image cleanup not confirmed'
            with self.db.cursor() as c:
                c.execute('DELETE FROM student_image WHERE id=%s AND user_id=%s', (iid, self.u['id']))
        for jid, marker in self.jobs:
            with self.db.cursor() as c:
                c.execute('DELETE FROM job_info WHERE id=%s AND job_name=%s AND job_id IS NULL', (jid, marker))
        self.results['finishedAt'] = time.strftime('%Y-%m-%d %H:%M:%S')
        self.results['extraFixturesCleaned'] = True
        OUT.write_text(json.dumps(self.results, ensure_ascii=False, indent=2), encoding='utf-8')
        self.db.close()


if __name__ == '__main__':
    run = Extended()
    try:
        if '--training-only' in sys.argv:
            prior = json.loads(OUT.read_text(encoding='utf-8'))
            run.results = prior
            run.results['checks'] = [x for x in prior['checks'] if '.v1状态' not in x['name']]
            with run.db.cursor() as cursor:
                cursor.execute('SELECT id FROM training_session WHERE user_id=%s', (run.u['id'],))
                old_sessions = [str(row[0]) for row in cursor.fetchall()]
            for sid in old_sessions:
                run.call(run.a, 'POST', '/training/sessions/' + sid + '/cancel')
            for template in ('interview_backend_intern.v1', 'communication_release.v1', 'office_review.v1'):
                run.check(template + '状态、隔离、草稿与取消', lambda t=template: run.training_lifecycle(t))
        else:
            run.run()
    finally:
        run.cleanup_extra()
    raise SystemExit(any(x['status'] not in ('PASS', 'SKIP') for x in run.results['checks']))
