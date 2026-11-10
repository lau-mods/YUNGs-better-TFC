package net.claustra01.yungsbettertfc.world.conversion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public record WoodHint(String namespace, String wood) {
    public static final WoodHint OAK = new WoodHint("tfc", "oak");

    public static WoodHint fromOriginalBlocks(
            List<StructureTemplate.StructureBlockInfo> blocks, DimensionKind dimension) {
        Map<WoodHint, Integer> counts = new LinkedHashMap<>();
        for (StructureTemplate.StructureBlockInfo block : blocks) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block.state().getBlock());
            if (id.getNamespace().equals("minecraft")) {
                WoodConversions.WoodBlock wood = WoodConversions.parse(id.getPath());
                if (wood != null && (dimension == DimensionKind.NETHER
                        || !wood.hint().namespace().equals("tfcoverdims"))) {
                    counts.merge(wood.hint(), 1, Integer::sum);
                }
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(OAK);
    }

}
