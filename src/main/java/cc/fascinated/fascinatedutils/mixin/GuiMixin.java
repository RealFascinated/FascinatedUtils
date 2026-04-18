package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ScoreboardModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.TitlesModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;

@Mixin(Gui.class)
public class GuiMixin {

    @ModifyArg(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V", ordinal = 2))
    private Component fascinatedutils$modifyScoreText(Component text) {
        Optional<ScoreboardModule> scoreboardOptional = ModuleRegistry.INSTANCE.getModule(ScoreboardModule.class);
        if (scoreboardOptional.isEmpty() || !scoreboardOptional.get().isEnabled()) {
            return text;
        }
        ScoreboardModule module = scoreboardOptional.get();
        if (module.getHideScoreboardLines().isEnabled()) {
            return Component.empty();
        }
        return text;
    }

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
