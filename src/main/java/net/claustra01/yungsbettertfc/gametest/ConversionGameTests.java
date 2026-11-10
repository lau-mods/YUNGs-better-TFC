package net.claustra01.yungsbettertfc.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.claustra01.yungsbettertfc.world.conversion.BlockConversions;
import net.claustra01.yungsbettertfc.world.conversion.ConversionContext;
import net.claustra01.yungsbettertfc.world.conversion.DimensionKind;
import net.claustra01.yungsbettertfc.world.conversion.WoodHint;
import net.claustra01.yungsbettertfc.world.processor.TfcBlockReplacementProcessor;
import net.claustra01.yungsbettertfc.world.processor.ModProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(YungsBetterTfc.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConversionGameTests {
    private static final String BATCH = "bootstrap";
    private static final ConversionContext OVERWORLD =
            new ConversionContext(DimensionKind.OVERWORLD, "rhyolite", "andisol", WoodHint.OAK, false);
    private static final ConversionContext NETHER =
            new ConversionContext(DimensionKind.NETHER, "basalt", "oxisol", WoodHint.OAK, false);
    private static final ConversionContext END =
            new ConversionContext(DimensionKind.NONE, "granite", "mollisol", WoodHint.OAK, false);

    private ConversionGameTests() {}

    @GameTest(template = "empty", batch = BATCH)
    public static void additionalRockFamiliesAndProperties(GameTestHelper helper) {
        helper.assertTrue(
                BuiltInRegistries.STRUCTURE_PROCESSOR.getKey(ModProcessors.TFC_BLOCK_REPLACEMENT.get())
                        .equals(ResourceLocation.fromNamespaceAndPath(YungsBetterTfc.MOD_ID, "tfc_block_replacement")),
                "The structure processor type must use the public M02 registry ID");
        Map<String, String> mappings = new HashMap<>();
        for (String family : List.of("granite", "diorite", "tuff")) {
            mappings.put(family, "tfc:rock/raw/rhyolite");
            mappings.put(family + "_stairs", "tfc:rock/raw/rhyolite_stairs");
            mappings.put(family + "_slab", "tfc:rock/raw/rhyolite_slab");
            mappings.put(family + "_wall", "tfc:rock/raw/rhyolite_wall");
            mappings.put("polished_" + family, "tfc:rock/smooth/rhyolite");
            mappings.put("polished_" + family + "_stairs", "tfc:rock/smooth/rhyolite_stairs");
            mappings.put("polished_" + family + "_slab", "tfc:rock/smooth/rhyolite_slab");
        }
        mappings.put("tuff_bricks", "tfc:rock/bricks/rhyolite");
        mappings.put("tuff_brick_stairs", "tfc:rock/bricks/rhyolite_stairs");
        mappings.put("tuff_brick_slab", "tfc:rock/bricks/rhyolite_slab");
        mappings.put("tuff_brick_wall", "tfc:rock/bricks/rhyolite_wall");
        mappings.put("chiseled_tuff", "tfc:rock/chiseled/rhyolite");
        mappings.put("chiseled_tuff_bricks", "tfc:rock/chiseled/rhyolite");
        assertMappings(helper, mappings, OVERWORLD);

        BlockState eastFacing = Blocks.GRANITE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        BlockState converted = BlockConversions.convert(eastFacing, OVERWORLD);
        helper.assertTrue(converted.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
                "Stair facing must be inherited");

        ConversionContext missingRock = new ConversionContext(
                DimensionKind.OVERWORLD, "not_registered", "andisol", WoodHint.OAK, false);
        helper.assertTrue(BlockConversions.convert(eastFacing, missingRock) == eastFacing,
                "A missing output registry ID must preserve the original state");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void legacyRockFamiliesAndTemplateGeology(GameTestHelper helper) {
        assertMappings(helper, Map.ofEntries(
                Map.entry("stone", "tfc:rock/raw/rhyolite"),
                Map.entry("stone_slab", "tfc:rock/raw/rhyolite_slab"),
                Map.entry("andesite", "tfc:rock/raw/rhyolite"),
                Map.entry("andesite_stairs", "tfc:rock/raw/rhyolite_stairs"),
                Map.entry("andesite_slab", "tfc:rock/raw/rhyolite_slab"),
                Map.entry("andesite_wall", "tfc:rock/raw/rhyolite_wall"),
                Map.entry("polished_andesite", "tfc:rock/smooth/rhyolite"),
                Map.entry("polished_andesite_stairs", "tfc:rock/smooth/rhyolite_stairs"),
                Map.entry("polished_andesite_slab", "tfc:rock/smooth/rhyolite_slab"),
                Map.entry("cobblestone", "tfc:rock/cobble/rhyolite"),
                Map.entry("cobblestone_stairs", "tfc:rock/cobble/rhyolite_stairs"),
                Map.entry("cobblestone_slab", "tfc:rock/cobble/rhyolite_slab"),
                Map.entry("cobblestone_wall", "tfc:rock/cobble/rhyolite_wall"),
                Map.entry("mossy_cobblestone", "tfc:rock/mossy_cobble/rhyolite"),
                Map.entry("mossy_cobblestone_stairs", "tfc:rock/mossy_cobble/rhyolite_stairs"),
                Map.entry("mossy_cobblestone_slab", "tfc:rock/mossy_cobble/rhyolite_slab"),
                Map.entry("mossy_cobblestone_wall", "tfc:rock/mossy_cobble/rhyolite_wall"),
                Map.entry("stone_bricks", "tfc:rock/bricks/rhyolite"),
                Map.entry("stone_brick_stairs", "tfc:rock/bricks/rhyolite_stairs"),
                Map.entry("stone_brick_slab", "tfc:rock/bricks/rhyolite_slab"),
                Map.entry("stone_brick_wall", "tfc:rock/bricks/rhyolite_wall"),
                Map.entry("mossy_stone_bricks", "tfc:rock/mossy_bricks/rhyolite"),
                Map.entry("mossy_stone_brick_stairs", "tfc:rock/mossy_bricks/rhyolite_stairs"),
                Map.entry("mossy_stone_brick_slab", "tfc:rock/mossy_bricks/rhyolite_slab"),
                Map.entry("mossy_stone_brick_wall", "tfc:rock/mossy_bricks/rhyolite_wall"),
                Map.entry("cracked_stone_bricks", "tfc:rock/cracked_bricks/rhyolite"),
                Map.entry("chiseled_stone_bricks", "tfc:rock/chiseled/rhyolite"),
                Map.entry("smooth_stone", "tfc:rock/smooth/rhyolite"),
                Map.entry("smooth_stone_slab", "tfc:rock/smooth/rhyolite_slab"),
                Map.entry("gravel", "tfc:rock/gravel/rhyolite"),
                Map.entry("stone_button", "tfc:rock/button/rhyolite"),
                Map.entry("stone_pressure_plate", "tfc:rock/pressure_plate/rhyolite"),
                Map.entry("infested_stone", "tfc:rock/raw/rhyolite"),
                Map.entry("infested_stone_bricks", "tfc:rock/bricks/rhyolite")), OVERWORLD);

        BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
        helper.getLevel().setBlock(origin, block("tfc:dirt/oxisol").defaultBlockState(), 3);
        helper.getLevel().setBlock(origin.below(), block("tfc:rock/raw/rhyolite").defaultBlockState(), 3);
        List<StructureTemplate.StructureBlockInfo> result = process(
                helper.getLevel(),
                origin,
                List.of(
                        info(0, 0, 0, Blocks.STONE.defaultBlockState()),
                        info(1, 0, 0, Blocks.DIRT.defaultBlockState())));
        assertId(helper, result.get(0).state(), "tfc:rock/raw/rhyolite");
        assertId(helper, result.get(1).state(), "tfc:dirt/oxisol");

        BlockPos shapedOrigin = helper.absolutePos(new BlockPos(4, 4, 4));
        helper.getLevel().setBlock(shapedOrigin, block("tfc:rock/raw/granite_stairs").defaultBlockState(), 3);
        helper.getLevel().setBlock(shapedOrigin.below(), block("tfc:rock/raw/rhyolite").defaultBlockState(), 3);
        assertId(helper, process(helper.getLevel(), shapedOrigin,
                List.of(info(0, 0, 0, Blocks.STONE.defaultBlockState()))).getFirst().state(),
                "tfc:rock/raw/rhyolite");

        BlockPos fallbackOrigin = helper.absolutePos(new BlockPos(6, 70, 6));
        clearScanColumn(helper.getLevel(), fallbackOrigin);
        helper.getLevel().setBlock(fallbackOrigin.below(), Blocks.BLACKSTONE.defaultBlockState(), 3);
        assertId(helper, process(helper.getLevel(), fallbackOrigin,
                List.of(info(0, 0, 0, Blocks.STONE.defaultBlockState()))).getFirst().state(),
                "tfc:rock/raw/granite");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void sandstoneAndSoilFamilies(GameTestHelper helper) {
        Map<String, String> mappings = new HashMap<>();
        addSandstoneMappings(mappings, "", "yellow");
        addSandstoneMappings(mappings, "red_", "red");
        mappings.put("dirt", "tfc:dirt/andisol");
        mappings.put("coarse_dirt", "tfc:coarse_dirt/andisol");
        mappings.put("grass_block", "tfc:grass/andisol");
        mappings.put("rooted_dirt", "tfc:rooted_dirt/andisol");
        mappings.put("farmland", "tfc:farmland/andisol");
        mappings.put("dirt_path", "tfc:grass_path/andisol");
        mappings.put("sand", "tfc:sand/yellow");
        mappings.put("red_sand", "tfc:sand/red");
        assertMappings(helper, mappings, OVERWORLD);

        BlockPos fallbackOrigin = helper.absolutePos(new BlockPos(8, 70, 8));
        clearScanColumn(helper.getLevel(), fallbackOrigin);
        List<StructureTemplate.StructureBlockInfo> fallback = process(helper.getLevel(), fallbackOrigin, List.of(
                info(0, 0, 0, Blocks.DIRT.defaultBlockState()),
                info(1, 0, 0, Blocks.DIRT_PATH.defaultBlockState())));
        assertId(helper, fallback.get(0).state(), "tfc:dirt/mollisol");
        assertId(helper, fallback.get(1).state(), "tfc:grass_path/mollisol");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void deepslateBlackstoneAndEndStayUnchanged(GameTestHelper helper) {
        for (String path : List.of(
                "deepslate",
                "cobbled_deepslate",
                "deepslate_bricks",
                "blackstone",
                "polished_blackstone")) {
            BlockState original = vanilla(path);
            helper.assertTrue(BlockConversions.convert(original, OVERWORLD) == original,
                    path + " must be preserved in the Overworld");
        }

        Map<String, String> allMappings = allRockMappings("granite");
        allMappings.putAll(allSoilMappings("mollisol"));
        allMappings.put("netherrack", "minecraft:netherrack");
        for (String path : allMappings.keySet()) {
            BlockState original = vanilla(path);
            helper.assertTrue(BlockConversions.convert(original, END) == original,
                    path + " must be preserved when the dimension is NONE");
        }
        helper.assertTrue(DimensionKind.resolve(helper.getLevel().getServer().getLevel(Level.END)) == DimensionKind.NONE,
                "The End must resolve to NONE");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void netherRockFamiliesUseBasalt(GameTestHelper helper) {
        assertMappings(helper, allRockMappings("basalt"), NETHER);
        helper.assertTrue(DimensionKind.resolve(helper.getLevel().getServer().getLevel(Level.NETHER)) == DimensionKind.NETHER,
                "The Nether must resolve to NETHER");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void netherSoilsUseOxisolAndSandKeepsColor(GameTestHelper helper) {
        assertMappings(helper, allSoilMappings("oxisol"), NETHER);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void hardeningUsesOnlyBlockDirectlyBelow(GameTestHelper helper) {
        ConversionContext unsupported = NETHER.withBelowIsAir(true);
        assertConversion(helper, "netherrack", "tfcoverdims:hardened_netherrack", unsupported);
        assertConversion(helper, "stone", "tfc:rock/hardened/basalt", unsupported);
        assertConversion(helper, "cobblestone", "tfc:rock/hardened/basalt", unsupported);
        assertConversion(helper, "mossy_cobblestone", "tfc:rock/hardened/basalt", unsupported);
        assertConversion(helper, "granite_stairs", "tfc:rock/raw/basalt_stairs", unsupported);
        assertConversion(helper, "granite_slab", "tfc:rock/raw/basalt_slab", unsupported);
        assertConversion(helper, "granite_wall", "tfc:rock/raw/basalt_wall", unsupported);
        assertConversion(helper, "netherrack", "minecraft:netherrack", NETHER);
        assertConversion(helper, "stone", "tfc:rock/raw/basalt", NETHER);

        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        BlockPos origin = new BlockPos(0, 80, 0);
        nether.setBlock(origin.offset(22, 0, 0), Blocks.AIR.defaultBlockState(), 3);
        nether.setBlock(origin.offset(24, 0, 0), Blocks.BLACKSTONE.defaultBlockState(), 3);
        List<StructureTemplate.StructureBlockInfo> result = process(nether, origin, List.of(
                info(0, 2, 0, Blocks.NETHERRACK.defaultBlockState()),
                info(0, 1, 0, Blocks.AIR.defaultBlockState()),
                info(2, 2, 0, Blocks.NETHERRACK.defaultBlockState()),
                info(2, 1, 0, Blocks.BLACKSTONE.defaultBlockState()),
                info(4, 2, 0, Blocks.STONE.defaultBlockState()),
                info(4, 1, 0, Blocks.AIR.defaultBlockState()),
                info(6, 2, 0, Blocks.COBBLESTONE.defaultBlockState()),
                info(6, 1, 0, Blocks.AIR.defaultBlockState()),
                info(8, 2, 0, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                info(8, 1, 0, Blocks.AIR.defaultBlockState()),
                info(10, 2, 0, Blocks.GRANITE_STAIRS.defaultBlockState()),
                info(10, 1, 0, Blocks.AIR.defaultBlockState()),
                info(12, 2, 0, Blocks.GRANITE_SLAB.defaultBlockState()),
                info(12, 1, 0, Blocks.AIR.defaultBlockState()),
                info(14, 2, 0, Blocks.GRANITE_WALL.defaultBlockState()),
                info(14, 1, 0, Blocks.AIR.defaultBlockState()),
                info(16, 2, 0, Blocks.STONE.defaultBlockState()),
                info(16, 1, 0, Blocks.BLACKSTONE.defaultBlockState()),
                info(18, 2, 0, Blocks.COBBLESTONE.defaultBlockState()),
                info(18, 1, 0, Blocks.BLACKSTONE.defaultBlockState()),
                info(20, 2, 0, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                info(20, 1, 0, Blocks.BLACKSTONE.defaultBlockState()),
                info(22, 1, 0, Blocks.STONE.defaultBlockState()),
                info(24, 1, 0, Blocks.STONE.defaultBlockState())));
        Map<BlockPos, BlockState> byPosition = new HashMap<>();
        result.forEach(info -> byPosition.put(info.pos(), info.state()));
        assertId(helper, byPosition.get(origin.offset(0, 2, 0)), "tfcoverdims:hardened_netherrack");
        assertId(helper, byPosition.get(origin.offset(2, 2, 0)), "minecraft:netherrack");
        assertId(helper, byPosition.get(origin.offset(4, 2, 0)), "tfc:rock/hardened/basalt");
        assertId(helper, byPosition.get(origin.offset(6, 2, 0)), "tfc:rock/hardened/basalt");
        assertId(helper, byPosition.get(origin.offset(8, 2, 0)), "tfc:rock/hardened/basalt");
        assertId(helper, byPosition.get(origin.offset(10, 2, 0)), "tfc:rock/raw/basalt_stairs");
        assertId(helper, byPosition.get(origin.offset(12, 2, 0)), "tfc:rock/raw/basalt_slab");
        assertId(helper, byPosition.get(origin.offset(14, 2, 0)), "tfc:rock/raw/basalt_wall");
        assertId(helper, byPosition.get(origin.offset(16, 2, 0)), "tfc:rock/raw/basalt");
        assertId(helper, byPosition.get(origin.offset(18, 2, 0)), "tfc:rock/cobble/basalt");
        assertId(helper, byPosition.get(origin.offset(20, 2, 0)), "tfc:rock/mossy_cobble/basalt");
        assertId(helper, byPosition.get(origin.offset(22, 1, 0)), "tfc:rock/hardened/basalt");
        assertId(helper, byPosition.get(origin.offset(24, 1, 0)), "tfc:rock/raw/basalt");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void netherNativeBlocksStayUnchanged(GameTestHelper helper) {
        for (String path : List.of(
                "blackstone",
                "polished_blackstone",
                "basalt",
                "polished_basalt",
                "soul_sand",
                "soul_soil",
                "nether_gold_ore",
                "nether_quartz_ore",
                "ancient_debris")) {
            BlockState original = vanilla(path);
            helper.assertTrue(BlockConversions.convert(original, NETHER) == original,
                    path + " must be preserved in the Nether");
        }
        helper.succeed();
    }

    private static void addSandstoneMappings(Map<String, String> mappings, String vanillaPrefix, String color) {
        String redPathPart = vanillaPrefix.equals("red_") ? "red_" : "";
        mappings.put(redPathPart + "sandstone", "tfc:raw_sandstone/" + color);
        mappings.put(redPathPart + "sandstone_stairs", "tfc:raw_sandstone/" + color + "_stairs");
        mappings.put(redPathPart + "sandstone_slab", "tfc:raw_sandstone/" + color + "_slab");
        mappings.put(redPathPart + "sandstone_wall", "tfc:raw_sandstone/" + color + "_wall");
        mappings.put("cut_" + redPathPart + "sandstone", "tfc:cut_sandstone/" + color);
        mappings.put("chiseled_" + redPathPart + "sandstone", "tfc:cut_sandstone/" + color);
        mappings.put("cut_" + redPathPart + "sandstone_slab", "tfc:cut_sandstone/" + color + "_slab");
        mappings.put("smooth_" + redPathPart + "sandstone", "tfc:smooth_sandstone/" + color);
        mappings.put("smooth_" + redPathPart + "sandstone_stairs", "tfc:smooth_sandstone/" + color + "_stairs");
        mappings.put("smooth_" + redPathPart + "sandstone_slab", "tfc:smooth_sandstone/" + color + "_slab");
    }

    private static Map<String, String> allRockMappings(String rock) {
        Map<String, String> mappings = new HashMap<>();
        for (String family : List.of("stone", "andesite", "granite", "diorite", "tuff")) {
            mappings.put(family, "tfc:rock/raw/" + rock);
            for (String shape : List.of("_stairs", "_slab", "_wall")) {
                ResourceLocation shapeId = ResourceLocation.fromNamespaceAndPath("minecraft", family + shape);
                if (BuiltInRegistries.BLOCK.containsKey(shapeId)) {
                    mappings.put(family + shape, "tfc:rock/raw/" + rock + shape);
                }
            }
        }
        for (String family : List.of(
                "polished_andesite", "polished_granite", "polished_diorite", "polished_tuff")) {
            mappings.put(family, "tfc:rock/smooth/" + rock);
            mappings.put(family + "_stairs", "tfc:rock/smooth/" + rock + "_stairs");
            mappings.put(family + "_slab", "tfc:rock/smooth/" + rock + "_slab");
        }
        for (String shape : List.of("", "_stairs", "_slab", "_wall")) {
            mappings.put("cobblestone" + shape, "tfc:rock/cobble/" + rock + shape);
            mappings.put("mossy_cobblestone" + shape, "tfc:rock/mossy_cobble/" + rock + shape);
            mappings.put("stone_brick" + (shape.isEmpty() ? "s" : shape),
                    "tfc:rock/bricks/" + rock + shape);
            mappings.put("tuff_brick" + (shape.isEmpty() ? "s" : shape),
                    "tfc:rock/bricks/" + rock + shape);
            mappings.put("mossy_stone_brick" + (shape.isEmpty() ? "s" : shape),
                    "tfc:rock/mossy_bricks/" + rock + shape);
        }
        mappings.put("cracked_stone_bricks", "tfc:rock/cracked_bricks/" + rock);
        mappings.put("chiseled_stone_bricks", "tfc:rock/chiseled/" + rock);
        mappings.put("chiseled_tuff", "tfc:rock/chiseled/" + rock);
        mappings.put("chiseled_tuff_bricks", "tfc:rock/chiseled/" + rock);
        mappings.put("smooth_stone", "tfc:rock/smooth/" + rock);
        mappings.put("smooth_stone_slab", "tfc:rock/smooth/" + rock + "_slab");
        mappings.put("gravel", "tfc:rock/gravel/" + rock);
        mappings.put("stone_button", "tfc:rock/button/" + rock);
        mappings.put("stone_pressure_plate", "tfc:rock/pressure_plate/" + rock);
        mappings.put("infested_stone", "tfc:rock/raw/" + rock);
        mappings.put("infested_stone_bricks", "tfc:rock/bricks/" + rock);
        return mappings;
    }

    private static Map<String, String> allSoilMappings(String soil) {
        Map<String, String> mappings = new HashMap<>();
        addSandstoneMappings(mappings, "", "yellow");
        addSandstoneMappings(mappings, "red_", "red");
        mappings.put("dirt", "tfc:dirt/" + soil);
        mappings.put("coarse_dirt", "tfc:coarse_dirt/" + soil);
        mappings.put("grass_block", "tfc:grass/" + soil);
        mappings.put("rooted_dirt", "tfc:rooted_dirt/" + soil);
        mappings.put("farmland", "tfc:farmland/" + soil);
        mappings.put("dirt_path", "tfc:grass_path/" + soil);
        mappings.put("sand", "tfc:sand/yellow");
        mappings.put("red_sand", "tfc:sand/red");
        return mappings;
    }

    private static List<StructureTemplate.StructureBlockInfo> process(
            ServerLevel level,
            BlockPos origin,
            List<StructureTemplate.StructureBlockInfo> blocks) {
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .addProcessor(TfcBlockReplacementProcessor.INSTANCE);
        return StructureTemplate.processBlockInfos(level, origin, origin, settings, blocks, null);
    }

    private static void clearScanColumn(ServerLevel level, BlockPos origin) {
        for (int distance = 0; distance < 64; distance++) {
            level.setBlock(origin.below(distance), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static StructureTemplate.StructureBlockInfo info(int x, int y, int z, BlockState state) {
        return new StructureTemplate.StructureBlockInfo(new BlockPos(x, y, z), state, null);
    }

    private static void assertMappings(
            GameTestHelper helper,
            Map<String, String> mappings,
            ConversionContext context) {
        mappings.forEach((input, expected) -> assertConversion(helper, input, expected, context));
    }

    private static void assertConversion(
            GameTestHelper helper,
            String input,
            String expected,
            ConversionContext context) {
        assertId(helper, BlockConversions.convert(vanilla(input), context), expected);
    }

    private static void assertId(GameTestHelper helper, BlockState state, String expected) {
        ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        helper.assertTrue(actual.equals(ResourceLocation.parse(expected)),
                "Expected " + expected + " but got " + actual);
    }

    private static BlockState vanilla(String path) {
        return block("minecraft:" + path).defaultBlockState();
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElseThrow();
    }
}
