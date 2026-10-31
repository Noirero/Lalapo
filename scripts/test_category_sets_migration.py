"""Targeted SQLite regression for Lalapo 143.sqm (no Android runtime required)."""
from pathlib import Path
import sqlite3
import unittest

MIGRATION = Path(__file__).resolve().parents[1] / "data/src/main/sqldelight/tachiyomi/migrations/143.sqm"


def old_database(with_categories):
    db = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys = ON")
    assert db.execute("PRAGMA foreign_keys").fetchone()[0] == 1
    db.executescript("""
        CREATE TABLE animes (_id INTEGER PRIMARY KEY);
        CREATE TABLE categories (_id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, sort INTEGER NOT NULL, flags INTEGER NOT NULL, hidden INTEGER NOT NULL DEFAULT 0);
        INSERT INTO categories VALUES (0, '', -1, 0, 0);
        CREATE TABLE animes_categories (
            _id INTEGER NOT NULL PRIMARY KEY,
            anime_id INTEGER NOT NULL REFERENCES animes(_id) ON DELETE CASCADE,
            category_id INTEGER NOT NULL REFERENCES categories(_id) ON DELETE CASCADE
        );
        INSERT INTO animes VALUES (10);
    """)
    if with_categories:
        db.executescript("""
            INSERT INTO categories VALUES (3, 'Watching', 1, 7, 0);
            INSERT INTO categories VALUES (9, 'Later', 2, 0, 1);
            INSERT INTO animes_categories VALUES (5, 10, 3);
            INSERT INTO animes_categories VALUES (8, 10, 9);
        """)
    db.commit()
    return db


class Migration143Test(unittest.TestCase):
    def migrate(self, db):
        before_categories = db.execute("SELECT * FROM categories ORDER BY _id").fetchall()
        before_links = db.execute("SELECT * FROM animes_categories ORDER BY _id").fetchall()
        db.executescript(MIGRATION.read_text())
        self.assertEqual(before_categories, db.execute("SELECT * FROM categories ORDER BY _id").fetchall())
        self.assertEqual(before_links, db.execute("SELECT * FROM animes_categories ORDER BY _id").fetchall())
        self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())
        self.assertEqual([(1, 'Utama')], db.execute("SELECT _id, name FROM category_sets").fetchall())
        self.assertEqual(
            [(row[0], 1) for row in before_categories if row[0] > 0],
            db.execute("SELECT category_id, set_id FROM category_set_assignments ORDER BY category_id").fetchall(),
        )

    def test_existing_categories_and_memberships(self):
        db = old_database(True)
        self.migrate(db)
        db.execute("INSERT INTO categories VALUES (12, 'New', 3, 0, 0)")
        self.assertEqual((12, 1), db.execute("SELECT * FROM category_set_assignments WHERE category_id = 12").fetchone())
        with self.assertRaises(sqlite3.IntegrityError):
            db.execute("INSERT INTO category_set_assignments VALUES (0, 1)")
        with self.assertRaises(sqlite3.IntegrityError):
            db.execute("UPDATE category_set_assignments SET category_id = 0 WHERE category_id = 3")
        with self.assertRaises(sqlite3.IntegrityError):
            db.execute("UPDATE category_set_assignments SET set_id = 999 WHERE category_id = 12")
        with self.assertRaises(sqlite3.IntegrityError):
            db.execute("DELETE FROM category_sets WHERE _id = 1")
        db.execute("INSERT INTO category_sets VALUES (2, 'second', 'Second', 1, 0, 0)")
        with self.assertRaises(sqlite3.IntegrityError):
            db.execute("DELETE FROM category_sets WHERE _id = 1")
        db.execute("UPDATE category_set_assignments SET set_id = 2")
        db.execute("DELETE FROM category_sets WHERE _id = 1")
        self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())

    def test_move_then_delete_preserves_category_and_anime_membership(self):
        db = old_database(True)
        self.migrate(db)
        before = db.execute("SELECT * FROM animes_categories ORDER BY _id").fetchall()
        db.execute("INSERT INTO category_sets VALUES (2, 'other', 'Other', 1, 0, 0)")
        with self.assertRaises(sqlite3.IntegrityError):
            db.execute("DELETE FROM category_sets WHERE _id = 1")
        db.commit()
        db.execute("BEGIN")
        db.execute("UPDATE category_set_assignments SET set_id = 2 WHERE set_id = 1")
        db.execute("DELETE FROM category_sets WHERE _id = 1")
        db.commit()
        self.assertEqual(before, db.execute("SELECT * FROM animes_categories ORDER BY _id").fetchall())
        self.assertEqual([(3, 2), (9, 2)], db.execute(
            "SELECT category_id, set_id FROM category_set_assignments ORDER BY category_id"
        ).fetchall())
        self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())

    def test_failed_move_and_delete_rolls_back(self):
        db = old_database(True)
        self.migrate(db)
        db.execute("INSERT INTO category_sets VALUES (2, 'other', 'Other', 1, 0, 0)")
        db.commit()
        before = db.execute("SELECT * FROM category_set_assignments ORDER BY category_id").fetchall()
        with self.assertRaises(sqlite3.IntegrityError):
            with db:
                db.execute("UPDATE category_set_assignments SET set_id = 2 WHERE category_id = 3")
                db.execute("DELETE FROM category_sets WHERE _id = 1")
        self.assertEqual(before, db.execute(
            "SELECT * FROM category_set_assignments ORDER BY category_id"
        ).fetchall())
        self.assertEqual(2, db.execute("SELECT count(*) FROM category_sets").fetchone()[0])

    def test_fresh_schema_matches_upgrade_constraints(self):
        # Extract the production CREATE TABLE/INDEX statements from SQLDelight .sq.
        # Trigger DDL comes from the production onOpen callback.
        import re
        root = Path(__file__).resolve().parents[1]
        schema = (root / "data/src/main/sqldelight/tachiyomi/data/category_sets.sq").read_text()
        bindings = (root / "app/src/main/java/mihon/app/di/AppBindings.kt").read_text()
        fresh = old_database(False)
        upgraded = old_database(False)
        self.migrate(upgraded)
        for match in re.finditer(r"CREATE (?:TABLE|INDEX) [\s\S]*?;", schema):
            fresh.execute(match.group())
        fresh.execute(
            "INSERT INTO category_sets VALUES (1, 'lalapo:category-set:default', 'Utama', 0, 0, 0)"
        )
        triggers = re.findall(r'"""(\s*CREATE TRIGGER IF NOT EXISTS[\s\S]*?)"""', bindings)
        self.assertEqual(2, len(triggers), "Expected both production onOpen triggers")
        for trigger in triggers:
            fresh.executescript(trigger)
        def normalized_triggers(db):
            rows = db.execute(
                "SELECT name, sql FROM sqlite_master WHERE type = 'trigger' ORDER BY name"
            ).fetchall()
            return {
                name: re.sub(
                    r"\\s+", " ",
                    definition.replace("IF NOT EXISTS ", "").strip(),
                ).lower()
                for name, definition in rows
                if name in {"category_set_auto_assign", "category_set_prevent_last_delete"}
            }

        self.assertEqual(normalized_triggers(fresh), normalized_triggers(upgraded))
        for db in (fresh, upgraded):
            # Reopen-equivalent initialization: the same CREATE statements must be idempotent.
            for trigger in triggers:
                db.executescript(trigger)
            self.assertEqual(2, len(normalized_triggers(db)))
        for db in (fresh, upgraded):
            db.execute("INSERT INTO categories VALUES (5, 'New', 0, 0, 0)")
            self.assertEqual((5, 1), db.execute(
                "SELECT category_id, set_id FROM category_set_assignments WHERE category_id = 5"
            ).fetchone())
            with self.assertRaises(sqlite3.IntegrityError):
                db.execute("DELETE FROM category_sets WHERE _id = 1")
            with self.assertRaises(sqlite3.IntegrityError):
                db.execute("INSERT INTO category_set_assignments VALUES (0, 1)")
            self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())

    def test_empty_user_library(self):
        db = old_database(False)
        self.migrate(db)
        self.assertEqual([], db.execute("SELECT * FROM category_set_assignments").fetchall())



if __name__ == "__main__":
    unittest.main()
