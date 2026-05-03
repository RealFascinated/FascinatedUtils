package cc.fascinated.fascinatedutils.systems.modules.impl.memory.hud;

import cc.fascinated.fascinatedutils.common.ByteFormatterUtil;
import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.memory.MemoryWidget;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

public class MemoryHudPanel extends MiniMessageHudPanel {

    public MemoryHudPanel(MemoryWidget memoryWidget) {
        super(memoryWidget, "process_memory", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        MemoryWidget memoryWidget = (MemoryWidget) hudHostModule();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedBytes = memoryBean.getHeapMemoryUsage().getUsed();
        long maxBytes = memoryBean.getHeapMemoryUsage().getMax();
        if (maxBytes <= 0L) {
            maxBytes = Runtime.getRuntime().maxMemory();
        }

        long percent = (usedBytes * 100L) / maxBytes;
        String hexColor = Colors.rgbHex(Colors.getGoodBadColor((float) percent / 100, true));
        MemoryWidget.Format fmt = memoryWidget.memoryFormatSetting().getValue();
        return switch (fmt) {
            case FULL -> {
                ByteFormatterUtil.ScaledByteComparison heap = ByteFormatterUtil.scaledByteComparison(2, usedBytes, maxBytes);
                List<String> amounts = heap.amounts();
                yield List.of("<%s>%s <grey>/ <white>%s <white>%s".formatted(hexColor, amounts.get(0), amounts.get(1), heap.unit()));
            }
            case PERCENT -> List.of("Mem: <%s>%s%%".formatted(hexColor, percent));
        };
    }
}
