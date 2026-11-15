package cc.fascinated.fascinatedutils.mixin.title;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.TitlesModule;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;

@Mixin(Gui.class)
public class GuiMixin {

    @Unique
    private static final PerfProbe.Section ALUMITE$RENDER = PerfProbe.INSTANCE.section("title.render");

    @ModifyArgs(method = "extractTitle", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;", ordinal = 0))
    private void alumite$scaleTitle(Args args) {
        long start = ALUMITE$RENDER.begin();
        try {
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
        } finally {
            ALUMITE$RENDER.end(start);
        }
    }

    @ModifyArgs(method = "extractTitle", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;", ordinal = 1))
    private void alumite$scaleSubtitle(Args args) {
        long start = ALUMITE$RENDER.begin();
        try {
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
        } finally {
            ALUMITE$RENDER.end(start);
        }
    }
}
