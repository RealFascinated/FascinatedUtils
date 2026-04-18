package cc.fascinated.fascinatedutils.gui.renderer;

import cc.fascinated.fascinatedutils.common.EntityUtils;
import cc.fascinated.fascinatedutils.renderer.MeshBuilder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draw-context helpers for 3D-style GUI content (living entities in slots, previews, etc.) via vanilla
 * {@link GuiGraphics#submitEntityRenderState}. Batched 2D mesh quads from {@link MeshBuilder} are flushed before each entity draw so
 * depth and ordering match {@link GuiRenderer} behaviour.
 */
public class Renderer3D {
    private static final float GUI_ENTITY_BASE_SCALE = 0.0625f;
    private static final float GUI_ENTITY_YAW_DEGREES = -45f;
    /**
     * Target visual span in block-like units for a humanoid in the GUI slot; the effective scale divisor also uses
     * {@link #GUI_ENTITY_COMBINED_SPAN_WEIGHT} so wide models and the slot yaw still fit inside {@code size}.
     */
    private static final float GUI_ENTITY_REFERENCE_SPAN = 1.82f;
    /**
     * Weight on {@code width + height} from {@link LivingEntityRenderState} so flat wide mobs are not sized only by
     * their shorter axis (which lets limbs or wings clip the slot edges after rotation).
     */
    private static final float GUI_ENTITY_COMBINED_SPAN_WEIGHT = 0.78f;

    private final GuiGraphicsExtractor graphics;

    public Renderer3D(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    private void flushBatchedGuiBeforeEntity() {
        MeshBuilder.INSTANCE.endSegment(graphics);
        MeshBuilder.INSTANCE.beginSegment(graphics);
    }

    /**
     * Draw a living entity in a GUI square slot. Large, wide, or visually bulky mobs are scaled down so the model
     * fits the slot; small mobs are not enlarged past {@code size}.
     *
     * @param entity    living entity to render
     * @param positionX left origin in logical pixels (current matrix stack)
     * @param positionY top origin in logical pixels
     * @param size      square side length in logical pixels
     */
    public void drawGuiEntity(LivingEntity entity, float positionX, float positionY, float size) {
        drawGuiEntity(entity, positionX, positionY, size, GUI_ENTITY_YAW_DEGREES);
    }

    /**
     * Draw a living entity in a GUI square slot with a custom body yaw for the preview.
     *
     * @param entity     living entity to render
     * @param positionX  left origin in logical pixels
     * @param positionY  top origin in logical pixels
     * @param size       square side length in logical pixels
     * @param yawDegrees yaw applied to the entity render state for the preview
     */
    public void drawGuiEntity(LivingEntity entity, float positionX, float positionY, float size, float yawDegrees) {
        if (size < 1e-3f) {
            return;
        }
        flushBatchedGuiBeforeEntity();
        Matrix3x2fStack matrices = graphics.pose();
        float transformedX = matrices.m00() * positionX + matrices.m10() * positionY + matrices.m20();
        float transformedY = matrices.m01() * positionX + matrices.m11() * positionY + matrices.m21();
        float axisX = matrices.m00();
        float axisY = matrices.m01();
        float scaledSize = size * Math.max(0.001f, (float) Math.hypot(axisX, axisY));
        int x1 = Mth.floor(transformedX);
        int y1 = Mth.floor(transformedY);
        int viewportExtent = Math.max(1, Math.round(scaledSize));
        int x2 = x1 + viewportExtent;
        int y2 = y1 + viewportExtent;
        EntityRenderState entityRenderState = EntityUtils.buildEntityRenderState(entity, yawDegrees);
        float modelScale = viewportExtent;
        if (entityRenderState instanceof LivingEntityRenderState livingEntityRenderState) {
            float width = Math.max(0.01f, livingEntityRenderState.boundingBoxWidth);
            float height = Math.max(0.01f, livingEntityRenderState.boundingBoxHeight);
            float stateAxis = Math.max(width, height);
            float entityAxis = Math.max(0.01f, Math.max(entity.getBbWidth(), entity.getBbHeight()));
            float combinedSpan = GUI_ENTITY_COMBINED_SPAN_WEIGHT * (width + height);
            float maxSpan = Math.max(Math.max(stateAxis, entityAxis), combinedSpan);
            modelScale = viewportExtent * Math.min(1f, GUI_ENTITY_REFERENCE_SPAN / maxSpan);
        }
        Quaternionf facing = new Quaternionf().rotateZ((float) Math.PI);
        Vector3f translation = new Vector3f(0f, entityRenderState.boundingBoxHeight * 0.5f + GUI_ENTITY_BASE_SCALE, 0f);
        graphics.entity(entityRenderState, modelScale, translation, facing, new Quaternionf(), x1, y1, x2, y2);
    }
}
