package net.claustra01.yungsbettertfc.mixin.betterdungeons;

import net.claustra01.yungsbettertfc.access.ConvertedStructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(
        targets = "com.yungnickyoung.minecraft.betterdungeons.world.structure.spider_dungeon.piece.SpiderDungeonPiece",
        remap = false)
public abstract class SpiderDungeonPieceMixin implements ConvertedStructurePiece {
}
