# Contributing to Lalapo

Thanks for helping improve Lalapo.

## Where to contribute

Use this repository for Lalapo issues and pull requests:

- Issues: https://github.com/Noirero/Lalapo/issues
- Pull requests: https://github.com/Noirero/Lalapo/pulls

Upstream bugs that also reproduce unchanged in Animiru, Aniyomi, or Mihon may be reported upstream when appropriate, but Lalapo-specific presentation, identity, release, and integration changes belong here.

## Prerequisites

Contributors should be comfortable with:

- Android development
- Kotlin
- Jetpack Compose
- Gradle

Recommended tools:

- Android Studio
- An emulator or Android phone with developer options enabled

## Development principles

Lalapo follows the product principle **Simple by default, powerful when needed**.

When changing presentation code:

- preserve source, playback, library, download, tracking, backup, and sync behavior unless the change explicitly requires domain work;
- keep phone and adaptive/tablet navigation coherent;
- avoid adding permanent dashboard cards for status that can be contextual;
- keep uncommon power-user controls discoverable without making them primary UI;
- preserve compatibility URI schemes documented in `docs/FORK_IDENTITY.md`.

Do not mass-rename the `eu.kanade.tachiyomi` package or compatibility schemes only for branding. They are internal/upstream compatibility surfaces and should be changed only with a concrete migration plan.

## Code style

Run:

```bash
./gradlew spotlessApply
```

before submitting changes.

The historical `AM (...)` marker convention from Animiru is **not required for new Lalapo code**. Existing attribution comments may remain when they still convey useful origin/context, but stale wrapper comments should be removed as files are touched.

Prefer clear commit history and normal Git attribution over adding new marker blocks around every edit.

## Quality gate

Before a change is considered ready:

- code formatting passes;
- unit tests pass;
- SQLDelight migration verification passes;
- the app builds successfully;
- affected navigation/back/deep-link behavior is checked;
- empty/loading/error/offline states are considered;
- phone runtime QA is performed for release candidates;
- tablet/adaptive and light/dark/AMOLED states are checked when affected.

## Translations

Lalapo currently inherits translation infrastructure from its upstream projects. New Lalapo-specific strings should be added to the appropriate Lalapo/Animiru resource module and kept semantically anime-first.

Do not reintroduce manga/reader-only strings without a live code path that requires them.

## Licensing and attribution

Contributions are accepted under the repository's Apache-2.0 license unless explicitly stated otherwise. Preserve applicable upstream copyright and attribution notices.

See `LICENSE`, `NOTICE`, and `docs/FORK_IDENTITY.md`.
