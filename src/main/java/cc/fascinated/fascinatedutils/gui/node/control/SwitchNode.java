package cc.fascinated.fascinatedutils.gui.node.control;

import cc.fascinated.fascinatedutils.gui.node.base.ToggleNode;

import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

/**
 * An on/off switch for settings that take effect immediately. Use {@link CheckboxNode} for choices that are only
 * confirmed later, such as a list selection.
 */
public class SwitchNode extends ToggleNode<SwitchNode> {
    public static final int TRACK_WIDTH = 30;
    public static final int TRACK_HEIGHT = 16;

    private static final int KNOB_INSET = 2;
    private static final float SLIDE_SPEED = 14f;
    private static final float KNOB_STRETCH = 5f;

    @Override
    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        return TRACK_WIDTH;
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return TRACK_HEIGHT;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        UiTheme theme = renderFrame.theme();
        boolean disabled = isDisabled();
        float hover = tickHover(deltaSeconds);
        float knobProgress = tickChecked(deltaSeconds, SLIDE_SPEED);
        float travelStretch = 1f - Math.abs(2f * knobProgress - 1f);
        int trackX = bounds().positionX() + (bounds().width() - TRACK_WIDTH) / 2;
        int trackY = bounds().positionY() + (bounds().height() - TRACK_HEIGHT) / 2;
        int radius = TRACK_HEIGHT / 2;
        int offTrack = Colors.mixArgb(hover, theme.surfaceRaised(), theme.surfaceActive());
        int onTrack = disabled ? theme.textSubtle() : Colors.mixArgb(hover, theme.accent(), theme.accentHover());
        renderFrame.drawRoundedRect(trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, radius, Colors.mixArgb(knobProgress, offTrack, onTrack));
        if (knobProgress < 1f) {
            int borderColor = Colors.mixArgb(knobProgress, theme.borderStrong(), theme.borderStrong() & 0x00FFFFFF);
            renderFrame.drawRoundedRectFrame(trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, radius, borderColor, 0, 1);
        }
        int knobSize = TRACK_HEIGHT - KNOB_INSET * 2;
        float knobWidth = knobSize + KNOB_STRETCH * travelStretch;
        float knobTravel = TRACK_WIDTH - KNOB_INSET * 2 - knobSize;
        float knobX = trackX + KNOB_INSET + (knobTravel - (knobWidth - knobSize)) * knobProgress;
        float knobY = trackY + (TRACK_HEIGHT - knobSize) / 2f;
        renderFrame.drawRoundedRect(knobX, knobY, knobWidth, knobSize, knobSize / 2f, Colors.mixArgb(knobProgress, theme.textMuted(), theme.onAccent()));
        drawFocusRing(renderFrame, trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, radius);
    }
}
