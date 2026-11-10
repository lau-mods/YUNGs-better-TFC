#!/usr/bin/env python3
"""Regenerate TFC-compatible loot tables from pristine 1.21.1 dependency JARs."""

from __future__ import annotations

import argparse
import copy
import difflib
import json
import shutil
import sys
import zipfile
from pathlib import Path


MOD_ID = "yungsbettertfc"
ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data"
SOURCE_TABLES = {
    "betterdungeons": (
        "skeleton_dungeon/chests/common",
        "skeleton_dungeon/chests/middle",
        "small_dungeon/chests/loot_piles",
        "small_nether_dungeon/chests/common",
        "spider_dungeon/chests/egg_room",
        "zombie_dungeon/chests/common",
        "zombie_dungeon/chests/special",
        "zombie_dungeon/chests/tombstone",
    ),
    "betterfortresses": (
        "chests/beacon",
        "chests/extra",
        "chests/hall",
        "chests/keep",
        "chests/obsidian",
        "chests/puzzle",
        "chests/quarters",
        "chests/storage",
        "chests/worship",
    ),
    "betteroceanmonuments": ("chests/upper_side_chamber",),
    "betterstrongholds": (
        "chests/armoury",
        "chests/cmd_yung",
        "chests/common",
        "chests/crypt",
        "chests/grand_library",
        "chests/library_md",
        "chests/mess",
        "chests/prison_lg",
        "chests/trap",
        "chests/treasure",
    ),
    "minecraft": (
        "chests/abandoned_mineshaft",
        "chests/simple_dungeon",
        "chests/stronghold_corridor",
        "chests/stronghold_crossing",
        "chests/stronghold_library",
    ),
}
DELEGATED_TABLES = {
    "minecraft:chests/end_city_treasure",
    "minecraft:chests/nether_bridge",
}
MATERIALS = {
    "ordinary": ("copper", "bronze", "bismuth_bronze", "black_bronze"),
    "dangerous": ("wrought_iron",),
    "lategame": ("steel",),
}
INGOTS = {
    "ordinary": ("copper", "bronze", "bismuth_bronze", "black_bronze", "wrought_iron"),
    "dangerous": ("wrought_iron", "steel"),
    "lategame": ("steel", "black_steel"),
}
FAMILIES = {
    "pickaxe": (("pickaxe_head", "propick_head"), ("pickaxe", "propick")),
    "axe": (("axe_head", "saw_blade"), ("axe", "saw")),
    "hoe": (("hoe_head", "scythe_blade"), ("hoe", "scythe")),
    "sword": (("sword_blade", "javelin_head", "mace_head"), ("sword", "javelin", "mace")),
    "shovel": (("shovel_head",), ("shovel",)),
    "knife": (("knife_blade",), ("knife",)),
    "shears": ((), ("shears",)),
    "shield": ((), ("shield",)),
    "helmet": (("unfinished_helmet",), ("helmet",)),
    "chestplate": (("unfinished_chestplate",), ("chestplate",)),
    "leggings": (("unfinished_greaves",), ("greaves",)),
    "boots": (("unfinished_boots",), ("boots",)),
}
GEMS = {"diamond", "emerald", "lapis_lazuli", "amethyst_shard"}
PRODUCE = {"apple", "bread", "carrot", "potato", "glow_berries", "wheat", "beetroot", "melon_slice"}
SEEDS = {"beetroot_seeds", "melon_seeds", "pumpkin_seeds", "wheat_seeds"}
UTILITY = {"obsidian", "crying_obsidian", "quartz", "glowstone_dust"}
LOOT_TAGS = {
    "produce": (
        "food/blackberry", "food/blueberry", "food/bunchberry", "food/cranberry",
        "food/elderberry", "food/gooseberry", "food/cloudberry", "food/raspberry",
        "food/snowberry", "food/strawberry", "food/wintergreen_berry", "food/red_apple",
        "food/green_apple", "food/plum", "food/beet", "food/cabbage", "food/carrot",
        "food/cassava", "food/garlic", "food/green_bean", "food/lentil", "food/maize",
        "food/melon_slice", "food/oat", "food/onion", "food/peanut", "food/potato",
        "food/pumpkin_chunks", "food/rice", "food/rye", "food/soybean", "food/squash",
        "food/sugarcane", "food/taro_root", "food/tomato", "food/wheat", "food/barley",
        "food/red_bell_pepper", "food/yellow_bell_pepper",
    ),
    "seeds": (
        "seeds/alfalfa", "seeds/barley", "seeds/beet", "seeds/cabbage", "seeds/canola",
        "seeds/carrot", "seeds/cassava", "seeds/garlic", "seeds/green_bean", "seeds/jute",
        "seeds/lentil", "seeds/maize", "seeds/melon", "seeds/oat", "seeds/onion",
        "seeds/papyrus", "seeds/peanut", "seeds/potato", "seeds/pumpkin", "seeds/radish",
        "seeds/red_bell_pepper", "seeds/rice", "seeds/rye", "seeds/squash",
        "seeds/soybean", "seeds/sugarcane", "seeds/tomato", "seeds/wheat",
        "seeds/yellow_bell_pepper",
    ),
    "fertilizers": (
        "compost", "rotten_compost", "powder/wood_ash", "powder/saltpeter",
        "powder/sylvite", "groundcover/guano",
    ),
}


def serialized(value: dict) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True, ensure_ascii=True) + "\n").encode("utf-8")


def dump(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(serialized(value))


def item(name: str, weight: int | None = None, functions: list | None = None) -> dict:
    value = {"type": "minecraft:item", "name": name}
    if weight is not None:
        value["weight"] = weight
    if functions:
        value["functions"] = functions
    return value


def table(name: str, source: dict, functions: list | None = None) -> dict:
    value = {key: copy.deepcopy(val) for key, val in source.items() if key not in {"type", "name", "functions"}}
    value.update({"type": "minecraft:loot_table", "value": name})
    if functions:
        value["functions"] = copy.deepcopy(functions)
    return value


def one_pool(entries: list[dict]) -> dict:
    return {"type": "minecraft:generic", "pools": [{"rolls": 1, "entries": entries}]}


def profile(namespace: str, logical_path: str) -> str:
    if namespace in {"betterfortresses", "betterstrongholds"}:
        return "dangerous"
    if namespace == "betterdungeons" and logical_path.startswith("small_nether_dungeon/"):
        return "dangerous"
    return "ordinary"


def equipment_family(path: str) -> str | None:
    if path.endswith("horse_armor"):
        return "horse_armor"
    families = (
        "chestplate", "leggings", "helmet", "boots", "pickaxe", "shovel", "shears", "shield",
        "crossbow", "knife", "sword", "hoe", "axe", "bow",
    )
    for family in families:
        if path == family or path.endswith("_" + family):
            return family
    return None


def equipment_table(current_profile: str, family: str) -> dict:
    if family in {"bow", "crossbow"}:
        return one_pool([item(f"minecraft:{family}")])
    if family == "horse_armor":
        return one_pool([item(f"tfc:metal/horse_armor/{metal}") for metal in MATERIALS[current_profile]])
    parts, completed = FAMILIES[family]
    entries = []
    for metal in MATERIALS[current_profile]:
        entries.extend(item(f"tfc:metal/{part}/{metal}", weight=2) for part in parts)
        entries.extend(item(f"tfc:metal/{tool}/{metal}", weight=1) for tool in completed)
    return one_pool(entries)


def transform_entry(entry: dict, current_profile: str, source_path: str) -> dict:
    value = copy.deepcopy(entry)
    if value.get("type") != "minecraft:item":
        return value
    name = value.get("name", "")
    namespace, _, path = name.partition(":")
    if namespace != "minecraft":
        return value
    functions = [
        copy.deepcopy(function)
        for function in value.get("functions", [])
        if function.get("function") != "minecraft:set_damage"
    ]
    if functions:
        value["functions"] = functions
    else:
        value.pop("functions", None)
    if source_path == "betterstrongholds/chests/cmd_yung" and path == "diamond_sword":
        value["name"] = "tfc:metal/sword/wrought_iron"
        return value
    if path in GEMS:
        return table(f"{MOD_ID}:shared/gems", value, functions)
    if path in {"iron_ingot", "gold_ingot"}:
        return table(f"{MOD_ID}:shared/ingots/{current_profile}", value, functions)
    if path == "iron_nugget":
        return table(f"{MOD_ID}:shared/small_ores/iron", value, functions)
    if path in {"gold_nugget", "raw_gold"}:
        value["name"] = "tfc:ore/small_native_gold"
        return value
    if path in {"coal", "charcoal"}:
        return table(f"{MOD_ID}:shared/coal_like", value, functions)
    if path == "torch":
        value["name"] = "tfc:torch"
        return value
    if path in PRODUCE:
        return table(f"{MOD_ID}:shared/produce", value, functions)
    if path in SEEDS:
        return table(f"{MOD_ID}:shared/seeds", value, functions)
    if path == "bone_meal":
        return table(f"{MOD_ID}:shared/fertilizers", value, functions)
    if path in UTILITY:
        return table(f"{MOD_ID}:shared/utility", value, functions)
    family = equipment_family(path)
    if family is None:
        return value
    return table(f"{MOD_ID}:shared/equipment/{current_profile}/{family}", value, functions)


def transform_node(value, current_profile: str, source_path: str):
    if isinstance(value, list):
        return [transform_node(child, current_profile, source_path) for child in value]
    if not isinstance(value, dict):
        return value
    if value.get("type") == "minecraft:item":
        return transform_entry(value, current_profile, source_path)
    return {key: transform_node(child, current_profile, source_path) for key, child in value.items()}


def shared_tables() -> dict[str, dict]:
    result = {
        "shared/gems": one_pool([item(f"tfc:gem/{gem}") for gem in (
            "amethyst", "diamond", "emerald", "lapis_lazuli", "opal", "pyrite", "ruby", "sapphire", "topaz",
        )]),
        "shared/coal_like": one_pool([item(name) for name in (
            "minecraft:charcoal", "tfc:ore/bituminous_coal", "tfc:ore/lignite", "tfc:ore/graphite",
        )]),
        "shared/utility": one_pool([item(name) for name in (
            "tfc:powder/flux", "minecraft:glass", "minecraft:glass_pane", "tfc:kaolin_clay",
            "tfc:powder/wood_ash", "tfc:powder/saltpeter", "tfc:powder/sylvite",
        )]),
        "shared/produce": one_pool([{"type": "minecraft:tag", "name": f"{MOD_ID}:loot/produce", "expand": True}]),
        "shared/seeds": one_pool([{"type": "minecraft:tag", "name": f"{MOD_ID}:loot/seeds", "expand": True}]),
        "shared/fertilizers": one_pool([{"type": "minecraft:tag", "name": f"{MOD_ID}:loot/fertilizers", "expand": True}]),
        "shared/small_ores/iron": one_pool([item(f"tfc:ore/small_{ore}") for ore in ("hematite", "magnetite", "limonite")]),
    }
    for current_profile, metals in INGOTS.items():
        result[f"shared/ingots/{current_profile}"] = one_pool([item(f"tfc:metal/ingot/{metal}") for metal in metals])
        for family in tuple(FAMILIES) + ("bow", "crossbow", "horse_armor"):
            result[f"shared/equipment/{current_profile}/{family}"] = equipment_table(current_profile, family)
    return result


def read_source(archive: zipfile.ZipFile, namespace: str, logical_path: str) -> dict:
    name = f"data/{namespace}/loot_table/{logical_path}.json"
    return json.loads(archive.read(name).decode("utf-8"))


def generate(jars: dict[str, Path]) -> dict[Path, dict]:
    generated: dict[Path, dict] = {}
    for namespace, logical_paths in SOURCE_TABLES.items():
        with zipfile.ZipFile(jars[namespace]) as archive:
            for logical_path in logical_paths:
                source = read_source(archive, namespace, logical_path)
                value = transform_node(source, profile(namespace, logical_path), f"{namespace}/{logical_path}")
                generated[DATA / namespace / "loot_table" / f"{logical_path}.json"] = value
    for logical_path, value in shared_tables().items():
        generated[DATA / MOD_ID / "loot_table" / f"{logical_path}.json"] = value
    for name, members in LOOT_TAGS.items():
        generated[DATA / MOD_ID / "tags/item/loot" / f"{name}.json"] = {
            "replace": False, "values": [f"tfc:{member}" for member in members],
        }
    return generated


def validate_tfc_item_ids(generated: dict[Path, dict], tfc_jar: Path) -> None:
    ids: set[str] = set()

    def collect(value) -> None:
        if isinstance(value, dict):
            if value.get("type") == "minecraft:item" and value.get("name", "").startswith("tfc:"):
                ids.add(value["name"].removeprefix("tfc:"))
            for child in value.values():
                collect(child)
        elif isinstance(value, list):
            for child in value:
                collect(child)

    for value in generated.values():
        collect(value)
    with zipfile.ZipFile(tfc_jar) as archive:
        resources = set(archive.namelist())
    missing = [item_id for item_id in sorted(ids) if f"assets/tfc/models/item/{item_id}.json" not in resources]
    if missing:
        raise ValueError("TFC JAR does not contain item models for: " + ", ".join(f"tfc:{item_id}" for item_id in missing))


def compare_sources(jars: dict[str, Path], comparisons: list[str]) -> bool:
    changed = False
    for comparison in comparisons:
        namespace, separator, old_path = comparison.partition("=")
        if not separator or namespace not in SOURCE_TABLES:
            raise ValueError("--compare-source must use NAMESPACE=JAR for a generated namespace")
        with zipfile.ZipFile(Path(old_path)) as old_archive, zipfile.ZipFile(jars[namespace]) as new_archive:
            for logical_path in SOURCE_TABLES[namespace]:
                old = json.dumps(read_source(old_archive, namespace, logical_path), indent=2, sort_keys=True).splitlines()
                new = json.dumps(read_source(new_archive, namespace, logical_path), indent=2, sort_keys=True).splitlines()
                diff = list(difflib.unified_diff(
                    old, new,
                    fromfile=f"old/{namespace}/{logical_path}.json",
                    tofile=f"new/{namespace}/{logical_path}.json",
                    lineterm="",
                ))
                if diff:
                    changed = True
                    print("\n".join(diff))
    return changed


def check_existing(generated: dict[Path, dict]) -> None:
    expected = set(generated)
    roots = [DATA / namespace / "loot_table" for namespace in (*SOURCE_TABLES, MOD_ID)]
    roots.append(DATA / MOD_ID / "tags/item/loot")
    actual = {path for root in roots if root.exists() for path in root.rglob("*.json")}
    delegated = {
        DATA / namespace / "loot_table" / f"{logical_path}.json"
        for namespace, logical_path in (value.split(":", 1) for value in DELEGATED_TABLES)
    }
    problems = []
    if actual != expected:
        problems.extend(f"unexpected output: {path.relative_to(ROOT)}" for path in sorted(actual - expected))
        problems.extend(f"missing output: {path.relative_to(ROOT)}" for path in sorted(expected - actual))
    problems.extend(f"delegated output exists: {path.relative_to(ROOT)}" for path in sorted(delegated) if path.exists())
    for path, expected_value in generated.items():
        if path.exists() and path.read_bytes() != serialized(expected_value):
            problems.append(f"bytes differ: {path.relative_to(ROOT)}")
    if problems:
        raise ValueError("\n".join(problems))


def write_generated(generated: dict[Path, dict]) -> None:
    shared_root = DATA / MOD_ID / "loot_table/shared"
    if shared_root.exists():
        shutil.rmtree(shared_root)
    tag_root = DATA / MOD_ID / "tags/item/loot"
    if tag_root.exists():
        shutil.rmtree(tag_root)
    for delegated in DELEGATED_TABLES:
        namespace, logical_path = delegated.split(":", 1)
        (DATA / namespace / "loot_table" / f"{logical_path}.json").unlink(missing_ok=True)
    for path, value in sorted(generated.items()):
        dump(path, value)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-jar", type=Path, required=True)
    parser.add_argument("--betterdungeons-jar", type=Path, required=True)
    parser.add_argument("--betterfortresses-jar", type=Path, required=True)
    parser.add_argument("--betteroceanmonuments-jar", type=Path, required=True)
    parser.add_argument("--betterstrongholds-jar", type=Path, required=True)
    parser.add_argument("--tfc-jar", type=Path, required=True)
    parser.add_argument("--check", action="store_true", help="compare generated bytes with tracked JSON without writing")
    parser.add_argument(
        "--compare-source", action="append", default=[], metavar="NAMESPACE=JAR",
        help="show normalized source-table changes between an older JAR and the selected input JAR",
    )
    parser.add_argument("--compare-only", action="store_true", help="only run requested source comparisons")
    args = parser.parse_args()
    jars = {namespace: getattr(args, namespace + "_jar") for namespace in SOURCE_TABLES}
    compare_sources(jars, args.compare_source)
    if args.compare_only:
        return
    generated = generate(jars)
    validate_tfc_item_ids(generated, args.tfc_jar)
    if args.check:
        check_existing(generated)
    else:
        write_generated(generated)
    print(f"validated {len(generated) - len(LOOT_TAGS)} loot tables and {len(LOOT_TAGS)} item tags; delegated {len(DELEGATED_TABLES)} tables to TFC Overdims")


if __name__ == "__main__":
    try:
        main()
    except (KeyError, OSError, ValueError, zipfile.BadZipFile) as error:
        print(error, file=sys.stderr)
        raise SystemExit(1) from error
