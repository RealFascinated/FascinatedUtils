package cc.fascinated.fascinatedutils.debug.perf.tests.text;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Renderer2D.parseMiniMessage on text it has not seen, cycling through more distinct strings than its cache holds,
 * so every call pays the real MiniMessage parse and Adventure to native conversion (the path every HUD label and the
 * tab ping label take on a cache miss).
 */
public class MiniMessageParsePerfTest extends PerfTest {
    private static final int DISTINCT_STRINGS = 4096;

    private final String[] inputs = new String[DISTINCT_STRINGS];
    private Renderer2D renderer;
    private int nextInput;
    private Component lastResult;

    public MiniMessageParsePerfTest() {
        super("renderer.minimessage_parse", "Renderer2D.parseMiniMessage on uncached gradient/colour text", 50, 2_000);
    }

    @Override
    public void setup() {
        for (int index = 0; index < DISTINCT_STRINGS; index++) {
            inputs[index] = String.format(Locale.ROOT, "<gradient:#ff5555:#55ffff>Row %d</gradient> <gray>(<white>%d</white> ms)</gray>", index, index % 300);
        }
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(Minecraft.getInstance(), new GuiRenderState(), 0, 0);
        renderer = new Renderer2D(graphics, () -> null);
    }

    @Override
    public void runOnce() {
        lastResult = renderer.parseMiniMessage(inputs[nextInput]);
        nextInput = (nextInput + 1) % DISTINCT_STRINGS;
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.renderer("Renderer2D"), PerfCoverage.common("LRUCache"));
    }
}
