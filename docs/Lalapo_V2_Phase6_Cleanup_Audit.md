# Lalapo v2 — Phase 6 Cleanup Audit

Date: 26 September 2026

## Scope

Phase 6 follows the frozen Lalapo v2 blueprint:

- remove reader/manga i18n that is demonstrably dead;
- audit orphan utilities and dependencies;
- remove stale fork-era conventions when they no longer help;
- rename internal symbols gradually only when safe;
- update Lalapo documentation, changelog, licensing attribution, and contribution guidance.

Runtime QA is intentionally scheduled after Phase 6 source/CI completion for this implementation pass.

## Cleanup decisions

### Removed

- `app/src/main/java/eu/kanade/tachiyomi/util/ReaderPageImageView.kt`.
  - No active Lalapo navigation or anime playback surface owns this reader image-view utility.
  - The app is anime-only and no reader screen remains in the repository tree.
- `subsampling-scale-image-view` app dependency and version-catalog entry.
  - It was retained for the orphan reader image view.
- The delimited Mihon `Reader section` from the base i18n resource file.
  - Build/resource generation is used as the second reference audit; any still-live identifier must be restored explicitly rather than keeping the entire dead reader surface.
- Clearly manga-only Aniyomi compatibility labels for removed manga navigation, library, database, source, migration, storage, and extension surfaces.

### Retained intentionally

- Generic upstream resource identifiers that may still back anime behavior even when their historical name says `chapter` or `manga`.
  - Naming alone is not proof that a resource is dead.
- Video **chapters** terminology used by mpv/AniSkip/player chapter markers.
- `eu.kanade.tachiyomi` internal package namespace.
- `aniyomi://` and `animiru://` compatibility URI/OAuth schemes documented in `FORK_IDENTITY.md`.
- Historical upstream commit/PR links and relevant source attribution.

### Renamed safely

- Sync temporary cache filename from `animiru_sync_data.proto.gz` to `lalapo_sync_data.proto.gz`.
  - This file is cache-only and is not durable backup/user data.

## Contribution convention cleanup

New Lalapo contributions no longer require wrapping every edit in historical `AM (...)` marker comments. Existing useful provenance comments can remain; stale wrappers should be removed progressively when the surrounding code is touched.

Git history, changelog entries, NOTICE, and upstream links are the preferred attribution mechanisms.

## Validation

Phase 6 source completion requires:

- Spotless/code format green;
- unit tests green;
- SQLDelight migration verification green;
- APK build and artifact upload green.

Runtime QA is deferred until after Phase 6 per the current implementation plan. That final pass must include phone behavior, responsive/tablet surfaces that changed, light/dark/AMOLED themes, empty/loading/error/offline states, and relevant navigation/back/deep-link paths.
