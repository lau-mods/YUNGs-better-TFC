# YUNG's Better TFC development specification

## Source of truth

`PROJECT.md` is the authoritative product specification. This file records repository-wide implementation rules and the currently implemented milestone. When the two disagree, follow `PROJECT.md`.

## Common rules

- Target Minecraft 1.21.1, NeoForge 21.1.264, and Java 21.
- Keep the mod ID and resource namespace `yungsbettertfc`, and keep Java sources under `net.claustra01.yungsbettertfc`.
- Prefer vanilla, NeoForge, TFC, Overdims, and YUNG APIs over custom systems.
- Never reference an optional mod class from an unconditional load path. Detect optional mods by string mod ID and keep their Mixins below the matching package gate.
- Keep version ranges open above a verified 1.21.1 lower bound. An open range is not a claim that untested future releases are compatible.
- Add no block, item, GUI, config screen, or texture unless a milestone explicitly requires it.
- Register state and logic checks as GameTests and rendering checks as E2E scenarios. Keep `tests/acceptance.json` synchronized with their reports.
- Run `./gradlew build` and `./gradlew runGameTestServer` before handoff.

## Dependency contract

| Mod ID | Role | Verified lower bound | Development artifact |
| --- | --- | --- | --- |
| `tfc` | required | 4.2.12 | TerraFirmaCraft 1.21.1 |
| `yungsapi` | required | 5.1.6 | YUNG's API 1.21.1 NeoForge |
| `tfcoverdims` | required | 0.1.4 | TFC Overdims 1.21.1 NeoForge |
| `betterdungeons` | optional | 5.1.3 | Better Dungeons 1.21.1 NeoForge |
| `bettermineshafts` | optional | 5.1.1 | Better Mineshafts 1.21.1 NeoForge |
| `betterstrongholds` | optional | 5.1.3 | Better Strongholds 1.21.1 NeoForge |
| `betteroceanmonuments` | optional | 4.1.1 | Better Ocean Monuments 1.21.1 NeoForge |
| `betterfortresses` | optional | 3.1.5 | Better Nether Fortresses 1.21.1 NeoForge |

## Current implementation

Milestone M01 provides only the bootstrap and dependency boundary:

- Distribution version `1.0.0` with `@Mod("yungsbettertfc")`.
- Required and optional dependencies exactly as listed above.
- No Beneath dependency or runtime integration.
- `yungsbettertfc.mixins.json` uses `YungsBetterTfcMixinPlugin`. Mixins below `mixin.bettermineshafts`, `mixin.betterdungeons`, `mixin.betterstrongholds`, `mixin.betteroceanmonuments`, and `mixin.betterfortresses` load only when their matching mod is present.
- No structure conversion behavior is implemented before its later milestone.

## Shared mcmods development rules

Child repositories can be checked out independently, so these rules remain in this repository.

### Specifications and canonical files

- Use the uppercase filename `AGENTS.md`; do not create a lowercase `agents.md`.
- Record maintained behavior in `AGENTS.md`, not unimplemented plans presented as current behavior.
- Keep user-facing names, a short description, distribution links, and the build entry point in `README.md`. Do not duplicate implementation history or the full specification there.
- Keep the repository's single existing licence file. Do not add another licence, template licence, or notice file.
- Update `AGENTS.md` with changes to visible behavior, supported versions, dependencies, IDs, registrations, activation conditions, generation rules, or verification procedures.
- Treat `gradle.properties`, Gradle configuration, mod metadata, implementation code, and bundled data as the canonical current values.
- Do not change versions unless the task or implementation requires it.

### Research and compatibility

- Verify Minecraft, loader, TFC, and integration-mod behavior against sources or actual artifacts for the target version before fixing IDs, APIs, or JSON formats.
- Prefer public APIs, registries, tags, recipes, loot, and world-generation data. Limit Mixins, accessors, and reflection to necessary targets.
- Preserve required and optional dependency roles and metadata ordering. Do not reference an optional mod class from an unconditional load path.
- Do not reference client-only classes from common or dedicated-server paths.
- Preserve published registry IDs, resource namespaces, and config keys unless a breaking change is explicitly required.
- Keep this mod's resources in its own namespace except for intentional tag additions or same-name data overrides.

### Files and generated output

- Put dependency JARs, extracted files, comparison images, and temporary analysis or verification scripts under the ignored `.tmp/` directory.
- Commit only scripts required by a normal clone, build, or CI workflow, unless `AGENTS.md` explicitly identifies another script as a maintained generator.
- Keep generated resources and their specification reproducible; do not make a temporary helper the only source of behavior.
- Save JSON as UTF-8 without a BOM and use the target Minecraft version's pack layout.
- Do not include `.gradle/`, `build/`, `run/`, `runs/`, IDE metadata, dependency JARs, or extracted output in changes.
- Treat this directory as an independent Git repository and preserve unrelated user changes.

### Implementation and verification

- Inspect existing registration, naming, resource layout, and generation paths before adding another implementation of the same responsibility.
- Keep changes within the requested behavior; do not mix unrelated formatting, renames, dependency updates, or generated-resource replacements.
- Use `./gradlew compileJava` for normal intermediate checks and `./gradlew build` for completion checks.
- Parse all JSON after data changes and add consistency checks appropriate to tags, recipes, loot, models, textures, and generators.
- Test optional integrations and Mixin and client/server boundaries both with and without the relevant mod, emphasizing dedicated-server class loading.
- Start the Minecraft client only for behavior that requires runtime rendering or when explicitly requested.
- Report verification that could not be run and do not present it as passing.

## M01 verification

- The GameTest namespace is `yungsbettertfc`. Bootstrap tests use `data/yungsbettertfc/structure/empty.nbt` and write JUnit XML through the `yungsbettertfc.gametest.report` system property. The Gradle run writes `build/test-results/gametest/TEST-yungsbettertfc.BootstrapGameTests-<configuration>.xml`.
- Run `./gradlew build` and `./gradlew runGameTestServer` for the five optional YUNG mods. Run `./gradlew -PoptionalYungMods=none runGameTestServer` and `./gradlew -PoptionalYungMods=betterdungeons runGameTestServer` for zero and one optional mod. Each run checks actual loaded mod IDs and the corresponding Mixin package gate, and places one structure for each loaded optional mod.
- Run `./gradlew -PwithoutTfcOverdims=true -PoptionalYungMods=none runGameTestServer` to verify that NeoForge refuses startup and reports `tfcoverdims` as a missing required dependency of `yungsbettertfc` in `run/logs/latest.log`. Check the FML message because the development launcher can return success after the loading error. This negative check cannot be an in-game GameTest because dependency validation stops loading first; its expected result is recorded beside the GameTest entries in `tests/acceptance.json`.
