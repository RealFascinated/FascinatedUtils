package cc.fascinated.fascinatedutils.common;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;

/**
 * Utility subset for custom GUI: framebuffer window size and orthographic projection toggles.
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
     * Orthographic projection over the full framebuffer (unscaledProjection).
     */
    public static void unscaledProjection() {
        Minecraft client = Minecraft.getInstance();
        float width = client.getWindow().getWidth();
        float height = client.getWindow().getHeight();
        RenderSystem.setProjectionMatrix(PROJECTION_MATRIX.getBuffer(projectionMatrixForFramebuffer(width, height)), ProjectionType.ORTHOGRAPHIC);
    }

    /**
     * Restore a perspective projection sized like scaled GUI dimensions (scaledProjection).
     */
    public static void scaledProjection() {
        Minecraft client = Minecraft.getInstance();
        float width = client.getWindow().getWidth() / (float) client.getWindow().getGuiScale();
        float height = client.getWindow().getHeight() / (float) client.getWindow().getGuiScale();
        RenderSystem.setProjectionMatrix(PROJECTION_MATRIX.getBuffer(projectionMatrixForFramebuffer(width, height)), ProjectionType.PERSPECTIVE);
    }

    /**
     * Current orthographic projection matrix for the given framebuffer size.
     *
     * @param width  framebuffer width
     * @param height framebuffer height
     * @return projection matrix
     */
    public static Matrix4f projectionMatrixForFramebuffer(float width, float height) {
        return new Matrix4f().setOrtho(0f, width, height, 0f, NEAR_PLANE, FAR_PLANE);
    }
}
