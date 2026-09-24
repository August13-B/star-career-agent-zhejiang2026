"""Start/stop an isolated offline acceptance sandbox (Windows, MySQL 8, Java 17).

Requires requests, pymysql, cryptography, aiohttp in this Python environment.
Only imports the repository's schema and public seed into a NEW MySQL datadir.
Never reads the real .env. Ports: MySQL 13307, backend 18080, platform double 18081.
"""
import argparse
import base64
import json
import os
from pathlib import Path
import secrets
import shutil
import socket
import subprocess
import sys
import time

import pymysql
import requests
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'logs/offline-acceptance'
STATE = OUT / 'processes.json'


def wait_for(test, seconds=100):
    until = time.monotonic() + seconds
    while time.monotonic() < until:
        try:
            if test():
                return
        except Exception:
            pass
        time.sleep(.5)
    raise RuntimeError('Sandbox startup timed out; inspect its local logs')


def start(args):
    for port in (13307, 18080, 18081):
        with socket.socket() as sock:
            if sock.connect_ex(('127.0.0.1', port)) == 0:
                raise RuntimeError(f'Port {port} is already in use; no existing process will be stopped')
    OUT.mkdir(parents=True, exist_ok=True)
    if STATE.exists():
        raise RuntimeError('Stop the recorded sandbox before starting a new one')
    # A unique fresh datadir means no DROP statements can touch an existing instance.
    run = OUT / ('run-' + time.strftime('%Y%m%d-%H%M%S'))
    run.mkdir()
    data = run / 'mysql'
    data.mkdir()
    mysqld = str(Path(args.mysql_bin) / 'mysqld.exe')
    common = [mysqld, '--no-defaults', '--basedir=' + str(Path(args.mysql_bin).parent), '--datadir=' + str(data)]
    with (run / 'mysql-init.log').open('w') as log:
        subprocess.run(common + ['--initialize-insecure'], stdout=log, stderr=log, check=True,
                       creationflags=subprocess.CREATE_NO_WINDOW)
    processes = []
    def launch(label, command, env=None):
        with (run / (label + '.log')).open('w', encoding='utf-8') as log:
            child = subprocess.Popen(command, cwd=run, env=env, stdout=log, stderr=log,
                                     creationflags=subprocess.CREATE_NO_WINDOW)
        processes.append({'label': label, 'pid': child.pid})
        STATE.write_text(json.dumps({'run': str(run), 'processes': processes}), encoding='utf-8')
        return child
    try:
        launch('mysql', common + ['--port=13307', '--bind-address=127.0.0.1', '--mysqlx=OFF', '--max_allowed_packet=128M', '--console'])
        def connect():
            return pymysql.connect(host='127.0.0.1', port=13307, user='root', charset='utf8mb4',
                autocommit=True, client_flag=pymysql.constants.CLIENT.MULTI_STATEMENTS)
        def ready():
            connect().close()
            return True
        wait_for(ready)
        db = connect()
        with db.cursor() as cur:
            cur.execute('SELECT @@port, @@datadir')
            port, actual = cur.fetchone()
            assert port == 13307 and Path(actual).resolve() == data.resolve()
            for name in ('数据库结构.sql', '数据库数据.sql'):
                cur.execute((ROOT / '数据库' / name).read_text(encoding='utf-8-sig'))
                while cur.nextset():
                    pass
            password = secrets.token_hex(24)
            cur.execute("ALTER USER 'root'@'localhost' IDENTIFIED BY %s", (password,))
        db.close()
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        env = {'TEST_PLATFORM_MODE': 'offline', 'SERVER_PORT': '18080',
            'DB_URL': 'jdbc:mysql://127.0.0.1:13307/youthpath?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&allowMultiQueries=true',
            'DB_USERNAME': 'root', 'DB_PASSWORD': password,
            'RSA_PRIVATE_KEY': base64.b64encode(key.private_bytes(serialization.Encoding.DER, serialization.PrivateFormat.PKCS8, serialization.NoEncryption())).decode(),
            'RSA_PUBLIC_KEY': base64.b64encode(key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)).decode(),
            'AES_KEY': secrets.token_hex(16), 'AES_IV': secrets.token_hex(8), 'JWT_SECRET': secrets.token_hex(32),
            'TBOX_API_URL': 'http://127.0.0.1:18081', 'TBOX_API_KEY': '', 'TBOX_REPORT_TOKEN': '', 'TBOX_AGENT_ID': '',
            'TBOX_CHAT_CHANNEL': 'ws', 'TBOX_HELLO_DELAY_MS': '20', 'TBOX_TIMEOUT_SECONDS': '10',
            'MAIL_HOST': '127.0.0.1', 'MAIL_PORT': '1', 'MAIL_USERNAME': '', 'MAIL_PASSWORD': ''}
        (OUT / '.env').write_text('\n'.join(k + '=' + v for k, v in env.items()) + '\n', encoding='utf-8')
        launch('mock', [sys.executable, str(ROOT / 'scripts/offline_platform.py')])
        wait_for(lambda: requests.get('http://127.0.0.1:18081/__control', timeout=1).ok)
        jar = run / 'offline-backend.jar'
        shutil.copy2(next((ROOT / '后端/target').glob('*.jar')), jar)
        launch('backend', [str(Path(args.java_home) / 'bin/java.exe'), '-jar', str(jar)], {**os.environ, **env})
        wait_for(lambda: requests.get('http://127.0.0.1:18080/api/training/sessions', timeout=2).status_code == 401)
        print('Offline sandbox ready: backend 18080, mock 18081, database 13307', flush=True)
    except BaseException:
        stop()
        raise


def stop():
    if not STATE.exists():
        return
    state = json.loads(STATE.read_text(encoding='utf-8'))
    # MySQL on Windows may spawn another mysqld process. Shut down the verified
    # isolated server via SQL instead of assuming the initial Popen PID owns it.
    with socket.socket() as sock:
        mysql_running = sock.connect_ex(('127.0.0.1', 13307)) == 0
    if mysql_running:
        config = dict(line.split('=', 1) for line in (OUT / '.env').read_text(encoding='utf-8').splitlines() if '=' in line) if (OUT / '.env').exists() else {}
        db = None
        for password in (config.get('DB_PASSWORD', ''), ''):
            try:
                db = pymysql.connect(host='127.0.0.1', port=13307, user='root', password=password, connect_timeout=3)
                break
            except pymysql.MySQLError:
                pass
        if db is None:
            raise RuntimeError('Cannot verify isolated MySQL for shutdown; process record retained')
        with db.cursor() as cur:
            cur.execute('SELECT @@port, @@datadir')
            port, actual = cur.fetchone()
            assert port == 13307 and Path(actual).resolve() == (Path(state['run']) / 'mysql').resolve()
            cur.execute('SHUTDOWN')
        db.close()
    # Stop only recorded processes whose command still includes this unique run path
    # or this repository's mock-server path. Avoid PID reuse affecting other work.
    for child in reversed(state['processes']):
        script = "$p=Get-CimInstance Win32_Process -Filter 'ProcessId=" + str(child['pid']) + "'; "
        expected = str(ROOT / 'scripts/offline_platform.py') if child['label'] == 'mock' else state['run']
        script += "if ($p -and $p.CommandLine.Contains('" + expected.replace("'", "''") + "')) { Stop-Process -Id $p.ProcessId -Force }"
        subprocess.run(['powershell', '-NoProfile', '-Command', script], check=True, creationflags=subprocess.CREATE_NO_WINDOW)
    def stopped():
        for port in (13307, 18080, 18081):
            with socket.socket() as sock:
                if sock.connect_ex(('127.0.0.1', port)) == 0:
                    return False
        return True
    wait_for(stopped, seconds=20)
    STATE.unlink()
    print('Recorded sandbox processes stopped; test artifacts retained.', flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['start', 'stop'])
    parser.add_argument('--mysql-bin', default='C:/Program Files/MySQL/MySQL Server 8.0/bin')
    parser.add_argument('--java-home', default='D:/my/.star-career-local/jdk/jdk-17.0.20.1+1')
    args = parser.parse_args()
    start(args) if args.action == 'start' else stop()
