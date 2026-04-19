package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.turboentities.EntityRendererAccess;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> implements EntityRendererAccess {

    @Shadow
    protected abstract boolean affectedByCulling(T entity);

    @Shadow
    protected abstract AABB getBoundingBoxForCulling(T entity);

    @Override
    @SuppressWarnings("unchecked")
    public boolean fascinatedutils$affectedByCulling(Entity entity) {
        return affectedByCulling((T) entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public AABB fascinatedutils$getCullingBox(Entity entity) {
        return getBoundingBoxForCulling((T) entity);
    }
}
