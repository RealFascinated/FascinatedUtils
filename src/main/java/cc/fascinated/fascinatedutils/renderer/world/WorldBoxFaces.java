package cc.fascinated.fascinatedutils.renderer.world;

import lombok.experimental.UtilityClass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@UtilityClass
public class WorldBoxFaces {

    /**
     * Emits the faces of an axis-aligned box as quads, with one ARGB color on every vertex.
     *
     * @param consumer    the buffer the quads are written to
     * @param pose        the pose the vertices are transformed by
     * @param minX        the minimum x of the box
     * @param minY        the minimum y of the box
     * @param minZ        the minimum z of the box
     * @param maxX        the maximum x of the box
     * @param maxY        the maximum y of the box
     * @param maxZ        the maximum z of the box
     * @param argb        the color of every vertex, as ARGB
     * @param includeCaps whether to emit the bottom and top faces as well as the four sides
     */
    public static void emitBoxFaces(VertexConsumer consumer, PoseStack.Pose pose, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int argb, boolean includeCaps) {
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        int alpha = (argb >> 24) & 0xFF;

        if (includeCaps) {
            // -Y (bottom)
            consumer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
            // +Y (top)
            consumer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        }
        // -Z (north)
        consumer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        // +Z (south)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        // -X (west)
        consumer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);
        // +X (east)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
    }
}
