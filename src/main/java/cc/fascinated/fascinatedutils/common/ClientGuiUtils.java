package cc.fascinated.fascinatedutils.common;

import cc.fascinated.fascinatedutils.gui.UIScale;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;

/**
 * Utility subset for custom GUI: framebuffer window size and orthographic projection management.
 */
public class ClientGuiUtils {
    private static final float NEAR_PLANE = -10f;
    private static final float FAR_PLANE = 100f;
    private static final ProjectionMatrixBuffer PROJECTION_MATRIX = new ProjectionMatrixBuffer("fascinatedutils:gui_projection");

    public static int getWindowWidth() {
        return Minecraft.getInstance().getWindow().getWidth();
    }

    public static int getWindowHeight() {
        return Minecraft.getInstance().getWindow().getHeight();
    }

    /**
     * Sets the orthographic projection to fixed scale-2 UI dimensions.
     */
    public static void scale2Projection() {
        RenderSystem.setProjectionMatrix(PROJECTION_MATRIX.getBuffer(projectionMatrixForFramebuffer(UIScale.uiWidth(), UIScale.uiHeight())), ProjectionType.ORTHOGRAPHIC);
    }

    /**
     * Restores the vanilla GUI-scaled projection from the provided dimensions.
     *
     * @param guiScaledWidth  vanilla GUI-scaled canvas width
     * @param guiScaledHeight vanilla GUI-scaled canvas height
     */
    public static void vanillaProjection(float guiScaledWidth, float guiScaledHeight) {
        RenderSystem.setProjectionMatrix(PROJECTION_MATRIX.getBuffer(projectionMatrixForFramebuffer(guiScaledWidth, guiScaledHeight)), ProjectionType.PERSPECTIVE);
    }

    /**
     * Current orthographic projection matrix for the given framebuffer size.
     *
     * @param width  canvas width
     * @param height canvas height
     * @return projection matrix
     */
    public static Matrix4f projectionMatrixForFramebuffer(float width, float height) {
        return new Matrix4f().setOrtho(0f, width, height, 0f, NEAR_PLANE, FAR_PLANE);
    }
}
