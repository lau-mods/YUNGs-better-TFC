package net.claustra01.yungsbettertfc.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.claustra01.yungsbettertfc.mixin.YungsBetterTfcMixinPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.language.IModInfo;

@GameTestHolder(YungsBetterTfc.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BootstrapGameTests {
    private static final String BATCH = "bootstrap";
    private static final Map<String, String> OPTIONAL_MIXINS = Map.of(
            "net.claustra01.yungsbettertfc.mixin.betterdungeons.CompatibilityMixin", "betterdungeons",
            "net.claustra01.yungsbettertfc.mixin.bettermineshafts.CompatibilityMixin", "bettermineshafts",
            "net.claustra01.yungsbettertfc.mixin.betterstrongholds.CompatibilityMixin", "betterstrongholds",
            "net.claustra01.yungsbettertfc.mixin.betteroceanmonuments.CompatibilityMixin", "betteroceanmonuments",
            "net.claustra01.yungsbettertfc.mixin.betterfortresses.CompatibilityMixin", "betterfortresses");

    @BeforeBatch(batch = BATCH)
    public static void configureReport(ServerLevel level) throws IOException, ParserConfigurationException {
        String report = System.getProperty("yungsbettertfc.gametest.report");
        if (report != null) {
            Path reportPath = Path.of(report);
            Files.createDirectories(reportPath.getParent());
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(reportPath.toFile()));
        }
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void modLoadsWithoutBeneath(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(YungsBetterTfc.MOD_ID), "yungsbettertfc must be loaded");
        helper.assertTrue(ModList.get().isLoaded("tfc"), "TFC must be loaded");
        helper.assertTrue(ModList.get().isLoaded("yungsapi"), "YUNG's API must be loaded");
        helper.assertTrue(ModList.get().isLoaded("tfcoverdims"), "TFC Overdims must be loaded");
        helper.assertTrue(!ModList.get().isLoaded("beneath"), "Beneath must not be loaded");

        IModInfo mod = ModList.get().getModContainerById(YungsBetterTfc.MOD_ID).orElseThrow().getModInfo();
        helper.assertTrue(mod.getVersion().toString().equals("1.0.0"), "The distribution version must be 1.0.0");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void requiredDependenciesDeclared(GameTestHelper helper) {
        List<? extends IModInfo.ModVersion> dependencies = ModList.get()
                .getModContainerById(YungsBetterTfc.MOD_ID)
                .orElseThrow()
                .getModInfo()
                .getDependencies();

        assertDependency(helper, dependencies, "tfc", IModInfo.DependencyType.REQUIRED, "[4.2.12,)");
        assertDependency(helper, dependencies, "yungsapi", IModInfo.DependencyType.REQUIRED,
                "[1.21.1-NeoForge-5.1.6,)");
        assertDependency(helper, dependencies, "tfcoverdims", IModInfo.DependencyType.REQUIRED, "[0.1.4,)");
        helper.assertTrue(dependencies.stream().noneMatch(dependency -> dependency.getModId().equals("beneath")),
                "Beneath must not be declared as a dependency");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void optionalYungMixinGate(GameTestHelper helper) {
        List<? extends IModInfo.ModVersion> dependencies = ModList.get()
                .getModContainerById(YungsBetterTfc.MOD_ID)
                .orElseThrow()
                .getModInfo()
                .getDependencies();
        assertDependency(helper, dependencies, "betterdungeons", IModInfo.DependencyType.OPTIONAL,
                "[1.21.1-NeoForge-5.1.3,)");
        assertDependency(helper, dependencies, "bettermineshafts", IModInfo.DependencyType.OPTIONAL,
                "[1.21.1-NeoForge-5.1.1,)");
        assertDependency(helper, dependencies, "betterstrongholds", IModInfo.DependencyType.OPTIONAL,
                "[1.21.1-NeoForge-5.1.3,)");
        assertDependency(helper, dependencies, "betteroceanmonuments", IModInfo.DependencyType.OPTIONAL,
                "[1.21.1-NeoForge-4.1.1,)");
        assertDependency(helper, dependencies, "betterfortresses", IModInfo.DependencyType.OPTIONAL,
                "[1.21.1-NeoForge-3.1.5,)");

        String configuration = System.getProperty("yungsbettertfc.gametest.optionalMods", "all");
        Set<String> expectedMods = switch (configuration) {
            case "none" -> Set.of();
            case "betterdungeons" -> Set.of("betterdungeons");
            case "all" -> Set.copyOf(OPTIONAL_MIXINS.values());
            default -> throw new IllegalArgumentException("Unknown optional YUNG configuration: " + configuration);
        };
        OPTIONAL_MIXINS.forEach((mixinClass, modId) -> {
            boolean expected = expectedMods.contains(modId);
            helper.assertTrue(ModList.get().isLoaded(modId) == expected,
                    modId + " loaded state must match the runtime configuration");
            helper.assertTrue(new YungsBetterTfcMixinPlugin().shouldApplyMixin("", mixinClass) == expected,
                    "Mixin gate must match the loaded state of " + modId);
        });

        MinecraftServer server = helper.getLevel().getServer();
        Map<String, String> structures = Map.of(
                "betterdungeons", "betterdungeons:small_dungeon",
                "bettermineshafts", "bettermineshafts:mineshaft_oak",
                "betterstrongholds", "betterstrongholds:stronghold",
                "betteroceanmonuments", "betteroceanmonuments:ocean_monument",
                "betterfortresses", "betterfortresses:fortress");
        for (Map.Entry<String, String> entry : structures.entrySet()) {
            String modId = entry.getKey();
            String structure = entry.getValue();
            if (expectedMods.contains(modId)) {
                ServerLevel level = modId.equals("betterfortresses") ? server.getLevel(Level.NETHER) : helper.getLevel();
                BlockPos origin = helper.absolutePos(new BlockPos(0, 128, 0));
                Structure target = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                        .get(ResourceLocation.parse(structure));
                helper.assertTrue(target != null, structure + " must be registered");
                var generator = level.getChunkSource().getGenerator();
                StructureStart start = target.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                        level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                        new ChunkPos(origin), 0, level, biome -> true);
                helper.assertTrue(start.isValid(), structure + " must generate pieces");
                BoundingBox bounds = start.getBoundingBox();
                ChunkPos.rangeClosed(new ChunkPos(bounds.minX() >> 4, bounds.minZ() >> 4),
                        new ChunkPos(bounds.maxX() >> 4, bounds.maxZ() >> 4)).forEach(chunk -> {
                            level.getChunk(chunk.x, chunk.z);
                            start.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
                                    new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                                            chunk.getMaxBlockX(), level.getMaxBuildHeight(), chunk.getMaxBlockZ()), chunk);
                        });
            }
        }
        helper.succeed();
    }

    private static void assertDependency(
            GameTestHelper helper,
            List<? extends IModInfo.ModVersion> dependencies,
            String modId,
            IModInfo.DependencyType type,
            String versionRange) {
        IModInfo.ModVersion dependency = dependencies.stream()
                .filter(candidate -> candidate.getModId().equals(modId))
                .findFirst()
                .orElseThrow();
        helper.assertTrue(dependency.getType() == type, modId + " must have dependency type " + type);
        helper.assertTrue(dependency.getOrdering() == IModInfo.Ordering.AFTER, modId + " must load before this mod");
        helper.assertTrue(dependency.getVersionRange().toString().equals(versionRange),
                modId + " must use the verified lower version bound");
    }
}
