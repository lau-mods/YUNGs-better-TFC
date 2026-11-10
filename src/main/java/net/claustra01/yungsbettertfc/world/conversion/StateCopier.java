package net.claustra01.yungsbettertfc.world.conversion;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class StateCopier {
    private StateCopier() {}

    public static BlockState copySharedProperties(BlockState from, BlockState to) {
        for (Property<?> fromProperty : from.getProperties()) {
            Property<?> toProperty = to.getBlock().getStateDefinition().getProperty(fromProperty.getName());
            if (toProperty != null) {
                to = copyProperty(from, to, fromProperty, toProperty);
            }
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState from,
            BlockState to,
            Property<T> fromProperty,
            Property<?> untypedToProperty) {
        Property<T> toProperty = castProperty(untypedToProperty);
        T value = from.getValue(fromProperty);
        return toProperty.getPossibleValues().contains(value) ? to.setValue(toProperty, value) : to;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> Property<T> castProperty(Property<?> property) {
        return (Property<T>) property;
    }
}
