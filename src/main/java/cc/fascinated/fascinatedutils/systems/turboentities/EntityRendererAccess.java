package cc.fascinated.fascinatedutils.systems.turboentities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public interface EntityRendererAccess {

    boolean alumite$affectedByCulling(Entity entity);

    AABB alumite$getCullingBox(Entity entity);
}
