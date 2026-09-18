# CookCue

CookCue is an Android + Wear OS cooking assistant that treats a recipe as an executable plan instead of a static list of steps.

Tasks do not have a vague `canRunInParallel` flag. They declare the resources they occupy. Two chopping jobs both require `cook`, so they cannot overlap. A simmering pot occupies `pot + burner`, but not `cook`, so preparation can continue while it cooks.

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
