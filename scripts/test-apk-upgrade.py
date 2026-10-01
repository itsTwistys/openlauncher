import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
spec = importlib.util.spec_from_file_location('upgrade', Path(__file__).with_name('check-apk-upgrade.py'))
upgrade = importlib.util.module_from_spec(spec)
spec.loader.exec_module(upgrade)

class UpgradeTest(unittest.TestCase):
    def test_pinned_baseline_and_original_certificate(self):
        baseline = upgrade.load_baseline(Path(__file__).with_name('preview-baseline.json'))
        self.assertEqual(baseline[:2], ('com.openlauncher.app.preview', 16))
        self.assertEqual(baseline[2], ['aeb220393976d7db6c4247785c1c3ab3ef6b9242ea3003dfaae23288fa2e43ca'])
        upgrade.validate(baseline, (baseline[0], 18, baseline[2]))
        with self.assertRaises(ValueError):
            upgrade.validate(baseline, (baseline[0], 18, ['0' * 64]))

    def test_invalid_baseline_fails_closed(self):
        valid = {'application_id': 'com.openlauncher.app.preview', 'version_code': 16,
                 'signer_sha256': ['a' * 64]}
        for field, value in [('application_id', 'another.app'), ('version_code', '16'),
                             ('version_code', True), ('version_code', 0),
                             ('signer_sha256', []), ('signer_sha256', ['short']),
                             ('signer_sha256', 'a' * 64)]:
            with self.subTest(field=field, value=value), tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / 'baseline.json'
                path.write_text(json.dumps(dict(valid, **{field: value})))
                with self.assertRaises(ValueError):
                    upgrade.load_baseline(path)

    def test_accepts_same_identity_and_newer_version(self):
        upgrade.validate(('com.openlauncher.app.preview', 16, ['known']), ('com.openlauncher.app.preview', 17, ['known']))
    def test_rejects_changed_identity_signer_or_downgrade(self):
        previous = ('com.openlauncher.app.preview', 16, ['known'])
        for candidate in [('other.app', 17, ['known']), ('com.openlauncher.app.preview', 17, ['new']),
                          ('com.openlauncher.app.preview', 16, ['known']), ('com.openlauncher.app.preview', 15, ['known'])]:
            with self.subTest(candidate=candidate), self.assertRaises(ValueError):
                upgrade.validate(previous, candidate)

if __name__ == '__main__':
    unittest.main()
