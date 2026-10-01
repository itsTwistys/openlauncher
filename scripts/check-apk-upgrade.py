"""Refuse publication when a preview cannot replace the pinned preview identity."""
import json
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


def load_baseline(path):
    """Use a checked-in public identity so deleted releases do not break updates."""
    data = json.loads(Path(path).read_text())
    package, version, certificates = data['application_id'], data['version_code'], data['signer_sha256']
    if (package != 'com.openlauncher.app.preview' or type(version) is not int or version <= 0
            or not isinstance(certificates, list) or not certificates
            or any(not isinstance(value, str) or not re.fullmatch(r'[0-9a-f]{64}', value)
                   for value in certificates)):
        raise ValueError('Invalid pinned preview identity')
    return package, version, sorted(certificates)


def validate(previous, candidate):
    if previous[0] != 'com.openlauncher.app.preview' or candidate[0] != previous[0]:
        raise ValueError('Preview application ID changed; refusing a separate installation')
    if candidate[2] != previous[2]:
        raise ValueError('Signing certificate changed; restore the permanent signing key; do not replace it again')
    if candidate[1] <= previous[1]:
        raise ValueError('Update versionCode must increase')


if __name__ == '__main__':
    sdk = Path(os.environ['ANDROID_HOME']) / 'build-tools' / '36.0.0'
    baseline = load_baseline(sys.argv[1]) if sys.argv[1].endswith('.json') else inspect(sys.argv[1], sdk)
    validate(baseline, inspect(sys.argv[2], sdk))
    print('Upgrade against pinned preview verified: same application ID/certificate and increased versionCode.')
