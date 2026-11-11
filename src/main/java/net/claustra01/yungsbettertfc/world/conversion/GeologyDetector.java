package net.claustra01.yungsbettertfc.world.conversion;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class GeologyDetector {
    private static final int MAX_CACHE_ENTRIES = 2048;
    private static final Map<CacheKey, Geology> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, Geology> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private GeologyDetector() {}

    public static Geology detect(ServerLevelAccessor level, BlockPos origin) {
        CacheKey key = new CacheKey(level.getLevel().dimension(), origin.asLong());
        synchronized (CACHE) {
            Geology cached = CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        Geology geology = scan(level, origin);
        synchronized (CACHE) {
            CACHE.put(key, geology);
        }
        return geology;
    }

    public static void clearCache(ServerStoppedEvent event) {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static Geology scan(ServerLevelAccessor level, BlockPos origin) {
        String rock = null;
        String soil = null;
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int distance = 0;
                distance < 64 && cursor.getY() >= level.getMinBuildHeight() && (rock == null || soil == null);
                distance++, cursor.move(0, -1, 0)) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(cursor).getBlock());
            if (id.getNamespace().equals("tfc")) {
                if (rock == null && id.getPath().startsWith("rock/raw/")
                        && !id.getPath().endsWith("_stairs")
                        && !id.getPath().endsWith("_slab")
                        && !id.getPath().endsWith("_wall")) {
                    rock = lastSegment(id);
                }
                if (soil == null && isSoil(id.getPath())) {
                    soil = lastSegment(id);
                }
            }
        }
        return new Geology(rock == null ? "granite" : rock, soil == null ? "mollisol" : soil);
    }

    private static boolean isSoil(String path) {
        return path.startsWith("dirt/")
                || path.startsWith("coarse_dirt/")
                || path.startsWith("grass/")
                || path.startsWith("grass_path/")
                || path.startsWith("rooted_dirt/")
                || path.startsWith("farmland/")
                || path.startsWith("clay_grass/");
    }

    private static String lastSegment(ResourceLocation id) {
        return id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
    }

    public record Geology(String rock, String soil) {}

    private record CacheKey(ResourceKey<Level> dimension, long origin) {}
}
