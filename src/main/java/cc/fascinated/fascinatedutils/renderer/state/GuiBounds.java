package cc.fascinated.fascinatedutils.renderer.state;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * Screen-space bounds for custom GUI render states, as consumed by vanilla's layering in
 * {@code GuiRenderState}.
 */
@UtilityClass
public class GuiBounds {

    /**
     * Transforms a local rectangle by the pose and clips it to the scissor.
     *
     * <p>The result is rounded outward so it always covers every pixel the element can touch; vanilla only uses it to
     * decide which elements overlap, so over-covering is safe and under-covering would break paint order.
     *
     * @param pose    the pose applied to the local rectangle
     * @param localMinX left of the local rectangle
     * @param localMinY top of the local rectangle
     * @param localMaxX right of the local rectangle
     * @param localMaxY bottom of the local rectangle
     * @param scissor the active scissor in screen space, or null for none
     * @return the clipped screen bounds, or null when nothing is visible so vanilla drops the element
     */
    @Nullable
    public static ScreenRectangle of(Matrix3x2fc pose, float localMinX, float localMinY, float localMaxX, float localMaxY, @Nullable ScreenRectangle scissor) {
        float minX;
        float maxX;
        float minY;
        float maxY;
        if (pose.m01() == 0f && pose.m10() == 0f) {
            // Translations and scales map each axis on its own, so two corners cover the rectangle.
            float left = pose.m00() * localMinX + pose.m20();
            float right = pose.m00() * localMaxX + pose.m20();
            float top = pose.m11() * localMinY + pose.m21();
            float bottom = pose.m11() * localMaxY + pose.m21();
            minX = Math.min(left, right);
            maxX = Math.max(left, right);
            minY = Math.min(top, bottom);
            maxY = Math.max(top, bottom);
        }
        else {
            float cornerX0 = pose.m00() * localMinX + pose.m10() * localMinY + pose.m20();
            float cornerY0 = pose.m01() * localMinX + pose.m11() * localMinY + pose.m21();
            float cornerX1 = pose.m00() * localMaxX + pose.m10() * localMinY + pose.m20();
            float cornerY1 = pose.m01() * localMaxX + pose.m11() * localMinY + pose.m21();
            float cornerX2 = pose.m00() * localMinX + pose.m10() * localMaxY + pose.m20();
            float cornerY2 = pose.m01() * localMinX + pose.m11() * localMaxY + pose.m21();
            float cornerX3 = pose.m00() * localMaxX + pose.m10() * localMaxY + pose.m20();
            float cornerY3 = pose.m01() * localMaxX + pose.m11() * localMaxY + pose.m21();
            minX = Math.min(Math.min(cornerX0, cornerX1), Math.min(cornerX2, cornerX3));
            maxX = Math.max(Math.max(cornerX0, cornerX1), Math.max(cornerX2, cornerX3));
            minY = Math.min(Math.min(cornerY0, cornerY1), Math.min(cornerY2, cornerY3));
            maxY = Math.max(Math.max(cornerY0, cornerY1), Math.max(cornerY2, cornerY3));
        }
        int left = Mth.floor(minX);
        int top = Mth.floor(minY);
        int right = Mth.ceil(maxX);
        int bottom = Mth.ceil(maxY);
        if (scissor != null) {
            left = Math.max(left, scissor.left());
            top = Math.max(top, scissor.top());
            right = Math.min(right, scissor.right());
            bottom = Math.min(bottom, scissor.bottom());
        }
        if (left >= right || top >= bottom) {
            return null;
        }
        return new ScreenRectangle(left, top, right - left, bottom - top);
    }
}
