package net.claustra01.yungsbettertfc.world.conversion;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class DirectPlacementContext {
    private static final int MAX_CACHE_ENTRIES = 2048;
    private static final Map<CacheKey, WoodHint> WOOD_HINTS = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, WoodHint> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private DirectPlacementContext() {}

    public static BlockState convert(
            ServerLevelAccessor level,
            BlockPos origin,
            BlockPos position,
            BlockState state) {
        DimensionKind dimension = DimensionKind.resolve(level);
        if (dimension == DimensionKind.NONE) {
            return state;
        }

        CacheKey key = new CacheKey(level.getLevel().dimension(), origin.asLong());
        WoodHint woodHint = rememberWoodHint(key, dimension, state);
        ConversionContext context = ConversionContext.at(level, origin, dimension, woodHint);
        BlockState below = level.getBlockState(position.below());
        return BlockConversions.convert(state, context.withPlacementBelow(
                below.isAir(), below.is(Blocks.CACTUS)));
    }

    public static void clearCache(ServerStoppedEvent event) {
        synchronized (WOOD_HINTS) {
            WOOD_HINTS.clear();
        }
    }

    private static WoodHint rememberWoodHint(CacheKey key, DimensionKind dimension, BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        WoodConversions.WoodBlock parsed = id.getNamespace().equals("minecraft")
                ? WoodConversions.parse(id.getPath())
                : null;
        synchronized (WOOD_HINTS) {
            if (parsed != null && (dimension == DimensionKind.NETHER
                    || !parsed.hint().namespace().equals("tfcoverdims"))) {
                WOOD_HINTS.put(key, parsed.hint());
            }
            return WOOD_HINTS.getOrDefault(key, WoodHint.OAK);
        }
    }

    private record CacheKey(ResourceKey<Level> dimension, long origin) {}
}
