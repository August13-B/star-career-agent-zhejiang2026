"""Loopback-only deterministic platform double. NEVER evaluates real ability.

Implements the current repository's HTTP/WS contract, not a replacement AI service.
Install aiohttp in the test Python environment. No outbound requests are made.
"""
import asyncio
import json
import uuid
from collections import Counter
from aiohttp import web, WSMsgType

MARK = '【本地模拟验收，非真实 AI 评价】'
jobs = {}
counts = Counter()
fault = {'mode': 'normal'}


def encode(value):
    return json.dumps(value, ensure_ascii=False)


async def control(request):
    if request.method == 'POST':
        fault.update(await request.json())
    return web.json_response({'mode': fault['mode'], 'counts': dict(counts), 'offline': True})


async def session(request):
    counts['session'] += 1
    if fault['mode'] == 'http_error':
        return web.json_response({'error': 'Synthetic unavailable'}, status=503)
    return web.json_response({'sessionId': 'offline-' + uuid.uuid4().hex})


async def conversation(request):
    return web.json_response({'conversationId': 'offline-' + uuid.uuid4().hex})


def answer(prompt):
    if '完整结果结构=' in prompt:
        counts['training_evaluation'] += 1
        root = json.JSONDecoder().raw_decode(prompt.split('完整结果结构=', 1)[1])[0]
        catalog = json.loads(prompt.split('冻结证据目录=', 1)[1])
        score = root['training_evaluation']
        refs = list(catalog)
        artifact = [k for k, v in catalog.items() if v['sourceType'] == 'artifact']
        score['dimensions'] = {k: 80 for k in score['dimensions']}
        score['total'] = 1  # Deliberately wrong: application must compute its own weighted score.
        score['comment'] = MARK + '固定分数，仅验证评分协议、证据及落库。'
        score['suggestions'] = ['核对原始材料并记录验证过程。']
        score['evidence'] = [{'dimension': k, 'evidenceId': (artifact or refs)[i % len(artifact or refs)]}
                             for i, k in enumerate(score['dimensions'])]
        if fault['mode'] == 'invalid_evidence':
            score['evidence'][0]['evidenceId'] = 'DOES_NOT_EXIST'
        return encode(root)
    if '这是独立职场模拟训练' in prompt:
        counts['training_round'] += 1
        return MARK + '请结合当前任务说明你的处理步骤、依据及验证方法；回答已由本地测试链路接收。'
    if 'matchedJobs' in prompt:
        counts['job_compare'] += 1
        return encode({'analysis': MARK + '固定岗位对比结果，用于验证解析和展示。',
                       'matchedJobs': [{'profileId': '232745912056446976', 'positionName': '后端开发', 'similarity': 0.8}]})
    if 'educationScore' in prompt or '学生画像和能力维度' in prompt:
        counts['ability_score'] += 1
        return encode({**{k + 'Score': 80 for k in ('education', 'internship', 'professional', 'certificate',
            'innovation', 'learning', 'pressure', 'communication', 'problemSolving', 'teamwork', 'total')}, 'scoreComment': MARK})
    counts['chat'] += 1
    return MARK + '已接收职业咨询。建议梳理项目职责、技术选择及验证记录，再制定练习计划。'


async def websocket(request):
    if fault['mode'] == 'ws_reject':
        return web.Response(status=503, text='Synthetic handshake failure')
    ws = web.WebSocketResponse()
    await ws.prepare(request)
    async for message in ws:
        if message.type != WSMsgType.TEXT:
            continue
        event = json.loads(message.data)
        if event.get('type') != 'SEND_MESSAGE':
            continue
        mode = fault['mode']
        counts['ws_runs'] += 1
        if mode == 'run_error':
            await ws.send_json({'type': 'RUN_ERROR', 'message': 'Synthetic upstream error'})
            break
        content = answer(event.get('content', ''))
        for start in range(0, len(content), 97):
            await ws.send_json({'type': 'TEXT_MESSAGE_CONTENT', 'delta': content[start:start + 97]})
            if mode == 'disconnect':
                await ws.close()
                return ws
            await asyncio.sleep(.005)
        await ws.send_json({'type': 'RUN_FINISHED', 'rawEvent': {'requestId': uuid.uuid4().hex}})
        break
    await ws.close()
    return ws


async def start_report(request):
    body = await request.json()
    key = 'offline-job-' + uuid.uuid4().hex
    jobs[key] = {'userId': body.get('userId'), 'polls': 0, 'canceled': False}
    counts['report'] += 1
    return web.json_response({'jobId': key})


async def report(request):
    key = request.match_info['job']
    job = jobs[key]
    if request.method == 'POST':
        job['canceled'] = True
        return web.json_response({'status': 'canceled'})
    job['polls'] += 1
    keys = ['profile_analysis', 'career_exploration', 'goal_setting', 'path_planning', 'action_planning', 'report_composition']
    goals = {'targetJob': '后端开发（模拟）', 'goals': [
        {'horizon': f'{year}年', 'title': f'{year}年模拟目标', 'goal': '完成学习和验证',
         'criteria': '提交可复现测试记录', 'skills': ['Java'], 'keyActions': ['完成练习并验证结果']}
        for year in (1, 3, 5)]}
    agents = [{'key': k, 'name': k, 'content': MARK + '\n## 模拟分析\n已有虚构资料足以测试报告生成与导出。'} for k in keys]
    agents[-1]['content'] += '\n<<<GOALS_JSON>>>' + encode(goals) + '<<<END_GOALS_JSON>>>'
    done = min(6, job['polls'] * 2)
    return web.json_response({'status': 'canceled' if job['canceled'] else ('done' if done == 6 else 'running'),
        'userId': job['userId'], 'reportId': key, 'reportName': MARK + '职业规划报告',
        'progressChars': 100 * done, 'currentAgent': keys[done - 1], 'agentsDone': keys[:done],
        'deltas': [{'agent': a['key'], 'data': a['content']} for a in agents[:done]],
        'segmentChars': {a['key']: len(a['content']) for a in agents[:done]},
        'content': {'agents': agents, 'final': MARK + '完整模拟报告'}})


async def delete_reports(request):
    body = await request.json()
    return web.json_response({'deleted': body.get('reportIds', []), 'failed': []})


if __name__ == '__main__':
    app = web.Application()
    app.add_routes([web.get('/__control', control), web.post('/__control', control),
        web.get('/api/tbox/session', session), web.post('/api/conversation/create', conversation),
        web.get('/ws', websocket), web.post('/api/report', start_report),
        web.get('/api/report/jobs/{job}', report), web.post('/api/report/jobs/{job}/cancel', report),
        web.post('/api/report/delete', delete_reports)])
    web.run_app(app, host='127.0.0.1', port=18081, print=None)
