#!/usr/bin/env python3
"""Smoke-test the real native reader through Android accessibility semantics."""
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = 'com.caceras.aibrief'
OUT = Path('device-evidence')
OUT.mkdir(exist_ok=True)
OUT.joinpath('result.json').unlink(missing_ok=True)
checks = []
ADB_TIMEOUT = int(os.environ.get('AIBRIEF_DEVICE_TIMEOUT', '45'))


def adb(*args, binary=False):
    return subprocess.check_output(['adb', *args], text=not binary, timeout=ADB_TIMEOUT)


def hierarchy():
    adb('shell', 'uiautomator', 'dump', '/sdcard/aibrief-window.xml')
    raw = adb('shell', 'cat', '/sdcard/aibrief-window.xml')
    OUT.joinpath('last-window.xml').write_text(raw)
    return ET.fromstring(raw)


def visible(needle, root=None):
    root = hierarchy() if root is None else root
    return [node for node in root.iter('node')
            if needle in (node.get('text', '') + ' ' + node.get('content-desc', ''))
            and node.get('bounds', '[0,0][0,0]') != '[0,0][0,0]']


def require_text(needle):
    for _ in range(6):
        if visible(needle):
            checks.append('Visible: ' + needle)
            return
        time.sleep(1)
    raise AssertionError('Not visible: ' + needle)


def tap(needle):
    candidates = visible(needle)
    if not candidates:
        raise AssertionError('Cannot tap: ' + needle)
    node = next((n for n in candidates if n.get('clickable') == 'true'), candidates[0])
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(1)


def capture(name):
    OUT.joinpath(name + '.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))


def scroll_until(needle, reverse=False):
    for _ in range(12):
        if visible(needle):
            checks.append('Readable after scrolling: ' + needle)
            return
        start, end = ('650', '1550') if reverse else ('1550', '650')
        adb('shell', 'input', 'swipe', '540', start, '540', end, '350')
        time.sleep(.6)
    raise AssertionError('Cannot read: ' + needle)


try:
    adb('install', '--no-streaming', '-r', 'app/build/outputs/apk/internalSharing/app-internalSharing.apk')
    adb('shell', 'wm', 'size', '1080x1920')
    adb('shell', 'settings', 'put', 'secure', 'show_ime_with_hard_keyboard', '1')
    adb('shell', 'svc', 'wifi', 'disable')
    adb('shell', 'svc', 'data', 'disable')
    adb('shell', 'pm', 'clear', PACKAGE)
    adb('logcat', '-c')
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    time.sleep(10)  # allow the bounded first refresh to fail while offline
    lead = json.loads(Path('content/posts.json').read_text())['posts'][0]['title']
    require_text('A clearer view of AI.')
    require_text('Offline edition')
    require_text(lead)
    capture('01-feed')
    tap(lead)
    require_text('Save article')
    capture('02-reader')
    tap('Save article')
    require_text('Remove from saved')
    scroll_until('Primary source: NIST')
    capture('03-article-sources')
    adb('shell', 'input', 'keyevent', '4')
    time.sleep(1)
    tap('Saved')
    require_text(lead)
    capture('04-saved')
    # A process recreation must retain the saved article.
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    time.sleep(2)
    tap('Saved')
    require_text(lead)
    checks.append('Saved article survives process recreation')
    tap('About')
    scroll_until('Read the privacy policy')
    require_text('Privacy')
    capture('05-about')
    tap('Latest') if visible('Latest') else adb('shell', 'input', 'keyevent', '4')
    # Back from a non-home tab returns to the edition.
    require_text('A clearer view of AI.')
    tap('Search articles')
    inputs = [n for n in hierarchy().iter('node') if n.get('class') == 'android.widget.EditText']
    if not inputs:
        raise AssertionError('Search input missing')
    node = inputs[0]
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    adb('shell','input','text','benchmark')
    adb('shell','input','keyevent','4')
    time.sleep(1)
    scroll_until('A model score is a starting point')
    capture('06-search')
    scroll_until('Close search', reverse=True)
    tap('Close search')
    adb('shell','settings','put','system','font_scale','2.0')
    adb('shell','am','force-stop',PACKAGE)
    adb('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
    time.sleep(2)
    require_text('AI Brief')
    capture('07-large-text')
    adb('shell','settings','put','system','font_scale','1.0')
    adb('shell','cmd','uimode','night','yes')
    adb('shell','am','force-stop',PACKAGE)
    adb('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
    time.sleep(2)
    require_text('A clearer view of AI.')
    capture('08-dark-feed')
    checks.append('Large text and dark mode render')
    log = adb('logcat','-d','-s','AndroidRuntime:E')
    if 'Process: '+PACKAGE in log:
        raise AssertionError('App crash in logcat')
    checks.append('No AI Brief crash in logcat')
    OUT.joinpath('result.json').write_text(json.dumps({'passed': True,'checks':checks,
        'source_sha':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),
        'android_sdk':adb('shell','getprop','ro.build.version.sdk').strip(),
        'page_size':adb('shell','getconf','PAGESIZE').strip()},indent=2)+'\n')
finally:
    OUT.joinpath('android-runtime.log').write_text(adb('logcat','-d','-s','AndroidRuntime:E'))
    if not OUT.joinpath('result.json').exists():
        OUT.joinpath('result.json').write_text(json.dumps({'passed':False,'checks':checks},indent=2)+'\n')
