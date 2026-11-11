package net.claustra01.yungsbettertfc.mixin;

import java.util.List;
import java.util.Set;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class YungsBetterTfcMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE = "net.claustra01.yungsbettertfc.mixin.";
    private static final Set<String> OPTIONAL_MODS = Set.of("betterdungeons", "bettermineshafts",
            "betterstrongholds", "betteroceanmonuments", "betterfortresses");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(MIXIN_PACKAGE)) {
            String packageName = mixinClassName.substring(MIXIN_PACKAGE.length()).split("\\.", 2)[0];
            if (OPTIONAL_MODS.contains(packageName)) {
                return LoadingModList.get().getModFileById(packageName) != null;
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
