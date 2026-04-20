package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import cc.fascinated.fascinatedutils.turboentities.EntitiesCullTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelTickMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$skipCulledEntityTick(Entity entity, CallbackInfo info) {
        if (!Client.TURBO_ENTITIES.isTurboEntitiesCullEnabled()) {
            return;
        }

        EntitiesCullTask cullTask = Client.TURBO_ENTITIES.getCullTask();
        if (cullTask == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (entity == minecraft.player || entity == minecraft.getCameraEntity()) {
            Client.TURBO_ENTITIES.incrementTickedEntities();
            return;
        }

        if (entity.isVehicle() || entity.isPassenger()) {
            Client.TURBO_ENTITIES.incrementTickedEntities();
            return;
        }

        if (!(entity instanceof Cullable cullable) || !cullable.fascinatedutils$isCulled()) {
            Client.TURBO_ENTITIES.incrementTickedEntities();
            return;
        }

        // Entity is culled — run a minimal tick to keep interpolation and hurt flash correct
        entity.setOldPosAndRot();
        ++entity.tickCount;
        if (entity instanceof LivingEntity living) {
            living.aiStep();
            if (living.hurtTime > 0) {
                living.hurtTime--;
            }
        }
        // the warden sounds are generated clientside instead of serverside, so simulate
        // that part of the code here.
        if (entity instanceof Warden warden) {
            if (minecraft.level.isClientSide() && !warden.isSilent() && warden.tickCount % getWardenHeartBeatDelay(warden) == 0) {
                minecraft.level.playLocalSound(warden.getX(), warden.getY(), warden.getZ(), SoundEvents.WARDEN_HEARTBEAT, warden.getSoundSource(), 5.0F, warden.getVoicePitch(), false);
            }
        }
        Client.TURBO_ENTITIES.incrementSkippedEntityTicks();
        info.cancel();
    }

    /**
     * Copy of that method, since it's private. No need to use an access widener for
     * this
     */
    @Unique
    private int getWardenHeartBeatDelay(Warden warden) {
        float f = (float) warden.getClientAngerLevel() / AngerLevel.ANGRY.getMinimumAnger();
        return 40 - Mth.floor(Mth.clamp(f, 0.0F, 1.0F) * 30.0F);
    }
}
