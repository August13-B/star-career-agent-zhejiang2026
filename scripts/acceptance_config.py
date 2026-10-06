"""Explicit isolated configuration for acceptance runs; normal defaults are unchanged."""
import os
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]


def load_test_env():
    import manage
    env = manage.load_env(Path(os.environ.get('ACCEPTANCE_ENV_FILE', manage.ENV_FILE)))
    if env.get('TEST_PLATFORM_MODE') == 'offline':
        db = urlparse(env['DB_URL'].removeprefix('jdbc:'))
        platform = urlparse(env['TBOX_API_URL'])
        assert db.hostname == '127.0.0.1' and db.port == 13307, 'Offline DB must be isolated'
        assert platform.hostname == '127.0.0.1' and platform.port == 18081
        assert env['SERVER_PORT'] == '18080'
    return env


def result_dir():
    # Alternate configurations never overwrite previous real-platform evidence.
    path = ROOT / 'logs' / ('offline-acceptance' if os.environ.get('ACCEPTANCE_ENV_FILE') else '')
    path.mkdir(parents=True, exist_ok=True)
    return path
