"""Local platform acceptance test. Creates fresh users; never reads existing account credentials.

Run with the project's Python environment (requests, pymysql, cryptography installed).
Uses the backend .env only for the local database and the public transport key.
Produces a sanitized report under ignored logs/. Does not send email.
ACCEPTANCE_ENV_FILE selects an isolated configuration; realPlatform is false for offline runs.
"""
from pathlib import Path
import base64
import hashlib
import json
import secrets
import sys
import time
import uuid
from urllib.parse import urlparse

import pymysql
import requests
from cryptography.hazmat.primitives import serialization, padding
from cryptography.hazmat.primitives.asymmetric import padding as rsa_padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import manage
from acceptance_config import load_test_env, result_dir


def main():
    env = load_test_env()
    db_url = urlparse(env['DB_URL'].removeprefix('jdbc:'))
    if db_url.hostname not in ('localhost', '127.0.0.1', '::1'):
        raise RuntimeError('This fixture test only runs against a local database')
    connection = pymysql.connect(host=db_url.hostname, port=db_url.port or 3306,
                                 user=env['DB_USERNAME'], password=env['DB_PASSWORD'],
                                 database=db_url.path.lstrip('/'), charset='utf8mb4', autocommit=True)
    public = serialization.load_der_public_key(base64.b64decode(env['RSA_PUBLIC_KEY']))
    base = 'http://127.0.0.1:' + env.get('SERVER_PORT', '8080') + '/api'
    users = []
    report = {'checks': [], 'realPlatform': env.get('TEST_PLATFORM_MODE') != 'offline', 'startedAt': time.strftime('%Y-%m-%d %H:%M:%S')}

    def checked(name):
        report['checks'].append(name)
        print(name + ': PASS', flush=True)

    def create_user():
        user_id = secrets.randbelow(2**62 - 1) + 1
        account = '__training_smoke_' + uuid.uuid4().hex[:12]
        password = secrets.token_urlsafe(24)
        with connection.cursor() as cursor:
            cursor.execute('INSERT INTO user(id,user_account,user_password,nickname,user_role,user_status,register_type,is_deleted) VALUES(%s,%s,%s,%s,1,1,5,0)',
                           (user_id, account, hashlib.sha256(password.encode()).hexdigest(), '训练联调' + account[-8:]))
        users.append((user_id, account))
        key, iv = secrets.token_hex(16).encode(), secrets.token_hex(8).encode()

        def encrypt(value):
            pad = padding.PKCS7(128).padder()
            data = pad.update(value.encode()) + pad.finalize()
            cipher = Cipher(algorithms.AES(key), modes.CBC(iv)).encryptor()
            return base64.b64encode(cipher.update(data) + cipher.finalize()).decode()

        def rsa(value):
            return base64.b64encode(public.encrypt(value, rsa_padding.PKCS1v15())).decode()

        response = requests.post(base + '/user/login', json={
            'login_way': 'auto', 'encryptedLoginValue': encrypt(account), 'encryptedPassword': encrypt(password),
            'AES': rsa(key), 'IV': rsa(iv)}, timeout=15)
        assert response.status_code == 200 and response.json().get('code') == 10001, 'Fresh fixture login failed'
        session = requests.Session()
        token = response.json()['data']['token']
        session.headers['Authorization'] = token if token.startswith('Bearer ') else 'Bearer ' + token
        return session

    def call(client, method, path, body=None, expected=200):
        response = client.request(method, base + '/training' + path, json=body, timeout=20)
        assert response.status_code == expected, f'{method} {path}: expected HTTP {expected}, got {response.status_code}'
        if expected >= 400:
            return None
        result = response.json()
        assert result.get('code') == 10001, f'{method} {path}: business operation failed'
        return result['data']

    def wait(client, session_id):
        deadline = time.monotonic() + 220
        while time.monotonic() < deadline:
            snapshot = call(client, 'GET', '/sessions/' + session_id)
            if snapshot['run']['status'] not in ('queued', 'running'):
                assert snapshot['run']['status'] == 'succeeded', 'Platform run failed: ' + str(snapshot['run'].get('errorCode'))
                return snapshot
            time.sleep(.7)
        raise AssertionError('Platform run did not finish within the test deadline')

    try:
        first, second = create_user(), create_user()
        checked('RSA/AES encrypted login with fresh isolated accounts')
        call(requests.Session(), 'GET', '/sessions', expected=401)
        templates = call(first, 'GET', '/templates')
        assert len(templates) == 3 and templates[0]['id'] == 'interview_backend_intern.v1'
        body = {'templateId': templates[0]['id'], 'clientRequestId': str(uuid.uuid4())}
        created = call(first, 'POST', '/sessions', body, 201)
        session_id = created['sessionId']
        assert isinstance(session_id, str)
        assert call(first, 'POST', '/sessions', body, 201)['sessionId'] == session_id
        call(second, 'GET', '/sessions/' + session_id, expected=404)
        call(second, 'GET', '/runs/' + created['runId'], expected=404)
        call(second, 'PUT', '/sessions/' + session_id + '/draft', {'content': 'other user', 'expectedVersion': 0}, 404)
        checked('authentication, ownership and duplicate create')
        snapshot = wait(first, session_id)
        call(first, 'PUT', '/sessions/' + session_id + '/draft', {'content': '切页后恢复的测试草稿', 'expectedVersion': snapshot['draftVersion']})
        restored = call(first, 'GET', '/sessions/' + session_id)
        assert restored['draft'] == '切页后恢复的测试草稿'
        call(first, 'PUT', '/sessions/' + session_id + '/draft', {'content': 'stale overwrite', 'expectedVersion': snapshot['draftVersion']}, 409)
        checked('server-side draft restore and stale version protection')
        answers = [
            '这是虚构训练案例：我负责校园报名项目的新增报名接口，用Spring Boot接收参数并校验，再用MySQL保存报名记录。我负责接口实现和并发测试。发现重复点击会插入重复数据后，增加用户与活动的唯一约束，并向产品说明重复报名返回已有记录，测试确认后才上线。',
            '我选择数据库唯一约束作为最终防线，因为只用内存锁无法覆盖多个服务实例。请求先校验业务条件，再在事务内插入；遇到唯一键冲突查询原记录并返回一致结果。我比较过Redis锁，但当前规模不需要增加运维依赖。我用两个线程同时发相同报名请求，确认只有一条记录。',
            '先从日志和数据库复现重复提交，核对同一用户、活动及请求标识。客户端按钮禁用只改善体验，服务端用幂等键和数据库唯一约束兜底；同一请求返回原结果，不同内容复用同一标识返回冲突。并发测试、超时重试和事务回滚测试都要验证，确认不会生成半条记录。',
            '我先问产品哪些导出字段是本周必须交付，并与开发核对人日和依赖，再请测试明确阻断缺陷和回归范围。提出先修复核心报名缺陷，导出拆成最小范围或下周交付；把取舍、负责人、周五验收时间写入确认单，未达成一致的部分标为待确认并请负责人协调。',
            '我会先判断缺陷是否影响数据一致性及用户报名，立即告知产品、测试和负责人，不隐瞒风险。优先修复关键路径并保留回滚版本；安排测试验证受影响功能。若上线前无法通过约定的回归条件，就建议延期；若已上线出现错误率超过阈值，执行回退并同步进展。',
            '我需要把技术解释说得更清楚。下次用情境、职责、行动、结果四部分整理一段两分钟回答，并请同学复述我负责的范围和验证结论。再做一次双线程重复提交实验，保存请求结果和数据库行数；如果对方能准确复述且只生成一条记录，就达成这次练习目标。',
        ]
        for index, answer in enumerate(answers):
            snapshot = call(first, 'GET', '/sessions/' + session_id)
            body = {'content': answer, 'clientRequestId': str(uuid.uuid4()), 'expectedVersion': snapshot['version']}
            accepted = call(first, 'POST', '/sessions/' + session_id + '/turns', body, 202)
            assert call(first, 'POST', '/sessions/' + session_id + '/turns', body, 202)['runId'] == accepted['runId']
            snapshot = wait(first, session_id)
            assert snapshot['answeredCount'] == index + 1
            assert len([message for message in snapshot['messages'] if message['role'] == 'user']) == index + 1
            checked('platform interview round ' + str(index + 1))
        body = {'clientRequestId': str(uuid.uuid4()), 'expectedVersion': snapshot['version']}
        evaluation = call(first, 'POST', '/sessions/' + session_id + '/finish', body, 202)
        assert call(first, 'POST', '/sessions/' + session_id + '/finish', body, 202)['runId'] == evaluation['runId']
        snapshot = wait(first, session_id)
        report['evaluationStatus'] = snapshot['evaluation']['status']
        report['evaluationMessage'] = snapshot['evaluation']['message']
        if snapshot['status'] != 'completed':
            # Synthetic fixture only: retain the exact failing protocol before scoped cleanup.
            with connection.cursor() as cursor:
                cursor.execute('SELECT r.raw_result FROM training_run r JOIN training_session s ON s.id=r.session_id WHERE r.id=%s AND s.user_id=%s',
                               (evaluation['runId'], users[0][0]))
                raw = cursor.fetchone()[0]
            encrypted = base64.b64decode(raw.removeprefix('g1:'))
            diagnostic = AESGCM(env['AES_KEY'].encode()).decrypt(encrypted[:12], encrypted[12:], None).decode()
            (result_dir() / 'training-smoke-diagnostic.json').write_text(diagnostic, encoding='utf-8')
        assert snapshot['status'] == 'completed', 'Evaluation requires review: ' + snapshot['evaluation']['message']
        assert snapshot['evaluation']['status'] == 'valid'
        assert snapshot['evaluation']['profileApplyStatus'] == 'skipped'
        checked('structured score, evidence, durable result and no profile overwrite')
        reopened = call(first, 'GET', '/sessions/' + session_id)
        assert reopened['evaluation'] == snapshot['evaluation'] and reopened['messages'] == snapshot['messages']
        checked('reopened session returns the same persisted transcript and score')
        for template_id, scenario_answers, artifact in workplace_cases():
            created = call(first, 'POST', '/sessions', {'templateId': template_id, 'clientRequestId': str(uuid.uuid4()), 'useForProfile': True}, 201)
            scenario_id = created['sessionId']
            state = wait(first, scenario_id)
            call(second, 'PUT', '/sessions/' + scenario_id + '/artifact-draft', {'content': artifact, 'expectedVersion': 0}, 404)
            for index, answer in enumerate(scenario_answers):
                call(first, 'POST', '/sessions/' + scenario_id + '/turns', {'content': answer, 'clientRequestId': str(uuid.uuid4()), 'expectedVersion': state['version']}, 202)
                state = wait(first, scenario_id)
                checked(template_id + ' platform round ' + str(index + 1))
            call(first, 'PUT', '/sessions/' + scenario_id + '/artifact-draft', {'content': artifact, 'expectedVersion': 0})
            saved = call(first, 'POST', '/sessions/' + scenario_id + '/artifacts', {'content': artifact, 'clientRequestId': str(uuid.uuid4()), 'expectedRevision': 0}, 201)
            call(second, 'GET', '/sessions/' + scenario_id + '/artifacts/1', expected=404)
            state = call(first, 'GET', '/sessions/' + scenario_id)
            call(first, 'POST', '/sessions/' + scenario_id + '/finish', {'clientRequestId': str(uuid.uuid4()), 'expectedVersion': state['version']}, 202)
            state = wait(first, scenario_id)
            assert state['evaluation']['status'] == 'valid', template_id + ': ' + state['evaluation']['message']
            assert any(e['sourceType'] == 'artifact' and e['sourceId'] == saved['id'] for e in state['evaluation']['result']['evidence'])
            if template_id.startswith('office'):
                assert all(f['passed'] for f in state['evaluation']['result']['factChecks'])
            checked(template_id + ' frozen artifact, evidence and structured evaluation')
            call(first, 'POST', '/sessions/' + scenario_id + '/profile/retry')
            state = call(first, 'GET', '/sessions/' + scenario_id)
            assert state['profileApplication']['status'] == 'skipped'  # Fixture deliberately has no baseline.
            checked(template_id + ' missing baseline does not fabricate ability scores')
        canceled = call(first, 'POST', '/sessions', {'templateId': templates[0]['id'], 'clientRequestId': str(uuid.uuid4())}, 201)
        call(first, 'POST', '/sessions/' + canceled['sessionId'] + '/cancel')
        assert call(first, 'GET', '/sessions/' + canceled['sessionId'])['status'] == 'canceled'
        checked('explicit cancel is persisted')
        report['passed'] = True
    except Exception as error:
        report['passed'] = False
        report['failure'] = str(error)[:250]
        raise
    finally:
        # Scope every deletion to newly-created accounts with the exact random fixture name.
        # Mark fixture executions canceled; late callbacks are rejected by state/ownership checks.
        for user_id, account in users:
            with connection.cursor() as cursor:
                cursor.execute('SELECT COUNT(*) FROM user WHERE id=%s AND user_account=%s', (user_id, account))
                assert cursor.fetchone()[0] == 1, 'Fixture cleanup ownership check failed'
                cursor.execute("UPDATE training_session SET status='canceled' WHERE user_id=%s AND status IN ('active','scoring')", (user_id,))
                cursor.execute("UPDATE training_run r JOIN training_session s ON s.id=r.session_id SET r.status='canceled' WHERE s.user_id=%s AND r.status IN ('queued','running')", (user_id,))
                for table in ('training_growth_link', 'training_profile_application', 'training_artifact', 'training_session_config', 'training_evaluation', 'training_turn', 'training_run'):
                    cursor.execute(f'DELETE FROM {table} WHERE session_id IN (SELECT id FROM training_session WHERE user_id=%s)', (user_id,))
                cursor.execute('DELETE FROM training_session WHERE user_id=%s', (user_id,))
                cursor.execute('DELETE FROM user WHERE id=%s AND user_account=%s', (user_id, account))
        connection.close()
        output = result_dir() / 'training-smoke-result.json'
        output.parent.mkdir(exist_ok=True)
        output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
        print('Sanitized acceptance report: ' + str(output), flush=True)


def workplace_cases():
    communication = {
        'scope': '建议本周只交付最小导出（姓名、报名时间），复杂筛选和图表移入后续讨论；先修复重复报名阻断缺陷。产品尚需确认。',
        'owners': '产品小周确认最小范围；开发小林负责修复和导出；测试小陈负责回归与验收。职责分工须各方确认，不代替其承诺。',
        'deadline': '周三上午澄清范围；开发总计1.5人日，不超过2人日；预留测试0.5天，争取周五18:00前验收。若验证未通过则延期并同步。',
        'acceptance': '重复点击只生成一条报名；导出姓名与报名时间与数据库一致；测试完成阻断缺陷与导出回归，产品确认字段后才建议发布。',
        'risks': '开发人日和测试回归时间分开安排，不假定增加资源；唯一约束需验证历史重复数据和并发；修复延期则重新评估发布窗口。',
        'openItems': '待产品确认最小范围、三方确认排期与职责、测试确认回归结论，负责人最终审批发布；未达成共识不写已同意。',
    }
    answers = [communication['scope'], communication['deadline'], communication['acceptance'], communication['risks'], communication['owners'] + communication['openItems']]
    office = {
        'summary': '虚构活动总报名54人，总意向300人，总体报名/意向转化率18%。社群转化率25%最高，公众号20%，短视频10%。样本不足以推断增加投放一定有效，会议未决定是否增加投放。',
        'actionItems': '小林周三18:00前完成复盘摘要；小陈核对54人、18%及渠道口径与会议决策。验收时逐项复算并对照纪要，不将建议写成已批准事项。',
        'totalRegistrations': '54', 'overallConversion': '18%', 'bestChannel': '社群', 'investmentDecision': '未决定，需进一步讨论',
        'verification': '对照原始材料：24+20+10=54；意向120+80+100=300；54/300=18%。社群20/80=25%，公众号24/120=20%，短视频10/100=10%。按纪要，增加投放尚未决定。修正了预设稿的64人、21.3%、短视频最高和已批准四项错误。人负责核验和决定，AI仅整理初稿。',
    }
    prompts = ['请为活动运营负责人写简短复盘摘要及待办，用给定三个渠道数据和会议纪要。统一转化率为报名/意向，列计算步骤、负责人和时间；不得补造数据、推断因果或把未决事项写成批准。', office['verification'], office['summary'] + office['actionItems'] + '我将按核验清单修订作品，今后复用先明确口径再独立复算的方法。']
    return [('communication_release.v1', answers, communication), ('office_review.v1', prompts, office)]


if __name__ == '__main__':
    main()
