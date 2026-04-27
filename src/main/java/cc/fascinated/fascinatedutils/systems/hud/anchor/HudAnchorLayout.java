package cc.fascinated.fascinatedutils.systems.hud.anchor;

/**
 * Pixel offsets for laying out content inside a padded HUD panel from {@link HudAnchorContentAlignment}.
 * Inner-band sizes are the width or height between horizontal or vertical padding (excluding the padding itself).
 */
public class HudAnchorLayout {

    /**
     * Offset from the top of an inner vertical band to place content of height {@code contentHeight}.
     *
     * @param innerBandHeight height between top and bottom padding
     * @param contentHeight   laid-out content height
     * @param vertical        alignment within the band
     * @return Y offset from the top of the inner band
     */
    public static float verticalOffsetInInnerBand(float innerBandHeight, float contentHeight, HudAnchorContentAlignment.Vertical vertical) {
        return switch (vertical) {
            case TOP -> 0f;
            case BOTTOM -> Math.max(0f, innerBandHeight - contentHeight);
            case CENTER -> Math.max(0f, (innerBandHeight - contentHeight) * 0.5f);
        };
    }

    /**
     * Offset from the left of an inner horizontal band to place content of width {@code contentWidth}.
     *
     * @param innerBandWidth width between left and right padding
     * @param contentWidth   laid-out content width
     * @param horizontal     alignment within the band
     * @return X offset from the left of the inner band
     */
    public static float horizontalOffsetInInnerBand(float innerBandWidth, float contentWidth, HudAnchorContentAlignment.Horizontal horizontal) {
        return switch (horizontal) {
            case LEFT -> 0f;
            case RIGHT -> Math.max(0f, innerBandWidth - contentWidth);
            case CENTER -> Math.max(0f, (innerBandWidth - contentWidth) * 0.5f);
        };
    }
}
