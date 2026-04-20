package cc.fascinated.fascinatedutils.mixin.title;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.TitlesModule;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;

@Mixin(Gui.class)
public class GuiMixin {

    @ModifyArgs(method = "extractTitle", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;", ordinal = 0))
    private void fascinatedutils$scaleTitle(Args args) {
        Optional<TitlesModule> titlesOptional = ModuleRegistry.INSTANCE.getModule(TitlesModule.class);
        if (titlesOptional.isEmpty() || !titlesOptional.get().isEnabled()) {
            return;
        }
        TitlesModule module = titlesOptional.get();
        SliderSetting scaleTitleAndSubtitle = module.getScaleTitleAndSubtitle();
        if (!scaleTitleAndSubtitle.isDefault()) {
            float value = ((float) args.get(0)) * scaleTitleAndSubtitle.getValue().floatValue();
            args.set(0, value);
            args.set(1, value);
        }
    }

    @ModifyArgs(method = "extractTitle", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;", ordinal = 1))
    private void fascinatedutils$scaleSubtitle(Args args) {
        Optional<TitlesModule> titlesOptional = ModuleRegistry.INSTANCE.getModule(TitlesModule.class);
        if (titlesOptional.isEmpty() || !titlesOptional.get().isEnabled()) {
            return;
        }
        TitlesModule module = titlesOptional.get();
        SliderSetting scaleTitleAndSubtitle = module.getScaleTitleAndSubtitle();
        if (!scaleTitleAndSubtitle.isDefault()) {
            float value = ((float) args.get(0)) * scaleTitleAndSubtitle.getValue().floatValue();
            args.set(0, value);
            args.set(1, value);
        }
    }
}
