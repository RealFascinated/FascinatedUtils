package cc.fascinated.fascinatedutils.mixin.scoreboard;

import net.minecraft.client.gui.Gui;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Comparator;

@Mixin(Gui.class)
public interface GuiScoreDisplayOrderAccessor {

    @Accessor("SCORE_DISPLAY_ORDER")
    static Comparator<PlayerScoreEntry> scoreDisplayOrder() {
        throw new AssertionError();
    }
}
