package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;
import net.minecraft.client.ClientClockManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.util.datafix.fixes.WorldBorderWarningTimeFix;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.WorldClock;

@Getter
public class TimeChangerModule extends Module {

    private final SliderSetting worldTime = SliderSetting.builder().id("world_time").defaultValue(8000f).minValue(0f).maxValue(23000f).step(500f).valueFormatter((value) -> {
        int ticks = value.intValue();
        int totalMinutes = (int) ((ticks / 24000.0) * 24 * 60);
        return String.format("%d:%02d", totalMinutes / 60, totalMinutes % 60);
    }).build();

    public TimeChangerModule() {
        super("Time Changer", ModuleCategory.MISC);
        addSetting(worldTime);
    }
}
