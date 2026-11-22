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

## Repeat cycles

A task may instead define `repeat: { "count": 3, "steps": [...] }` with a stable top-level `id` and `title`. Cycle phases use normal task fields and execute sequentially for each repetition. A phase with `kind: "EVENT"` waits for confirmation, while `kind: "WAIT"` is a fixed timer. Dependencies on the top-level cycle id resolve to its final phase. A cycle is shown as one plan row with the completed repetition count; all phase IDs are stable across restarts. Nested cycles and custom intra-cycle dependencies are not supported.

Set `minAppVersion` high enough for clients that support the repeat format (10 for the first release). Older clients skip the scenario rather than trying to decode it.

## Recipe options

A recipe may define an optional `options` array for ingredient choices that affect the actual cooking instructions.
Each entry has a unique `id`, a `label`, and optional `overrides`.
The first entry is the default; users select an option before cooking and the choice is persisted with their session.

Supported overrides: `description`, `preCookingNote`, `ingredients` (keyed by original ingredient name,
with `name` and/or `amount`), `taskInstructions` (keyed by task id), and
`troubleshooting` (keyed by original problem, with `problem` and/or `advice`).
Unknown ingredient, task, and troubleshooting keys are rejected.
Option recipes keep the original recipe and task IDs for stable scheduling and restoration.
Older apps ignore `options` and use the base recipe, so place the legacy default in the top-level fields.
