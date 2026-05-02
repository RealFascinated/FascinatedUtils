package cc.fascinated.fascinatedutils.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.NonNull;

/**
 * Frame-level segment boundaries for batched GUI mesh recording ({@link MeshRenderer}). Replaces the former
 * {@code FascinatedGuiBatch} indirection: each segment pairs {@link #beginSegment} / {@link #endSegment} around scissor
 * and immediate-draw flushes.
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
     * Open a mesh recording segment (typically after a scissor push or before immediate draws that must sort above
     * pending quads).
     *
     * @param drawContext active draw context
     */
    public void beginSegment(@NonNull GuiGraphicsExtractor drawContext) {
        MeshRenderer.INSTANCE.beginSegment(drawContext);
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
