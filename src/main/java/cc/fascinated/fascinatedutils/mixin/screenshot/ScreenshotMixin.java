package cc.fascinated.fascinatedutils.mixin.screenshot;

import cc.fascinated.fascinatedutils.systems.screenshot.Screenshot;
import cc.fascinated.fascinatedutils.systems.screenshot.ScreenshotManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.io.File;
import java.util.function.Consumer;

@Mixin(targets = "net.minecraft.client.Screenshot")
public class ScreenshotMixin {

    @ModifyArgs(
        method = "grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V")
    )
    private static void onGrab(Args args) {
        File workDir = args.get(0);
        Consumer<Component> originalCallback = args.get(4);
        args.set(4, (Consumer<Component>) component -> {
            originalCallback.accept(component);
            if (!(component.getContents() instanceof TranslatableContents contents)) {
                return;
            }
            if (!contents.getKey().equals("screenshot.success") || contents.getArgs().length == 0) {
                return;
            }
            if (!(contents.getArgs()[0] instanceof Component filenameComponent)) {
                return;
            }
            String filename = filenameComponent.getString();
            File screenshotFile = new File(new File(workDir, "screenshots"), filename);
            Minecraft.getInstance().execute(() -> ScreenshotManager.addScreenshot(new Screenshot(screenshotFile.toPath())));
        });
    }
}
