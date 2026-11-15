package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.gui.core.PixelSize;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UIScale;
import cc.fascinated.fascinatedutils.gui.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.systems.TextureManager;
import net.minecraft.resources.Identifier;

import java.util.function.Function;
import java.util.function.Supplier;

public class TextureNode extends PositionedNode<TextureNode> {

    private Supplier<Identifier> textureSupplier;
    private Supplier<PixelSize> naturalSizeSupplier;
    private Supplier<Integer> tintSupplier = () -> 0xFFFFFFFF;
    private int cornerRadius = 0;
    private boolean useNaturalSize = false;
    private boolean cover = false;
    private boolean showLoader = false;
    private LoadingSpinnerNode spinnerNode;

    public TextureNode setTexture(Identifier texture) {
        textureSupplier = () -> texture;
        return this;
    }

    public TextureNode setTexture(TextureManager.LoadedTexture texture) {
        textureSupplier = () -> texture != null ? texture.id() : null;
        naturalSizeSupplier = () -> texture != null ? texture.pixelSize() : null;
        return this;
    }

    public TextureNode setLoadedTextureSupplier(Supplier<TextureManager.LoadedTexture> loadedSupplier) {
        textureSupplier = () -> {
            TextureManager.LoadedTexture tex = loadedSupplier != null ? loadedSupplier.get() : null;
            return tex != null ? tex.id() : null;
        };
        naturalSizeSupplier = () -> {
            TextureManager.LoadedTexture tex = loadedSupplier != null ? loadedSupplier.get() : null;
            return tex != null ? tex.pixelSize() : null;
        };
        return this;
    }

    public TextureNode setTintResolver(Function<UiTheme, Integer> tintResolver) {
        this.tintSupplier = tintResolver == null ? () -> 0xFFFFFFFF : () -> tintResolver.apply(UiThemeRepository.get());
        return this;
    }

    public TextureNode setCornerRadius(int cornerRadius) {
        this.cornerRadius = Math.max(0, cornerRadius);
        return this;
    }

    public TextureNode naturalSize() {
        this.useNaturalSize = true;
        return this;
    }

    public TextureNode setShowLoader(boolean showLoader) {
        this.showLoader = showLoader;
        if (showLoader && spinnerNode == null) {
            spinnerNode = new LoadingSpinnerNode();
            spinnerNode.setVisible(false);
            addChild(spinnerNode);
        } else if (!showLoader && spinnerNode != null) {
            removeChild(spinnerNode);
            spinnerNode = null;
        }
        return this;
    }

    /**
     * When enabled, the texture fills the entire node bounds (cover/crop scaling) rather than
     * fitting within them. Overflow is clipped to the node bounds.
     */
    public TextureNode cover() {
        this.cover = true;
        return this;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        Identifier textureId = textureSupplier != null ? textureSupplier.get() : null;
        if (showLoader && spinnerNode != null) {
            spinnerNode.setVisible(textureId == null);
        }
        if (textureId == null) {
            return;
        }
        int tintArgb = tintSupplier.get();
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        if (this.useNaturalSize) {
            PixelSize naturalSize = naturalSizeSupplier != null ? naturalSizeSupplier.get() : null;
            if (naturalSize == null || naturalSize.width() <= 0 || naturalSize.height() <= 0) {
                return;
            }
            // naturalSize is in physical pixels; the UI renders 1 logical pixel = 2 physical pixels,
            // so divide by UIScale.SCALE to get the natural display size in logical coordinates.
            float naturalW = naturalSize.width() / UIScale.screenScale();
            float naturalH = naturalSize.height() / UIScale.screenScale();
            float drawScale = cover
                    ? Math.max((float) width / naturalW, (float) height / naturalH)
                    : Math.min((float) width / naturalW, (float) height / naturalH);
            int drawW = Math.round(naturalW * drawScale);
            int drawH = Math.round(naturalH * drawScale);
            int drawX = posX + (width - drawW) / 2;
            int drawY = posY + (height - drawH) / 2;
            if (cover) {
                renderFrame.pushClip(new ClipRegion(posX, posY, width, height));
            }
            if (cornerRadius > 0) {
                renderFrame.drawRoundedTexture(textureId, drawX, drawY, drawW, drawH, cornerRadius, tintArgb);
            } else {
                renderFrame.drawTexture(textureId, drawX, drawY, drawW, drawH, tintArgb);
            }
            if (cover) {
                renderFrame.popClip();
            }
            return;
        }
        if (cornerRadius > 0) {
            renderFrame.drawRoundedTexture(textureId, posX, posY, width, height, cornerRadius, tintArgb);
        } else {
            renderFrame.drawTexture(textureId, posX, posY, width, height, tintArgb);
        }
    }
}
