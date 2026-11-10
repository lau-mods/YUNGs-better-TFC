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

Milestone M04 provides the bootstrap, dependency boundary, stone, soil, wood, utility, light, metal, plant, ore, display-entity, and structural hardening conversion core:

- Distribution version `1.0.0` with `@Mod("yungsbettertfc")`.
- Required and optional dependencies exactly as listed above.
- No Beneath dependency or runtime integration.
- `yungsbettertfc.mixins.json` uses `YungsBetterTfcMixinPlugin`. Mixins below `mixin.bettermineshafts`, `mixin.betterdungeons`, `mixin.betterstrongholds`, `mixin.betteroceanmonuments`, and `mixin.betterfortresses` load only when their matching mod is present.
- `yungsbettertfc:tfc_block_replacement` is a registered unit-codec structure processor. It converts only `minecraft:` input blocks in the Overworld and Nether and performs no conversion in the End or an unknown/custom dimension.
- Overworld geology scans downward from the placement origin for at most 64 blocks. It detects `tfc:rock/raw/<rock>` and TFC soil-family blocks, caches the combined result in a bounded thread-safe placement-origin cache, and defaults to `granite` and `mollisol`. Nether conversion always uses `basalt` and `oxisol`.
- Conversion output is accepted only when the block registry contains it, and compatible state properties are copied by property name and value. Missing output IDs preserve the original block.
- Hardening uses only the block directly below each result. The processor indexes the processed template list in `finalizeProcessing`; positions outside that list use the existing level state.
- The nine Overworld Vanilla wood families map to their TFC wood families. Crimson and Warped wood map to registered `tfcoverdims:wood/*` blocks only in the Nether; hanging signs remain Vanilla.
- `finalizeProcessing` counts explicit wood IDs in the original template palette once per placement. Chest, trapped chest, lectern, and crafting table use that hint, default to TFC Oak, retain their original NBT, and use Overdims wood utilities for a Crimson or Warped hint.
- Furnace, blast furnace, and smoker become `tfc:firepit`; their horizontal facing supplies the TFC firepit axis and only these conversions discard Block Entity NBT. Vanilla cauldrons and barrels remain unchanged with their state and NBT.
- Vanilla torch and candle families map to registered TFC blocks. Lantern, soul lantern, both campfires, and soul torches remain Vanilla.
- Wrought Iron, Gold, and Copper use the maintained M03 mappings. Copper oxidation stage and registered stairs/slab shape are retained. Nether coal blocks become `tfc:bituminous_coal`; the specified utility and decoration keep list remains Vanilla.
- Kelp becomes water; seagrass and the lower half of tall seagrass become TFC eel grass; the upper tall-seagrass half becomes water. Sea pickles, vines, cactus bases, and dead bushes use their registered TFC equivalents. Additional cactus blocks become air based on the processed template position index. Registered `tfc:plant/potted/*` outputs are used, while other plants remain Vanilla.
- Vanilla Overworld and Deepslate ores use the maintained TFC ore priority table and the detected rock, with a Granite registry fallback. Nether Gold Ore, Nether Quartz Ore, and Ancient Debris always remain Vanilla. The same rules rewrite parseable Vanilla ore states in `final_state` NBT, copying only compatible properties and retaining trailing NBT.
- Item frames, glow item frames, and armor stands convert supported Vanilla equipment to registered TFC Wrought Iron equipment in the Overworld and Nether. Incompatible item component, tag, and damage data is removed after replacement. Display entities are unchanged in the End or an unknown dimension.

## M04 conversion tables

| Vanilla input | Output |
| --- | --- |
| `kelp`, `kelp_plant` | `minecraft:water` |
| `seagrass`, lower `tall_seagrass` | `tfc:plant/eel_grass` |
| upper `tall_seagrass` | `minecraft:water` |
| `sea_pickle`, `vine`, `dead_bush` | `tfc:sea_pickle`, `tfc:plant/ivy`, `tfc:plant/dead_bush` |
| base `cactus`, upper `cactus` | `tfc:plant/silken_pincushion_cactus`, air |
| `potted_*` | registered `tfc:plant/potted/<plant>` only |

Coal ore tries `tfc:lignite` and then `tfc:bituminous_coal`. Iron ore tries normal Hematite, Magnetite, and Limonite. Copper, Gold, Lapis, Diamond, Emerald, and Redstone use normal Native Copper, normal Native Gold, Lapis Lazuli, Diamond, Emerald, and Cinnabar respectively. Rock-specific candidates try the detected rock and then Granite for each ore candidate. Normal and Deepslate inputs share the same result.

Vanilla swords, axes, pickaxes, shovels, hoes, helmets, chestplates, leggings, boots, shields, and maces map to the matching TFC Wrought Iron item, with leggings mapped to greaves. Bows, crossbows, and tridents map to a Wrought Iron javelin. Missing output items preserve the original stack.

## M03 conversion tables

| Vanilla wood | TFC wood |
| --- | --- |
| `oak` | `oak` |
| `spruce` | `spruce` |
| `birch` | `birch` |
| `jungle`, `acacia` | `acacia` |
| `dark_oak`, `cherry`, `bamboo` | `oak` |
| `mangrove` | `mangrove` |

For these nine families, planks use `tfc:wood/planks/<wood>`, stairs and slabs use `tfc:wood/planks/<wood>_stairs` and `_slab`, and log, wood, stripped_log, stripped_wood, fence, fence_gate, door, trapdoor, button, pressure_plate, sign, and wall_sign use `tfc:wood/<shape>/<wood>`. Nether Crimson and Warped use the same shape forms under `tfcoverdims:wood/`, with stem and hyphae corresponding to log and wood. They convert only in the Nether; hanging signs and other Nether vegetation remain Vanilla. Oak and other Overworld woods still use TFC forms in the Nether.

`finalizeProcessing` counts explicit wood block IDs from the original palette, using the longest matching wood name, once per block. Crimson and Warped count only in the Nether. The most frequent mapped wood supplies the utility hint; ties retain the first wood encountered, and an empty count defaults to TFC Oak. `chest`, `trapped_chest`, `lectern`, and `crafting_table` become `wood/chest/<wood>`, `wood/trapped_chest/<wood>`, `wood/lectern/<wood>`, and `wood/workbench/<wood>` in the hinted `tfc` or `tfcoverdims` namespace. Their NBT passes through unchanged.

| Vanilla input | Output |
| --- | --- |
| `furnace`, `blast_furnace`, `smoker` | `tfc:firepit`; discard original Block Entity NBT and copy horizontal facing axis |
| `torch`, `wall_torch` | `tfc:torch`, `tfc:wall_torch` |
| `candle`, `<color>_candle` | `tfc:candle`, `tfc:candle/<color>` when registered |
| `candle_cake`, `<color>_candle_cake` | `tfc:candle_cake`, `tfc:candle_cake/<color>` when registered |
| `iron_bars`, `chain`, `iron_block`, `iron_trapdoor` | `tfc:metal/bars/wrought_iron`, `metal/chain/wrought_iron`, `metal/block/wrought_iron`, `metal/trapdoor/wrought_iron` |
| `gold_block`, `raw_gold_block` | `tfc:metal/block/gold` |
| `copper_block`, `cut_copper`, `cut_copper_slab`, `cut_copper_stairs` | `tfc:metal/block/copper`, `copper`, `copper_slab`, `copper_stairs` in that family |
| `exposed_copper`, `exposed_cut_copper` and its slab/stairs; analogous `weathered_*` and `oxidized_*` | `tfc:metal/exposed_block/copper`, `weathered_block/copper`, or `oxidized_block/copper`, with `_slab` or `_stairs` for shapes |
| `waxed_oxidized_cut_copper` and its slab/stairs only | `tfc:metal/oxidized_block/copper` and matching shapes; other waxed variants stay Vanilla |
| Nether `coal_block` | `tfc:bituminous_coal`; Overworld coal block stays Vanilla |

Cauldron, water/lava/powder snow cauldrons, barrel, lantern, soul lantern, campfire, soul campfire, soul torch, soul wall torch, anvil variants, smithing table, stonecutter, spawner, rail, and beds stay Vanilla with their state and NBT.

## M02 conversion tables

The rock placeholder is detected geology in the Overworld and `basalt` in the Nether.

| Vanilla input | TFC output family |
| --- | --- |
| `stone`, `andesite`, `granite`, `diorite`, `tuff`, including existing stairs/slabs/walls | `rock/raw/<rock>` and matching shapes |
| `polished_andesite`, `polished_granite`, `polished_diorite`, `polished_tuff`, including stairs/slabs | `rock/smooth/<rock>` and matching shapes |
| `cobblestone`, `mossy_cobblestone`, including stairs/slabs/walls | `rock/cobble/<rock>` and `rock/mossy_cobble/<rock>` shapes |
| `stone_bricks`, `tuff_bricks`, including stairs/slabs/walls | `rock/bricks/<rock>` and matching shapes |
| `mossy_stone_bricks`, including stairs/slabs/walls | `rock/mossy_bricks/<rock>` and matching shapes |
| `cracked_stone_bricks` | `rock/cracked_bricks/<rock>` |
| `chiseled_stone_bricks`, `chiseled_tuff`, `chiseled_tuff_bricks` | `rock/chiseled/<rock>` |
| `smooth_stone`, `smooth_stone_slab` | `rock/smooth/<rock>` and slab |
| `gravel`, `stone_button`, `stone_pressure_plate` | `rock/gravel`, `rock/button`, `rock/pressure_plate` for `<rock>` |

`sandstone` and red sandstone use the matching yellow/red `raw_sandstone` family. Cut and chiseled variants use `cut_sandstone`; smooth variants use `smooth_sandstone`; registered stairs, slabs, and walls retain their shape. `sand` and `red_sand` become yellow and red TFC sand.

| Vanilla soil | TFC soil output |
| --- | --- |
| `dirt` | `dirt/<soil>` |
| `coarse_dirt` | `coarse_dirt/<soil>` |
| `grass_block` | `grass/<soil>` |
| `rooted_dirt` | `rooted_dirt/<soil>` |
| `farmland` | `farmland/<soil>` |
| `dirt_path` | `grass_path/<soil>` |

Deepslate and Blackstone general building families remain unchanged. The explicit Nether keep list is Blackstone, Polished Blackstone, Basalt, Polished Basalt, Soul Sand, Soul Soil, Nether Gold Ore, Nether Quartz Ore, and Ancient Debris. Raw/cobble/mossy-cobble full-block conversion results become `tfc:rock/hardened/<rock>` only when the block directly below is air. Nether Netherrack similarly becomes `tfcoverdims:hardened_netherrack`; supported blocks and all stairs, slabs, and walls do not harden. No Overdims tags are changed.

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

## M02 verification

- `ConversionGameTests` covers every rock, sandstone, and soil table row in Overworld/Nether contexts, verifies all mapped categories remain unchanged in an End/NONE context, and checks state inheritance and missing-registry fallback.
- Its structure-pipeline checks detect TFC raw rock and soil below a placement origin and exercise template-indexed hardening for unsupported and supported full blocks and unsupported stairs, slabs, and walls.

## M03 verification

- `M03GameTests` covers all registered target shapes for every Overworld wood row and all Crimson/Warped rows, including inherited door, stair, and slab state and longest-match `dark_oak` hint parsing.
- Structure-pipeline tests cover TFC and Overdims wood utilities, Oak fallback, unchanged hanging signs and chest NBT, all three firepit conversions with axis and NBT removal, unchanged cauldron state and barrel loot NBT, every candle color, light exceptions, and the maintained metal and keep-list rows.

## M04 verification

- `M04GameTests` covers every maintained plant row, both tall-seagrass halves, a three-block cactus, unchanged non-target plants, registered and missing potted outputs, all normal and Deepslate ore rows, detected-rock outputs, Granite fallback, and Nether-native ore exclusions.
- Structure-pipeline checks cover ore replacement inside `final_state` NBT and its Nether exclusions. Entity-pipeline checks cover item frames, glow item frames, armor stands, every maintained Wrought Iron equipment category, incompatible data removal, unrelated entities, and the End exclusion.
