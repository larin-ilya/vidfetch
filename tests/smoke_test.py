#!/usr/bin/env python3
"""End-to-end smoke test for the VidFetch engine.

Spawns engine.py, exercises ping / analyze / download (audio->mp3), and asserts
a terminal 'done' event with an existing file on disk.
"""
import json
import os
import subprocess
import sys
import tempfile
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ENGINE = os.path.join(HERE, '..', 'engine', 'engine.py')
FFMPEG = os.path.join(HERE, '..', 'build', 'ffmpeg')

URL = os.environ.get('VIDFETCH_TEST_URL', 'https://www.youtube.com/watch?v=jNQXAC9IVRw')


def main():
    env = dict(os.environ, VIDFETCH_FFMPEG_DIR=FFMPEG)
    proc = subprocess.Popen(
        [sys.executable, ENGINE],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        text=True, encoding='utf-8', env=env,
    )

    def send(obj):
        proc.stdin.write(json.dumps(obj) + '\n')
        proc.stdin.flush()

    def read_until(cond, timeout=240):
        deadline = time.time() + timeout
        lines = []
        while time.time() < deadline:
            line = proc.stdout.readline()
            if not line:
                time.sleep(0.1)
                continue
            line = line.strip()
            if not line:
                continue
            lines.append(line)
            try:
                obj = json.loads(line)
            except Exception:
                continue
            if cond(obj):
                return obj, lines
        return None, lines

    # ping
    send({'id': 1, 'method': 'ping', 'params': {}})
    r, _ = read_until(lambda o: o.get('id') == 1)
    assert r and r['ok'] and r['result']['pong'], f'ping failed: {r}'
    print('PING ok, ffmpeg=', r['result'].get('ffmpeg'))

    # analyze
    send({'id': 2, 'method': 'analyze', 'params': {'url': URL}})
    r, _ = read_until(lambda o: o.get('id') == 2)
    assert r and r['ok'], f'analyze failed: {r}'
    e = r['result'].get('entry') or {}
    print('ANALYZE type=', r['result'].get('type'), 'title=', e.get('title'))
    assert r['result']['type'] == 'video', r['result']

    # download (audio -> mp3)
    outdir = tempfile.mkdtemp(prefix='vidfetch_test_')
    send({'id': 3, 'method': 'download', 'params': {'options': {
        'url': URL,
        'out_dir': outdir,
        'audio_only': True,
        'audio_format': 'mp3',
        'audio_quality': 'best',
        'embed_metadata': True,
        'embed_thumbnail': True,
    }}})
    r, _ = read_until(lambda o: o.get('id') == 3)
    assert r and r['ok'], f'download start failed: {r}'
    did = r['result']['download_id']
    print('DOWNLOAD id=', did)

    # wait for terminal event
    ev, lines = read_until(
        lambda o: o.get('event') in ('done', 'error', 'cancelled')
        and o.get('data', {}).get('download_id') == did,
        timeout=300,
    )
    progress = [l for l in lines if '"event": "progress"' in l]
    print('PROGRESS events=', len(progress))
    print('TERMINAL=', ev)
    assert ev is not None, 'no terminal event within timeout'
    assert ev['event'] == 'done', f'expected done, got {ev}'
    fp = ev['data'].get('filepath')
    assert fp and os.path.exists(fp), f'file missing: {fp}'
    print('FILE=', fp)
    print('SIZE=', os.path.getsize(fp), 'bytes')
    print('OUTDIR=', sorted(os.listdir(outdir)))
    print('SMOKE TEST OK')

    proc.stdin.close()
    proc.terminate()


if __name__ == '__main__':
    main()
