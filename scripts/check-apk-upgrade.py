"""Refuse publication when a preview cannot replace the known installed preview."""
import os
from pathlib import Path
import re
import subprocess
import sys


def inspect(apk, build_tools):
    badging = subprocess.check_output([str(build_tools / 'aapt'), 'dump', 'badging', apk], text=True)
    package = re.search(r"package: name='([^']+)' versionCode='(\d+)'", badging)
    if not package:
        raise ValueError('Cannot read APK application ID/version')
    signing = subprocess.check_output([str(build_tools / 'apksigner'), 'verify', '--print-certs', apk], text=True)
    certificates = sorted(re.findall(r'Signer #\d+ certificate SHA-256 digest: (\S+)', signing))
    if not certificates:
        raise ValueError('APK has no verified signing certificate')
    return package[1], int(package[2]), certificates


def validate(previous, candidate):
    if previous[0] != 'com.openlauncher.app.preview' or candidate[0] != previous[0]:
        raise ValueError('Preview application ID changed; refusing a separate installation')
    if candidate[2] != previous[2]:
        raise ValueError('Signing certificate changed; restore the existing signing key, do not uninstall')
    if candidate[1] <= previous[1]:
        raise ValueError('Update versionCode must increase')


if __name__ == '__main__':
    sdk = Path(os.environ['ANDROID_HOME']) / 'build-tools' / '36.0.0'
    validate(inspect(sys.argv[1], sdk), inspect(sys.argv[2], sdk))
    print('In-place upgrade verified: same application ID/certificate and increased versionCode.')
