package cc.fascinated.fascinatedutils.systems.hud.panel;

import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;

import cc.fascinated.fascinatedutils.gui.node.display.RichTextNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ColumnNode;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;

import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * What a {@link HudPanel} sees while it composes its content for one frame: the frame inputs, the host's text
 * settings, the alignment the anchor asks for, and helpers that build the common content nodes. Each panel owns one
 * instance that the framework refills every frame, so reading it never allocates.
 *
 * <p>Panels only describe content. Background, border, rounded and merged corners, padding, minimum width, scale
 * and anchored position are applied around the returned node by {@code HudPanelNode}.
 */
public class HudComposeContext {
    private static final DoubleSupplier OPAQUE = () -> 1d;

    private final HudPanel panel;
    private float deltaSeconds;
    private boolean editorMode;
    private boolean padded = true;
    private DoubleSupplier opacity = OPAQUE;

    HudComposeContext(HudPanel panel) {
        this.panel = panel;
    }

    void begin(float deltaSeconds, boolean editorMode) {
        this.deltaSeconds = deltaSeconds;
        this.editorMode = editorMode;
        this.padded = true;
        this.opacity = OPAQUE;
    }

    /**
     * The panel being composed.
     *
     * @return the panel
     */
    public HudPanel panel() {
        return panel;
    }

    /**
     * The host module that owns the panel and its appearance settings.
     *
     * @return the host module
     */
    public HudHostModule host() {
        return panel.hudHostModule();
    }

    /**
     * Frame delta in real seconds, for animations.
     *
     * @return seconds since the previous frame
     */
    public float deltaSeconds() {
        return deltaSeconds;
    }

    /**
     * Whether the panel is composed for the HUD editor preview instead of the game.
     *
     * @return true inside the HUD editor
     */
    public boolean editorMode() {
        return editorMode;
    }

    /**
     * Whether text drawn by this panel has a drop shadow, from the host's text shadow setting.
     *
     * @return true when text is shadowed
     */
    public boolean textShadow() {
        return host().isTextShadowEnabled();
    }

    /**
     * Horizontal alignment of text lines inside the panel: {@code 0} left, {@code 0.5} centered, {@code 1} right.
     * Centered unless the panel overrides it. Pass it to {@code alignX} of a line or of a column of lines.
     *
     * @return the alignment fraction
     */
    public float textAlignment() {
        return alignment(panel.hudTextLineHorizontalAlignment());
    }

    /**
     * Horizontal alignment the anchor asks content blocks (rows of icons, text columns) to take: left for panels
     * anchored left, right for panels anchored right, centered for centered anchors.
     *
     * @return the alignment fraction, {@code 0} left, {@code 0.5} centered, {@code 1} right
     */
    public float contentAlignment() {
        return alignment(panel.hudContentHorizontalAlignment());
    }

    private static float alignment(HudAnchorContentAlignment.Horizontal horizontal) {
        return switch (horizontal) {
            case LEFT -> 0f;
            case CENTER -> 0.5f;
            case RIGHT -> 1f;
        };
    }

    /**
     * Builds a line of MiniMessage text drawn with the host's text shadow setting.
     *
     * @param miniMessage MiniMessage markup
     * @return the text node, aligned left in its parent until given an alignment
     */
    public RichTextNode text(String miniMessage) {
        return RichTextNode.miniMessage(miniMessage, textShadow());
    }

    /**
     * Builds a column of MiniMessage lines aligned by {@link #textAlignment()}, both the column inside the panel and
     * each line inside the column, so the lines align exactly as text does in a panel wider than its text.
     *
     * @param miniMessageLines the lines, top to bottom; a null entry is an empty line
     * @return the column node
     */
    public ColumnNode textLines(List<String> miniMessageLines) {
        float alignment = textAlignment();
        ColumnNode column = new ColumnNode(Math.round(HudAnchorLayout.LINE_GAP_PX)).alignX(alignment);
        for (String line : miniMessageLines) {
            column.addChild(text(line == null ? "" : line).alignX(alignment));
        }
        return column;
    }

    /**
     * Fades the whole panel, background and border included.
     *
     * @param opacity supplies the opacity from 0 to 1; read each frame when the panel draws
     */
    public void opacity(DoubleSupplier opacity) {
        this.opacity = opacity;
    }

    /**
     * Supplies the panel opacity set with {@link #opacity(DoubleSupplier)}.
     *
     * @return the opacity supplier, fully opaque by default
     */
    public DoubleSupplier opacity() {
        return opacity;
    }

    /**
     * Turns the host's padding around the content off, for content that lays out its own padding.
     *
     * @param padded false to place the content flush against the panel edge
     */
    public void padded(boolean padded) {
        this.padded = padded;
    }

    /**
     * The host's padding in pixels, for content that lays out its own padding after {@link #padded(boolean)} turned
     * the panel's padding off.
     *
     * @return the padding in pixels
     */
    public int padding() {
        return Math.round(host().getPaddingValue());
    }

    /**
     * Whether the host's padding is applied around the content.
     *
     * @return true unless {@link #padded(boolean)} turned it off
     */
    public boolean padded() {
        return padded;
    }
}
