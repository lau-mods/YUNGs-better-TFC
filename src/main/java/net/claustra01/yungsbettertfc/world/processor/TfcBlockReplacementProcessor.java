package net.claustra01.yungsbettertfc.world.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.claustra01.yungsbettertfc.world.conversion.BlockConversions;
import net.claustra01.yungsbettertfc.world.conversion.ConversionContext;
import net.claustra01.yungsbettertfc.world.conversion.DimensionKind;
import net.claustra01.yungsbettertfc.world.conversion.GeologyDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class TfcBlockReplacementProcessor extends StructureProcessor {
    public static final TfcBlockReplacementProcessor INSTANCE = new TfcBlockReplacementProcessor();
    public static final MapCodec<TfcBlockReplacementProcessor> CODEC = MapCodec.unit(INSTANCE);

    private TfcBlockReplacementProcessor() {}

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

        ConversionContext context;
        if (dimension == DimensionKind.NETHER) {
            context = new ConversionContext(dimension, "basalt", "oxisol", "oak", false);
        } else {
            GeologyDetector.Geology geology = GeologyDetector.detect(level, offset);
            context = new ConversionContext(dimension, geology.rock(), geology.soil(), "oak", false);
        }

        Map<BlockPos, BlockState> templateByPosition = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            templateByPosition.put(info.pos(), info.state());
        }

        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(processedBlockInfos.size());
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            BlockPos below = info.pos().below();
            BlockState templateStateBelow = templateByPosition.get(below);
            boolean belowIsAir = templateStateBelow != null
                    ? templateStateBelow.isAir()
                    : level.getBlockState(below).isAir();
            BlockState converted = BlockConversions.convert(info.state(), context.withBelowIsAir(belowIsAir));
            result.add(new StructureTemplate.StructureBlockInfo(info.pos(), converted, info.nbt()));
        }
        return result;
    }
}
