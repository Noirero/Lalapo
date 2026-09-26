<div align="center">

# Lalapo

Lalapo is an Android anime streaming and library application forked from [Animiru](https://github.com/quickdesh/Animiru), which builds on [Aniyomi](https://github.com/aniyomiorg/aniyomi) and the [Mihon](https://github.com/mihonapp/mihon) ecosystem.

**Simple by default, powerful when needed.**

[![GitHub downloads](https://img.shields.io/github/downloads/Noirero/Lalapo/total?label=downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/Noirero/Lalapo/releases/latest)
[![CI](https://img.shields.io/github/actions/workflow/status/Noirero/Lalapo/build.yml?branch=main&labelColor=27303D)](https://github.com/Noirero/Lalapo/actions/workflows/build.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/Noirero/Lalapo?labelColor=27303D&color=0877d2)](/LICENSE)

## Download

[![Lalapo](https://img.shields.io/github/release/Noirero/Lalapo.svg?maxAge=3600&label=Stable&labelColor=06599d&color=043b69)](https://github.com/Noirero/Lalapo/releases/latest)

*Requires Android 8.0 or higher.*

</div>

## Product structure

Lalapo's primary navigation is intentionally small:

- **Library** — manage saved anime and continue watching.
- **Discover** — search content, browse sources, and manage extensions.
- **Activity** — History, Updates, and Downloads.
- **More** — Private Session, Offline Mode, contextual health/status, Sync & Backup, Statistics, Settings, and About.

The redesign keeps the existing source, download, tracking, backup, and mpv engines while simplifying how those capabilities are presented.

## Highlights

- Anime extensions and local/downloaded playback.
- Continue Watching presentation in Library.
- Global search scopes for Library / Sources / All.
- Contextual player controls with mpv-backed advanced settings.
- Internal, Cast, Picture-in-Picture, and external playback targets.
- Tracking support for supported anime trackers.
- Categories, filtering, sorting, grouping, and bulk library actions.
- Downloads, backup/restore, Cross Sync, and Google Drive/SyncYomi integrations where configured.
- Private Session and Offline Mode.
- Contextual sync, extension/source, download, and low-storage status.
- Phone navigation plus adaptive/tablet presentation.
- Light, dark, AMOLED, and curated theme presentation.

## Development status

The Lalapo v2 presentation redesign is implemented through Phase 6 on `main`. The frozen UI/UX baseline is documented in [`docs/Lalapo_V2_UI_UX_Spec.md`](./docs/Lalapo_V2_UI_UX_Spec.md).

Runtime QA on physical devices is tracked separately from source/CI completion and should be completed before treating a build as release-ready.

## Release setup

Release tags, signing requirements, and repository secrets are documented in [`.github/RELEASE.md`](./.github/RELEASE.md).

## Contributing

[Code of conduct](./CODE_OF_CONDUCT.md) · [Contributing guide](./CONTRIBUTING.md) · [Changelog](./CHANGELOG.md)

Issues and pull requests for Lalapo should be filed in this repository.

## Upstream attribution

Lalapo preserves Apache-2.0 licensing and upstream attribution. Historical changelog links intentionally continue to point to the original upstream pull requests and commits where those changes were made.

- [Animiru](https://github.com/quickdesh/Animiru)
- [Aniyomi](https://github.com/aniyomiorg/aniyomi)
- [Mihon](https://github.com/mihonapp/mihon)
- Original Tachiyomi lineage and contributors

Additional attribution is recorded in [`NOTICE`](./NOTICE).

## Disclaimer

Lalapo is not affiliated with content providers available through extensions and does not host media content.

## License

Lalapo is distributed under the Apache License 2.0. See [`LICENSE`](./LICENSE).
