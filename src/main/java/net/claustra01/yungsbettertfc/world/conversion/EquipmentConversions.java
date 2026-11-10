package net.claustra01.yungsbettertfc.world.conversion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class EquipmentConversions {
    private EquipmentConversions() {}

    public static CompoundTag rewriteEntity(CompoundTag entityNbt) {
        ResourceLocation entityId = ResourceLocation.tryParse(entityNbt.getString("id"));
        if (entityId == null || !entityId.getNamespace().equals("minecraft")) {
            return entityNbt;
        }

        CompoundTag rewritten = entityNbt.copy();
        boolean changed = switch (entityId.getPath()) {
            case "item_frame", "glow_item_frame" -> rewriteStack(rewritten, "Item");
            case "armor_stand" -> rewriteStacks(rewritten, "ArmorItems") | rewriteStacks(rewritten, "HandItems");
            default -> false;
        };
        return changed ? rewritten : entityNbt;
    }

    private static boolean rewriteStack(CompoundTag owner, String key) {
        if (!owner.contains(key, Tag.TAG_COMPOUND)) {
            return false;
        }
        return rewriteStackId(owner.getCompound(key));
    }

    private static boolean rewriteStacks(CompoundTag owner, String key) {
        if (!owner.contains(key, Tag.TAG_LIST)) {
            return false;
        }
        ListTag stacks = owner.getList(key, Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int index = 0; index < stacks.size(); index++) {
            changed |= rewriteStackId(stacks.getCompound(index));
        }
        return changed;
    }

    private static boolean rewriteStackId(CompoundTag stack) {
        ResourceLocation itemId = ResourceLocation.tryParse(stack.getString("id"));
        if (itemId == null || !itemId.getNamespace().equals("minecraft")) {
            return false;
        }
        ResourceLocation replacement = candidate(itemId.getPath());
        if (replacement == null || !BuiltInRegistries.ITEM.containsKey(replacement)) {
            return false;
        }

        stack.putString("id", replacement.toString());
        stack.remove("components");
        stack.remove("tag");
        stack.remove("Damage");
        stack.remove("damage");
        return true;
    }

    private static ResourceLocation candidate(String path) {
        if (path.endsWith("_sword")) {
            return tfc("metal/sword/wrought_iron");
        }
        if (path.endsWith("_axe")) {
            return tfc("metal/axe/wrought_iron");
        }
        if (path.endsWith("_pickaxe")) {
            return tfc("metal/pickaxe/wrought_iron");
        }
        if (path.endsWith("_shovel")) {
            return tfc("metal/shovel/wrought_iron");
        }
        if (path.endsWith("_hoe")) {
            return tfc("metal/hoe/wrought_iron");
        }
        if (path.endsWith("_helmet")) {
            return tfc("metal/helmet/wrought_iron");
        }
        if (path.endsWith("_chestplate")) {
            return tfc("metal/chestplate/wrought_iron");
        }
        if (path.endsWith("_leggings")) {
            return tfc("metal/greaves/wrought_iron");
        }
        if (path.endsWith("_boots")) {
            return tfc("metal/boots/wrought_iron");
        }
        return switch (path) {
            case "shield" -> tfc("metal/shield/wrought_iron");
            case "bow", "crossbow", "trident" -> tfc("metal/javelin/wrought_iron");
            case "mace" -> tfc("metal/mace/wrought_iron");
            default -> null;
        };
    }

    private static ResourceLocation tfc(String path) {
        return ResourceLocation.fromNamespaceAndPath("tfc", path);
    }
}
