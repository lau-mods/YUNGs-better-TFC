package net.claustra01.yungsbettertfc.world.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import net.claustra01.yungsbettertfc.world.conversion.RegistryLookup;
import net.minecraft.resources.ResourceLocation;

import net.claustra01.yungsbettertfc.world.conversion.BlockConversions;
import net.claustra01.yungsbettertfc.world.conversion.ConversionContext;
import net.claustra01.yungsbettertfc.world.conversion.DimensionKind;
import net.claustra01.yungsbettertfc.world.conversion.EquipmentConversions;
import net.claustra01.yungsbettertfc.world.conversion.FinalStateRewriter;
import net.claustra01.yungsbettertfc.world.conversion.WoodHint;
import net.minecraft.core.BlockPos;
import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class TfcBlockReplacementProcessor extends StructureProcessor {
    public static final TfcBlockReplacementProcessor INSTANCE = new TfcBlockReplacementProcessor(false);
    public static final TfcBlockReplacementProcessor OCEAN_MONUMENT = new TfcBlockReplacementProcessor(true);
    public static final MapCodec<TfcBlockReplacementProcessor> CODEC = Codec.BOOL
            .optionalFieldOf("salt_water", false)
            .xmap(saltWater -> saltWater ? OCEAN_MONUMENT : INSTANCE, processor -> processor.saltWater);
    private final boolean saltWater;

    private TfcBlockReplacementProcessor(boolean saltWater) {
        this.saltWater = saltWater;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModProcessors.TFC_BLOCK_REPLACEMENT.get();
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,
            BlockPos offset,
            BlockPos pos,
            List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
            StructurePlaceSettings settings) {
        DimensionKind dimension = DimensionKind.resolve(level);
        if (dimension == DimensionKind.NONE) {
            return processedBlockInfos;
        }

        WoodHint woodHint = WoodHint.fromOriginalBlocks(originalBlockInfos, dimension);
        ConversionContext context = ConversionContext.at(level, offset, dimension, woodHint);

        Map<BlockPos, BlockState> templateByPosition = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            templateByPosition.put(info.pos(), info.state());
        }

        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(processedBlockInfos.size());
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            // Jigsaw pieces are placed once per intersecting chunk. Vanilla
            // discards positions outside this placement's bounding box; avoid
            // converting them or loading neighboring chunks just to read below.
            if (settings.getBoundingBox() != null && !settings.getBoundingBox().isInside(info.pos())) {
                result.add(info);
                continue;
            }
            BlockPos below = info.pos().below();
            BlockState templateStateBelow = templateByPosition.get(below);
            boolean belowIsAir = templateStateBelow != null
                    ? templateStateBelow.isAir()
                    : level.getBlockState(below).isAir();
            boolean cactusBelow = templateStateBelow != null
                    ? templateStateBelow.is(Blocks.CACTUS)
                    : level.getBlockState(below).is(Blocks.CACTUS);
            BlockState converted = BlockConversions.convert(
                    info.state(), context.withPlacementBelow(belowIsAir, cactusBelow));
            if (saltWater && dimension == DimensionKind.OVERWORLD && converted.is(Blocks.WATER)) {
                converted = RegistryLookup.stateOrOriginal(ResourceLocation.parse("tfc:fluid/salt_water"), converted);
            }
            boolean clearNbt = converted.getBlock() instanceof FirepitBlock
                    && info.state().getBlock() != converted.getBlock();
            result.add(new StructureTemplate.StructureBlockInfo(
                    info.pos(), converted, clearNbt ? null : FinalStateRewriter.rewrite(info.nbt(), context.rock())));
        }
        return result;
    }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(
            LevelReader level,
            BlockPos pos,
            StructureTemplate.StructureEntityInfo rawEntityInfo,
            StructureTemplate.StructureEntityInfo processedEntityInfo,
            StructurePlaceSettings settings,
            StructureTemplate template) {
        if (DimensionKind.resolve(level) == DimensionKind.NONE) {
            return processedEntityInfo;
        }
        var rewritten = EquipmentConversions.rewriteEntity(processedEntityInfo.nbt);
        return rewritten == processedEntityInfo.nbt
                ? processedEntityInfo
                : new StructureTemplate.StructureEntityInfo(
                        processedEntityInfo.pos, processedEntityInfo.blockPos, rewritten);
    }
}
