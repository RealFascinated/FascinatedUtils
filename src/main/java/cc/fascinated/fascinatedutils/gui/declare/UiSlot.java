package cc.fascinated.fascinatedutils.gui.declare;

import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * One child of a declarative container, optionally keyed for stable reconciliation.
 *
 * @param key         stable identity for dynamic lists; {@code null} uses positional keys
 * @param constraints layout constraints copied into the mounted widget; {@code null} uses defaults
 * @param node        child view
 */
public record UiSlot(@Nullable String key, @Nullable FCellConstraints constraints, UiView node) {
    public UiSlot {
        Objects.requireNonNull(node, "node");
        constraints = FCellConstraints.copyNullable(constraints);
    }

    /**
     * Positional slot with default constraints.
     *
     * @param node child view
     * @return slot
     */
    public static UiSlot of(UiView node) {
        return new UiSlot(null, null, node);
    }

    /**
     * Keyed slot with default constraints.
     *
     * @param slotKey stable key
     * @param node    child view
     * @return slot
     */
    public static UiSlot keyed(String slotKey, UiView node) {
        return new UiSlot(Objects.requireNonNull(slotKey, "slotKey"), null, node);
    }

    /**
     * Keyed slot with explicit constraints.
     *
     * @param slotKey     stable key
     * @param constraints layout constraints
     * @param node        child view
     * @return slot
     */
    public static UiSlot keyed(String slotKey, FCellConstraints constraints, UiView node) {
        return new UiSlot(Objects.requireNonNull(slotKey, "slotKey"), constraints, node);
    }
}
