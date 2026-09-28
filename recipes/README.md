# Remote recipes

CookCue can load recipe scenarios from this directory without shipping a new APK.

- `index.json` is the catalog.
- Every catalog entry has a stable `id`, monotonically increasing `version`, JSON `file`, and optional `minAppVersion`.
- Recipe files use `schemaVersion: 1`.
- Increment `version` whenever a recipe file changes so installed apps fetch the new file.
- Existing Kotlin recipes remain bundled as an offline fallback. A remote recipe with the same `id` overrides its bundled copy.
- New recipe ids appear in the app after the catalog refreshes; no app rebuild is required as long as the current schema can express the recipe.
