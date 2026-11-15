package cc.fascinated.fascinatedutils.debug.perf.tests.hooks;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.SoundFilterModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The sound filter check that SoundEngineMixin runs for every sound the game plays: the module's enabled state, the
 * lookup of the sound id in its toggles, and the toggle's value. The ids cycle through every built-in sound event,
 * with every fourth toggle switched off during setup and a few unregistered ids that miss the lookup.
 */
public class SoundFilterPlayPerfTest extends PerfTest {
    private static final int BLOCKED_EVERY = 4;
    private static final List<String> UNREGISTERED_IDS = List.of("alumite:perf_unregistered_sound", "minecraft:perf_missing_sound");

    private final List<String> playedIds = new ArrayList<>();
    private final List<BooleanSetting> switchedOff = new ArrayList<>();
    private SoundFilterModule module;
    private boolean originalEnabled;
    private int cursor;
    private boolean lastBlocked;

    public SoundFilterPlayPerfTest() {
        super("hook.soundfilter.play", "SoundFilterModule toggle lookup as run by SoundEngineMixin for every played sound", 50, 20_000);
    }

    @Override
    public void setup() {
        module = ModuleRegistry.INSTANCE.getModule(SoundFilterModule.class).orElseThrow();
        originalEnabled = module.isEnabled();
        module.setEnabled(true);
        int soundIndex = 0;
        for (Identifier identifier : BuiltInRegistries.SOUND_EVENT.keySet()) {
            String id = identifier.getNamespace() + ":" + identifier.getPath();
            playedIds.add(id);
            BooleanSetting toggle = module.getTogglesById().get(id);
            if (toggle != null && toggle.getValue() && soundIndex % BLOCKED_EVERY == 0) {
                toggle.setValue(false);
                switchedOff.add(toggle);
            }
            soundIndex++;
        }
        playedIds.addAll(UNREGISTERED_IDS);
    }

    @Override
    public void runOnce() {
        String id = playedIds.get(cursor);
        cursor = (cursor + 1) % playedIds.size();
        lastBlocked = false;
        if (module.isEnabled()) {
            BooleanSetting toggle = module.getTogglesById().get(id);
            lastBlocked = toggle != null && !toggle.getValue();
        }
    }

    @Override
    public void teardown() {
        switchedOff.forEach(toggle -> toggle.setValue(true));
        switchedOff.clear();
        if (module != null) {
            module.setEnabled(originalEnabled);
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("soundfilter.SoundEngineMixin"), PerfCoverage.module(SoundFilterModule.class));
    }
}
