package cc.fascinated.fascinatedutils.systems.modules.impl.memory;

import cc.fascinated.fascinatedutils.common.ByteFormatterUtil;
import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.systems.modules.impl.memory.hud.MemoryHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

public class MemoryWidget extends HudMiniMessageModule {

    private final EnumSetting<Format> memoryFormat = EnumSetting.<Format>builder().id("memory_format")
            .defaultValue(Format.FULL)
            .valueFormatter(Format::name)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    public MemoryWidget() {
        super("process_memory", "Process Memory", UTILITY_WIDGET_MIN_WIDTH);
        addSetting(memoryFormat);
        registerHudPanel(new MemoryHudPanel(this));
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedBytes = memoryBean.getHeapMemoryUsage().getUsed();
        long maxBytes = memoryBean.getHeapMemoryUsage().getMax();
        if (maxBytes <= 0L) {
            maxBytes = Runtime.getRuntime().maxMemory();
        }

        long percent = (usedBytes * 100L) / maxBytes;
        String hexColor = Colors.rgbHex(Colors.getGoodBadColor((float) percent / 100, true));
        switch (memoryFormat.getValue()) {
            case FULL -> {
                ByteFormatterUtil.ScaledByteComparison heap = ByteFormatterUtil.scaledByteComparison(2, usedBytes, maxBytes);
                List<String> amounts = heap.amounts();
                return List.of("<%s>%s <grey>/ <white>%s <white>%s".formatted(hexColor, amounts.get(0), amounts.get(1), heap.unit()));
            }
            case PERCENT -> {
                return List.of("Mem: <%s>%s%%".formatted(hexColor, percent));
            }
        }

        return null;
    }

    @Getter @AllArgsConstructor
    private enum Format {
        FULL("Full"),
        PERCENT("Percent");

        private final String name;
    }
}
