package net.claustra01.yungsbettertfc.gametest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.claustra01.yungsbettertfc.world.conversion.BlockConversions;
import net.claustra01.yungsbettertfc.world.conversion.ConversionContext;
import net.claustra01.yungsbettertfc.world.conversion.DimensionKind;
import net.claustra01.yungsbettertfc.world.conversion.WoodHint;
import net.claustra01.yungsbettertfc.world.processor.TfcBlockReplacementProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(YungsBetterTfc.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M04GameTests {
    private static final String BATCH = "bootstrap";
    private static final ConversionContext RHYOLITE =
            new ConversionContext(DimensionKind.OVERWORLD, "rhyolite", "mollisol", WoodHint.OAK, false);
    private static final ConversionContext UNKNOWN_ROCK =
            new ConversionContext(DimensionKind.OVERWORLD, "missing_rock", "mollisol", WoodHint.OAK, false);

    private M04GameTests() {}

    @GameTest(template = "empty", batch = BATCH)
    public static void aquaticPlantsAndCactus(GameTestHelper helper) {
        List<StructureTemplate.StructureBlockInfo> converted = process(helper.getLevel(), new BlockPos(80, 80, 0), List.of(
                info(0, 0, 0, Blocks.KELP.defaultBlockState(), null),
                info(1, 0, 0, Blocks.KELP_PLANT.defaultBlockState(), null),
                info(2, 0, 0, Blocks.SEAGRASS.defaultBlockState(), null),
                info(3, 0, 0, Blocks.TALL_SEAGRASS.defaultBlockState()
                        .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), null),
                info(3, 1, 0, Blocks.TALL_SEAGRASS.defaultBlockState()
                        .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), null),
                info(4, 0, 0, Blocks.SEA_PICKLE.defaultBlockState(), null),
                info(5, 0, 0, Blocks.VINE.defaultBlockState(), null),
                info(6, 0, 0, Blocks.CACTUS.defaultBlockState(), null),
                info(6, 1, 0, Blocks.CACTUS.defaultBlockState(), null),
                info(6, 2, 0, Blocks.CACTUS.defaultBlockState(), null),
                info(7, 0, 0, Blocks.DEAD_BUSH.defaultBlockState(), null)));

        List<String> expected = List.of(
                "minecraft:water",
                "minecraft:water",
                "tfc:plant/eel_grass",
                "tfc:plant/eel_grass",
                "minecraft:water",
                "tfc:sea_pickle",
                "tfc:plant/ivy",
                "tfc:plant/silken_pincushion_cactus",
                "minecraft:air",
                "minecraft:air",
                "tfc:plant/dead_bush");
        for (int index = 0; index < expected.size(); index++) {
            assertId(helper, converted.get(index).state(), expected.get(index));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void untargetedPlantsAndRegisteredPots(GameTestHelper helper) {
        List<Block> inputs = List.of(
                Blocks.SHORT_GRASS,
                Blocks.FERN,
                Blocks.BROWN_MUSHROOM,
                Blocks.POPPY,
                Blocks.OAK_LEAVES,
                Blocks.OAK_SAPLING,
                Blocks.POTTED_POPPY,
                Blocks.POTTED_BAMBOO);
        List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>();
        for (int index = 0; index < inputs.size(); index++) {
            blocks.add(info(index, 0, 0, inputs.get(index).defaultBlockState(), null));
        }
        List<StructureTemplate.StructureBlockInfo> converted = process(
                helper.getLevel(), new BlockPos(88, 80, 0), blocks);
        for (int index = 0; index < 6; index++) {
            helper.assertTrue(converted.get(index).state() == inputs.get(index).defaultBlockState(),
                    BuiltInRegistries.BLOCK.getKey(inputs.get(index)) + " must remain unchanged");
        }
        assertId(helper, converted.get(6).state(), "tfc:plant/potted/poppy");
        helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("tfc:plant/potted/bamboo")),
                "The missing-output test requires the TFC potted bamboo ID to be absent");
        helper.assertTrue(converted.get(7).state() == Blocks.POTTED_BAMBOO.defaultBlockState(),
                "A potted plant without a registered TFC output must remain vanilla");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void generalOresAndGraniteFallback(GameTestHelper helper) {
        Map<String, String> ores = new LinkedHashMap<>();
        ores.put("coal_ore", "tfc:lignite");
        ores.put("iron_ore", "tfc:ore/normal_hematite/rhyolite");
        ores.put("copper_ore", "tfc:ore/normal_native_copper/rhyolite");
        ores.put("gold_ore", "tfc:ore/normal_native_gold/rhyolite");
        ores.put("lapis_ore", "tfc:ore/lapis_lazuli/rhyolite");
        ores.put("diamond_ore", "tfc:ore/diamond/rhyolite");
        ores.put("emerald_ore", "tfc:ore/emerald/rhyolite");
        ores.put("redstone_ore", "tfc:ore/cinnabar/rhyolite");

        BlockPos origin = new BlockPos(104, 80, 0);
        helper.getLevel().setBlock(origin.below(), block("tfc:rock/raw/rhyolite").defaultBlockState(), 3);
        List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>();
        for (String input : ores.keySet()) {
            blocks.add(info(blocks.size(), 0, 0, vanilla(input), null));
            blocks.add(info(blocks.size(), 0, 0, vanilla("deepslate_" + input), null));
        }
        List<StructureTemplate.StructureBlockInfo> converted = process(helper.getLevel(), origin, blocks);
        int index = 0;
        for (Map.Entry<String, String> entry : ores.entrySet()) {
            String input = entry.getKey();
            String expected = entry.getValue();
            assertId(helper, converted.get(index++).state(), expected);
            assertId(helper, converted.get(index++).state(), expected);
            if (!input.equals("coal_ore")) {
                assertId(helper, BlockConversions.convert(vanilla(input), UNKNOWN_ROCK),
                        expected.substring(0, expected.lastIndexOf('/') + 1) + "granite");
                assertId(helper, BlockConversions.convert(vanilla("deepslate_" + input), UNKNOWN_ROCK),
                        expected.substring(0, expected.lastIndexOf('/') + 1) + "granite");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void finalStateOreRewriting(GameTestHelper helper) {
        CompoundTag redstone = new CompoundTag();
        redstone.putString("final_state", "minecraft:redstone_ore[lit=true]");
        CompoundTag netherGold = new CompoundTag();
        netherGold.putString("final_state", "minecraft:nether_gold_ore");
        CompoundTag diamond = new CompoundTag();
        diamond.putString("final_state", "minecraft:deepslate_diamond_ore");
        CompoundTag ironWithNbt = new CompoundTag();
        ironWithNbt.putString("final_state", "minecraft:iron_ore{foo:1}");
        CompoundTag invalid = new CompoundTag();
        invalid.putString("final_state", "minecraft:iron_ore[lit=true]");
        List<StructureTemplate.StructureBlockInfo> converted = process(helper.getLevel(), new BlockPos(96, 80, 0), List.of(
                info(0, 0, 0, Blocks.JIGSAW.defaultBlockState(), redstone),
                info(1, 0, 0, Blocks.STRUCTURE_BLOCK.defaultBlockState(), netherGold),
                info(2, 0, 0, Blocks.JIGSAW.defaultBlockState(), diamond),
                info(3, 0, 0, Blocks.JIGSAW.defaultBlockState(), ironWithNbt),
                info(4, 0, 0, Blocks.JIGSAW.defaultBlockState(), invalid)));

        helper.assertTrue(converted.get(0).nbt().getString("final_state")
                        .equals("tfc:ore/cinnabar/granite"),
                "Unsupported ore properties must not be copied into final_state");
        try {
            helper.assertTrue(BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(),
                            converted.get(0).nbt().getString("final_state"), true).blockState()
                            .is(block("tfc:ore/cinnabar/granite")),
                    "Rewritten final_state must be parseable by Minecraft");
            BlockStateParser.BlockResult withNbt = BlockStateParser.parseForBlock(
                    BuiltInRegistries.BLOCK.asLookup(), converted.get(3).nbt().getString("final_state"), true);
            helper.assertTrue(withNbt.blockState().is(block("tfc:ore/normal_hematite/granite"))
                            && withNbt.nbt() != null && withNbt.nbt().getInt("foo") == 1,
                    "Rewritten final_state must preserve trailing NBT");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new AssertionError("Rewritten final_state must be parseable by Minecraft", exception);
        }
        helper.assertTrue(converted.get(1).nbt().equals(netherGold),
                "Nether-native ore final_state must remain unchanged");
        helper.assertTrue(converted.get(2).nbt().getString("final_state")
                        .equals("tfc:ore/diamond/granite"),
                "Deepslate ore final_state must use the same TFC ore mapping");
        helper.assertTrue(converted.get(4).nbt().equals(invalid),
                "Invalid final_state must remain unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void netherOresRemain(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        ConversionContext netherContext =
                new ConversionContext(DimensionKind.NETHER, "basalt", "oxisol", WoodHint.OAK, false);
        for (Block block : List.of(Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS)) {
            BlockState original = block.defaultBlockState();
            helper.assertTrue(BlockConversions.convert(original, netherContext) == original,
                    BuiltInRegistries.BLOCK.getKey(block) + " must remain unchanged");
        }

        CompoundTag quartz = new CompoundTag();
        quartz.putString("final_state", "minecraft:nether_quartz_ore");
        List<StructureTemplate.StructureBlockInfo> converted = process(nether, new BlockPos(0, 80, 32), List.of(
                info(0, 0, 0, Blocks.NETHER_GOLD_ORE.defaultBlockState(), null),
                info(1, 0, 0, Blocks.NETHER_QUARTZ_ORE.defaultBlockState(), null),
                info(2, 0, 0, Blocks.ANCIENT_DEBRIS.defaultBlockState(), null),
                info(3, 0, 0, Blocks.JIGSAW.defaultBlockState(), quartz)));
        assertId(helper, converted.get(0).state(), "minecraft:nether_gold_ore");
        assertId(helper, converted.get(1).state(), "minecraft:nether_quartz_ore");
        assertId(helper, converted.get(2).state(), "minecraft:ancient_debris");
        helper.assertTrue(converted.get(3).nbt().equals(quartz),
                "Nether quartz in final_state must remain unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void displayEntityEquipmentAndEndExclusion(GameTestHelper helper) {
        List<String> inputs = List.of(
                "iron_sword", "iron_axe", "iron_pickaxe", "iron_shovel", "iron_hoe",
                "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots", "shield",
                "bow", "crossbow", "trident", "mace");
        List<String> expected = List.of(
                "metal/sword/wrought_iron", "metal/axe/wrought_iron", "metal/pickaxe/wrought_iron",
                "metal/shovel/wrought_iron", "metal/hoe/wrought_iron", "metal/helmet/wrought_iron",
                "metal/chestplate/wrought_iron", "metal/greaves/wrought_iron", "metal/boots/wrought_iron",
                "metal/shield/wrought_iron", "metal/javelin/wrought_iron", "metal/javelin/wrought_iron",
                "metal/javelin/wrought_iron", "metal/mace/wrought_iron");

        CompoundTag itemFrame = entity("minecraft:item_frame");
        itemFrame.put("Item", stack(inputs.getFirst()));
        CompoundTag glowItemFrame = entity("minecraft:glow_item_frame");
        glowItemFrame.put("Item", stack(inputs.get(1)));
        List<StructureTemplate.StructureEntityInfo> source = new ArrayList<>();
        source.add(entityInfo(0, itemFrame));
        source.add(entityInfo(1, glowItemFrame));
        List<Integer> slots = new ArrayList<>();
        for (int index = 2; index < inputs.size(); index++) {
            CompoundTag armorStand = entity("minecraft:armor_stand");
            ListTag armorItems = new ListTag();
            ListTag handItems = new ListTag();
            for (int slot = 0; slot < 4; slot++) {
                armorItems.add(new CompoundTag());
            }
            for (int slot = 0; slot < 2; slot++) {
                handItems.add(new CompoundTag());
            }
            int slot = switch (inputs.get(index)) {
                case "iron_boots" -> 0;
                case "iron_leggings" -> 1;
                case "iron_chestplate" -> 2;
                case "iron_helmet" -> 3;
                default -> -1;
            };
            if (slot >= 0) {
                armorItems.set(slot, stack(inputs.get(index)));
            } else {
                handItems.set(0, stack(inputs.get(index)));
            }
            slots.add(slot);
            armorStand.put("ArmorItems", armorItems);
            armorStand.put("HandItems", handItems);
            source.add(entityInfo(index, armorStand));
        }
        List<StructureTemplate.StructureEntityInfo> converted = processEntities(helper.getLevel(), source);
        List<CompoundTag> stacks = new ArrayList<>();
        stacks.add(converted.get(0).nbt.getCompound("Item"));
        stacks.add(converted.get(1).nbt.getCompound("Item"));
        for (int index = 2; index < inputs.size(); index++) {
            int slot = slots.get(index - 2);
            stacks.add(converted.get(index).nbt.getList(
                    slot >= 0 ? "ArmorItems" : "HandItems", CompoundTag.TAG_COMPOUND)
                    .getCompound(slot >= 0 ? slot : 0));
        }
        for (int index = 0; index < expected.size(); index++) {
            CompoundTag stack = stacks.get(index);
            helper.assertTrue(stack.getString("id").equals("tfc:" + expected.get(index)),
                    inputs.get(index) + " must map to " + expected.get(index));
            for (String incompatible : List.of("components", "tag", "Damage", "damage")) {
                helper.assertTrue(!stack.contains(incompatible), incompatible + " must be removed after replacement");
            }
        }

        CompoundTag painting = entity("minecraft:painting");
        painting.put("Item", stack("iron_sword"));
        StructureTemplate.StructureEntityInfo unchangedPainting = processEntities(
                helper.getLevel(), List.of(entityInfo(3, painting))).getFirst();
        helper.assertTrue(unchangedPainting.nbt.equals(painting), "Untargeted entities must remain unchanged");

        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        StructureTemplate.StructureEntityInfo netherResult = processEntities(
                nether, List.of(entityInfo(4, itemFrame))).getFirst();
        helper.assertTrue(netherResult.nbt.getCompound("Item").getString("id")
                        .equals("tfc:metal/sword/wrought_iron"),
                "Display equipment must also convert in the Nether");

        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        List<StructureTemplate.StructureEntityInfo> endResult = processEntities(end, source);
        helper.assertTrue(endResult.get(0).nbt.equals(itemFrame), "Item frames must remain unchanged in the End");
        helper.assertTrue(endResult.get(1).nbt.equals(glowItemFrame),
                "Glow item frames must remain unchanged in the End");
        for (int index = 2; index < source.size(); index++) {
            helper.assertTrue(endResult.get(index).nbt.equals(source.get(index).nbt),
                    "Armor stands must remain unchanged in the End");
        }
        helper.succeed();
    }

    private static List<StructureTemplate.StructureBlockInfo> process(
            ServerLevel level,
            BlockPos origin,
            List<StructureTemplate.StructureBlockInfo> blocks) {
        return StructureTemplate.processBlockInfos(level, origin, origin,
                new StructurePlaceSettings().addProcessor(TfcBlockReplacementProcessor.INSTANCE), blocks, null);
    }

    private static List<StructureTemplate.StructureEntityInfo> processEntities(
            ServerLevel level,
            List<StructureTemplate.StructureEntityInfo> entities) {
        return StructureTemplate.processEntityInfos(null, level, BlockPos.ZERO,
                new StructurePlaceSettings().addProcessor(TfcBlockReplacementProcessor.INSTANCE), entities);
    }

    private static StructureTemplate.StructureBlockInfo info(
            int x, int y, int z, BlockState state, CompoundTag nbt) {
        return new StructureTemplate.StructureBlockInfo(new BlockPos(x, y, z), state, nbt);
    }

    private static StructureTemplate.StructureEntityInfo entityInfo(int x, CompoundTag nbt) {
        return new StructureTemplate.StructureEntityInfo(new Vec3(x, 0, 0), new BlockPos(x, 0, 0), nbt);
    }

    private static CompoundTag entity(String id) {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", id);
        return entity;
    }

    private static CompoundTag stack(String item) {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "minecraft:" + item);
        stack.putInt("count", 1);
        stack.put("components", new CompoundTag());
        stack.put("tag", new CompoundTag());
        stack.putInt("Damage", 4);
        stack.putInt("damage", 4);
        return stack;
    }

    private static BlockState vanilla(String path) {
        return block("minecraft:" + path).defaultBlockState();
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElseThrow();
    }

    private static void assertId(GameTestHelper helper, BlockState state, String expected) {
        ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        helper.assertTrue(actual.equals(ResourceLocation.parse(expected)), "Expected " + expected + " but got " + actual);
    }
}
