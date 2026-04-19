package cc.fascinated.fascinatedutils.mixin.turboentities;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import cc.fascinated.fascinatedutils.turboentities.CullTask;
import cc.fascinated.fascinatedutils.common.culling.Cullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelTickMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$skipCulledEntityTick(Entity entity, CallbackInfo info) {
        if (!SettingsRegistry.INSTANCE.getSettings().getTurboEntities().isEnabled()) {
            return;
        }

        CullTask cullTask = Client.TURBO_ENTITIES.getCullTask();
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
        if (entity instanceof LivingEntity living && living.hurtTime > 0) {
            living.hurtTime--;
        }
        Client.TURBO_ENTITIES.incrementSkippedEntityTicks();
        info.cancel();
    }
}
