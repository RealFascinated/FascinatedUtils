package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.turboentities.Cullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = {Entity.class, BlockEntity.class})
public class CullableMixin implements Cullable {

    @Unique
    private long fascinatedutils$forcedVisibleUntil = 0;

    @Unique
    private boolean fascinatedutils$culled = false;

    @Unique
    private boolean fascinatedutils$outOfCamera = false;

    @Override
    public void fascinatedutils$setTimeout() {
        this.fascinatedutils$forcedVisibleUntil = System.currentTimeMillis() + 1000;
    }

    @Override
    public boolean fascinatedutils$isForcedVisible() {
        return fascinatedutils$forcedVisibleUntil > System.currentTimeMillis();
    }

    @Override
    public void fascinatedutils$setCulled(boolean culled) {
        this.fascinatedutils$culled = culled;
        if (!culled) {
            fascinatedutils$setTimeout();
        }
    }

    @Override
    public boolean fascinatedutils$isCulled() {
        return fascinatedutils$culled;
    }

    @Override
    public void fascinatedutils$setOutOfCamera(boolean outOfCamera) {
        this.fascinatedutils$outOfCamera = outOfCamera;
    }

    @Override
    public boolean fascinatedutils$isOutOfCamera() {
        return fascinatedutils$outOfCamera;
    }
}
