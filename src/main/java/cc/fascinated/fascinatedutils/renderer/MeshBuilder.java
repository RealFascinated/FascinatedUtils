package cc.fascinated.fascinatedutils.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.NonNull;

/**
 * Frame-level flush boundaries for batched GUI mesh recording ({@link MeshRenderer}). Replaces the former
 * {@code FascinatedGuiBatch} indirection: {@link #endSegment} flushes pending quads around scissor and immediate-draw
 * changes.
 */
public class MeshBuilder {
    public static final MeshBuilder INSTANCE = new MeshBuilder();

    private MeshBuilder() {
    }

    /**
     * Root mesh scope for a GUI frame (pairs with {@link #endFrame}).
     *
     * @param drawContext     active draw context
     * @param minecraftClient client instance (reserved for future device hooks)
     */
    public void beginFrame(@NonNull GuiGraphicsExtractor drawContext, @NonNull Minecraft minecraftClient) {
    }

    /**
     * Flush pending mesh quads for the current segment.
     *
     * @param drawContext active draw context
     */
    public void endSegment(@NonNull GuiGraphicsExtractor drawContext) {
        MeshRenderer.INSTANCE.endSegment(drawContext);
    }

    /**
     * Final flush at end of GUI frame (same as {@link #endSegment} for this implementation).
     *
     * @param drawContext active draw context
     */
    public void endFrame(@NonNull GuiGraphicsExtractor drawContext) {
        MeshRenderer.INSTANCE.endSegment(drawContext);
    }
}
