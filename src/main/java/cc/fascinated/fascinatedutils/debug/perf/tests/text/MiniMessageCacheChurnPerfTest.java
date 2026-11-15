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
 * Renderer2D.parseMiniMessage on a realistic mix: a hot set of strings that fits the cache, with one brand-new string
 * every eighth call. The hot set should stay cached even though the new strings keep filling the cache, which a cache
 * that clears everything when full cannot do.
 */
public class MiniMessageCacheChurnPerfTest extends PerfTest {
    private static final int HOT_STRINGS = 700;
    private static final int NEW_STRING_INTERVAL = 8;

    private final String[] hotInputs = new String[HOT_STRINGS];
    private Renderer2D renderer;
    private int callCount;
    private int nextHotInput;
    private Component lastResult;

    public MiniMessageCacheChurnPerfTest() {
        super("renderer.minimessage_cache_churn", "Renderer2D.parseMiniMessage with a 700-string hot set and one new string every 8th call", 2_000, 20_000);
    }

    @Override
    public void setup() {
        for (int index = 0; index < HOT_STRINGS; index++) {
            hotInputs[index] = String.format(Locale.ROOT, "<gradient:#ff5555:#55ffff>Row %d</gradient> <gray>(<white>%d</white> ms)</gray>", index, index % 300);
        }
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(Minecraft.getInstance(), new GuiRenderState(), 0, 0);
        renderer = new Renderer2D(graphics, () -> null);
    }

    @Override
    public void runOnce() {
        callCount++;
        String input;
        if (callCount % NEW_STRING_INTERVAL == 0) {
            input = String.format(Locale.ROOT, "<gradient:#55ffff:#ff5555>Fresh %d</gradient> <gray>(<white>%d</white> ms)</gray>", callCount, callCount % 300);
        } else {
            input = hotInputs[nextHotInput];
            nextHotInput = (nextHotInput + 1) % HOT_STRINGS;
        }
        lastResult = renderer.parseMiniMessage(input);
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.renderer("Renderer2D"));
    }
}
