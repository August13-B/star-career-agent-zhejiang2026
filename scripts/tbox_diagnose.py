"""Read-only Tbox routing diagnostic. Never sends credentials or model requests."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
import json
from pathlib import Path
import re
import socket
import ssl
import sys
import time
from urllib.parse import urlsplit

import requests

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import manage


def diagnose(base):
    base = base.strip().rstrip('/')
    url = urlsplit(base)
    if url.scheme not in ('http', 'https') or not url.hostname or url.username or url.password or url.query or url.fragment:
        raise ValueError('Expected an HTTP(S) application base URL without credentials, query or fragment')
    result = {'checkedAt': datetime.now().astimezone().isoformat(), 'baseUrl': base, 'credentialsSent': False}
    port = url.port or (443 if url.scheme == 'https' else 80)
    try:
        result['dns'] = sorted({entry[4][0] for entry in socket.getaddrinfo(url.hostname, port)})
    except OSError as error:
        result['dnsError'] = type(error).__name__
    if url.scheme == 'https':
        try:
            with socket.create_connection((url.hostname, port), timeout=10) as tcp:
                with ssl.create_default_context().wrap_socket(tcp, server_hostname=url.hostname) as tls:
                    result['tls'] = {'verified': True, 'version': tls.version(), 'expires': tls.getpeercert().get('notAfter')}
        except (OSError, ssl.SSLError) as error:
            result['tls'] = {'verified': False, 'errorType': type(error).__name__}

    def probe(item):
        path, direct = item
        row = {'path': path, 'direct': direct}
        start = time.monotonic()
        try:
            with requests.Session() as session:
                session.trust_env = not direct
                # Disable .netrc authentication even when comparing system proxy routing.
                session.auth = lambda request: request
                with session.get(base + path, timeout=(10, 20), allow_redirects=False, stream=True) as response:
                    row['status'] = response.status_code
                    row['headers'] = {key: value for key, value in response.headers.items()
                                      if key.lower() in ('server', 'date', 'content-type', 'retry-after', 'via')}
                    # Store only an allowlisted gateway signature, never arbitrary app content.
                    body = next(response.iter_content(chunk_size=4096), b'').decode('utf-8', errors='replace')
                    missing = re.search(r'Agent not found:\s*([A-Za-z0-9_-]{1,128})', body, re.I)
                    if missing:
                        row['gatewayError'] = 'Agent not found: ' + missing.group(1)
                    if path == '/api/health' and response.status_code == 200:
                        try:
                            row['healthOk'] = json.loads(body).get('status') == 'ok'
                        except (ValueError, AttributeError):
                            row['healthOk'] = False
        except requests.RequestException as error:
            row['errorType'] = type(error).__name__
        row['seconds'] = round(time.monotonic() - start, 2)
        return row

    # The health endpoint and root are documented in 百宝箱/接口清单与接入说明.md.
    with ThreadPoolExecutor(max_workers=4) as pool:
        result['probes'] = list(pool.map(probe, [('/', False), ('/', True), ('/api/health', False), ('/api/health', True)]))
    if all(row.get('status') == 503 and row.get('gatewayError') for row in result['probes']):
        result['diagnosis'] = 'GATEWAY_AGENT_NOT_FOUND'
    elif all(row.get('status') in (401, 403) for row in result['probes']):
        result['diagnosis'] = 'AUTHENTICATION_REQUIRED'
    elif any(row.get('healthOk') for row in result['probes']):
        result['diagnosis'] = 'HEALTHY_ENTRYPOINT_AI_STILL_REQUIRES_TESTING'
    else:
        result['diagnosis'] = 'ENTRYPOINT_UNAVAILABLE_OR_UNEXPECTED_RESPONSE'
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', help='Optional current platform URL; does not modify .env')
    args = parser.parse_args()
    base = args.url or manage.load_env(manage.ENV_FILE).get('TBOX_API_URL', '')
    result = diagnose(base)
    output = ROOT / 'logs/tbox-diagnostic-result.json'
    output.parent.mkdir(exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result['diagnosis'] == 'HEALTHY_ENTRYPOINT_AI_STILL_REQUIRES_TESTING' else 1


if __name__ == '__main__':
    raise SystemExit(main())
