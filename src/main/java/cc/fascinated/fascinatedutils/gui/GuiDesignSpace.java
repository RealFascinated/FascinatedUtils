package cc.fascinated.fascinatedutils.gui;

import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

/**
 * Thread-local scaling from design GUI pixels into the active shell coordinate space (framebuffer-aligned units used by
 * mod settings and HUD overlay chrome).
 */
public class GuiDesignSpace {
    private static int nestDepth;
    private static float scaleX = 1f;
    private static float scaleY = 1f;

    private GuiDesignSpace() {
    }

    /**
     * Enter shell hi-res layout for the current thread (client render/input thread).
     *
     * @param framebufferScaleX horizontal framebuffer pixels per GUI pixel
     * @param framebufferScaleY vertical framebuffer pixels per GUI pixel
     */
    public static void begin(float framebufferScaleX, float framebufferScaleY) {
        nestDepth++;
        scaleX = framebufferScaleX;
        scaleY = framebufferScaleY;
    }

    /**
     * Leave the innermost shell scope; when the nest reaches zero, scaling reverts to identity.
     */
    public static void end() {
        nestDepth = Math.max(0, nestDepth - 1);
        if (nestDepth == 0) {
            scaleX = 1f;
            scaleY = 1f;
        }
    }

    /**
     * Whether {@link #begin(float, float)} is active for the current thread.
     *
     * @return true when nested depth is positive, false otherwise
     */
    public static boolean isActive() {
        return nestDepth > 0;
    }

    public static float scaleX() {
        return nestDepth > 0 ? scaleX : 1f;
    }

    public static float scaleY() {
        return nestDepth > 0 ? scaleY : 1f;
    }

    /**
     * Map a horizontal design measurement (GUI pixels) into shell units.
     *
     * @param designPixels design size in GUI pixels
     * @return scaled horizontal size
     */
    public static float pxX(float designPixels) {
        return designPixels * scaleX();
    }

    /**
     * Map a vertical design measurement (GUI pixels) into shell units.
     *
     * @param designPixels design size in GUI pixels
     * @return scaled vertical size
     */
    public static float pxY(float designPixels) {
        return designPixels * scaleY();
    }

    /**
     * Map a uniform design measurement using the smaller axis scale so squares stay square.
     *
     * @param designPixels design size in GUI pixels
     * @return scaled uniform size
     */
    public static float pxUniform(float designPixels) {
        return designPixels * Math.min(scaleX(), scaleY());
    }

    /**
     * Vertical scroll wheel multiplier so wheel deltas move the same visual distance in shell space.
     *
     * @return vertical scale factor for scroll deltas
     */
    public static float scrollWheelScaleY() {
        return scaleY();
    }

    /**
     * Wrap width in the same units as {@link UIRenderer#measureTextWidth(
     *String, boolean)} for the current shell (do not divide by horizontal scale; that shrinks the budget vs.
     * measurement).
     *
     * @param layoutWidthPx layout width in shell pixels
     * @return text wrap budget compatible with measurement
     */
    public static float guiTextWrapBudgetPx(float layoutWidthPx) {
        return layoutWidthPx;
    }

    /**
     * Line height for wrapped body text in shell pixels.
     *
     * @param vanillaLineHeightPx vanilla line height in pixels
     * @return line height for layout
     */
    public static float layoutTextLineHeightPx(float vanillaLineHeightPx) {
        return vanillaLineHeightPx;
    }
}
