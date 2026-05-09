package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PixelSize;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UIScale;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import net.minecraft.resources.Identifier;

import java.util.function.Function;
import java.util.function.Supplier;

public class ImageNode extends PositionedNode {

    private Supplier<Identifier> textureSupplier;
    private Supplier<PixelSize> naturalSizeSupplier;
    private Supplier<Integer> tintSupplier = () -> 0xFFFFFFFF;
    private int cornerRadius = 0;

    public ImageNode setTexture(Identifier texture) {
        textureSupplier = () -> texture;
        return this;
    }

    public ImageNode setTextureSupplier(Supplier<Identifier> textureSupplier) {
        this.textureSupplier = textureSupplier;
        return this;
    }

    /**
     * Provides the natural pixel dimensions {@code [width, height]} of the texture. When set, the
     * image is rendered with aspect-ratio-preserving scaling that never upscales beyond its natural
     * size, centred within the node bounds.
     *
     * @param naturalSizeSupplier supplier returning {@code [width, height]}, or {@code null} while dimensions are unknown
     */
    public ImageNode setNaturalSizeSupplier(Supplier<PixelSize> naturalSizeSupplier) {
        this.naturalSizeSupplier = naturalSizeSupplier;
        return this;
    }

    public ImageNode setTintArgb(int tintArgb) {
        tintSupplier = () -> tintArgb;
        return this;
    }

    public ImageNode setTintSupplier(Supplier<Integer> tintSupplier) {
        this.tintSupplier = tintSupplier == null ? () -> 0xFFFFFFFF : tintSupplier;
        return this;
    }

    public ImageNode setTintResolver(Function<UiTheme, Integer> tintResolver) {
        this.tintSupplier = tintResolver == null ? () -> 0xFFFFFFFF : () -> tintResolver.apply(UiThemeRepository.get());
        return this;
    }

    public ImageNode setCornerRadius(int cornerRadius) {
        this.cornerRadius = Math.max(0, cornerRadius);
        return this;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        Identifier texture = textureSupplier != null ? textureSupplier.get() : null;
        if (texture == null) {
            return;
        }
        int tintArgb = tintSupplier.get();
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        if (naturalSizeSupplier != null) {
            PixelSize naturalSize = naturalSizeSupplier.get();
            if (naturalSize == null || naturalSize.width() <= 0 || naturalSize.height() <= 0) {
                return;
            }
            // naturalSize is in physical pixels; the UI renders 1 logical pixel = 2 physical pixels,
            // so divide by UIScale.SCALE to get the natural display size in logical coordinates.
            float naturalW = naturalSize.width() / UIScale.SCALE;
            float naturalH = naturalSize.height() / UIScale.SCALE;
            float scale = Math.min(1f, Math.min((float) width / naturalW, (float) height / naturalH));
            int drawW = Math.round(naturalW * scale);
            int drawH = Math.round(naturalH * scale);
            int drawX = posX + (width - drawW) / 2;
            int drawY = posY + (height - drawH) / 2;
            if (cornerRadius > 0) {
                renderFrame.drawRoundedTexture(texture, drawX, drawY, drawW, drawH, cornerRadius, tintArgb);
            } else {
                renderFrame.drawTexture(texture, drawX, drawY, drawW, drawH, tintArgb);
            }
            return;
        }
        if (cornerRadius > 0) {
            renderFrame.drawRoundedTexture(texture, posX, posY, width, height, cornerRadius, tintArgb);
        } else {
            renderFrame.drawTexture(texture, posX, posY, width, height, tintArgb);
        }
    }
}
