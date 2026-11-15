package cc.fascinated.fascinatedutils.debug.perf.tests.hooks;

import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.TitlesModule;

import java.util.List;
import java.util.Optional;

/**
 * The scale decision that the title GuiMixin runs for the title and the subtitle: the module lookup and enabled gate,
 * then the scale slider, multiplied into the vanilla scale when it is not at its default. The slider is set to a
 * non-default value during setup so the scaling branch runs. The mixin's Args reads and writes are not included.
 */
public class TitleRenderPerfTest extends PerfTest {
    private static final float SCALE_SETTING_VALUE = 0.5F;
    private static final float VANILLA_TITLE_SCALE = 4.0F;

    private TitlesModule module;
    private boolean originalEnabled;
    private Number originalScale;
    private float lastScale;

    public TitleRenderPerfTest() {
        super("hook.title.render", "TitlesModule scale decision as run by the title GuiMixin for the title and subtitle", 50, 20_000);
    }

    @Override
    public void setup() {
        module = ModuleRegistry.INSTANCE.getModule(TitlesModule.class).orElseThrow();
        originalEnabled = module.isEnabled();
        originalScale = module.getScaleTitleAndSubtitle().getValue();
        module.setEnabled(true);
        module.getScaleTitleAndSubtitle().setValue(SCALE_SETTING_VALUE);
    }

    @Override
    public void runOnce() {
        Optional<TitlesModule> titlesOptional = ModuleRegistry.INSTANCE.getModule(TitlesModule.class);
        if (titlesOptional.isEmpty() || !titlesOptional.get().isEnabled()) {
            lastScale = VANILLA_TITLE_SCALE;
            return;
        }
        SliderSetting scaleTitleAndSubtitle = titlesOptional.get().getScaleTitleAndSubtitle();
        lastScale = scaleTitleAndSubtitle.isDefault() ? VANILLA_TITLE_SCALE : VANILLA_TITLE_SCALE * scaleTitleAndSubtitle.getValue().floatValue();
    }

    @Override
    public void teardown() {
        if (module != null) {
            module.getScaleTitleAndSubtitle().setValue(originalScale);
            module.setEnabled(originalEnabled);
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("title.GuiMixin"), PerfCoverage.module(TitlesModule.class));
    }
}
