import importlib.util
from pathlib import Path
import unittest
spec = importlib.util.spec_from_file_location('upgrade', Path(__file__).with_name('check-apk-upgrade.py'))
upgrade = importlib.util.module_from_spec(spec)
spec.loader.exec_module(upgrade)

class UpgradeTest(unittest.TestCase):
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
