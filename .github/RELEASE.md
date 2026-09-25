# Lalapo release setup

Lalapo releases are published from tags on `main`.

- Stable: tag `v<version>` (for example `v0.20.0.1`).
- Preview: tag `r<commit-count>` (for example `r1234`). Preview tags are published as GitHub prereleases.
- Stable builds publish universal, arm64-v8a, armeabi-v7a, x86, x86_64, and FOSS APKs.
- Preview builds publish universal and ABI-specific preview APKs.

## Required repository secrets

- `SIGNING_KEY` — base64-encoded Android signing keystore.
- `KEY_STORE_PASSWORD` — keystore password.
- `ALIAS` — signing key alias.
- `KEY_PASSWORD` — signing key password.
- `CLIENT_SECRETS_TEXT` — contents of `client_secrets.json` used by Google Drive integration.

The workflow uses the repository-provided `GITHUB_TOKEN` to create the GitHub Release.

Do not commit signing keys, passwords, or OAuth client secrets to the repository.
