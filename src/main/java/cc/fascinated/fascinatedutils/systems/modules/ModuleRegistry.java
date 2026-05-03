package cc.fascinated.fascinatedutils.systems.modules;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.module.ModuleEnabledStateChangedEvent;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.*;
import cc.fascinated.fascinatedutils.systems.modules.impl.armor.ArmorModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.BossbarModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.clock.ClockWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.CoordinatesWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.cps.CpsWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.days.DaysCounterWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.debug.DebugWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.fps.FpsWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.hypixel.HypixelModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.memory.MemoryWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.movement.MovementModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.ping.PingWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.reach.ReachWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.ScoreboardModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.speed.SpeedWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.systemcpu.SystemCpuUsageWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.tps.TpsWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.waypoint.WaypointsModule;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class ModuleRegistry {
    public static final ModuleRegistry INSTANCE = new ModuleRegistry();
    private final List<Module> modules = new ArrayList<>();
    private final List<Module> enabledModules = new ArrayList<>();
    private final List<HudPanel> hudPanels = new ArrayList<>();
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
        modules.add(new BlurModule());
        modules.add(new HurtcamModule());
        modules.add(new BossbarModule());
        modules.add(new InventoryTweaksModule());
        modules.add(new HypixelModule());
        modules.add(new FogCustomizerModule());
        modules.add(new FpsWidget());
        modules.add(new DebugWidget());
        modules.add(new CpsWidget());
        modules.add(new TpsWidget());
        modules.add(new SystemCpuUsageWidget());
        modules.add(new ClockWidget());
        modules.add(new DaysCounterWidget());
        modules.add(new MemoryWidget());
        modules.add(new CoordinatesWidget());
        modules.add(new PingWidget());
        modules.add(new ArmorModule());
        modules.add(new WawlaWidget());
        modules.add(new TimeChangerModule());
        modules.add(new BlockOutlineModule());
        modules.add(new SoundFilterModule());
        modules.add(new ParticleFilterModule());
        modules.add(new SpeedWidget());
        modules.add(new ReachWidget());
        modules.add(new FullbrightModule());
        modules.add(new ItemTooltipModule());
        modules.add(new WaypointsModule());
        modules.add(new WorldSizeModule());

        for (Module module : modules) {
            FascinatedEventBus.INSTANCE.subscribe(module);
            if (module instanceof HudHostModule hudHost) {
                hudPanels.addAll(hudHost.registeredHudPanels());
            }

            Client.LOG.info("Loaded module {} with {} settings", module.getDisplayName(), module.getAllSettings().size());
        }

        ModConfig.profiles().loadActiveProfile();
        rebuildEnabledModules();
        ModConfig.config().load();
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

    public List<HudPanel> getHudPanels() {
        return Collections.unmodifiableList(hudPanels);
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
