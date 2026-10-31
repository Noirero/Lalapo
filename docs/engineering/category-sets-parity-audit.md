# Kelola Set — parity and compatibility audit (work in progress)

Baseline: Lalapo main `d6676fc01178e5d355dadf065806264d9c43ba26`; Miyorare main `613dc16e20354132ad0eb0266f401a19efafb44c`. Scope: category sets only; player investigations excluded. This document is evidence, not a claim of feature completion.

## Proven Miyorare source-of-truth

- `favourites/data/FavouriteCategorySetEntity.kt`: a Set has setId, syncKey, title, sortKey, createdAt, updatedAt, deletedAt, space.
- `favourites/data/FavouriteCategorySetAssignmentEntity.kt`: one category-to-Set assignment per category, with FK CASCADE on category and RESTRICT on Set; Set owns categories, not manga.
- `favourites/data/FavouriteCategorySetsDao.kt`: observe sets ordered by sortKey/syncKey, observe category counts, observe categories in a set, upsert assignments.
- `favourites/ui/sets/CategorySetsViewModel.kt`: create/rename/reorder/delete via FavouritesRepository; deletion obtains category and distinct manga impact first; mutation guard prevents concurrent operations.
- `favourites/domain/FavouriteSetSelectionStore.kt`: selected Set is persisted by stable syncKey, scoped by FavouriteSpace and FavouriteContentType, with fallback if missing after deletion/restore.
- `core/db/migrations/Migration49To50.kt`: default 'Utama' Set per space, legacy category assignment, SQL triggers for insertion, space consistency, last-Set deletion guard, and nonempty-Set deletion guard.
- `favourites/ui/sets/CategorySetSelector.kt`: choose active Set or open create/manage.
- `backup/local/data/model/CategorySetBackup.kt` and `sync/data/model/SyncModels.kt`: Set-specific backup/sync representations exist; full execution paths still require review.

## Proven Lalapo source-of-truth

- `data/src/main/sqldelight/tachiyomi/data/categories.sq`: categories(_id, name, sort, flags, hidden), system category ID 0 with delete trigger; category queries and mutations.
- `data/src/main/sqldelight/tachiyomi/data/animes_categories.sq`: anime-to-category join with FK CASCADE on both sides. Existing category IDs are referenced by these rows.
- `domain/src/main/java/tachiyomi/domain/category/model/Category.kt`: Category(id,name,order,flags,hidden).
- `domain/src/main/java/tachiyomi/domain/category/repository/CategoryRepository.kt` and `data/src/main/java/tachiyomi/data/category/CategoryRepositoryImpl.kt`: existing category CRUD, ordering and visibility; SQLDelight rather than Miyorare Room.
- `app/src/main/java/eu/kanade/tachiyomi/ui/category/CategoryScreen.kt`: existing Compose create/rename/delete/hide/reorder UI.
- `app/src/main/java/eu/kanade/tachiyomi/data/backup/models/BackupCategory.kt`: protobuf fields 1 name, 2 order, 3 id, 100 flags; no Set information.
- `app/src/main/java/eu/kanade/tachiyomi/data/backup/restore/restorers/CategoriesRestorer.kt`: restore matches existing categories by name, inserts missing ones; category IDs may differ from backup IDs.
- `app/src/main/java/eu/kanade/tachiyomi/data/backup/create/creators/CategoriesBackupCreator.kt`: backs up non-system categories.
- `app/src/main/java/eu/kanade/tachiyomi/data/backup/models/Backup.kt`: protobuf backupCategories field 502; optional new fields must use distinct unused numbers.

## Parity checklist

| Behavior | Status in Lalapo | Evidence / next action |
| --- | --- | --- |
| Category CRUD/reorder | Available | Existing category repository and Compose UI |
| Set CRUD/reorder | Needed | Miyorare CategorySetsViewModel / FavouritesRepository |
| Category assignment and movement between Sets | Needed | Miyorare assignment entity and repository; audit mutation rules |
| Set selection persisted | Needed | Miyorare FavouriteSetSelectionStore; adapt scope for Lalapo anime-only model |
| Default Set and legacy category backfill | Needed | Miyorare Migration49To50; design SQLDelight migration |
| Deletion impact and active-set fallback | Needed | Miyorare ViewModel and selection store |
| Favorites filtering/navigation by Set | Adaptation | Lalapo library flows and navigation not yet fully traced |
| Set backup/restore with remapped category IDs | Needed | Existing name-based restore requires explicit old-to-new mapping |
| Import/export/sync parity | Unverified | Inspect Miyorare and Lalapo supported integrations |
| Validation, duplicate titles, empty state | Unverified | Inspect repository, dialogs and UI tests |

## Safety invariants before schema changes

1. Preserve every existing category ID, name, order, flags, hidden value and anime membership.
2. Add Set identity and category assignment without replacing `categories` or `animes_categories`.
3. Assign every non-system category to exactly one valid Set; never assign the system category as a user category.
4. Prevent deletion of the final Set; implement explicit move-or-delete semantics with transactionality for nonempty Sets.
5. Persist active Set using stable identity and recover from missing/deleted selections.
6. Extend protobuf backup compatibly; old backups must restore into the default Set, and restored categories must be remapped to valid IDs before assignments.
7. Verify actual SQLDelight migration version/path and generated API before committing schema or Kotlin implementation.

## Remaining verification

Trace Miyorare `FavouritesRepository` set mutations and dialog validation; Lalapo library filter/navigation and SQLDelight migration configuration; backup restore sequencing and sync/export behavior. No data schema or production code has been modified by this audit.
