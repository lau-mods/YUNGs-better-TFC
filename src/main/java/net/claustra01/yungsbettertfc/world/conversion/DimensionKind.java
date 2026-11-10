package net.claustra01.yungsbettertfc.world.conversion;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

public enum DimensionKind {
    OVERWORLD,
    NETHER,
    NONE;

    public static DimensionKind resolve(LevelReader level) {
        if (!(level instanceof ServerLevelAccessor accessor)) {
            return NONE;
        }

        try {
            if (accessor.getLevel().dimension() == Level.OVERWORLD) {
                return OVERWORLD;
            }
            if (accessor.getLevel().dimension() == Level.NETHER) {
                return NETHER;
            }
        } catch (RuntimeException ignored) {
            return NONE;
        }
        return NONE;
    }
}
