"""Run the MySQL-backed suite with .env loaded, without printing credentials."""
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import manage
from acceptance_config import load_test_env, result_dir


def main():
    env = {**os.environ, **load_test_env(), 'TRAINING_DB_TEST': 'true'}
    port = int(env.get('SERVER_PORT', '8080'))
    if manage._port_listening(port):
        raise SystemExit('Stop the local backend before DB tests: python manage.py stop backend')
    for name in ('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'RSA_PRIVATE_KEY', 'RSA_PUBLIC_KEY', 'AES_KEY', 'AES_IV', 'JWT_SECRET'):
        if not env.get(name):
            raise SystemExit('Missing configuration: ' + name)
    backend = ROOT / '后端'
    wrapper = backend / ('mvnw.cmd' if os.name == 'nt' else 'mvnw')
    log = result_dir() / 'project-acceptance-tests.log'
    log.parent.mkdir(exist_ok=True)
    with log.open('w', encoding='utf-8') as out:
        code = subprocess.run([str(wrapper), '-B', '-ntp', 'test'], cwd=backend,
                              env=env, stdout=out, stderr=subprocess.STDOUT).returncode
    print('Backend tests exit:', code, '| report:', log)
    return code


if __name__ == '__main__':
    raise SystemExit(main())
