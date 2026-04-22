package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.ByteFormatterUtil;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

public class MemoryWidget extends HudMiniMessageModule {
    public MemoryWidget() {
        super("process_memory", "Process Memory", UTILITY_WIDGET_MIN_WIDTH);
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedBytes = memoryBean.getHeapMemoryUsage().getUsed();
        long maxBytes = memoryBean.getHeapMemoryUsage().getMax();
        if (maxBytes <= 0L) {
            maxBytes = Runtime.getRuntime().maxMemory();
        }
        ByteFormatterUtil.ScaledByteComparison heap = ByteFormatterUtil.scaledByteComparison(2, usedBytes, maxBytes);
        List<String> amounts = heap.amounts();
        return List.of(amounts.get(0) + " <grey>/ <white>" + amounts.get(1) + " <white>" + heap.unit());
    }
}
