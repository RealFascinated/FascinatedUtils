package cc.fascinated.fascinatedutils.mixin.bossbar;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public interface BossHealthOverlayEventsAccessor {

    @Accessor("events")
    Map<UUID, BossEvent> getEvents();
}
