# Lalapo fork identity

## Android identity

- Final application ID: `com.noirero.lalapo`.
- Kotlin/Android namespace remains `eu.kanade.tachiyomi` for now. It is an internal source/API namespace and is intentionally not mass-renamed.
- Lalapo can therefore be installed alongside an Animiru build that uses `xyz.Quickdev.Animiru.mi`.
- Because the package ID changed, an installed Animiru app is not upgraded in-place by Lalapo. Use backup/restore when migrating user data.

## Components that follow the application ID automatically

- `FileProvider` authority uses `${applicationId}.provider`.
- Shizuku provider authority uses `${applicationId}.shizuku`.
- Dynamic shortcut targets replace `${applicationId}` at build time.
- Extension installer broadcasts use `BuildConfig.APPLICATION_ID` or `Context.packageName`.

These components therefore follow `com.noirero.lalapo` without renaming Kotlin packages.

## URI schemes intentionally retained for compatibility

- `aniyomi://add-repo` and `aniyomi://extension-store` remain supported so existing extension-store links keep working.
- `animiru://...-auth` tracking callbacks are retained because tracker OAuth applications may have those redirect URIs registered upstream.
- `animiru.google.oauth://` is retained for the current Google Drive OAuth flow and `CLIENT_SECRETS_TEXT` configuration.

Do not rename the OAuth callback schemes until the corresponding provider registrations/client secrets have been updated and tested together.

## Release identity

- GitHub release repository: `Noirero/Lalapo`.
- Stable tags use `v<version>`.
- Preview tags use `r<commit-count>` and are GitHub prereleases.
