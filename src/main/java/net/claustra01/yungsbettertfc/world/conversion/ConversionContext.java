package net.claustra01.yungsbettertfc.world.conversion;

public record ConversionContext(
        DimensionKind dimension,
        String rock,
        String soil,
        WoodHint woodHint,
        boolean belowIsAir) {

    public ConversionContext withBelowIsAir(boolean belowIsAir) {
        return new ConversionContext(dimension, rock, soil, woodHint, belowIsAir);
    }
}
