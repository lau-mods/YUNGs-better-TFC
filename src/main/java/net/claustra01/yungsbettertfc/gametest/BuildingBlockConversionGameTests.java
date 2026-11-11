package net.claustra01.yungsbettertfc.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.claustra01.yungsbettertfc.world.conversion.BlockConversions;
import net.claustra01.yungsbettertfc.world.conversion.ConversionContext;
import net.claustra01.yungsbettertfc.world.conversion.DimensionKind;
import net.claustra01.yungsbettertfc.world.conversion.WoodHint;
import net.claustra01.yungsbettertfc.world.conversion.WoodConversions;
import net.claustra01.yungsbettertfc.world.processor.TfcBlockReplacementProcessor;
import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(YungsBetterTfc.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BuildingBlockConversionGameTests {
    private static final String BATCH = "bootstrap";
    private static final ConversionContext OVERWORLD =
            new ConversionContext(DimensionKind.OVERWORLD, "granite", "mollisol", WoodHint.OAK, false);
    private static final ConversionContext NETHER =
            new ConversionContext(DimensionKind.NETHER, "basalt", "oxisol", WoodHint.OAK, false);

    private BuildingBlockConversionGameTests() {}

    @GameTest(template = "empty", batch = BATCH)
    public static void overworldWoodFamiliesAndStates(GameTestHelper helper) {
        Map<String, String> woods = new LinkedHashMap<>();
        woods.put("oak", "oak");
        woods.put("spruce", "spruce");
        woods.put("birch", "birch");
        woods.put("jungle", "acacia");
        woods.put("acacia", "acacia");
        woods.put("dark_oak", "oak");
        woods.put("mangrove", "mangrove");
        woods.put("cherry", "oak");
        woods.put("bamboo", "oak");
        for (Map.Entry<String, String> wood : woods.entrySet()) {
            for (String shape : List.of(
                    "planks", "stairs", "slab", "log", "wood", "stripped_log", "stripped_wood", "fence",
                    "fence_gate", "door", "trapdoor", "button", "pressure_plate", "sign", "wall_sign")) {
                String input = vanillaWoodPath(wood.getKey(), shape);
                if (!containsBlock("minecraft:" + input)) {
                    continue;
                }
                String outputShape = switch (shape) {
                    case "planks" -> "planks/" + wood.getValue();
                    case "stairs", "slab" -> "planks/" + wood.getValue() + "_" + shape;
                    default -> shape + "/" + wood.getValue();
                };
                assertConversion(helper, input, "tfc:wood/" + outputShape, OVERWORLD);
            }
        }

        BlockState door = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        BlockState convertedDoor = BlockConversions.convert(door, OVERWORLD);
        helper.assertTrue(convertedDoor.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER,
                "The upper door half must be inherited");
        helper.assertTrue(convertedDoor.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.WEST,
                "Door facing must be inherited");
        helper.assertTrue(BlockConversions.convert(Blocks.OAK_DOOR.defaultBlockState(), OVERWORLD)
                        .getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER,
                "The lower door half must be inherited");

        BlockState stairs = Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        helper.assertTrue(BlockConversions.convert(stairs, OVERWORLD)
                        .getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
                "Stair facing must be inherited");
        BlockState slab = Blocks.BIRCH_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
        helper.assertTrue(BlockConversions.convert(slab, OVERWORLD).getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP,
                "Slab type must be inherited");
        helper.assertTrue(WoodConversions.parse("dark_oak_planks").hint().equals(WoodHint.OAK),
                "dark_oak must be parsed as one complete wood name");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void netherWoodFamilies(GameTestHelper helper) {
        for (String wood : List.of("crimson", "warped")) {
            Map<String, String> mappings = Map.ofEntries(
                    Map.entry(wood + "_planks", "wood/planks/" + wood),
                    Map.entry(wood + "_stairs", "wood/planks/" + wood + "_stairs"),
                    Map.entry(wood + "_slab", "wood/planks/" + wood + "_slab"),
                    Map.entry(wood + "_stem", "wood/log/" + wood),
                    Map.entry(wood + "_hyphae", "wood/wood/" + wood),
                    Map.entry("stripped_" + wood + "_stem", "wood/stripped_log/" + wood),
                    Map.entry("stripped_" + wood + "_hyphae", "wood/stripped_wood/" + wood),
                    Map.entry(wood + "_fence", "wood/fence/" + wood),
                    Map.entry(wood + "_fence_gate", "wood/fence_gate/" + wood),
                    Map.entry(wood + "_door", "wood/door/" + wood),
                    Map.entry(wood + "_trapdoor", "wood/trapdoor/" + wood),
                    Map.entry(wood + "_button", "wood/button/" + wood),
                    Map.entry(wood + "_pressure_plate", "wood/pressure_plate/" + wood),
                    Map.entry(wood + "_sign", "wood/sign/" + wood),
                    Map.entry(wood + "_wall_sign", "wood/wall_sign/" + wood));
            mappings.forEach((input, output) -> assertConversion(helper, input, "tfcoverdims:" + output, NETHER));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void woodHintsUtilitiesNbtAndHangingSigns(GameTestHelper helper) {
        CompoundTag chestNbt = new CompoundTag();
        chestNbt.putString("LootTable", "minecraft:chests/simple_dungeon");
        List<StructureTemplate.StructureBlockInfo> spruce = process(helper.getLevel(), new BlockPos(0, 80, 0), List.of(
                info(0, 0, 0, Blocks.CHEST.defaultBlockState(), chestNbt),
                info(1, 0, 0, Blocks.TRAPPED_CHEST.defaultBlockState(), null),
                info(2, 0, 0, Blocks.LECTERN.defaultBlockState(), null),
                info(3, 0, 0, Blocks.CRAFTING_TABLE.defaultBlockState(), null),
                info(4, 0, 0, Blocks.SPRUCE_PLANKS.defaultBlockState(), null)));
        assertId(helper, spruce.get(0).state(), "tfc:wood/chest/spruce");
        assertId(helper, spruce.get(1).state(), "tfc:wood/trapped_chest/spruce");
        assertId(helper, spruce.get(2).state(), "tfc:wood/lectern/spruce");
        assertId(helper, spruce.get(3).state(), "tfc:wood/workbench/spruce");
        helper.assertTrue(spruce.get(0).nbt().equals(chestNbt),
                "Chest NBT must be passed through without mod-side conversion");

        List<StructureTemplate.StructureBlockInfo> fallback = process(helper.getLevel(), new BlockPos(16, 80, 0), List.of(
                info(0, 0, 0, Blocks.CHEST.defaultBlockState(), null),
                info(1, 0, 0, Blocks.LECTERN.defaultBlockState(), null)));
        assertId(helper, fallback.get(0).state(), "tfc:wood/chest/oak");
        assertId(helper, fallback.get(1).state(), "tfc:wood/lectern/oak");

        List<StructureTemplate.StructureBlockInfo> overworldCrimson = process(
                helper.getLevel(), new BlockPos(24, 80, 0), List.of(
                        info(0, 0, 0, Blocks.CHEST.defaultBlockState(), null),
                        info(1, 0, 0, Blocks.CRIMSON_PLANKS.defaultBlockState(), null)));
        assertId(helper, overworldCrimson.get(0).state(), "tfc:wood/chest/oak");

        ServerLevel nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        List<StructureTemplate.StructureBlockInfo> crimson = process(nether, new BlockPos(0, 80, 0), List.of(
                info(0, 0, 0, Blocks.CHEST.defaultBlockState(), chestNbt),
                info(1, 0, 0, Blocks.TRAPPED_CHEST.defaultBlockState(), null),
                info(2, 0, 0, Blocks.LECTERN.defaultBlockState(), null),
                info(3, 0, 0, Blocks.CRAFTING_TABLE.defaultBlockState(), null),
                info(4, 0, 0, Blocks.CRIMSON_PLANKS.defaultBlockState(), null),
                info(5, 0, 0, Blocks.CRIMSON_HANGING_SIGN.defaultBlockState(), null),
                info(6, 0, 0, Blocks.WARPED_WALL_HANGING_SIGN.defaultBlockState(), null)));
        assertId(helper, crimson.get(0).state(), "tfcoverdims:wood/chest/crimson");
        assertId(helper, crimson.get(1).state(), "tfcoverdims:wood/trapped_chest/crimson");
        assertId(helper, crimson.get(2).state(), "tfcoverdims:wood/lectern/crimson");
        assertId(helper, crimson.get(3).state(), "tfcoverdims:wood/workbench/crimson");
        assertId(helper, crimson.get(5).state(), "minecraft:crimson_hanging_sign");
        assertId(helper, crimson.get(6).state(), "minecraft:warped_wall_hanging_sign");
        helper.assertTrue(crimson.get(0).nbt().equals(chestNbt),
                "Overdims chest NBT must also be passed through unchanged");

        List<StructureTemplate.StructureBlockInfo> warped = process(nether, new BlockPos(8, 80, 0), List.of(
                info(0, 0, 0, Blocks.CHEST.defaultBlockState(), null),
                info(1, 0, 0, Blocks.TRAPPED_CHEST.defaultBlockState(), null),
                info(2, 0, 0, Blocks.LECTERN.defaultBlockState(), null),
                info(3, 0, 0, Blocks.CRAFTING_TABLE.defaultBlockState(), null),
                info(4, 0, 0, Blocks.WARPED_PLANKS.defaultBlockState(), null),
                info(5, 0, 0, Blocks.WARPED_STEM.defaultBlockState(), null),
                info(6, 0, 0, Blocks.CRIMSON_PLANKS.defaultBlockState(), null)));
        assertId(helper, warped.get(0).state(), "tfcoverdims:wood/chest/warped");
        assertId(helper, warped.get(1).state(), "tfcoverdims:wood/trapped_chest/warped");
        assertId(helper, warped.get(2).state(), "tfcoverdims:wood/lectern/warped");
        assertId(helper, warped.get(3).state(), "tfcoverdims:wood/workbench/warped");

        List<StructureTemplate.StructureBlockInfo> netherFallback = process(nether, new BlockPos(16, 80, 0), List.of(
                info(0, 0, 0, Blocks.CHEST.defaultBlockState(), null),
                info(1, 0, 0, Blocks.LECTERN.defaultBlockState(), null)));
        assertId(helper, netherFallback.get(0).state(), "tfc:wood/chest/oak");
        assertId(helper, netherFallback.get(1).state(), "tfc:wood/lectern/oak");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void furnacesBecomeFirepitsAndDiscardNbt(GameTestHelper helper) {
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("CookTime", 10);
        CompoundTag coal = new CompoundTag();
        coal.putByte("Slot", (byte) 0);
        coal.putString("id", "minecraft:coal");
        coal.putInt("count", 1);
        ListTag items = new ListTag();
        items.add(coal);
        inventory.put("Items", items);
        List<StructureTemplate.StructureBlockInfo> converted = process(helper.getLevel(), new BlockPos(32, 80, 0), List.of(
                info(0, 0, 0, Blocks.FURNACE.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), inventory),
                info(1, 0, 0, Blocks.BLAST_FURNACE.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), inventory),
                info(2, 0, 0, Blocks.SMOKER.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), inventory)));
        for (int index = 0; index < converted.size(); index++) {
            assertId(helper, converted.get(index).state(), "tfc:firepit");
            helper.assertTrue(converted.get(index).nbt() == null, "Firepit conversion must discard furnace NBT");
        }
        helper.assertTrue(converted.get(0).state().getValue(FirepitBlock.AXIS) == Direction.Axis.X,
                "East-facing furnace must produce an X-axis firepit");
        helper.assertTrue(converted.get(1).state().getValue(FirepitBlock.AXIS) == Direction.Axis.Z,
                "North-facing blast furnace must produce a Z-axis firepit");
        helper.assertTrue(converted.get(2).state().getValue(FirepitBlock.AXIS) == Direction.Axis.X,
                "West-facing smoker must produce an X-axis firepit");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void cauldronsAndBarrelStayVanilla(GameTestHelper helper) {
        CompoundTag barrelNbt = new CompoundTag();
        barrelNbt.putString("LootTable", "minecraft:chests/abandoned_mineshaft");
        BlockState water = Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 2);
        BlockState powderSnow = Blocks.POWDER_SNOW_CAULDRON.defaultBlockState()
                .setValue(BlockStateProperties.LEVEL_CAULDRON, 1);
        List<StructureTemplate.StructureBlockInfo> converted = process(helper.getLevel(), new BlockPos(48, 80, 0), List.of(
                info(0, 0, 0, Blocks.CAULDRON.defaultBlockState(), null),
                info(1, 0, 0, water, null),
                info(2, 0, 0, Blocks.LAVA_CAULDRON.defaultBlockState(), null),
                info(3, 0, 0, powderSnow, null),
                info(4, 0, 0, Blocks.BARREL.defaultBlockState(), barrelNbt)));
        for (int index = 0; index < converted.size(); index++) {
            assertId(helper, converted.get(index).state(), BuiltInRegistries.BLOCK.getKey(
                    List.of(Blocks.CAULDRON, Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON,
                            Blocks.POWDER_SNOW_CAULDRON, Blocks.BARREL).get(index)).toString());
        }
        helper.assertTrue(converted.get(1).state().getValue(BlockStateProperties.LEVEL_CAULDRON) == 2,
                "Water cauldron level must be preserved");
        helper.assertTrue(converted.get(3).state() == powderSnow,
                "Powder snow cauldron level must be preserved");
        helper.assertTrue(converted.get(4).nbt().equals(barrelNbt),
                "Barrel loot table NBT must be passed through unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void lightsConvertAndExceptionsStayVanilla(GameTestHelper helper) {
        assertConversion(helper, "torch", "tfc:torch", OVERWORLD);
        assertConversion(helper, "wall_torch", "tfc:wall_torch", OVERWORLD);
        assertConversion(helper, "candle", "tfc:candle", OVERWORLD);
        assertConversion(helper, "candle_cake", "tfc:candle_cake", OVERWORLD);
        for (String color : List.of(
                "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
                "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            assertConversion(helper, color + "_candle", "tfc:candle/" + color, OVERWORLD);
            assertConversion(helper, color + "_candle_cake", "tfc:candle_cake/" + color, OVERWORLD);
        }
        for (String path : List.of(
                "lantern", "soul_lantern", "campfire", "soul_campfire", "soul_torch", "soul_wall_torch")) {
            BlockState original = vanilla(path);
            helper.assertTrue(BlockConversions.convert(original, OVERWORLD) == original, path + " must stay vanilla");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void legacyMetalMappingsAndKeepList(GameTestHelper helper) {
        Map<String, String> mappings = Map.ofEntries(
                Map.entry("iron_bars", "tfc:metal/bars/wrought_iron"),
                Map.entry("chain", "tfc:metal/chain/wrought_iron"),
                Map.entry("iron_block", "tfc:metal/block/wrought_iron"),
                Map.entry("iron_trapdoor", "tfc:metal/trapdoor/wrought_iron"),
                Map.entry("gold_block", "tfc:metal/block/gold"),
                Map.entry("raw_gold_block", "tfc:metal/block/gold"),
                Map.entry("copper_block", "tfc:metal/block/copper"),
                Map.entry("cut_copper", "tfc:metal/block/copper"),
                Map.entry("cut_copper_slab", "tfc:metal/block/copper_slab"),
                Map.entry("cut_copper_stairs", "tfc:metal/block/copper_stairs"),
                Map.entry("exposed_copper", "tfc:metal/exposed_block/copper"),
                Map.entry("exposed_cut_copper", "tfc:metal/exposed_block/copper"),
                Map.entry("exposed_cut_copper_slab", "tfc:metal/exposed_block/copper_slab"),
                Map.entry("exposed_cut_copper_stairs", "tfc:metal/exposed_block/copper_stairs"),
                Map.entry("weathered_copper", "tfc:metal/weathered_block/copper"),
                Map.entry("weathered_cut_copper", "tfc:metal/weathered_block/copper"),
                Map.entry("weathered_cut_copper_slab", "tfc:metal/weathered_block/copper_slab"),
                Map.entry("weathered_cut_copper_stairs", "tfc:metal/weathered_block/copper_stairs"),
                Map.entry("oxidized_copper", "tfc:metal/oxidized_block/copper"),
                Map.entry("oxidized_cut_copper", "tfc:metal/oxidized_block/copper"),
                Map.entry("oxidized_cut_copper_slab", "tfc:metal/oxidized_block/copper_slab"),
                Map.entry("oxidized_cut_copper_stairs", "tfc:metal/oxidized_block/copper_stairs"),
                Map.entry("waxed_oxidized_cut_copper", "tfc:metal/oxidized_block/copper"),
                Map.entry("waxed_oxidized_cut_copper_slab", "tfc:metal/oxidized_block/copper_slab"),
                Map.entry("waxed_oxidized_cut_copper_stairs", "tfc:metal/oxidized_block/copper_stairs"));
        mappings.forEach((input, output) -> assertConversion(helper, input, output, OVERWORLD));
        assertConversion(helper, "coal_block", "tfc:bituminous_coal", NETHER);
        helper.assertTrue(BlockConversions.convert(Blocks.COAL_BLOCK.defaultBlockState(), OVERWORLD)
                == Blocks.COAL_BLOCK.defaultBlockState(), "Coal block must only convert in the Nether");

        BlockState copperStairs = Blocks.EXPOSED_CUT_COPPER_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        helper.assertTrue(BlockConversions.convert(copperStairs, OVERWORLD)
                        .getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.SOUTH,
                "Copper stair facing must be inherited");
        BlockState copperSlab = Blocks.WEATHERED_CUT_COPPER_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
        helper.assertTrue(BlockConversions.convert(copperSlab, OVERWORLD)
                        .getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP,
                "Copper slab type must be inherited");

        for (String path : List.of(
                "anvil", "chipped_anvil", "damaged_anvil", "smithing_table", "stonecutter", "spawner", "rail",
                "white_bed")) {
            BlockState original = vanilla(path);
            helper.assertTrue(BlockConversions.convert(original, OVERWORLD) == original, path + " must stay vanilla");
        }
        helper.succeed();
    }

    private static String vanillaWoodPath(String wood, String shape) {
        return switch (shape) {
            case "stripped_log" -> "stripped_" + wood + "_log";
            case "stripped_wood" -> "stripped_" + wood + "_wood";
            default -> wood + "_" + shape;
        };
    }

    private static List<StructureTemplate.StructureBlockInfo> process(
            ServerLevel level,
            BlockPos origin,
            List<StructureTemplate.StructureBlockInfo> blocks) {
        return StructureTemplate.processBlockInfos(level, origin, origin,
                new StructurePlaceSettings().addProcessor(TfcBlockReplacementProcessor.INSTANCE), blocks, null);
    }

    private static StructureTemplate.StructureBlockInfo info(
            int x, int y, int z, BlockState state, CompoundTag nbt) {
        return new StructureTemplate.StructureBlockInfo(new BlockPos(x, y, z), state, nbt);
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
        helper.assertTrue(actual.equals(ResourceLocation.parse(expected)), "Expected " + expected + " but got " + actual);
    }

    private static BlockState vanilla(String path) {
        return block("minecraft:" + path).defaultBlockState();
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElseThrow();
    }

    private static boolean containsBlock(String id) {
        return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(id));
    }
}
