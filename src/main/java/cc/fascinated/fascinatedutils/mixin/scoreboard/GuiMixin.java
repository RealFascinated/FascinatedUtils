package cc.fascinated.fascinatedutils.mixin.scoreboard;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.ScoreboardModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Unique
    private static final PerfProbe.Section ALUMITE$SIDEBAR = PerfProbe.INSTANCE.section("scoreboard.sidebar");

    @Inject(method = "displayScoreboardSidebar", at = @At(value = "HEAD"), cancellable = true)
    private void alumite$displayScoreboardSidebar(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        long start = ALUMITE$SIDEBAR.begin();
        try {
            if (ModuleRegistry.INSTANCE.isEnabled(ScoreboardModule.class)) {
                ci.cancel();
            }
        } finally {
            ALUMITE$SIDEBAR.end(start);
        }
    }
}
