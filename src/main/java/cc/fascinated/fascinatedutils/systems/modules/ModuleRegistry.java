package cc.fascinated.fascinatedutils.systems.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.module.ModuleEnabledStateChangedEvent;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.ArmorWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.BossbarModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.ClockWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.CoordinatesWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.CpsWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.DebugWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.FpsWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.FreelookModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.HurtcamModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.MemoryWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.MovementModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.PingWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.ScoreboardModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.StatusEffectsModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.SystemCpuUsageWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.TabModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.TitlesModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.TpsWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.VibrancyModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.WawlaWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.WorldModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.ZoomModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.hypixel.HypixelModule;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;

@Getter
public class ModuleRegistry {
    public static final ModuleRegistry INSTANCE = new ModuleRegistry();
    private final List<Module> modules = new ArrayList<>();
    private final List<Module> enabledModules = new ArrayList<>();
    private final List<HudModule> hudModules = new ArrayList<>();
    private boolean initialized;

    private ModuleRegistry() {
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    /**
     * Registers built-in modules, loads persisted config, and registers the end-of-tick hook.
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        modules.add(new ScoreboardModule());
        modules.add(new StatusEffectsModule());
        modules.add(new MovementModule());
        modules.add(new TitlesModule());
        modules.add(new ZoomModule());
        modules.add(new FreelookModule());
        modules.add(new TabModule());
        modules.add(new VibrancyModule());
        modules.add(new HurtcamModule());
        modules.add(new BossbarModule());
        modules.add(new HypixelModule());
        modules.add(new WorldModule());
        modules.add(new FpsWidget());
        modules.add(new DebugWidget());
        modules.add(new CpsWidget());
        modules.add(new TpsWidget());
        modules.add(new SystemCpuUsageWidget());
        modules.add(new ClockWidget());
        modules.add(new MemoryWidget());
        modules.add(new CoordinatesWidget());
        modules.add(new PingWidget());
        modules.add(new ArmorWidget());
        modules.add(new WawlaWidget());

        for (Module module : modules) {
            FascinatedEventBus.INSTANCE.subscribe(module);
            if (module instanceof HudModule hudModule) {
                hudModules.add(hudModule);
            }
        }

        ModConfig.loadAllModuleSettings();
        rebuildEnabledModules();
        ModConfig.loadSettings();
        HUDManager.INSTANCE.init();

        initialized = true;
    }

    /**
     * Lists every registered module.
     *
     * @return an unmodifiable list of modules
     */
    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<HudModule> getHudModules() {
        return Collections.unmodifiableList(hudModules);
    }

    /**
     * Resolves a module by its concrete class.
     *
     * @param moduleClass the module implementation class
     * @param <T>         module type
     * @return the registered module instance, or an empty optional when none match
     */
    public <T extends Module> Optional<T> getModule(Class<T> moduleClass) {
        return modules.stream().filter(module -> moduleClass.equals(module.getClass())).map(moduleClass::cast).findFirst();
    }

    public void setModuleEnabled(Module module, boolean enabled) {
        module.setEnabled(enabled);
    }

    @EventHandler
    private void fascinatedutils$onModuleEnabledStateChanged(ModuleEnabledStateChangedEvent event) {
        Module module = event.module();
        if (module instanceof HudModule hudModule) {
            hudModule.getHudState().setVisible(module.isEnabled());
            HUDManager.INSTANCE.onWidgetVisibilityStateChanged(hudModule);
        }
        if (module.isEnabled()) {
            if (!enabledModules.contains(module)) {
                enabledModules.add(module);
            }
            return;
        }
        enabledModules.remove(module);
    }

    private void rebuildEnabledModules() {
        enabledModules.clear();
        for (Module module : modules) {
            if (module.isEnabled()) {
                enabledModules.add(module);
            }
        }
    }
}
