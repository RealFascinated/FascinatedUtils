package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.common.culling.Cullable;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {Entity.class, BlockEntity.class, Particle.class})
public class CullableMixin implements Cullable {

    @Unique
    private boolean alumite$culled = false;

    @Unique
    private boolean alumite$outOfCamera = false;

    @Override
    public void alumite$setCulled(boolean culled) {
        this.alumite$culled = culled;
    }

    @Override
    public boolean alumite$isCulled() {
        return alumite$culled;
    }

    @Override
    public void alumite$setOutOfCamera(boolean outOfCamera) {
        this.alumite$outOfCamera = outOfCamera;
    }

    @Override
    public boolean alumite$isOutOfCamera() {
        return alumite$outOfCamera;
    }
}
