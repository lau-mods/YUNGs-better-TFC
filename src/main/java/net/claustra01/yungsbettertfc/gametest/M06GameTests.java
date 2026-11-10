package net.claustra01.yungsbettertfc.gametest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.claustra01.yungsbettertfc.access.ConvertedStructurePiece;
import net.claustra01.yungsbettertfc.access.StructureTemplateIdAccess;
import net.claustra01.yungsbettertfc.world.conversion.DirectPlacementContext;
import net.claustra01.yungsbettertfc.world.processor.TfcBlockReplacementProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

@GameTestHolder(YungsBetterTfc.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M06GameTests {
    private static final String BATCH = "bootstrap";
    private static final Map<ResourceLocation, ResourceLocation> ADDED_BIOMES = addedBiomes();

    private M06GameTests() {}

    @GameTest(template = "empty", batch = BATCH)
    public static void biomeTagsExtendYungTags(GameTestHelper helper) {
        IModFile modFile = ModList.get().getModFileById(YungsBetterTfc.MOD_ID).getFile();
        long overrideCount = 0;
        try {
            for (String namespace : List.of(
                    "betterdungeons", "betterstrongholds", "betteroceanmonuments", "bettermineshafts")) {
                Path directory = modFile.findResource(
                        "data", namespace, "tags", "worldgen", "biome", "has_structure");
                try (var files = Files.list(directory)) {
                    overrideCount += files.filter(path -> path.toString().endsWith(".json")).count();
                }
            }
            for (ResourceLocation tag : ADDED_BIOMES.keySet()) {
                Path path = modFile.findResource(
                        "data", tag.getNamespace(), "tags", "worldgen", "biome", tag.getPath() + ".json");
                helper.assertTrue(Files.exists(path), tag + " must be provided by this mod");
                JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                helper.assertTrue(!json.get("replace").getAsBoolean(), tag + " must use replace:false");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect biome tag resources", exception);
        }
        helper.assertTrue(overrideCount == 12, "Exactly 12 YUNG biome tags must be extended");

        ADDED_BIOMES.forEach((tag, biome) -> assertBiomeInTag(helper, biome, tag));
        for (ResourceLocation tag : ADDED_BIOMES.keySet()) {
            if (!ModList.get().isLoaded(tag.getNamespace())) {
                continue;
            }
            String name = tag.getPath().substring("has_structure/".length());
            String original = switch (name) {
                case "better_ocean_monument" -> "deep_ocean";
                case "better_mineshaft_desert" -> "desert";
                case "better_mineshaft_ice" -> "ice_spikes";
                case "better_mineshaft_mesa", "better_mineshaft_red_desert" -> "badlands";
                case "better_mineshaft_spruce_snowy" -> "snowy_taiga";
                default -> "plains";
            };
            assertBiomeInTag(helper, ResourceLocation.parse("minecraft:" + original), tag);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void targetTemplatesAppendProcessorOnce(GameTestHelper helper) {
        ResourceLocation loadedId = ResourceLocation.parse("yungsbettertfc:empty");
        StructureTemplate loaded = helper.getLevel().getStructureManager().getOrCreate(loadedId);
        helper.assertTrue(((StructureTemplateIdAccess) (Object) loaded).yungsbettertfc$getTemplateId().equals(loadedId),
                "StructureTemplateManager must attach the requested template ID");
        for (String namespace : List.of(
                "betterstrongholds", "betterdungeons", "betteroceanmonuments", "betterfortresses")) {
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
            StructureTemplate template = identifiedTemplate(namespace + ":test/room");
            invokePlacement(template, helper.getLevel(), settings);
            invokePlacement(template, helper.getLevel(), settings);
            helper.assertTrue(settings.getProcessors().size() == 2,
                    namespace + " must append exactly one processor");
            helper.assertTrue(settings.getProcessors().get(0) == BlockIgnoreProcessor.STRUCTURE_BLOCK,
                    namespace + " must preserve the existing processor order");
            helper.assertTrue(settings.getProcessors().get(1) instanceof TfcBlockReplacementProcessor,
                    namespace + " must append this mod's processor last");
        }

        for (String id : List.of("beneath:test", "minecraft:end_city/ship", "minecraft:village/plains", "test:room")) {
            StructurePlaceSettings settings = new StructurePlaceSettings();
            invokePlacement(identifiedTemplate(id), helper.getLevel(), settings);
            helper.assertTrue(settings.getProcessors().isEmpty(), id + " must not receive the processor");
        }
        StructurePlaceSettings unknown = new StructurePlaceSettings();
        new StructureTemplate().placeInWorld(
                helper.getLevel(), BlockPos.ZERO, BlockPos.ZERO, unknown, helper.getLevel().getRandom(), 2);
        helper.assertTrue(unknown.getProcessors().isEmpty(), "A template with no known ID must not be targeted");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void overworldTemplateUsesLocalGeology(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(32, 8, 0));
        helper.getLevel().setBlock(origin.below(), state("tfc:rock/raw/rhyolite"), 2);
        helper.getLevel().setBlock(origin.below(2), state("tfc:dirt/alfisol"), 2);
        StructurePlaceSettings settings = injectedSettings(helper.getLevel(), "betterstrongholds:test", origin);
        List<StructureTemplate.StructureBlockInfo> result = process(helper.getLevel(), origin, settings, List.of(
                info(BlockPos.ZERO, Blocks.STONE.defaultBlockState()),
                info(BlockPos.ZERO.east(), Blocks.SPRUCE_PLANKS.defaultBlockState())));
        assertBlock(helper, result.get(0).state(), "tfc:rock/raw/rhyolite");
        assertBlock(helper, result.get(1).state(), "tfc:wood/planks/spruce");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void netherTemplateUsesDedicatedConversions(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        BlockPos origin = new BlockPos(0, 80, 96);
        for (int x = 0; x < 4; x++) {
            nether.setBlock(origin.offset(x, -1, 0), Blocks.BLACKSTONE.defaultBlockState(), 2);
        }
        StructurePlaceSettings settings = injectedSettings(nether, "betterfortresses:test", origin);
        List<StructureTemplate.StructureBlockInfo> result = process(nether, origin, settings, List.of(
                info(BlockPos.ZERO, Blocks.STONE.defaultBlockState()),
                info(BlockPos.ZERO.east(), Blocks.DIRT.defaultBlockState()),
                info(BlockPos.ZERO.east(2), Blocks.CRIMSON_PLANKS.defaultBlockState()),
                info(BlockPos.ZERO.east(3), Blocks.BLACKSTONE.defaultBlockState())));
        assertBlock(helper, result.get(0).state(), "tfc:rock/raw/basalt");
        assertBlock(helper, result.get(1).state(), "tfc:dirt/oxisol");
        assertBlock(helper, result.get(2).state(), "tfcoverdims:wood/planks/crimson");
        assertBlock(helper, result.get(3).state(), "minecraft:blackstone");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void endCityTemplateRemainsUnchanged(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        BlockPos origin = new BlockPos(0, 80, 0);
        StructureTemplate ship = end.getStructureManager().getOrCreate(
                ResourceLocation.parse("minecraft:end_city/ship"));
        StructurePlaceSettings settings = new StructurePlaceSettings();
        ship.placeInWorld(end, origin, origin, settings, end.getRandom(), 2);
        helper.assertTrue(settings.getProcessors().isEmpty(), "End City must not receive this mod's processor");
        CompoundTag chestNbt = new CompoundTag();
        chestNbt.putString("LootTable", "minecraft:chests/end_city_treasure");
        List<StructureTemplate.StructureBlockInfo> result = process(end, origin, settings, List.of(
                info(BlockPos.ZERO, Blocks.STONE.defaultBlockState()),
                new StructureTemplate.StructureBlockInfo(BlockPos.ZERO.east(), Blocks.CHEST.defaultBlockState(), chestNbt),
                info(BlockPos.ZERO.east(2), Blocks.FURNACE.defaultBlockState())));
        assertBlock(helper, result.get(0).state(), "minecraft:stone");
        assertBlock(helper, result.get(1).state(), "minecraft:chest");
        assertBlock(helper, result.get(2).state(), "minecraft:furnace");
        List<StructureTemplate.StructureBlockInfo> endConverted = TfcBlockReplacementProcessor.INSTANCE
                .finalizeProcessing(end, origin, origin, result, result, settings);
        helper.assertTrue(endConverted.equals(result), "The End must preserve blocks and Block Entity NBT");
        CompoundTag equipment = new CompoundTag();
        equipment.putString("id", "minecraft:armor_stand");
        CompoundTag sword = new CompoundTag();
        sword.putString("id", "minecraft:iron_sword");
        equipment.put("HandItems", new net.minecraft.nbt.ListTag());
        equipment.getList("HandItems", CompoundTag.TAG_COMPOUND).add(sword);
        var entity = new StructureTemplate.StructureEntityInfo(Vec3.ZERO, BlockPos.ZERO, equipment);
        helper.assertTrue(TfcBlockReplacementProcessor.INSTANCE.processEntity(
                end, origin, entity, entity, settings, ship) == entity,
                "The End must preserve armor-stand equipment");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void directPieceContextAndOptionalMixins(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(48, 8, 0));
        helper.getLevel().setBlock(origin.below(), state("tfc:rock/raw/slate"), 2);
        helper.getLevel().setBlock(origin.below(2), state("tfc:dirt/alfisol"), 2);
        assertBlock(helper, DirectPlacementContext.convert(
                helper.getLevel(), origin, origin, Blocks.COBBLESTONE.defaultBlockState()),
                "tfc:rock/cobble/slate");
        DirectPlacementContext.convert(helper.getLevel(), origin, origin.east(), Blocks.SPRUCE_LOG.defaultBlockState());
        assertBlock(helper, DirectPlacementContext.convert(
                helper.getLevel(), origin, origin.east(2), Blocks.CRAFTING_TABLE.defaultBlockState()),
                "tfc:wood/workbench/spruce");

        assertOptionalPieceMarker(helper, "bettermineshafts",
                "com.yungnickyoung.minecraft.bettermineshafts.world.generator.pieces.BetterMineshaftPiece");
        assertOptionalPieceMarker(helper, "betterdungeons",
                "com.yungnickyoung.minecraft.betterdungeons.world.structure.spider_dungeon.piece.SpiderDungeonPiece");
        IModFile modFile = ModList.get().getModFileById(YungsBetterTfc.MOD_ID).getFile();
        helper.assertTrue(Files.exists(modFile.findResource(
                "data", "minecraft", "loot_table", "chests", "abandoned_mineshaft.json")),
                "The abandoned mineshaft loot override must remain present");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void optionalPiecesPlaceConvertedBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        try {
            if (ModList.get().isLoaded("bettermineshafts")) {
                Object structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                        .get(ResourceLocation.parse("bettermineshafts:mineshaft_oak"));
                Field configField = structure.getClass().getDeclaredField("config");
                configField.setAccessible(true);
                Object config = configField.get(structure);
                Class<?> pieceClass = Class.forName(
                        "com.yungnickyoung.minecraft.bettermineshafts.world.generator.pieces.SmallTunnel");
                Constructor<?> constructor = pieceClass.getConstructor(
                        int.class, BoundingBox.class, Direction.class, config.getClass());
                BlockPos start = helper.absolutePos(new BlockPos(80, 8, 0));
                BoundingBox box = new BoundingBox(start.getX(), start.getY(), start.getZ(),
                        start.getX() + 4, start.getY() + 4, start.getZ() + 9);
                StructurePiece piece = (StructurePiece) constructor.newInstance(0, box, Direction.SOUTH, config);
                level.setBlock(start.below(), state("tfc:rock/raw/slate"), 2);
                Method placeBlock = StructurePiece.class.getDeclaredMethod("placeBlock",
                        net.minecraft.world.level.WorldGenLevel.class, BlockState.class,
                        int.class, int.class, int.class, BoundingBox.class);
                placeBlock.setAccessible(true);
                placeBlock.invoke(piece, level, Blocks.OAK_PLANKS.defaultBlockState(), 1, 1, 1, box);
                helper.assertTrue(containsBlock(level, box, "tfc:wood/planks/oak"),
                        "A real Mineshaft piece must convert inherited placement");
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        level.getRandom(), box, new ChunkPos(start), start);
                helper.assertTrue(containsBlock(level, box, "tfc:wood/planks/oak"),
                        "A real Mineshaft piece must retain converted wood after generation");
                BlockPos chestPos = start.offset(2, 2, 2);
                level.setBlock(chestPos, Blocks.AIR.defaultBlockState(), 2);
                ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot = ResourceKey.create(
                        Registries.LOOT_TABLE, ResourceLocation.parse("minecraft:chests/abandoned_mineshaft"));
                Method createChest = StructurePiece.class.getDeclaredMethod("createChest",
                        net.minecraft.world.level.ServerLevelAccessor.class, BoundingBox.class,
                        net.minecraft.util.RandomSource.class, BlockPos.class, ResourceKey.class, BlockState.class);
                createChest.setAccessible(true);
                helper.assertTrue((boolean) createChest.invoke(piece, level, box, level.getRandom(), chestPos,
                        loot, Blocks.CHEST.defaultBlockState()), "The piece must place its Dungeon Chest");
                helper.assertTrue(level.getBlockEntity(chestPos) instanceof RandomizableContainerBlockEntity chest
                        && loot.equals(chest.getLootTable()),
                        "Mineshaft Dungeon Chest must retain the abandoned-mineshaft loot table");
                BlockPos barrelPos = start.offset(1, 2, 3);
                Method addBarrel = pieceClass.getSuperclass().getDeclaredMethod("addBarrel",
                        net.minecraft.world.level.WorldGenLevel.class, BoundingBox.class,
                        net.minecraft.util.RandomSource.class, BlockPos.class, ResourceKey.class);
                addBarrel.setAccessible(true);
                helper.assertTrue((boolean) addBarrel.invoke(piece, level, box, level.getRandom(), barrelPos, loot),
                        "The Mineshaft piece must place a barrel");
                helper.assertTrue(level.getBlockEntity(barrelPos) instanceof RandomizableContainerBlockEntity barrel
                        && loot.equals(barrel.getLootTable()),
                        "Mineshaft barrel must retain the abandoned-mineshaft loot table");
                BlockPos pillarBase = start.offset(3, 1, 3);
                level.setBlock(pillarBase, Blocks.STONE.defaultBlockState(), 2);
                level.setBlock(pillarBase.above(), Blocks.AIR.defaultBlockState(), 2);
                Method generatePillar = pieceClass.getSuperclass().getDeclaredMethod(
                        "generatePillarDownOrChainUp", net.minecraft.world.level.WorldGenLevel.class,
                        net.minecraft.util.RandomSource.class, BoundingBox.class,
                        int.class, int.class, int.class, int.class, BlockState.class);
                generatePillar.setAccessible(true);
                generatePillar.invoke(piece, level, level.getRandom(), box, 3, 3, 3, 3,
                        Blocks.STRIPPED_OAK_LOG.defaultBlockState());
                assertBlock(helper, level.getBlockState(pillarBase.above()), "tfc:wood/stripped_log/oak");
                Method getWorldPos = StructurePiece.class.getDeclaredMethod(
                        "getWorldPos", int.class, int.class, int.class);
                getWorldPos.setAccessible(true);
                for (int z = 0; z < 8; z++) {
                    BlockPos cartPos = (BlockPos) getWorldPos.invoke(piece, 2, 1, z);
                    level.setBlock(cartPos.below(), Blocks.STONE.defaultBlockState(), 2);
                }
                AABB cartBounds = new AABB(box.minX(), box.minY(), box.minZ(),
                        box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
                List<MinecartChest> existingCarts = level.getEntitiesOfClass(MinecartChest.class, cartBounds);
                Method generateChestCarts = pieceClass.getDeclaredMethod("generateChestCarts",
                        net.minecraft.world.level.WorldGenLevel.class, BoundingBox.class,
                        net.minecraft.util.RandomSource.class);
                generateChestCarts.setAccessible(true);
                generateChestCarts.invoke(piece, level, box, new LegacyRandomSource(0) {
                    @Override
                    public float nextFloat() {
                        return 0.0F;
                    }
                });
                List<MinecartChest> carts = level.getEntitiesOfClass(MinecartChest.class, cartBounds)
                        .stream().filter(cart -> !existingCarts.contains(cart)).toList();
                helper.assertTrue(!carts.isEmpty() && carts.stream().allMatch(
                        cart -> loot.equals(cart.getLootTable())),
                        "Mineshaft chest minecarts must retain the abandoned-mineshaft loot table");
            }
            if (ModList.get().isLoaded("betterdungeons")) {
                Class<?> pieceClass = Class.forName("com.yungnickyoung.minecraft.betterdungeons.world"
                        + ".structure.spider_dungeon.piece.SpiderDungeonSmallTunnelPiece");
                BlockPos start = helper.absolutePos(new BlockPos(112, 72, 0));
                StructurePiece piece = (StructurePiece) pieceClass
                        .getConstructor(BlockPos.class, float.class, int.class)
                        .newInstance(start, 0.0F, 0);
                BoundingBox box = piece.getBoundingBox();
                level.setBlock(new BlockPos(box.minX(), box.minY() - 1, box.minZ()),
                        state("tfc:rock/raw/slate"), 2);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        level.getRandom(), box, new ChunkPos(start), start);
                level.setBlock(new BlockPos(box.minX(), box.minY() - 1, box.minZ()),
                        state("tfc:rock/raw/slate"), 2);
                Method placeBlock = StructurePiece.class.getDeclaredMethod("placeBlock",
                        net.minecraft.world.level.WorldGenLevel.class, BlockState.class,
                        int.class, int.class, int.class, BoundingBox.class);
                placeBlock.setAccessible(true);
                placeBlock.invoke(piece, level, Blocks.COBBLESTONE.defaultBlockState(),
                        box.minX() + 1, box.minY() + 1, box.minZ() + 1, box);
                helper.assertTrue(containsBlock(level, box, "tfc:rock/cobble/slate")
                        || containsBlock(level, box, "tfc:rock/hardened/slate"),
                        "A real Spider Dungeon piece must convert cobblestone placement: "
                                + BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(
                                        box.minX() + 1, box.minY() + 1, box.minZ() + 1)).getBlock()));
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to construct an optional YUNG piece", exception);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void betterFortressesAddsNoWorldgenDefinition(GameTestHelper helper) {
        if (ModList.get().isLoaded("betterfortresses")) {
            ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
            BlockPos origin = new BlockPos(128, 90, 128);
            ResourceLocation id = ResourceLocation.parse("betterfortresses:halls/halls_entrance_tunnel");
            StructureTemplate template = nether.getStructureManager().getOrCreate(id);
            helper.assertTrue(id.equals(((StructureTemplateIdAccess) (Object) template)
                    .yungsbettertfc$getTemplateId()), "A real fortress template must retain its ID");
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
            template.placeInWorld(nether, origin, origin, settings, nether.getRandom(), 2);
            helper.assertTrue(settings.getProcessors().size() == 2
                    && settings.getProcessors().get(0) == BlockIgnoreProcessor.STRUCTURE_BLOCK
                    && settings.getProcessors().get(1) instanceof TfcBlockReplacementProcessor,
                    "A real fortress template must append the conversion processor last");
            List<StructureTemplate.StructureBlockInfo> original = template.filterBlocks(
                    origin, settings, Blocks.STONE_BRICK_STAIRS, true);
            helper.assertTrue(!original.isEmpty(), "The real fortress template must contain stone brick stairs");
            assertBlock(helper, nether.getBlockState(original.getFirst().pos()),
                    "tfc:rock/bricks/basalt_stairs");
            List<StructureTemplate.StructureBlockInfo> converted = process(nether, origin, settings, original);
            for (StructureTemplate.StructureBlockInfo block : converted) {
                assertBlock(helper, block.state(), "tfc:rock/bricks/basalt_stairs");
            }
        }
        IModFile modFile = ModList.get().getModFileById(YungsBetterTfc.MOD_ID).getFile();
        try (var files = Files.walk(modFile.findResource("data"))) {
            long definitions = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().contains("/worldgen/structure_set/")
                            || path.toString().contains("/worldgen/structure/"))
                    .count();
            helper.assertTrue(definitions == 0,
                    "This mod must not add a fortress structure or structure_set definition");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect bundled worldgen resources", exception);
        }
        helper.succeed();
    }

    private static StructureTemplate identifiedTemplate(String id) {
        StructureTemplate template = new StructureTemplate();
        ((StructureTemplateIdAccess) (Object) template).yungsbettertfc$setTemplateId(ResourceLocation.parse(id));
        return template;
    }

    private static void invokePlacement(
            StructureTemplate template, ServerLevel level, StructurePlaceSettings settings) {
        template.placeInWorld(level, BlockPos.ZERO, BlockPos.ZERO, settings, level.getRandom(), 2);
    }

    private static StructurePlaceSettings injectedSettings(ServerLevel level, String id, BlockPos origin) {
        StructurePlaceSettings settings = new StructurePlaceSettings();
        identifiedTemplate(id).placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
        return settings;
    }

    private static List<StructureTemplate.StructureBlockInfo> process(
            ServerLevel level,
            BlockPos origin,
            StructurePlaceSettings settings,
            List<StructureTemplate.StructureBlockInfo> blocks) {
        return StructureTemplate.processBlockInfos(level, origin, origin, settings, blocks, null);
    }

    private static StructureTemplate.StructureBlockInfo info(BlockPos position, BlockState state) {
        return new StructureTemplate.StructureBlockInfo(position, state, null);
    }

    private static BlockState state(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        if (block == Blocks.AIR) {
            throw new IllegalStateException("Missing test block " + id);
        }
        return block.defaultBlockState();
    }

    private static boolean containsBlock(ServerLevel level, BoundingBox box, String id) {
        Block target = state(id).getBlock();
        for (BlockPos position : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ())) {
            if (level.getBlockState(position).is(target)) {
                return true;
            }
        }
        return false;
    }

    private static void assertBlock(GameTestHelper helper, BlockState state, String expected) {
        helper.assertTrue(BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(ResourceLocation.parse(expected)),
                "Expected " + expected + ", found " + BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static void assertBiomeInTag(
            GameTestHelper helper, ResourceLocation biomeId, ResourceLocation tagId) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);
        Holder.Reference<Biome> biome = biomes.getHolder(biomeKey).orElseThrow();
        TagKey<Biome> tag = TagKey.create(Registries.BIOME, tagId);
        helper.assertTrue(biome.is(tag), biomeId + " must remain in or be added to " + tagId);
    }

    private static void assertOptionalPieceMarker(GameTestHelper helper, String modId, String className) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        try {
            helper.assertTrue(ConvertedStructurePiece.class.isAssignableFrom(Class.forName(className)),
                    className + " must receive its gated conversion marker mixin");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Loaded optional mod is missing " + className, exception);
        }
    }

    private static Map<ResourceLocation, ResourceLocation> addedBiomes() {
        Map<ResourceLocation, ResourceLocation> tags = new LinkedHashMap<>();
        for (String dungeon : List.of("skeleton_dungeon", "small_dungeon", "spider_dungeon", "zombie_dungeon")) {
            tags.put(ResourceLocation.parse("betterdungeons:has_structure/" + dungeon),
                    ResourceLocation.parse("tfc:plains"));
        }
        tags.put(ResourceLocation.parse("betterstrongholds:has_structure/better_stronghold"),
                ResourceLocation.parse("tfc:plains"));
        tags.put(ResourceLocation.parse("betteroceanmonuments:has_structure/better_ocean_monument"),
                ResourceLocation.parse("tfc:ocean_reef"));
        tags.put(ResourceLocation.parse("bettermineshafts:has_structure/better_mineshaft_oak"),
                ResourceLocation.parse("tfc:plains"));
        tags.put(ResourceLocation.parse("bettermineshafts:has_structure/better_mineshaft_desert"),
                ResourceLocation.parse("tfc:dune_sea"));
        tags.put(ResourceLocation.parse("bettermineshafts:has_structure/better_mineshaft_red_desert"),
                ResourceLocation.parse("tfc:badlands"));
        tags.put(ResourceLocation.parse("bettermineshafts:has_structure/better_mineshaft_mesa"),
                ResourceLocation.parse("tfc:mesas"));
        tags.put(ResourceLocation.parse("bettermineshafts:has_structure/better_mineshaft_ice"),
                ResourceLocation.parse("tfc:ice_sheet"));
        tags.put(ResourceLocation.parse("bettermineshafts:has_structure/better_mineshaft_spruce_snowy"),
                ResourceLocation.parse("tfc:glacially_carved_mountains"));
        return Map.copyOf(tags);
    }
}
