package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.NumberUtils;
import cc.fascinated.fascinatedutils.common.PingColors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.chat.Component;

@Getter
public class TabModule extends Module {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final EnumSetting<PingMode> pingMode = EnumSetting.<PingMode>builder().id("ping_mode").defaultValue(PingMode.BARS).build();
    private final BooleanSetting coloredPing = BooleanSetting.builder().id("colored_ping").defaultValue(true).build();
    private final SliderSetting maxPlayerSlots = SliderSetting.builder().id("max_player_slots").defaultValue(80f).minValue(80f).maxValue(180f).step(20f).valueFormatter((slots) -> NumberUtils.formatNumber(slots, 0) + " slots").build();

    public TabModule() {
        super("Tab", ModuleCategory.GENERAL);
        addSetting(pingMode);
        addSetting(coloredPing);
        addSetting(maxPlayerSlots);
    }

    /**
     * Resolves the colour of a millisecond ping label.
     *
     * @param latency the player latency in milliseconds
     * @return the ARGB colour, white when coloured ping is off
     */
    public int pingColor(int latency) {
        return coloredPing.getValue() ? PingColors.getPingColor(latency) : 0xFFFFFFFF;
    }

    /**
     * Builds the coloured "NNms" label drawn in the tab list in millisecond ping mode.
     *
     * @param latency the player latency in milliseconds
     * @return the label as a native component
     */
    public Component millisecondsPingComponent(int latency) {
        String miniMessageText = "<color:#" + String.format("%06X", pingColor(latency) & 0xFFFFFF) + ">" + "%sms".formatted(latency) + "</color>";
        return MinecraftClientAudiences.of().asNative(MINI_MESSAGE.deserialize(miniMessageText));
    }

    public enum PingMode {
        BARS, MILLISECONDS, NONE
    }
}
