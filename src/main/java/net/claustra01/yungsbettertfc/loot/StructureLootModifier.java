package net.claustra01.yungsbettertfc.loot;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.claustra01.yungsbettertfc.YungsBetterTfc;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Replaces stacks rolled by an unmodified structure chest loot table according to {@link StructureLootRules}. */
public final class StructureLootModifier extends LootModifier {
    public static final MapCodec<StructureLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).and(instance.group(
                    StructureLootRules.Profile.CODEC.fieldOf("profile").forGetter(modifier -> modifier.profile),
                    Codec.unboundedMap(ResourceLocation.CODEC, LootItemFunctions.ROOT_CODEC)
                            .optionalFieldOf("enchantments", Map.of()).forGetter(modifier -> modifier.enchantments),
                    Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), BuiltInRegistries.ITEM.byNameCodec())
                            .optionalFieldOf("fixed", Map.of()).forGetter(modifier -> modifier.fixed)))
                    .apply(instance, StructureLootModifier::new));
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, YungsBetterTfc.MOD_ID);
    public static final Supplier<MapCodec<StructureLootModifier>> TYPE =
            SERIALIZERS.register("structure_loot_replacement", () -> CODEC);

    private final StructureLootRules.Profile profile;
    /** The source entry's enchantment function per source equipment item, re-run on finished equipment only. */
    private final Map<ResourceLocation, LootItemFunction> enchantments;
    /** Table-specific finished replacements that take precedence over the category rules. */
    private final Map<Item, Item> fixed;

    public StructureLootModifier(LootItemCondition[] conditions, StructureLootRules.Profile profile,
            Map<ResourceLocation, LootItemFunction> enchantments, Map<Item, Item> fixed) {
        super(conditions);
        this.profile = profile;
        this.enchantments = Map.copyOf(enchantments);
        this.fixed = Map.copyOf(fixed);
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>(generatedLoot.size());
        for (ItemStack stack : generatedLoot) {
            result.addAll(StructureLootRules.replace(stack, profile, enchantments, fixed, context));
        }
        return result;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return TYPE.get();
    }
}
