package net.claustra01.yungsbettertfc.world.processor;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModProcessors {
    private static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, YungsBetterTfc.MOD_ID);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<TfcBlockReplacementProcessor>>
            TFC_BLOCK_REPLACEMENT = PROCESSORS.register(
                    "tfc_block_replacement", () -> () -> TfcBlockReplacementProcessor.CODEC);

    private ModProcessors() {}

    public static void register(IEventBus modEventBus) {
        PROCESSORS.register(modEventBus);
    }
}
