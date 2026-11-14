package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.client.command.ClientCommandBootstrap;
import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.debug.perf.PerfAutoRun;
import cc.fascinated.fascinatedutils.debug.perf.PerfCommand;
import cc.fascinated.fascinatedutils.debug.perf.PerfProbe;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.oldgui.ModUiClientEntry;
import cc.fascinated.fascinatedutils.renderer.FascinatedWorldRenderTypes;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.screenshot.ScreenshotManager;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.systems.turboparticles.TurboParticles;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import cc.fascinated.fascinatedutils.updater.Updater;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger(AlumiteMod.MOD_ID);

    public static final TurboEntities TURBO_ENTITIES = new TurboEntities();
    public static final TurboParticles TURBO_PARTICLES = new TurboParticles();

    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless", "false");

        FascinatedEventBus eventBus = FascinatedEventBus.INSTANCE;
        eventBus.ensureSetup();
        eventBus.subscribe(TURBO_PARTICLES);
        eventBus.subscribe(TURBO_ENTITIES);
        ClientCommandBootstrap.register(PerfCommand.class);
        PerfAutoRun.INSTANCE.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            eventBus.post(new ClientStartedEvent(client));
            ModUiTextures.loadTextures(client);
            ScreenshotManager.init();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            eventBus.post(new ClientStoppingEvent(client));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long tickStart = PerfProbe.INSTANCE.begin();
            KeybindsWrapper.dispatchRegisteredCallbacks();
            FascinatedEventBus.INSTANCE.post(new ClientTickEvent(client));
            PerfProbe.INSTANCE.end("client.tick", tickStart);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> {
            ClientCommandBootstrap.registerWithFabric(dispatcher);
        });

        ModuleRegistry.INSTANCE.initialize();
        RainbowColors.init();
        FascinatedWorldRenderTypes.registerWithIris();
        ModUiClientEntry.register();
        new WaypointRepository();

        // Register updater shutdown hook; check and download will run on game exit
        if (!Constants.DEBUG_MODE) {
            Updater.INSTANCE.start();
        }
    }
}
