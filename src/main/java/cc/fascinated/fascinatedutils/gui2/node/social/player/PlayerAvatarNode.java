package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.caches.UrlTextureCache;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import net.minecraft.resources.Identifier;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Square avatar node that renders a Minecraft player skin face (via URL cache) with a rounded
 * status-dot indicator in the bottom-right corner.
 *
 * <p>When the skin texture is not yet available the node renders a solid colored tile with the
 * player's initial letter as fallback.
 */
public class PlayerAvatarNode extends PositionedNode {

    private static final int CORNER_RADIUS = 4;
    private static final int DOT_SIZE = 6;
    private static final int DOT_RING = 2;

    private final Supplier<String> minecraftUuidSupplier;
    private final Supplier<String> displayNameSupplier;
    private final IntSupplier statusColorSupplier;
    private boolean showStatusDot = true;

    public PlayerAvatarNode(int pixelSize, Supplier<String> minecraftUuidSupplier, Supplier<String> displayNameSupplier, IntSupplier statusColorSupplier) {
        this.minecraftUuidSupplier = minecraftUuidSupplier;
        this.displayNameSupplier = displayNameSupplier;
        this.statusColorSupplier = statusColorSupplier;
        size(pixelSize);
    }

    public PlayerAvatarNode setShowStatusDot(boolean showStatusDot) {
        this.showStatusDot = showStatusDot;
        return this;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int side = bounds().width();

        String uuid = minecraftUuidSupplier.get();
        Identifier texture = uuid != null && !uuid.isBlank()
                ? UrlTextureCache.INSTANCE.get(uuid, "https://mc.fascinated.cc/api/skins/%s/face.png".formatted(uuid), () -> {})
                : null;

        int circleRadius = side / 2;
        if (texture != null) {
            renderFrame.drawRoundedRect(posX, posY, side, side, circleRadius, renderFrame.theme().avatarRing());
            renderFrame.drawRoundedTexture(texture, posX, posY, side, side, circleRadius, renderFrame.theme().textPrimary());
        } else {
            String name = displayNameSupplier.get();
            String initial = name == null || name.isBlank() ? "?" : String.valueOf(Character.toUpperCase(name.charAt(0)));
            renderFrame.drawRoundedRect(posX, posY, side, side, CORNER_RADIUS, renderFrame.theme().avatarFallbackFill());
            int textWidth = renderFrame.measureTextWidth(initial, true);
            int textX = posX + (side - textWidth) / 2;
            int textY = posY + (side - renderFrame.fontHeight()) / 2;
            renderFrame.drawText(initial, textX, textY, renderFrame.theme().textPrimary(), false, true);
        }

        if (showStatusDot) {
            int right = posX + side + 2;
            int bottom = posY + side + 2;
            int dotX = right - DOT_RING - DOT_SIZE;
            int dotY = bottom - DOT_RING - DOT_SIZE;
            int ringSize = DOT_SIZE + DOT_RING * 2;
            renderFrame.drawRoundedRect(dotX - DOT_RING, dotY - DOT_RING, ringSize, ringSize, ringSize / 2, renderFrame.theme().avatarRing());
            renderFrame.drawRoundedRect(dotX, dotY, DOT_SIZE, DOT_SIZE, DOT_SIZE / 2, statusColorSupplier.getAsInt());
        }
    }
}
