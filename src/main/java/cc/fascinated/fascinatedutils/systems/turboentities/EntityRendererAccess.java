package cc.fascinated.fascinatedutils.systems.turboentities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public interface EntityRendererAccess {

    boolean fascinatedutils$affectedByCulling(Entity entity);

    AABB fascinatedutils$getCullingBox(Entity entity);
}
