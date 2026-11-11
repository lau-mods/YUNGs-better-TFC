package net.claustra01.yungsbettertfc;

import net.claustra01.yungsbettertfc.loot.StructureLootModifier;
import net.claustra01.yungsbettertfc.world.processor.ModProcessors;
import net.claustra01.yungsbettertfc.world.conversion.DirectPlacementContext;
import net.claustra01.yungsbettertfc.world.conversion.GeologyDetector;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(YungsBetterTfc.MOD_ID)
public final class YungsBetterTfc {
    public static final String MOD_ID = "yungsbettertfc";

    public YungsBetterTfc(IEventBus modEventBus) {
        ModProcessors.register(modEventBus);
        StructureLootModifier.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(DirectPlacementContext::clearCache);
        NeoForge.EVENT_BUS.addListener(GeologyDetector::clearCache);
    }
}
