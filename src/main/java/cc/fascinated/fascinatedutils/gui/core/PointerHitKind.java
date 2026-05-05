package cc.fascinated.fascinatedutils.gui.core;

/**
 * How a widget participates in pointer hit-testing.
 *
 * <p>{@link #NONE} does not stop hits beneath it. {@link #BLOCK} stops traversal for empty
 * space (scrims, panels). {@link #TARGET} marks an interactive surface (buttons, fields) and
 * still blocks hits like {@link #BLOCK}.</p>
 */
public enum PointerHitKind {
    NONE, BLOCK, TARGET
}
