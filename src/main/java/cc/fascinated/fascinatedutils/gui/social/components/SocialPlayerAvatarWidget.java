package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.widgets.FAvatarWidget;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A player avatar widget for the social UI that always renders the avatar skin (or initial fallback)
 * together with a presence-dot indicator in the bottom-right quadrant.
 *
 * <p>Wraps {@link FAvatarWidget} with a fixed 4px corner radius and mandatory presence color.
 * Use {@code size} to control the square side length so that the chat list, friends list, and
 * chat header can all share the same component at different scales.
 *
 * @param size          logical side length of the square avatar
 * @param minecraftUuid supplier of the player's Minecraft UUID
 * @param displayName   supplier of the display name used for the initial-letter fallback
 * @param presenceColor supplier of the packed ARGB presence color rendered as the dot
 */
public class SocialPlayerAvatarWidget extends FAvatarWidget {

    private final IntSupplier presenceColor;

    public SocialPlayerAvatarWidget(float size, Supplier<String> minecraftUuid, Supplier<String> displayName, IntSupplier presenceColor) {
        super(size, 4f, minecraftUuid, displayName);
        this.presenceColor = presenceColor;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        super.renderSelf(graphics, frame, deltaSeconds);
        float dotX = x() + w() - 7f;
        float dotY = y() + h() - 7f;
        graphics.fillRoundedRect(dotX - 1f, dotY - 1f, 8f, 8f, 4f, 0xFF1A1E24, RectCornerRoundMask.ALL);
        graphics.fillRoundedRect(dotX, dotY, 6f, 6f, 3f, presenceColor.getAsInt(), RectCornerRoundMask.ALL);
    }
}
