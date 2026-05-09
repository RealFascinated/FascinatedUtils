package cc.fascinated.fascinatedutils.gui2.node.social.channel;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.ImageNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;

public class CloseChannelNode extends PositionedNode {

    static final int SIZE = 14;
    private static final int ICON_INSET = 2;
    private static final int CORNER_RADIUS = 3;

    private final DmChannel channel;
    private boolean hovered;

    public CloseChannelNode(DmChannel channel) {
        this.channel = channel;
        size(SIZE);

        addChild(new RectNode()
                .setFillSupplier(() -> hovered ? UiThemeRepository.get().rowHoverFill() : 0)
                .setCornerRadius(CORNER_RADIUS)
                .full());

        addChild(new ImageNode()
                .setTextureSupplier(() -> ModUiTextures.CLOSE.getId())
                .setTintSupplier(() -> hovered ? UiThemeRepository.get().textPrimary() : UiThemeRepository.get().textMuted())
                .left(ICON_INSET).right(ICON_INSET).top(ICON_INSET).bottom(ICON_INSET));
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean onPointerEnter(float pointerX, float pointerY) {
        hovered = true;
        return false;
    }

    @Override
    public boolean onPointerLeave(float pointerX, float pointerY) {
        hovered = false;
        return false;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                channel.hide();
            } catch (Exception ignored) {
            }
        });
        return true;
    }
}
