"""Database startup safety regressions; mocks never connect to a database."""
import contextlib
import io
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import manage


class DatabaseStartupSafetyTest(unittest.TestCase):
    def setUp(self):
        self.stack = contextlib.ExitStack()
        self.addCleanup(self.stack.close)
        self.stack.enter_context(contextlib.redirect_stdout(io.StringIO()))
        self.stack.enter_context(patch.object(manage, '_find_mysql', return_value='mysql'))
        self.stack.enter_context(patch.object(manage, '_db_params', return_value={
            'db': 'test_fixture', 'user': 'test', 'host': 'localhost', 'port': 3306}))
        self.import_file = self.stack.enter_context(patch.object(manage, '_mysql_file', return_value=True))

    def test_partial_existing_database_is_never_rebuilt(self):
        with patch.object(manage, '_mysql_scalar', return_value=12):
            self.assertFalse(manage.db_seed())
        self.import_file.assert_not_called()

    def test_empty_database_can_initialize(self):
        with patch.object(manage, '_mysql_scalar', side_effect=[0, 40, 0, 9958]):
            self.assertTrue(manage.db_seed())
        self.assertEqual(self.import_file.call_args_list[0].args, (manage.STRUCTURE_SQL,))

    def test_existing_database_only_applies_migrations(self):
        with patch.object(manage, '_mysql_scalar', side_effect=[40, 9958]):
            self.assertTrue(manage.db_seed())
        self.assertNotIn((manage.STRUCTURE_SQL,), [c.args for c in self.import_file.call_args_list])
        self.assertNotIn((manage.DATA_SQL,), [c.args for c in self.import_file.call_args_list])

    def test_failed_migration_blocks_startup(self):
        self.import_file.return_value = False
        with patch.object(manage, '_mysql_scalar', side_effect=[40, 9958]):
            self.assertFalse(manage.db_seed())

    def test_missing_migration_blocks_startup(self):
        self.assertFalse(manage._apply_migrations([Path('missing-test-migration.sql')]))
        self.import_file.assert_not_called()


if __name__ == '__main__':
    unittest.main()
