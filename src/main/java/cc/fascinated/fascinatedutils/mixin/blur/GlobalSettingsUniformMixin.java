package cc.fascinated.fascinatedutils.mixin.blur;

import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlurModule;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(GlobalSettingsUniform.class)
public abstract class GlobalSettingsUniformMixin {

    @Shadow private GpuBuffer buffer;

    /**
     * Replaces the vanilla UBO write to use a {@code float} for {@code MenuBlurRadius} instead
     * of an {@code int}, enabling the blur shader to receive fractional values for smooth
     * animation. When the Blur module is active the value is {@code progress * strength};
     * otherwise it falls back to the vanilla integer cast as a float so shader behaviour is
     * identical to vanilla when the module is off.
     */
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void fascinatedutils$writeFloatBlurRadius(
            int width, int height, double glintAlpha, long gameTime,
            DeltaTracker deltaTracker, int menuBlurRadius,
            Vec3 cameraPos, boolean useRgss, CallbackInfo ci) {

        BlurModule module = ModuleRegistry.INSTANCE.getModule(BlurModule.class).orElse(null);
        float floatRadius = (module != null && module.isEnabled())
                ? module.getBlurStrength().getValue().floatValue() * module.getProgress()
                : (float) menuBlurRadius;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int cameraX = Mth.floor(cameraPos.x);
            int cameraY = Mth.floor(cameraPos.y);
            int cameraZ = Mth.floor(cameraPos.z);
            ByteBuffer data = Std140Builder.onStack(stack, GlobalSettingsUniform.UBO_SIZE)
                    .putIVec3(cameraX, cameraY, cameraZ)
                    .putVec3(
                            (float) ((double) cameraX - cameraPos.x),
                            (float) ((double) cameraY - cameraPos.y),
                            (float) ((double) cameraZ - cameraPos.z))
                    .putVec2(width, height)
                    .putFloat((float) glintAlpha)
                    .putFloat(((float) (gameTime % 24000L) + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24000.0f)
                    .putFloat(floatRadius)
                    .putInt(useRgss ? 1 : 0)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), data);
        }
        RenderSystem.setGlobalSettingsUniform(this.buffer);
        ci.cancel();
    }
}
