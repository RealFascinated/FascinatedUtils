package cc.fascinated.fascinatedutils.systems.modules.impl.tab;

import cc.fascinated.fascinatedutils.common.util.NumberUtils;
import cc.fascinated.fascinatedutils.common.color.PingColors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

@Getter
public class TabModule extends Module {
    private static final int PING_CACHE_SIZE = 1024;

    private final Component[] pingComponentCache = new Component[PING_CACHE_SIZE];
    private final int[] pingComponentColors = new int[PING_CACHE_SIZE];

    private final EnumSetting<PingMode> pingMode = EnumSetting.<PingMode>builder().id("ping_mode").defaultValue(PingMode.BARS).build();
    private final BooleanSetting coloredPing = BooleanSetting.builder().id("colored_ping").defaultValue(true).build();
    private final SliderSetting maxPlayerSlots = SliderSetting.builder().id("max_player_slots").defaultValue(80f).minValue(20f).maxValue(180f).step(20f).valueFormatter((slots) -> NumberUtils.formatNumber(slots, 0) + " slots").build();

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
        int color = pingColor(latency) & 0xFFFFFF;
        boolean cacheable = latency >= 0 && latency < PING_CACHE_SIZE;
        if (cacheable) {
            Component cached = pingComponentCache[latency];
            if (cached != null && pingComponentColors[latency] == color) {
                return cached;
            }
        }
        Component component = Component.literal(latency + "ms").withStyle(style -> style.withColor(TextColor.fromRgb(color)));
        if (cacheable) {
            pingComponentCache[latency] = component;
            pingComponentColors[latency] = color;
        }
        return component;
    }

    public enum PingMode {
        BARS, MILLISECONDS, NONE
    }
}
