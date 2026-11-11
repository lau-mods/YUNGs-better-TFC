package net.claustra01.yungsbettertfc.gametest;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.claustra01.yungsbettertfc.loot.StructureLootRules;
import net.claustra01.yungsbettertfc.loot.StructureLootRules.Profile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforgespi.locating.IModFile;

@GameTestHolder(YungsBetterTfc.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LootTableGameTests {
    private static final String BATCH = "bootstrap";
    private static final Map<String, List<String>> TARGET_TABLES = targetTables();
    private static final Set<ResourceLocation> TARGETS = targets();
    private static final Set<ResourceLocation> DELEGATED_TABLES = Set.of(
            ResourceLocation.parse("minecraft:chests/end_city_treasure"),
            ResourceLocation.parse("minecraft:chests/nether_bridge"));
    /** Replacement sources that may also be a rolled replacement result. */
    private static final Set<Item> SELF_CANDIDATES = Set.of(Items.BOW, Items.CROSSBOW, Items.CHARCOAL);

    private LootTableGameTests() {}

    @GameTest(template = "empty", batch = BATCH)
    public static void yungLootTablesLoad(GameTestHelper helper) {
        Set<ResourceLocation> loaded = Set.copyOf(
                helper.getLevel().getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE));
        for (ResourceLocation id : loadedTargets()) {
            helper.assertTrue(loaded.contains(id), id + " must be provided by its own mod after data reload");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void lootResourcesAreExactAndValid(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        IModFile modFile = ModList.get().getModFileById(YungsBetterTfc.MOD_ID).getFile();
        try {
            for (String namespace : List.of("betterdungeons", "betterfortresses", "betteroceanmonuments",
                    "betterstrongholds", "minecraft", YungsBetterTfc.MOD_ID)) {
                helper.assertTrue(Files.notExists(modFile.findResource("data", namespace, "loot_table")),
                        "This mod must not override " + namespace + " loot tables");
            }
            Path root = modFile.findResource("data", YungsBetterTfc.MOD_ID, "loot_modifiers");
            Set<ResourceLocation> files = new LinkedHashSet<>();
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(path -> path.toString().endsWith(".json")).toList()) {
                    String name = root.relativize(path).toString().replace('\\', '/');
                    files.add(ResourceLocation.fromNamespaceAndPath(YungsBetterTfc.MOD_ID, name.substring(0, name.length() - 5)));
                    JsonElement json = readJson(helper, path, name);
                    IGlobalLootModifier.DIRECT_CODEC
                            .parse(server.registryAccess().createSerializationContext(JsonOps.INSTANCE), json)
                            .getOrThrow(error -> new IllegalStateException(name + " must decode: " + error));
                    ResourceLocation table = ResourceLocation.parse(json.getAsJsonObject().getAsJsonArray("conditions")
                            .get(0).getAsJsonObject().get("loot_table_id").getAsString());
                    helper.assertTrue(TARGETS.contains(table) && name.equals(table.getNamespace() + "/" + table.getPath() + ".json"),
                            name + " must target its own maintained table, not " + table);
                    String tablePath = table.getPath();
                    boolean dangerous = table.getNamespace().equals("betterfortresses")
                            || table.getNamespace().equals("betterstrongholds")
                            || (table.getNamespace().equals("betterdungeons") && tablePath.startsWith("small_nether_dungeon/"))
                            || (table.getNamespace().equals("minecraft") && tablePath.startsWith("chests/stronghold_"));
                    helper.assertTrue(json.getAsJsonObject().get("profile").getAsString().equals(dangerous ? "dangerous" : "ordinary"),
                            name + " must use the " + (dangerous ? "dangerous" : "ordinary") + " profile");
                }
            }
            Set<ResourceLocation> expected = new LinkedHashSet<>();
            TARGETS.forEach(table -> expected.add(modifierId(table)));
            helper.assertTrue(files.equals(expected), "The mod must contain exactly the 33 structure loot modifiers");
            Set<String> globalEntries = new LinkedHashSet<>();
            for (Resource resource : server.getResourceManager().getResourceStack(
                    ResourceLocation.fromNamespaceAndPath("neoforge", "loot_modifiers/global_loot_modifiers.json"))) {
                try (Reader reader = resource.openAsReader()) {
                    JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("entries")
                            .forEach(entry -> globalEntries.add(entry.getAsString()));
                }
            }
            expected.forEach(id -> helper.assertTrue(globalEntries.contains(id.toString()),
                    id + " must be listed in the NeoForge global loot modifiers"));
            Path tags = modFile.findResource("data", YungsBetterTfc.MOD_ID, "tags", "item", "loot");
            for (TagKey<Item> tag : List.of(StructureLootRules.FERTILIZERS)) {
                String name = tag.location().getPath().substring("loot/".length());
                JsonObject json = readJson(helper, tags.resolve(name + ".json"), name).getAsJsonObject();
                json.getAsJsonArray("values").forEach(member -> helper.assertTrue(
                        BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(member.getAsString())),
                        name + " tag references missing item " + member));
                helper.assertTrue(BuiltInRegistries.ITEM.getTag(tag).map(members -> members.size() > 0).orElse(false),
                        name + " loot item tag must be bound with members");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect the mod loot resources", exception);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void structureLootModifiersReplaceRolledItems(GameTestHelper helper) {
        int tfc = 0;
        for (ResourceLocation id : loadedTargets()) {
            for (ItemStack stack : roll(helper, id)) {
                helper.assertTrue(!StructureLootRules.isReplaced(stack.getItem()) || SELF_CANDIDATES.contains(stack.getItem()),
                        id + " must not keep the replaced Vanilla item " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
                if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("tfc")) {
                    tfc++;
                }
            }
        }
        helper.assertTrue(tfc > 0, "The maintained structure tables must produce TFC loot");

        LootContext context = context(helper);
        List<ItemStack> ingots = StructureLootRules.replace(new ItemStack(Items.IRON_INGOT, 5), Profile.DANGEROUS, Map.of(), Map.of(), context);
        helper.assertTrue(ingots.size() == 1 && ingots.get(0).getCount() == 5 && Set.of("tfc:metal/ingot/wrought_iron",
                        "tfc:metal/ingot/steel").contains(BuiltInRegistries.ITEM.getKey(ingots.get(0).getItem()).toString()),
                "Ingots must keep their count and use the Dangerous ingot tier");
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 2);
        sword.setDamageValue(10);
        helper.assertTrue(StructureLootRules.replace(sword, Profile.ORDINARY, Map.of(), Map.of(), context).get(0) == sword,
                "An enchanted item without a known source enchantment function must stay unchanged");
        Map<ResourceLocation, LootItemFunction> enchantments = Map.of(ResourceLocation.parse("minecraft:iron_sword"),
                EnchantWithLevelsFunction.enchantWithLevels(helper.getLevel().registryAccess(), ConstantValue.exactly(30)).build());
        int finishedEnchanted = 0;
        for (int i = 0; i < 64; i++) {
            ItemStack part = StructureLootRules.replace(sword.copy(), Profile.ORDINARY, enchantments, Map.of(), context).get(0);
            String path = BuiltInRegistries.ITEM.getKey(part.getItem()).getPath();
            helper.assertTrue(path.matches("metal/(sword_blade|javelin_head|mace_head|sword|javelin|mace)/(copper|bronze|bismuth_bronze|black_bronze)"),
                    "Ordinary swords must use the Ordinary sword family, found " + path);
            helper.assertTrue(part.getDamageValue() == 0, "Replacements must drop source damage");
            boolean unfinished = path.matches("metal/(sword_blade|javelin_head|mace_head)/.*");
            helper.assertTrue(!unfinished || part.getEnchantments().isEmpty(), "Unfinished parts must not be enchanted: " + path);
            if (!unfinished && !part.getEnchantments().isEmpty()) {
                finishedEnchanted++;
            }
        }
        helper.assertTrue(finishedEnchanted > 0, "Finished equipment must re-run the source enchantment function");
        Map<Item, String> fixed = Map.of(
                Items.COOKED_BEEF, "tfc:food/cooked_beef", Items.PORKCHOP, "tfc:food/pork",
                Items.BREAD, "tfc:food/wheat_bread", Items.BEETROOT, "tfc:food/beet",
                Items.WHEAT_SEEDS, "tfc:seeds/wheat", Items.MELON_SEEDS, "tfc:seeds/melon",
                Items.RAW_GOLD, "tfc:ore/normal_native_gold", Items.GOLD_NUGGET, "tfc:ore/small_native_gold",
                Items.CHAIN, "tfc:metal/chain/wrought_iron", Items.TORCH, "tfc:torch");
        fixed.forEach((source, target) -> {
            List<ItemStack> output = StructureLootRules.replace(new ItemStack(source, 3), Profile.DANGEROUS, Map.of(), Map.of(), context);
            helper.assertTrue(output.size() == 1 && output.get(0).getCount() == 3
                            && BuiltInRegistries.ITEM.getKey(output.get(0).getItem()).toString().equals(target),
                    source + " must become 3 of " + target);
        });
        for (int i = 0; i < 16; i++) {
            String copper = key(StructureLootRules.replace(new ItemStack(Items.COPPER_INGOT), Profile.ORDINARY, Map.of(), Map.of(), context));
            helper.assertTrue(copper.matches("tfc:metal/ingot/(copper|bronze|bismuth_bronze|black_bronze|wrought_iron)"),
                    "Copper ingots must use the Ordinary ingot tier, found " + copper);
            String block = key(StructureLootRules.replace(new ItemStack(Items.IRON_BLOCK, 2), Profile.DANGEROUS, Map.of(), Map.of(), context));
            helper.assertTrue(block.matches("tfc:metal/double_ingot/(wrought_iron|steel)"),
                    "Metal blocks must become a Dangerous double ingot, found " + block);
            String utility = key(StructureLootRules.replace(new ItemStack(Items.QUARTZ), Profile.ORDINARY, Map.of(), Map.of(), context));
            helper.assertTrue(utility.matches("tfc:(powder/(flux|wood_ash|saltpeter|sylvite)|kaolin_clay)|minecraft:glass(_pane)?"),
                    "Quartz must become a utility item, found " + utility);
        }
        for (Item kept : List.of(Items.OBSIDIAN, Items.GLOWSTONE_DUST, Items.GLOW_BERRIES, Items.WHEAT,
                Items.TURTLE_HELMET, Items.LEATHER_HORSE_ARMOR, Items.GOLDEN_APPLE, Items.TRIDENT)) {
            ItemStack stack = new ItemStack(kept, 2);
            helper.assertTrue(StructureLootRules.replace(stack, Profile.ORDINARY, Map.of(), Map.of(), context).get(0) == stack,
                    kept + " has no replacement rule and must stay unchanged");
        }
        if (ModList.get().isLoaded("betterstrongholds")) {
            List<ItemStack> special = roll(helper, ResourceLocation.parse("betterstrongholds:chests/cmd_yung"));
            Item tfcSword = BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:metal/sword/wrought_iron"));
            helper.assertTrue(!special.isEmpty() && special.stream().allMatch(stack -> stack.is(tfcSword)
                            && stack.has(DataComponents.ITEM_NAME) && !stack.getEnchantments().isEmpty()),
                    "cmd_yung must become a named and enchanted TFC Wrought Iron sword");
        }
        ItemStack named = new ItemStack(Items.DIAMOND);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("special"));
        ItemStack gem = StructureLootRules.replace(named, Profile.ORDINARY, Map.of(), Map.of(), context).get(0);
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(gem.getItem()).getPath().startsWith("gem/")
                        && named.get(DataComponents.CUSTOM_NAME).equals(gem.get(DataComponents.CUSTOM_NAME)),
                "Replacements must keep components set by the source loot functions");
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
            helper.assertTrue(server.getResourceManager().getResource(
                    ResourceLocation.fromNamespaceAndPath(YungsBetterTfc.MOD_ID, "loot_modifiers/" + id.getNamespace()
                            + "/" + id.getPath() + ".json")).isEmpty(), id + " must not have a modifier from this mod");
            helper.assertTrue(roll(helper, id).stream().anyMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem())
                    .getNamespace().equals("tfc")), id + " must receive TFC loot from the Overdims modifier");
        }
        helper.succeed();
    }

    private static LootContext context(GameTestHelper helper) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        return new LootContext.Builder(params).withOptionalRandomSeed(731031L).create(Optional.empty());
    }

    private static String key(List<ItemStack> stacks) {
        return BuiltInRegistries.ITEM.getKey(stacks.get(0).getItem()).toString();
    }

    private static List<ItemStack> roll(GameTestHelper helper, ResourceLocation id) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        helper.assertTrue(table != LootTable.EMPTY, id + " must resolve to a loot table");
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        List<ItemStack> generated = new ArrayList<>();
        for (long seed = 1; seed <= 128; seed++) {
            generated.addAll(table.getRandomItems(params, seed));
        }
        return generated;
    }

    private static JsonElement readJson(GameTestHelper helper, Path path, String name) throws IOException {
        helper.assertTrue(Files.exists(path), name + " must exist");
        byte[] bytes = Files.readAllBytes(path);
        helper.assertTrue(!(bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF),
                name + " must be UTF-8 without a BOM");
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
    }

    private static ResourceLocation modifierId(ResourceLocation table) {
        return ResourceLocation.fromNamespaceAndPath(YungsBetterTfc.MOD_ID, table.getNamespace() + "/" + table.getPath());
    }

    private static List<ResourceLocation> loadedTargets() {
        return TARGETS.stream().filter(id -> ModList.get().isLoaded(id.getNamespace())).toList();
    }

    private static Set<ResourceLocation> targets() {
        Set<ResourceLocation> tables = new LinkedHashSet<>();
        TARGET_TABLES.forEach((namespace, paths) -> paths.forEach(
                path -> tables.add(ResourceLocation.fromNamespaceAndPath(namespace, path))));
        return Set.copyOf(tables);
    }

    private static Map<String, List<String>> targetTables() {
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
