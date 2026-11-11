package net.claustra01.yungsbettertfc.world.conversion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;

public record ConversionContext(
        DimensionKind dimension,
        String rock,
        String soil,
        WoodHint woodHint,
        boolean belowIsAir,
        boolean cactusBelow) {

    public static ConversionContext at(
            ServerLevelAccessor level, BlockPos origin, DimensionKind dimension, WoodHint woodHint) {
        if (dimension == DimensionKind.NETHER) {
            return new ConversionContext(dimension, "basalt", "oxisol", woodHint, false);
        }
        if (dimension == DimensionKind.OVERWORLD) {
            GeologyDetector.Geology geology = GeologyDetector.detect(level, origin);
            return new ConversionContext(dimension, geology.rock(), geology.soil(), woodHint, false);
        }
        return new ConversionContext(DimensionKind.NONE, "granite", "mollisol", woodHint, false);
    }

    public ConversionContext(
            DimensionKind dimension,
            String rock,
            String soil,
            WoodHint woodHint,
            boolean belowIsAir) {
        this(dimension, rock, soil, woodHint, belowIsAir, false);
    }

    public ConversionContext withPlacementBelow(boolean belowIsAir, boolean cactusBelow) {
        return new ConversionContext(dimension, rock, soil, woodHint, belowIsAir, cactusBelow);
    }
}
