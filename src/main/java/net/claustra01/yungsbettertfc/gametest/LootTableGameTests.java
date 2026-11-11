package net.claustra01.yungsbettertfc.gametest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforgespi.locating.IModFile;

@GameTestHolder(YungsBetterTfc.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LootTableGameTests {
    private static final String BATCH = "bootstrap";
    private static final Map<String, List<String>> SOURCE_TABLES = sourceTables();
    private static final Set<ResourceLocation> EXPECTED_TABLES = expectedTables();
    private static final Set<ResourceLocation> DELEGATED_TABLES = Set.of(
            ResourceLocation.parse("minecraft:chests/end_city_treasure"),
            ResourceLocation.parse("minecraft:chests/nether_bridge"));

    private LootTableGameTests() {}

    @GameTest(template = "empty", batch = BATCH)
    public static void yungLootTablesLoad(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Set<ResourceLocation> loaded = Set.copyOf(server.reloadableRegistries().getKeys(Registries.LOOT_TABLE));
        for (Map.Entry<String, List<String>> namespace : SOURCE_TABLES.entrySet()) {
            if (namespace.getKey().equals("minecraft")) {
                continue;
            }
            for (String path : namespace.getValue()) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.getKey(), path);
                helper.assertTrue(loaded.contains(id), id + " must be present after data reload");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void lootResourcesAreExactAndValid(GameTestHelper helper) {
        IModFile modFile = ModList.get().getModFileById(YungsBetterTfc.MOD_ID).getFile();
        Map<String, Integer> expectedCounts = Map.of(
                "betterdungeons", 8,
                "betterfortresses", 9,
                "betteroceanmonuments", 1,
                "betterstrongholds", 10,
                "minecraft", 5,
                YungsBetterTfc.MOD_ID, 55);
        int total = 0;
        try {
            for (Map.Entry<String, Integer> expected : expectedCounts.entrySet()) {
                Path root = modFile.findResource("data", expected.getKey(), "loot_table");
                long count;
                try (var files = Files.walk(root)) {
                    count = files.filter(path -> path.toString().endsWith(".json")).count();
                }
                helper.assertTrue(count == expected.getValue(),
                        expected.getKey() + " must contain " + expected.getValue() + " loot tables, found " + count);
                total += Math.toIntExact(count);
            }
            helper.assertTrue(total == 88, "The mod resource pack must contain exactly 88 loot tables");
            for (ResourceLocation id : DELEGATED_TABLES) {
                Path path = lootPath(modFile, id);
                helper.assertTrue(Files.notExists(path), id + " must not be overridden by this mod");
            }
            for (ResourceLocation id : EXPECTED_TABLES) {
                Path path = lootPath(modFile, id);
                helper.assertTrue(Files.exists(path), id + " must exist in this mod resource pack");
                byte[] bytes = Files.readAllBytes(path);
                helper.assertTrue(!hasBom(bytes), id + " must be UTF-8 without a BOM");
                JsonElement json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
                assertRegisteredItems(helper, id, json);
            }
            Path tags = modFile.findResource("data", YungsBetterTfc.MOD_ID, "tags", "item", "loot");
            for (String name : List.of("produce", "seeds", "fertilizers")) {
                Path path = tags.resolve(name + ".json");
                helper.assertTrue(Files.exists(path), "The " + name + " loot item tag must exist");
                byte[] bytes = Files.readAllBytes(path);
                helper.assertTrue(!hasBom(bytes), name + " loot item tag must be UTF-8 without a BOM");
                JsonObject json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                helper.assertTrue(!json.getAsJsonArray("values").isEmpty(), name + " loot item tag must have members");
                json.getAsJsonArray("values").forEach(member -> {
                    ResourceLocation id = ResourceLocation.parse(member.getAsString());
                    helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), name + " tag references missing item " + id);
                });
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect the mod loot-table resources", exception);
        }
        Set<ResourceLocation> loaded = Set.copyOf(
                helper.getLevel().getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE));
        helper.assertTrue(loaded.containsAll(EXPECTED_TABLES), "All 88 tables must be present after data reload");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void overDimsHandlesDelegatedLoot(GameTestHelper helper) {
        ResourceLocation serializer = ResourceLocation.parse("tfcoverdims:tfc_loot_replacement");
        helper.assertTrue(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.containsKey(serializer),
                "TFC Overdims must register its loot replacement modifier codec");
        MinecraftServer server = helper.getLevel().getServer();
        for (ResourceLocation id : DELEGATED_TABLES) {
            ResourceLocation modifier = ResourceLocation.fromNamespaceAndPath(
                    "tfcoverdims", "loot_modifiers/chests/" + id.getPath().substring("chests/".length()) + ".json");
            helper.assertTrue(server.getResourceManager().getResource(modifier).isPresent(),
                    "TFC Overdims modifier data must be loaded for " + id);
            assertDelegatedTableProducesTfcLoot(helper, id);
        }
        helper.succeed();
    }

    private static void assertDelegatedTableProducesTfcLoot(GameTestHelper helper, ResourceLocation id) {
        ServerLevel level = helper.getLevel();
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        helper.assertTrue(table != LootTable.EMPTY, id + " must resolve to the Vanilla loot table");
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        List<ItemStack> generated = new ArrayList<>();
        for (long seed = 1; seed <= 128; seed++) {
            generated.addAll(table.getRandomItems(params, seed));
        }
        helper.assertTrue(generated.stream().anyMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem())
                .getNamespace().equals("tfc")), id + " must receive TFC loot from the Overdims modifier");
    }

    private static void assertRegisteredItems(GameTestHelper helper, ResourceLocation table, JsonElement value) {
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(child -> assertRegisteredItems(helper, table, child));
            return;
        }
        if (!value.isJsonObject()) {
            return;
        }
        JsonObject object = value.getAsJsonObject();
        if (object.has("type") && object.has("name")
                && object.get("type").getAsString().equals("minecraft:item")) {
            ResourceLocation item = ResourceLocation.parse(object.get("name").getAsString());
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(item), table + " references missing item " + item);
        } else if (object.has("type") && object.has("name")
                && object.get("type").getAsString().equals("minecraft:tag")) {
            ResourceLocation id = ResourceLocation.parse(object.get("name").getAsString());
            TagKey<net.minecraft.world.item.Item> tag = TagKey.create(Registries.ITEM, id);
            helper.assertTrue(BuiltInRegistries.ITEM.getTag(tag).map(members -> members.size() > 0).orElse(false),
                    table + " references an empty or unbound item tag " + id);
        }
        object.asMap().values().forEach(child -> assertRegisteredItems(helper, table, child));
    }

    private static boolean hasBom(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF;
    }

    private static Path lootPath(IModFile modFile, ResourceLocation id) {
        return modFile.findResource("data", id.getNamespace(), "loot_table", id.getPath() + ".json");
    }

    private static Set<ResourceLocation> expectedTables() {
        Set<ResourceLocation> tables = new LinkedHashSet<>();
        SOURCE_TABLES.forEach((namespace, paths) -> paths.forEach(
                path -> tables.add(ResourceLocation.fromNamespaceAndPath(namespace, path))));
        List<String> families = List.of(
                "axe", "boots", "bow", "chestplate", "crossbow", "helmet", "hoe", "horse_armor",
                "knife", "leggings", "pickaxe", "shears", "shield", "shovel", "sword");
        for (String tier : List.of("dangerous", "lategame", "ordinary")) {
            for (String family : families) {
                tables.add(ResourceLocation.fromNamespaceAndPath(
                        YungsBetterTfc.MOD_ID, "shared/equipment/" + tier + "/" + family));
            }
        }
        for (String path : List.of(
                "shared/coal_like", "shared/fertilizers", "shared/gems", "shared/ingots/dangerous",
                "shared/ingots/lategame", "shared/ingots/ordinary", "shared/produce", "shared/seeds",
                "shared/small_ores/iron", "shared/utility")) {
            tables.add(ResourceLocation.fromNamespaceAndPath(YungsBetterTfc.MOD_ID, path));
        }
        return Set.copyOf(tables);
    }

    private static Map<String, List<String>> sourceTables() {
        Map<String, List<String>> tables = new LinkedHashMap<>();
        tables.put("betterdungeons", List.of(
                "skeleton_dungeon/chests/common", "skeleton_dungeon/chests/middle",
                "small_dungeon/chests/loot_piles", "small_nether_dungeon/chests/common",
                "spider_dungeon/chests/egg_room", "zombie_dungeon/chests/common",
                "zombie_dungeon/chests/special", "zombie_dungeon/chests/tombstone"));
        tables.put("betterfortresses", List.of(
                "chests/beacon", "chests/extra", "chests/hall", "chests/keep", "chests/obsidian",
                "chests/puzzle", "chests/quarters", "chests/storage", "chests/worship"));
        tables.put("betteroceanmonuments", List.of("chests/upper_side_chamber"));
        tables.put("betterstrongholds", List.of(
                "chests/armoury", "chests/cmd_yung", "chests/common", "chests/crypt",
                "chests/grand_library", "chests/library_md", "chests/mess", "chests/prison_lg",
                "chests/trap", "chests/treasure"));
        tables.put("minecraft", List.of(
                "chests/abandoned_mineshaft", "chests/simple_dungeon", "chests/stronghold_corridor",
                "chests/stronghold_crossing", "chests/stronghold_library"));
        return Map.copyOf(tables);
    }
}
