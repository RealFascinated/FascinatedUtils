package cc.fascinated.fascinatedutils.mixin.culling;

import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the raw-coordinate frustum box test, so cull passes can test boxes without allocating an AABB per subject.
 */
@Mixin(Frustum.class)
public interface FrustumAccess {

    /**
     * Tests a box against the frustum.
     *
     * @param minX the box minimum x
     * @param minY the box minimum y
     * @param minZ the box minimum z
     * @param maxX the box maximum x
     * @param maxY the box maximum y
     * @param maxZ the box maximum z
     * @return the JOML intersection result; -2 (inside) and -1 (intersecting) mean visible
     */
    @Invoker("cubeInFrustum")
    int alumite$cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
