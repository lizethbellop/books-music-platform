import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('setup_local_env', Path(__file__).with_name('setup-local-env.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class LocalSettingsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for service in ('users', 'music', 'books', 'profile'):
            folder = self.root / 'backend' / f'{service}-service'
            folder.mkdir(parents=True)
            (folder / '.env.example').write_text('DB_PASSWORD=\nJWT_SECRET=\n')

    def settings(self):
        return list((self.root / 'backend').glob('*/.env'))

    def test_fresh_install_uses_one_key_for_all_services(self):
        module.setup(self.root)
        keys = {p.read_text().split('JWT_SECRET=')[1].strip() for p in self.settings()}
        self.assertEqual(1, len(keys))
        self.assertEqual(4, len(self.settings()))

    def test_rerun_preserves_all_existing_settings(self):
        module.setup(self.root)
        before = {p: p.read_bytes() for p in self.settings()}
        module.setup(self.root)
        self.assertEqual(before, {p: p.read_bytes() for p in self.settings()})

    def test_blank_key_reuses_existing_key_without_changing_password(self):
        module.setup(self.root)
        p = self.root / 'backend' / 'books-service' / '.env'
        p.write_text('DB_PASSWORD=local-password\nJWT_SECRET=\n')
        module.setup(self.root)
        self.assertIn('DB_PASSWORD=local-password\n', p.read_text())
        keys = {p.read_text().split('JWT_SECRET=')[1].strip() for p in self.settings()}
        self.assertEqual(1, len(keys))

    def test_conflicting_keys_do_not_modify_files(self):
        module.setup(self.root)
        p = self.root / 'backend' / 'books-service' / '.env'
        p.write_text('JWT_SECRET=conflicting\n')
        before = {p: p.read_bytes() for p in self.settings()}
        with self.assertRaises(SystemExit):
            module.setup(self.root)
        self.assertEqual(before, {p: p.read_bytes() for p in self.settings()})

if __name__ == '__main__':
    unittest.main()
