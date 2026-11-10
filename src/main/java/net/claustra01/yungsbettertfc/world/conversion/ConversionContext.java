package net.claustra01.yungsbettertfc.world.conversion;

public record ConversionContext(
        DimensionKind dimension,
        String rock,
        String soil,
        WoodHint woodHint,
        boolean belowIsAir,
        boolean cactusBelow) {

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
