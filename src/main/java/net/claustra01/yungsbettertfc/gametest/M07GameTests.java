package net.claustra01.yungsbettertfc.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.claustra01.yungsbettertfc.world.processor.TfcBlockReplacementProcessor;
import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(YungsBetterTfc.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M07GameTests {
    private static final String BATCH = "bootstrap";
    private static final ResourceKey<LootTable> ABANDONED_MINESHAFT = ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.parse("minecraft:chests/abandoned_mineshaft"));

    private M07GameTests() {}

    @GameTest(template = "empty", batch = BATCH)
    public static void visualRegressionStatesAreLegal(GameTestHelper helper) {
        List<StructureTemplate.StructureBlockInfo> overworld = process(
                helper.getLevel(), helper.absolutePos(new BlockPos(112, 8, 0)), List.of(
                        info(0, 0, 0, Blocks.STONE_BRICKS.defaultBlockState()),
                        info(1, 0, 0, Blocks.SPRUCE_PLANKS.defaultBlockState()),
                        info(2, 0, 0, Blocks.SPRUCE_STAIRS.defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)),
                        info(3, 0, 0, Blocks.SPRUCE_DOOR.defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)),
                        info(3, 1, 0, Blocks.SPRUCE_DOOR.defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)),
                        info(4, 0, 0, Blocks.SPRUCE_SIGN.defaultBlockState()
                                .setValue(BlockStateProperties.ROTATION_16, 8)),
                        info(5, 0, 0, Blocks.FURNACE.defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)),
                        info(6, 0, 0, Blocks.BARREL.defaultBlockState()
                                .setValue(BlockStateProperties.FACING, Direction.SOUTH)),
                        info(7, 0, 0, Blocks.WATER_CAULDRON.defaultBlockState()
                                .setValue(BlockStateProperties.LEVEL_CAULDRON, 2)),
                        info(8, 0, 0, Blocks.CACTUS.defaultBlockState()),
                        info(8, 1, 0, Blocks.CACTUS.defaultBlockState()),
                        info(9, 0, 0, Blocks.TALL_SEAGRASS.defaultBlockState()),
                        info(9, 1, 0, Blocks.TALL_SEAGRASS.defaultBlockState()
                                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)),
                        info(10, 0, 0, Blocks.LANTERN.defaultBlockState())));

        assertBlock(helper, overworld.get(0).state(), "tfc:rock/bricks/granite");
        assertBlock(helper, overworld.get(1).state(), "tfc:wood/planks/spruce");
        assertBlock(helper, overworld.get(2).state(), "tfc:wood/planks/spruce_stairs");
        assertProperty(helper, overworld.get(2).state(), BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        assertBlock(helper, overworld.get(3).state(), "tfc:wood/door/spruce");
        assertProperty(helper, overworld.get(3).state(), BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        assertProperty(helper, overworld.get(4).state(), BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        assertProperty(helper, overworld.get(4).state(), BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        assertBlock(helper, overworld.get(5).state(), "tfc:wood/sign/spruce");
        assertProperty(helper, overworld.get(5).state(), BlockStateProperties.ROTATION_16, 8);
        assertBlock(helper, overworld.get(6).state(), "tfc:firepit");
        assertProperty(helper, overworld.get(6).state(), FirepitBlock.AXIS, Direction.Axis.X);
        assertBlock(helper, overworld.get(7).state(), "minecraft:barrel");
        assertProperty(helper, overworld.get(7).state(), BlockStateProperties.FACING, Direction.SOUTH);
        assertBlock(helper, overworld.get(8).state(), "minecraft:water_cauldron");
        assertProperty(helper, overworld.get(8).state(), BlockStateProperties.LEVEL_CAULDRON, 2);
        assertBlock(helper, overworld.get(9).state(), "tfc:plant/silken_pincushion_cactus");
        assertBlock(helper, overworld.get(10).state(), "minecraft:air");
        assertBlock(helper, overworld.get(11).state(), "tfc:plant/eel_grass");
        assertBlock(helper, overworld.get(12).state(), "minecraft:water");
        assertBlock(helper, overworld.get(13).state(), "minecraft:lantern");

        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        List<StructureTemplate.StructureBlockInfo> netherBlocks = process(
                nether, new BlockPos(0, 80, 112), List.of(
                        info(0, 0, 0, Blocks.CRIMSON_PLANKS.defaultBlockState()),
                        info(1, 0, 0, Blocks.CRIMSON_STAIRS.defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)),
                        info(2, 0, 0, Blocks.CRIMSON_DOOR.defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)),
                        info(2, 1, 0, Blocks.CRIMSON_DOOR.defaultBlockState()
                                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)),
                        info(3, 0, 0, Blocks.CRIMSON_SIGN.defaultBlockState()
                                .setValue(BlockStateProperties.ROTATION_16, 4)),
                        info(4, 0, 0, Blocks.BLACKSTONE.defaultBlockState()),
                        info(5, 0, 0, Blocks.LAVA_CAULDRON.defaultBlockState())));
        assertBlock(helper, netherBlocks.get(0).state(), "tfcoverdims:wood/planks/crimson");
        assertBlock(helper, netherBlocks.get(1).state(), "tfcoverdims:wood/planks/crimson_stairs");
        assertProperty(helper, netherBlocks.get(1).state(), BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        assertBlock(helper, netherBlocks.get(2).state(), "tfcoverdims:wood/door/crimson");
        assertProperty(helper, netherBlocks.get(2).state(), BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        assertProperty(helper, netherBlocks.get(3).state(), BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        assertBlock(helper, netherBlocks.get(4).state(), "tfcoverdims:wood/sign/crimson");
        assertProperty(helper, netherBlocks.get(4).state(), BlockStateProperties.ROTATION_16, 4);
        assertBlock(helper, netherBlocks.get(5).state(), "minecraft:blackstone");
        assertBlock(helper, netherBlocks.get(6).state(), "minecraft:lava_cauldron");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void convertedStatesAndBlockEntitiesReload(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        List<StructureTemplate.StructureBlockInfo> overworldBlocks = process(
                overworld, helper.absolutePos(new BlockPos(128, 8, 0)), persistentPalette(false));
        place(overworld, overworldBlocks);
        setBarrelLoot(overworld, overworldBlocks.get(2).pos());

        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        List<StructureTemplate.StructureBlockInfo> netherBlocks = process(
                nether, new BlockPos(0, 80, 128), persistentPalette(true));
        place(nether, netherBlocks);
        setBarrelLoot(nether, netherBlocks.get(2).pos());

        reload(overworld, overworldBlocks);
        reload(nether, netherBlocks);

        assertReloadedPalette(helper, overworld, overworldBlocks, "tfc", "spruce",
                Direction.WEST, Direction.Axis.X, "minecraft:water_cauldron", 2);
        assertReloadedPalette(helper, nether, netherBlocks, "tfcoverdims", "crimson",
                Direction.NORTH, Direction.Axis.Z, "minecraft:powder_snow_cauldron", 1);
        helper.succeed();
    }

    private static List<StructureTemplate.StructureBlockInfo> persistentPalette(boolean nether) {
        BlockState planks = nether ? Blocks.CRIMSON_PLANKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState door = (nether ? Blocks.CRIMSON_DOOR : Blocks.SPRUCE_DOOR).defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, nether ? Direction.NORTH : Direction.WEST);
        BlockState furnace = (nether ? Blocks.SMOKER : Blocks.FURNACE).defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, nether ? Direction.NORTH : Direction.EAST);
        BlockState cauldron = (nether ? Blocks.POWDER_SNOW_CAULDRON : Blocks.WATER_CAULDRON).defaultBlockState()
                .setValue(BlockStateProperties.LEVEL_CAULDRON, nether ? 1 : 2);
        return List.of(
                info(0, 0, 0, planks),
                info(1, 0, 0, Blocks.CHEST.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)),
                info(2, 0, 0, Blocks.BARREL.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST)),
                info(3, 0, 0, furnace),
                info(4, 0, 0, door),
                info(4, 1, 0, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)),
                info(5, 0, 0, cauldron));
    }

    private static void place(ServerLevel level, List<StructureTemplate.StructureBlockInfo> blocks) {
        Set<BlockPos> positions = new HashSet<>();
        for (StructureTemplate.StructureBlockInfo block : blocks) {
            positions.add(block.pos());
        }
        for (StructureTemplate.StructureBlockInfo block : blocks) {
            if (!positions.contains(block.pos().below())) {
                level.setBlock(block.pos().below(), Blocks.STONE.defaultBlockState(), 2);
            }
        }
        for (StructureTemplate.StructureBlockInfo block : blocks) {
            level.setBlock(block.pos(), block.state(), 2);
        }
    }

    private static void setBarrelLoot(ServerLevel level, BlockPos pos) {
        RandomizableContainerBlockEntity barrel = (RandomizableContainerBlockEntity) level.getBlockEntity(pos);
        barrel.setLootTable(ABANDONED_MINESHAFT);
        barrel.setLootTableSeed(42L);
        barrel.setChanged();
    }

    private static void reload(ServerLevel level, List<StructureTemplate.StructureBlockInfo> blocks) {
        List<SavedBlock> saved = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo block : blocks) {
            BlockEntity blockEntity = level.getBlockEntity(block.pos());
            saved.add(new SavedBlock(block.pos(), NbtUtils.writeBlockState(level.getBlockState(block.pos())),
                    blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess())));
        }
        for (SavedBlock block : saved) {
            level.setBlock(block.pos(), Blocks.AIR.defaultBlockState(), 2);
        }
        for (SavedBlock block : saved) {
            BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), block.state());
            level.setBlock(block.pos(), state, 2);
            if (block.blockEntity() != null) {
                BlockEntity loaded = BlockEntity.loadStatic(
                        block.pos(), state, block.blockEntity(), level.registryAccess());
                if (loaded == null) {
                    throw new IllegalStateException("Unable to reload Block Entity at " + block.pos());
                }
                level.setBlockEntity(loaded);
            }
        }
    }

    private static void assertReloadedPalette(
            GameTestHelper helper,
            ServerLevel level,
            List<StructureTemplate.StructureBlockInfo> blocks,
            String woodNamespace,
            String wood,
            Direction doorFacing,
            Direction.Axis firepitAxis,
            String cauldronId,
            int cauldronLevel) {
        assertBlock(helper, level.getBlockState(blocks.get(0).pos()), woodNamespace + ":wood/planks/" + wood);
        assertBlock(helper, level.getBlockState(blocks.get(1).pos()), woodNamespace + ":wood/chest/" + wood);
        assertProperty(helper, level.getBlockState(blocks.get(1).pos()),
                BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        helper.assertTrue(level.getBlockEntity(blocks.get(1).pos()) != null,
                "The converted chest Block Entity must reload without crashing");
        assertBlock(helper, level.getBlockState(blocks.get(2).pos()), "minecraft:barrel");
        assertProperty(helper, level.getBlockState(blocks.get(2).pos()),
                BlockStateProperties.FACING, Direction.EAST);
        helper.assertTrue(level.getBlockEntity(blocks.get(2).pos()) instanceof RandomizableContainerBlockEntity barrel
                        && ABANDONED_MINESHAFT.equals(barrel.getLootTable())
                        && barrel.getLootTableSeed() == 42L,
                "The barrel loot table and seed must survive reload");
        assertBlock(helper, level.getBlockState(blocks.get(3).pos()), "tfc:firepit");
        assertProperty(helper, level.getBlockState(blocks.get(3).pos()), FirepitBlock.AXIS, firepitAxis);
        helper.assertTrue(level.getBlockEntity(blocks.get(3).pos()) != null,
                "The firepit Block Entity must reload without crashing");
        assertBlock(helper, level.getBlockState(blocks.get(4).pos()), woodNamespace + ":wood/door/" + wood);
        assertBlock(helper, level.getBlockState(blocks.get(5).pos()), woodNamespace + ":wood/door/" + wood);
        assertProperty(helper, level.getBlockState(blocks.get(4).pos()),
                BlockStateProperties.HORIZONTAL_FACING, doorFacing);
        assertProperty(helper, level.getBlockState(blocks.get(4).pos()),
                BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        assertProperty(helper, level.getBlockState(blocks.get(5).pos()),
                BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        assertBlock(helper, level.getBlockState(blocks.get(6).pos()), cauldronId);
        assertProperty(helper, level.getBlockState(blocks.get(6).pos()),
                BlockStateProperties.LEVEL_CAULDRON, cauldronLevel);
    }

    private static List<StructureTemplate.StructureBlockInfo> process(
            ServerLevel level,
            BlockPos origin,
            List<StructureTemplate.StructureBlockInfo> blocks) {
        return StructureTemplate.processBlockInfos(level, origin, origin,
                new StructurePlaceSettings().addProcessor(TfcBlockReplacementProcessor.INSTANCE), blocks, null);
    }

    private static StructureTemplate.StructureBlockInfo info(int x, int y, int z, BlockState state) {
        return new StructureTemplate.StructureBlockInfo(new BlockPos(x, y, z), state, null);
    }

    private static void assertBlock(GameTestHelper helper, BlockState state, String expected) {
        ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        helper.assertTrue(actual.equals(ResourceLocation.parse(expected)), "Expected " + expected + " but got " + actual);
    }

    private static <T extends Comparable<T>> void assertProperty(
            GameTestHelper helper,
            BlockState state,
            net.minecraft.world.level.block.state.properties.Property<T> property,
            T expected) {
        helper.assertTrue(state.hasProperty(property), state + " must contain property " + property.getName());
        helper.assertTrue(state.getValue(property).equals(expected),
                property.getName() + " must be " + expected + " on " + state);
    }

    private record SavedBlock(BlockPos pos, CompoundTag state, CompoundTag blockEntity) {}
}
