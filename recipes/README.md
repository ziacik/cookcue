# Online recipes

CookCue loads all recipe scenarios from this directory; there is no bundled recipe catalog in the production app.

- `index.json` is the single source of truth for the recipe catalog.
- Every catalog entry has a stable `id`, monotonically increasing `version`, JSON `file`, and optional `minAppVersion`.
- Recipe files use `schemaVersion: 1`.
- Increment `version` whenever a recipe changes so installed apps fetch the new version.
- Downloaded recipes are cached on the device for offline use, including after restarting the app.
- On the first launch with no cached recipes, CookCue shows a loading/error state and offers a retry rather than falling back to built-in recipes.
- Existing sessions retain their loaded recipe until cooking ends; catalog updates are applied after the session.
- New recipe IDs appear after a catalog refresh with no app rebuild, provided the current schema can express them.
