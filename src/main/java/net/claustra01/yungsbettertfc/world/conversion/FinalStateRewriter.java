package net.claustra01.yungsbettertfc.world.conversion;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class FinalStateRewriter {
    private FinalStateRewriter() {}

    public static CompoundTag rewrite(CompoundTag nbt, String rock) {
        if (nbt == null || !nbt.contains("final_state", Tag.TAG_STRING)) {
            return nbt;
        }

        try {
            BlockStateParser.BlockResult parsed = BlockStateParser.parseForBlock(
                    BuiltInRegistries.BLOCK.asLookup(), nbt.getString("final_state"), true);
            BlockState original = parsed.blockState();
            ResourceLocation originalId = BuiltInRegistries.BLOCK.getKey(original.getBlock());
            if (!originalId.getNamespace().equals("minecraft")) {
                return nbt;
            }
            ResourceLocation candidate = OreConversions.candidate(originalId.getPath(), rock);
            if (candidate == null) {
                return nbt;
            }
            BlockState converted = RegistryLookup.stateOrOriginal(candidate, original);
            if (converted == original) {
                return nbt;
            }
            CompoundTag rewritten = nbt.copy();
            rewritten.putString("final_state", BlockStateParser.serialize(converted)
                    + (parsed.nbt() == null ? "" : parsed.nbt().toString()));
            return rewritten;
        } catch (CommandSyntaxException exception) {
            return nbt;
        }
    }
}
