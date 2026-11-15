package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.common.culling.Cullable;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {Entity.class, BlockEntity.class, Particle.class})
public class CullableMixin implements Cullable {

    // Written by the cull thread, read by the render and tick threads.
    @Unique
    private volatile boolean alumite$culled = false;

    @Override
    public void alumite$setCulled(boolean culled) {
        this.alumite$culled = culled;
    }

    @Override
    public boolean alumite$isCulled() {
        return alumite$culled;
    }
}
