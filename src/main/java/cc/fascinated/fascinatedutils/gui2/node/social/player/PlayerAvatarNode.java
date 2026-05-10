package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.caches.UrlTextureCache;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.render.UiText;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class PlayerAvatarNode extends PositionedNode {

    private static final int CORNER_RADIUS = 4;
    private static final int DOT_SIZE = 6;
    private static final int DOT_RING = 2;

    private final Supplier<User> userSupplier;
    private boolean showStatusDot = true;

    public PlayerAvatarNode(int pixelSize, Supplier<User> userSupplier) {
        this.userSupplier = userSupplier;
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

        User user = userSupplier.get();
        String uuid = user != null ? user.minecraftUuid() : null;
        Identifier texture = uuid != null && !uuid.isBlank()
                ? UrlTextureCache.INSTANCE.get("https://mc.fascinated.cc/api/skins/%s/face.png".formatted(uuid), () -> {})
                : null;

        int circleRadius = side / 2;
        if (texture != null) {
            renderFrame.drawRoundedRect(posX, posY, side, side, circleRadius, renderFrame.theme().avatarRing());
            renderFrame.drawRoundedTexture(texture, posX, posY, side, side, circleRadius, renderFrame.theme().textPrimary());
        } else {
            String name = user != null ? user.minecraftName() : null;
            String initial = name == null || name.isBlank() ? "?" : String.valueOf(Character.toUpperCase(name.charAt(0)));
            renderFrame.drawRoundedRect(posX, posY, side, side, CORNER_RADIUS, renderFrame.theme().avatarFallbackFill());
            UiText initialText = UiText.of(initial).color(renderFrame.theme().textPrimary()).bold();
            int textX = posX + (side - initialText.width(renderFrame)) / 2;
            int textY = posY + (side - renderFrame.fontHeight()) / 2;
            initialText.draw(renderFrame, textX, textY);
        }

        if (showStatusDot) {
            int right = posX + side + 2;
            int bottom = posY + side + 2;
            int dotX = right - DOT_RING - DOT_SIZE;
            int dotY = bottom - DOT_RING - DOT_SIZE;
            int ringSize = DOT_SIZE + DOT_RING * 2;
            UserStatus status = user != null ? user.userStatus() : null;
            if (user != null && Alumite.INSTANCE != null && Alumite.INSTANCE.users().selfUser() != null
                    && user.id().equals(Alumite.INSTANCE.users().selfUser().user().id())) {
                status = Alumite.INSTANCE.users().selfUser().preferredUserStatus();
            }
            Identifier icon = status != null ? status.icon() : null;
            if (icon != null) {
                renderFrame.drawRoundedRect(dotX - DOT_RING, dotY - DOT_RING, ringSize, ringSize, ringSize / 2, renderFrame.theme().avatarRing());
                renderFrame.drawTexture(icon, dotX, dotY, DOT_SIZE, DOT_SIZE, 0xFFFFFFFF);
            }
        }
    }
}
