package net.claustra01.yungsbettertfc.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

/** Converts one already rolled structure chest stack into TFC resources and equipment. */
public final class StructureLootRules {
    public static final TagKey<Item> FERTILIZERS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(YungsBetterTfc.MOD_ID, "loot/fertilizers"));
    private static final List<String> GEMS = prefixed("tfc:gem/",
            "amethyst", "diamond", "emerald", "lapis_lazuli", "opal", "pyrite", "ruby", "sapphire", "topaz");
    private static final List<String> COAL_LIKE = List.of(
            "minecraft:charcoal", "tfc:ore/bituminous_coal", "tfc:ore/lignite", "tfc:ore/graphite");
    private static final List<String> UTILITY = List.of(
            "tfc:powder/flux", "minecraft:glass", "minecraft:glass_pane", "tfc:kaolin_clay",
            "tfc:powder/wood_ash", "tfc:powder/saltpeter", "tfc:powder/sylvite");
    private static final List<String> SMALL_IRON = prefixed("tfc:ore/small_", "hematite", "magnetite", "limonite");
    /** Single-target replacements: food keeps its kind and cooking state, seeds stay plantable. */
    private static final Map<String, String> FIXED = fixed();
    /** Vanilla item path to replacement category or equipment family. */
    private static final Map<String, String> CATEGORIES = categories();
    /** Unfinished parts (2/3 in total) and completed equipment (1/3 in total) per family; each uniform. */
    private static final Map<String, List<List<String>>> FAMILIES = families();

    private StructureLootRules() {}

    /**
     * Returns the stacks that replace {@code stack}, or a list holding only {@code stack} when no rule applies.
     * Count and the source loot functions' other components, such as names, are kept and damage is dropped.
     * Enchantments are never copied: an enchanted equipment stack is replaced only when {@code enchantments} holds
     * its source entry's enchantment function, which is then re-run on finished equipment only. A {@code fixed}
     * entry replaces its source item with that finished item before the category rules apply.
     */
    public static List<ItemStack> replace(ItemStack stack, Profile profile,
            Map<ResourceLocation, LootItemFunction> enchantments, Map<Item, Item> fixed, LootContext context) {
        if (stack.isEmpty()) {
            return List.of(stack);
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean enchanted = !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
        LootItemFunction enchantment = isEquipment(stack.getItem()) ? enchantments.get(id) : null;
        if (enchanted && enchantment == null) {
            return List.of(stack);
        }
        RandomSource random = context.getRandom();
        Target target = fixed.containsKey(stack.getItem())
                ? new Target(fixed.get(stack.getItem()), true) : target(id, profile, random);
        if (target == null || target.item() == null || target.item() == Items.AIR) {
            return List.of(stack);
        }
        DataComponentPatch components = stack.getComponentsPatch().forget(
                type -> type == DataComponents.DAMAGE || type == DataComponents.ENCHANTMENTS);
        List<ItemStack> result = new ArrayList<>();
        int remaining = stack.getCount();
        while (remaining > 0) {
            ItemStack output = new ItemStack(target.item());
            output.applyComponents(components);
            output.setCount(Math.min(remaining, output.getMaxStackSize()));
            remaining -= output.getCount();
            if (enchanted && target.finished()) {
                output = enchantment.apply(output, context);
            }
            result.add(output);
        }
        return result;
    }

    /** Whether a Vanilla item is equipment whose enchantment function a modifier may carry. */
    public static boolean isEquipment(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals("minecraft") && FAMILIES.containsKey(CATEGORIES.getOrDefault(id.getPath(), ""));
    }

    /** Whether a Vanilla item belongs to a maintained replacement rule. */
    public static boolean isReplaced(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals("minecraft")
                && (FIXED.containsKey(id.getPath()) || CATEGORIES.containsKey(id.getPath()));
    }

    private static Target target(ResourceLocation id, Profile profile, RandomSource random) {
        if (!id.getNamespace().equals("minecraft")) {
            return null;
        }
        String path = id.getPath();
        if (FIXED.containsKey(path)) {
            return new Target(item(FIXED.get(path)), false);
        }
        String category = CATEGORIES.get(path);
        if (category == null) {
            return null;
        }
        if (FAMILIES.containsKey(category)) {
            List<List<String>> family = FAMILIES.get(category);
            boolean unfinished = !family.get(0).isEmpty() && random.nextInt(3) < 2;
            String shape = pick(family.get(unfinished ? 0 : 1), random);
            if (shape.startsWith("minecraft:")) {
                return new Target(item(shape), true);
            }
            return new Target(item("tfc:metal/" + shape + "/" + pick(profile.equipment, random)), !unfinished);
        }
        Item item = switch (category) {
            case "gem" -> item(pick(GEMS, random));
            case "ingot" -> item("tfc:metal/ingot/" + pick(profile.ingots, random));
            case "metal_block" -> item("tfc:metal/double_ingot/" + pick(profile.ingots, random));
            case "iron_nugget" -> item(pick(SMALL_IRON, random));
            case "coal_like" -> item(pick(COAL_LIKE, random));
            case "fertilizers" -> BuiltInRegistries.ITEM.getTag(FERTILIZERS)
                    .flatMap(members -> members.getRandomElement(random)).map(Holder::value).orElse(null);
            case "utility" -> item(pick(UTILITY, random));
            default -> null;
        };
        return new Target(item, false);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
    }

    private static String pick(List<String> values, RandomSource random) {
        return values.get(random.nextInt(values.size()));
    }

    private static List<String> prefixed(String prefix, String... names) {
        return java.util.Arrays.stream(names).map(name -> prefix + name).toList();
    }

    private static Map<String, String> fixed() {
        Map<String, String> result = new HashMap<>();
        for (String meat : List.of("beef", "chicken", "mutton", "rabbit", "cod", "salmon")) {
            result.put(meat, "tfc:food/" + meat);
            result.put("cooked_" + meat, "tfc:food/cooked_" + meat);
        }
        result.put("porkchop", "tfc:food/pork");
        result.put("cooked_porkchop", "tfc:food/cooked_pork");
        result.put("apple", "tfc:food/red_apple");
        result.put("melon_slice", "tfc:food/melon_slice");
        result.put("carrot", "tfc:food/carrot");
        result.put("potato", "tfc:food/potato");
        result.put("baked_potato", "tfc:food/baked_potato");
        result.put("beetroot", "tfc:food/beet");
        result.put("bread", "tfc:food/wheat_bread");
        result.put("dried_kelp", "tfc:food/dried_kelp");
        result.put("wheat_seeds", "tfc:seeds/wheat");
        result.put("beetroot_seeds", "tfc:seeds/beet");
        result.put("pumpkin_seeds", "tfc:seeds/pumpkin");
        result.put("melon_seeds", "tfc:seeds/melon");
        result.put("gold_nugget", "tfc:ore/small_native_gold");
        result.put("raw_gold", "tfc:ore/normal_native_gold");
        result.put("torch", "tfc:torch");
        result.put("chain", "tfc:metal/chain/wrought_iron");
        return Map.copyOf(result);
    }

    private static Map<String, String> categories() {
        Map<String, String> result = new HashMap<>();
        for (String id : List.of("diamond", "emerald", "lapis_lazuli", "amethyst_shard")) {
            result.put(id, "gem");
        }
        for (String metal : List.of("iron", "gold", "copper")) {
            result.put(metal + "_ingot", "ingot");
            result.put(metal + "_block", "metal_block");
        }
        result.put("iron_nugget", "iron_nugget");
        result.put("coal", "coal_like");
        result.put("charcoal", "coal_like");
        result.put("bone_meal", "fertilizers");
        result.put("crying_obsidian", "utility");
        result.put("quartz", "utility");
        for (String material : List.of("wooden", "stone", "golden", "iron", "diamond", "netherite")) {
            for (String family : List.of("pickaxe", "axe", "hoe", "sword", "shovel")) {
                result.put(material + "_" + family, family);
            }
        }
        for (String material : List.of("leather", "chainmail", "golden", "iron", "diamond", "netherite")) {
            for (String family : List.of("helmet", "chestplate", "leggings", "boots")) {
                result.put(material + "_" + family, family);
            }
        }
        for (String family : List.of("shears", "shield", "bow", "crossbow")) {
            result.put(family, family);
        }
        for (String material : List.of("iron", "golden", "diamond")) {
            result.put(material + "_horse_armor", "horse_armor");
        }
        return Map.copyOf(result);
    }

    private static Map<String, List<List<String>>> families() {
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        result.put("pickaxe", List.of(List.of("pickaxe_head", "propick_head"), List.of("pickaxe", "propick")));
        result.put("axe", List.of(List.of("axe_head", "saw_blade"), List.of("axe", "saw")));
        result.put("hoe", List.of(List.of("hoe_head", "scythe_blade"), List.of("hoe", "scythe")));
        result.put("sword", List.of(List.of("sword_blade", "javelin_head", "mace_head"), List.of("sword", "javelin", "mace")));
        result.put("shovel", List.of(List.of("shovel_head"), List.of("shovel")));
        result.put("shears", List.of(List.of(), List.of("shears")));
        result.put("shield", List.of(List.of(), List.of("shield")));
        result.put("horse_armor", List.of(List.of(), List.of("horse_armor")));
        result.put("bow", List.of(List.of(), List.of("minecraft:bow")));
        result.put("crossbow", List.of(List.of(), List.of("minecraft:crossbow")));
        result.put("helmet", List.of(List.of("unfinished_helmet"), List.of("helmet")));
        result.put("chestplate", List.of(List.of("unfinished_chestplate"), List.of("chestplate")));
        result.put("leggings", List.of(List.of("unfinished_greaves"), List.of("greaves")));
        result.put("boots", List.of(List.of("unfinished_boots"), List.of("boots")));
        return result;
    }

    private record Target(Item item, boolean finished) {}

    public enum Profile implements StringRepresentable {
        ORDINARY("ordinary", List.of("copper", "bronze", "bismuth_bronze", "black_bronze", "wrought_iron"),
                List.of("copper", "bronze", "bismuth_bronze", "black_bronze")),
        DANGEROUS("dangerous", List.of("wrought_iron", "steel"), List.of("wrought_iron")),
        LATEGAME("lategame", List.of("steel", "black_steel"), List.of("steel"));

        public static final Codec<Profile> CODEC = StringRepresentable.fromEnum(Profile::values);

        private final String name;
        private final List<String> ingots;
        private final List<String> equipment;

        Profile(String name, List<String> ingots, List<String> equipment) {
            this.name = name;
            this.ingots = ingots;
            this.equipment = equipment;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
