package cc.fascinated.fascinatedutils.mixin.freelook;

import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.FreelookModule;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraFreelookMixin {

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float fascinatedutils$redirectViewYRot(Entity entity, float partialTick) {
        FreelookModule module = ModuleRegistry.INSTANCE.getModule(FreelookModule.class).filter(Module::isEnabled).filter(FreelookModule::isFreelookActive).orElse(null);
        if (module == null) {
            return entity.getViewYRot(partialTick);
        }
        return entity.getViewYRot(partialTick) + module.getFreelookYaw();
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float fascinatedutils$redirectViewXRot(Entity entity, float partialTick) {
        FreelookModule module = ModuleRegistry.INSTANCE.getModule(FreelookModule.class).filter(Module::isEnabled).filter(FreelookModule::isFreelookActive).orElse(null);
        if (module == null) {
            return entity.getViewXRot(partialTick);
        }
        return Mth.clamp(entity.getViewXRot(partialTick) + module.getFreelookPitch(), -90f, 90f);
    }
}
