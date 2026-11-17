package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.core.PixelSize;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class TextureNode extends PositionedNode<TextureNode> {

    /**
     * How the texture is fitted into the node's bounds.
     */
    public enum FitMode {
        /**
         * Stretches the texture over the whole node, ignoring its aspect ratio.
         */
        STRETCH,
        /**
         * Scales the texture to fit inside the node, keeping its aspect ratio. Needs the texture's pixel size and falls
         * back to {@link #STRETCH} without it.
         */
        CONTAIN,
        /**
         * Scales the texture to fill the node, keeping its aspect ratio and clipping the overflow to the node's
         * rectangle. Needs the texture's pixel size and falls back to {@link #STRETCH} without it. Rounded corners
         * apply to the node's rectangle.
         */
        COVER
    }

    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    private Supplier<Identifier> textureSupplier;
    private Supplier<TextureManager.LoadedTexture> loadedSupplier;
    private ToIntFunction<UiTheme> tintResolver = theme -> OPAQUE_WHITE;
    private int cornerRadius = 0;
    private FitMode fitMode = FitMode.STRETCH;
    private LoadingSpinnerNode spinnerNode;

    public TextureNode setTexture(Identifier texture) {
        textureSupplier = () -> texture;
        loadedSupplier = null;
        return this;
    }

    public TextureNode setTexture(TextureManager.LoadedTexture texture) {
        return setLoadedTextureSupplier(() -> texture);
    }

    /**
     * Shows a texture that may still be loading. The supplier is read once per frame; it supplies the texture id and
     * the pixel size used for {@link FitMode#CONTAIN} and {@link FitMode#COVER}.
     *
     * @param loadedSupplier supplies the loaded texture, or null while it is not available
     * @return this node
     */
    public TextureNode setLoadedTextureSupplier(Supplier<TextureManager.LoadedTexture> loadedSupplier) {
        this.loadedSupplier = loadedSupplier;
        textureSupplier = null;
        return this;
    }

    public TextureNode setTintResolver(ToIntFunction<UiTheme> tintResolver) {
        this.tintResolver = tintResolver == null ? theme -> OPAQUE_WHITE : tintResolver;
        return this;
    }

    public TextureNode setCornerRadius(int cornerRadius) {
        this.cornerRadius = Math.max(0, cornerRadius);
        return this;
    }

    /**
     * Sets how the texture is fitted into the node.
     *
     * @param fitMode the fit mode; null means {@link FitMode#STRETCH}
     * @return this node
     */
    public TextureNode setFit(FitMode fitMode) {
        this.fitMode = fitMode == null ? FitMode.STRETCH : fitMode;
        return this;
    }

    public TextureNode setShowLoader(boolean showLoader) {
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

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        TextureManager.LoadedTexture loaded = loadedSupplier != null ? loadedSupplier.get() : null;
        Identifier textureId = loaded != null ? loaded.id() : textureSupplier != null ? textureSupplier.get() : null;
        if (spinnerNode != null) {
            spinnerNode.setVisible(textureId == null);
        }
        if (textureId == null) {
            return;
        }
        int tintArgb = tintResolver.applyAsInt(renderFrame.theme());
        int positionX = bounds().positionX();
        int positionY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        PixelSize pixelSize = loaded != null ? loaded.pixelSize() : null;
        if (fitMode == FitMode.STRETCH || pixelSize == null || pixelSize.width() <= 0 || pixelSize.height() <= 0) {
            drawTexture(renderFrame, textureId, positionX, positionY, width, height, tintArgb);
            return;
        }
        float widthScale = (float) width / pixelSize.width();
        float heightScale = (float) height / pixelSize.height();
        float drawScale = fitMode == FitMode.COVER ? Math.max(widthScale, heightScale) : Math.min(widthScale, heightScale);
        int drawWidth = Math.round(pixelSize.width() * drawScale);
        int drawHeight = Math.round(pixelSize.height() * drawScale);
        int drawX = positionX + (width - drawWidth) / 2;
        int drawY = positionY + (height - drawHeight) / 2;
        if (fitMode == FitMode.COVER && cornerRadius > 0) {
            float cropU = (float) width / drawWidth;
            float cropV = (float) height / drawHeight;
            renderFrame.drawRoundedTexture(textureId, positionX, positionY, width, height, cornerRadius, tintArgb, (1f - cropU) * 0.5f, (1f - cropV) * 0.5f, (1f + cropU) * 0.5f, (1f + cropV) * 0.5f);
            return;
        }
        boolean clipped = fitMode == FitMode.COVER;
        if (clipped) {
            renderFrame.pushClip(new ClipRegion(positionX, positionY, width, height));
        }
        drawTexture(renderFrame, textureId, drawX, drawY, drawWidth, drawHeight, tintArgb);
        if (clipped) {
            renderFrame.popClip();
        }
    }

    private void drawTexture(AlumiteGuiRenderer renderFrame, Identifier textureId, int positionX, int positionY, int width, int height, int tintArgb) {
        if (cornerRadius > 0) {
            renderFrame.drawRoundedTexture(textureId, positionX, positionY, width, height, cornerRadius, tintArgb, 0f, 0f, 1f, 1f);
        } else {
            renderFrame.drawTexture(textureId, positionX, positionY, width, height, tintArgb);
        }
    }
}
