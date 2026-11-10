package net.claustra01.yungsbettertfc.world.conversion;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public final class WoodConversions {
    private static final List<String> WOODS = List.of(
            "dark_oak", "mangrove", "crimson", "warped", "spruce", "jungle", "acacia", "cherry", "bamboo",
            "birch", "oak");
    private static final List<String> SHAPES = List.of(
            "planks", "stairs", "slab", "log", "wood", "fence_gate", "fence", "door", "trapdoor",
            "pressure_plate", "button", "wall_sign", "sign");

    private WoodConversions() {}

    public static ResourceLocation candidate(String path, ConversionContext context) {
        ResourceLocation utility = utilityCandidate(path, context.woodHint());
        if (utility != null) {
            return utility;
        }

        WoodBlock explicit = parse(path);
        if (explicit == null) {
            return null;
        }
        if (explicit.hint().namespace().equals("tfcoverdims") && context.dimension() != DimensionKind.NETHER) {
            return null;
        }

        String shape = explicit.shape();
        if (shape.equals("hanging_sign") || shape.equals("wall_hanging_sign")) {
            return null;
        }
        String prefix = switch (shape) {
            case "planks", "stairs", "slab" -> "wood/planks/";
            case "log", "stem" -> "wood/log/";
            case "wood", "hyphae" -> "wood/wood/";
            case "stripped_log", "stripped_stem" -> "wood/stripped_log/";
            case "stripped_wood", "stripped_hyphae" -> "wood/stripped_wood/";
            default -> "wood/" + shape + "/";
        };
        String suffix = switch (shape) {
            case "stairs" -> "_stairs";
            case "slab" -> "_slab";
            default -> "";
        };
        return ResourceLocation.fromNamespaceAndPath(
                explicit.hint().namespace(), prefix + explicit.hint().wood() + suffix);
    }

    public static WoodBlock parse(String path) {
        String unstripped = path.startsWith("stripped_") ? path.substring("stripped_".length()) : path;
        for (String wood : WOODS) {
            if (unstripped.startsWith(wood + "_")) {
                String shape = shape(path, wood);
                if (shape != null) {
                    String namespace = wood.equals("crimson") || wood.equals("warped") ? "tfcoverdims" : "tfc";
                    return new WoodBlock(new WoodHint(namespace, normalizeOverworldWood(wood)), shape);
                }
            }
        }
        return null;
    }

    public record WoodBlock(WoodHint hint, String shape) {}

    private static ResourceLocation utilityCandidate(String path, WoodHint hint) {
        String type = switch (path) {
            case "chest" -> "chest";
            case "trapped_chest" -> "trapped_chest";
            case "lectern" -> "lectern";
            case "crafting_table" -> "workbench";
            default -> null;
        };
        return type == null ? null : ResourceLocation.fromNamespaceAndPath(
                hint.namespace(), "wood/" + type + "/" + hint.wood());
    }

    private static String normalizeOverworldWood(String wood) {
        return switch (wood) {
            case "jungle" -> "acacia";
            case "dark_oak", "cherry", "bamboo" -> "oak";
            default -> wood;
        };
    }

    private static String shape(String path, String wood) {
        String strippedPrefix = "stripped_" + wood + "_";
        if (path.startsWith(strippedPrefix)) {
            String strippedShape = path.substring(strippedPrefix.length());
            return switch (strippedShape) {
                case "log" -> "stripped_log";
                case "wood" -> "stripped_wood";
                case "stem" -> "stripped_stem";
                case "hyphae" -> "stripped_hyphae";
                default -> null;
            };
        }
        String prefix = wood + "_";
        if (!path.startsWith(prefix)) {
            return null;
        }
        String shape = path.substring(prefix.length());
        if (shape.equals("stem") || shape.equals("hyphae") || shape.equals("hanging_sign")
                || shape.equals("wall_hanging_sign")) {
            return shape;
        }
        return SHAPES.contains(shape) ? shape : null;
    }
}
