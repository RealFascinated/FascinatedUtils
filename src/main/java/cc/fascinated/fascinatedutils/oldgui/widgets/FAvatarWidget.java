package cc.fascinated.fascinatedutils.oldgui.widgets;

import cc.fascinated.fascinatedutils.caches.UrlTextureCache;
import cc.fascinated.fascinatedutils.oldgui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.oldgui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.oldgui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;
import lombok.Setter;
import net.minecraft.resources.Identifier;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A square avatar widget that shows a Minecraft player skin, or falls back to a colored tile
 * with the player's initial when the texture is unavailable.
 *
 * <p>Optionally renders a user status dot indicator in the bottom-right quadrant.
 * Optionally renders a bordered frame around the avatar (e.g. for profile cards).
 * Not interactive — {@link cc.fascinated.fascinatedutils.oldgui.core.PointerHitKind#NONE}.
 */
public class FAvatarWidget extends FWidget {

    private final float size;
    private final float cornerRadius;
    private final Supplier<String> minecraftUuidSupplier;
    private final Supplier<String> displayNameSupplier;
    @Setter
    private int fallbackColor = 0xFF3B445A;
    @Setter
    private int textureBackgroundArgb = 0xFF000000;
    private Integer borderArgb = null;
    private float borderThickness = 1f;
    @Setter
    private IntSupplier userStatusDotColorSupplier = null;

    /**
     * Creates an avatar widget that draws a player skin or initials fallback.
     *
     * @param size               logical side length of the square avatar
     * @param cornerRadius       corner rounding in logical pixels
     * @param minecraftUuid      supplier of the player's Minecraft UUID; {@code null} or blank shows the fallback
     * @param displayName        supplier of the display name used to derive the initial letter
     */
    public FAvatarWidget(float size, float cornerRadius, Supplier<String> minecraftUuid, Supplier<String> displayName) {
        this.size = size;
        this.cornerRadius = cornerRadius;
        this.minecraftUuidSupplier = minecraftUuid;
        this.displayNameSupplier = displayName;
    }

    /**
     * Enables a bordered frame drawn around the full avatar area (e.g. for profile cards).
     * When set, the texture and fallback are both inset by {@code borderThickness}.
     *
     * @param borderArgb      packed ARGB border color
     * @param borderThickness width of the border ring in logical pixels
     */
    public void setBorderArgb(int borderArgb, float borderThickness) {
        this.borderArgb = borderArgb;
        this.borderThickness = borderThickness;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, size, size);
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return size;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return size;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        String uuid = minecraftUuidSupplier.get();
        Identifier texture = uuid != null && !uuid.isBlank()
                ? UrlTextureCache.INSTANCE.get(uuid, "https://mc.fascinated.cc/api/skins/%s/face.png".formatted(uuid), () -> {})
                : null;
        float inset = borderArgb != null ? borderThickness : 0f;
        if (borderArgb != null) {
            graphics.fillRoundedRectFrame(x(), y(), size, size, cornerRadius,
                    borderArgb, textureBackgroundArgb, borderThickness, borderThickness, RectCornerRoundMask.ALL);
        }
        if (texture != null) {
            if (borderArgb == null) {
                graphics.fillRoundedRect(x(), y(), size, size, cornerRadius, textureBackgroundArgb, RectCornerRoundMask.ALL);
            }
            graphics.drawTexture(texture, x() + inset, y() + inset, size - 2f * inset, size - 2f * inset, 0xFFFFFFFF);
        } else {
            String name = displayNameSupplier.get();
            String initial = name == null || name.isBlank() ? "?" : String.valueOf(Character.toUpperCase(name.charAt(0)));
            if (borderArgb == null) {
                graphics.fillRoundedRect(x(), y(), size, size, cornerRadius, fallbackColor, RectCornerRoundMask.ALL);
            } else {
                float innerRadius = Math.max(0f, cornerRadius - borderThickness);
                graphics.fillRoundedRect(x() + inset, y() + inset, size - 2f * inset, size - 2f * inset,
                        innerRadius, fallbackColor, RectCornerRoundMask.ALL);
            }
            graphics.drawCenteredText(initial, x() + size * 0.5f, y() + (size - graphics.getFontCapHeight()) * 0.5f,
                    0xFFFFFFFF, false, true);
        }
        if (userStatusDotColorSupplier != null) {
            float dotX = x() + size * 0.75f;
            float dotY = y() + size * 0.75f;
            graphics.fillRoundedRect(dotX - 1f, dotY - 1f, 8f, 8f, 4f, 0xFF1A1E24, RectCornerRoundMask.ALL);
            graphics.fillRoundedRect(dotX, dotY, 6f, 6f, 3f, userStatusDotColorSupplier.getAsInt(), RectCornerRoundMask.ALL);
        }
    }
}
