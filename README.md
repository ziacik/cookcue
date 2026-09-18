# CookCue

CookCue is an Android + Wear OS cooking assistant that treats a recipe as an executable plan instead of a static list of steps.

The scheduler understands dependencies, limited resources and work that can happen in parallel. For example, two knife-prep tasks compete for the cook, while a pot can keep simmering in the background while you prepare something else.

## Modules

- `core` — recipe model, resource-aware scheduler, cook-speed profile, bean-soup scenario and unit tests
- `mobile` — phone timeline UI
- `wear` — glanceable current-task UI
- `scripts` — local checks and ADB deploy helpers

## Build

```bash
bash ./scripts/check.sh
```

## Deploy

```bash
bash ./scripts/deploy-mobile.sh
bash ./scripts/deploy-wear.sh
```

Pass an ADB serial as the first argument if multiple devices are connected.

## Rules

- primary development branch: `master`
- tabs in Kotlin, Gradle Kotlin DSL and shell scripts
- YAML uses spaces because YAML itself forbids tab indentation
