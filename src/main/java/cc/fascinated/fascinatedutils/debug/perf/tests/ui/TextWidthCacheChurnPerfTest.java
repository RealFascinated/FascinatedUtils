package cc.fascinated.fascinatedutils.debug.perf.tests.ui;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.List;
import java.util.Locale;

/**
 * AlumiteGuiRenderer.measureTextWidth on a realistic mix: a hot set of labels that fits the width cache, with one brand-new
 * label every eighth call. The hot set should stay cached even though the new labels keep filling the cache, which a
 * cache that clears everything when full cannot do.
 */
public class TextWidthCacheChurnPerfTest extends PerfTest {
    private static final int HOT_STRINGS = 3_000;
    private static final int NEW_STRING_INTERVAL = 8;

    private final String[] hotInputs = new String[HOT_STRINGS];
    private AlumiteGuiRenderer guiRenderer;
    private int callCount;
    private int nextHotInput;
    private int lastWidth;

    public TextWidthCacheChurnPerfTest() {
        super("gui.text_width_cache_churn", "AlumiteGuiRenderer.measureTextWidth with a 3000-label hot set and one new label every 8th call", 2_000, 20_000);
    }

    @Override
    public void setup() {
        for (int index = 0; index < HOT_STRINGS; index++) {
            hotInputs[index] = String.format(Locale.ROOT, "Module %d: value %d ms", index, index % 300);
        }
        guiRenderer = new AlumiteGuiRenderer(new GuiGraphicsExtractor(Minecraft.getInstance(), new GuiRenderState(), 0, 0));
    }

    @Override
    public void runOnce() {
        callCount++;
        String input;
        if (callCount % NEW_STRING_INTERVAL == 0) {
            input = String.format(Locale.ROOT, "Fresh label %d: %d ms", callCount, callCount % 300);
        } else {
            input = hotInputs[nextHotInput];
            nextHotInput = (nextHotInput + 1) % HOT_STRINGS;
        }
        lastWidth = guiRenderer.measureTextWidth(input, false);
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.gui("AlumiteGuiRenderer"));
    }
}
