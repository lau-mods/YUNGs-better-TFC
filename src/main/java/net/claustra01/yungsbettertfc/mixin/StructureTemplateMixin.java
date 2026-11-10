package net.claustra01.yungsbettertfc.mixin;

import java.util.Set;

import javax.annotation.Nullable;

import net.claustra01.yungsbettertfc.access.StructureTemplateIdAccess;
import net.claustra01.yungsbettertfc.world.processor.TfcBlockReplacementProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin implements StructureTemplateIdAccess {
    @Unique
    private static final Set<String> YUNGSBETTERTFC_TARGET_NAMESPACES = Set.of(
            "betterstrongholds", "betterdungeons", "betteroceanmonuments", "betterfortresses");

    @Unique @Nullable private ResourceLocation yungsbettertfc$templateId;

    @Override
    @Nullable
    public ResourceLocation yungsbettertfc$getTemplateId() {
        return yungsbettertfc$templateId;
    }

    @Override
    public void yungsbettertfc$setTemplateId(ResourceLocation id) {
        yungsbettertfc$templateId = id;
    }

    @Inject(method = "placeInWorld", at = @At("HEAD"), remap = false)
    private void yungsbettertfc$appendProcessor(
            ServerLevelAccessor level,
            BlockPos offset,
            BlockPos position,
            StructurePlaceSettings settings,
            RandomSource random,
            int flags,
            CallbackInfoReturnable<Boolean> callback) {
        if (yungsbettertfc$templateId == null
                || !YUNGSBETTERTFC_TARGET_NAMESPACES.contains(yungsbettertfc$templateId.getNamespace())) {
            return;
        }
        if (settings.getProcessors().stream().noneMatch(TfcBlockReplacementProcessor.class::isInstance)) {
            settings.addProcessor(TfcBlockReplacementProcessor.INSTANCE);
        }
    }
}
