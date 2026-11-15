package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.debug.AlumiteDebugScreenEntry;
import cc.fascinated.fascinatedutils.client.command.ClientCommandBootstrap;
import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.debug.perf.command.PerfCommand;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfStartup;
import cc.fascinated.fascinatedutils.debug.perf.run.PerfAutoRun;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.gui.ModUiClientEntry;
import cc.fascinated.fascinatedutils.renderer.FascinatedWorldRenderTypes;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.screenshot.ScreenshotManager;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.systems.turboparticles.TurboParticles;
import cc.fascinated.fascinatedutils.systems.turboweather.TurboWeather;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import cc.fascinated.fascinatedutils.updater.Updater;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger(AlumiteMod.MOD_ID);

    public static final TurboEntities TURBO_ENTITIES = new TurboEntities();
    public static final TurboParticles TURBO_PARTICLES = new TurboParticles();
    public static final TurboWeather TURBO_WEATHER = new TurboWeather();
    private static final PerfProbe.Section CLIENT_TICK_SECTION = PerfProbe.INSTANCE.section("client.tick");

    /**
     * Runs the mod's end-of-client-tick work: keybind callbacks, then the {@link ClientTickEvent} post.
     *
     * @param client the Minecraft client
     */
    public static void dispatchEndClientTick(Minecraft client) {
        long tickStart = CLIENT_TICK_SECTION.begin();
        try {
            KeybindsWrapper.dispatchRegisteredCallbacks();
            FascinatedEventBus.INSTANCE.post(new ClientTickEvent(client));
        } finally {
            CLIENT_TICK_SECTION.end(tickStart);
        }
    }

    /**
     * Clears the cached MiniMessage text widths after fonts and languages reload, since both change measured widths.
     */
    private static void registerTextWidthCacheReloadListener() {
        ResourceLoader resourceLoader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        Identifier listenerId = Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, "mini_message_width_cache");
        resourceLoader.registerReloadListener(listenerId, (sharedState, prepareExecutor, barrier, applyExecutor) -> barrier.wait(null).thenRunAsync(() -> {
            Renderer2D.clearMiniMessageWidthCache();
            AlumiteGuiRenderer.clearTextWidthCache();
        }, applyExecutor));
        resourceLoader.addListenerOrdering(ResourceReloaderKeys.Client.FONTS, listenerId);
        resourceLoader.addListenerOrdering(ResourceReloaderKeys.Client.LANGUAGES, listenerId);
    }

    @Override
    public void onInitializeClient() {
        PerfStartup.INSTANCE.markInitStart();
        AlumiteDebugScreenEntry.register();
        System.setProperty("java.awt.headless", "false");
        new Alumite(); // Init the api and auth system

        FascinatedEventBus eventBus = FascinatedEventBus.INSTANCE;
        eventBus.ensureSetup();
        eventBus.subscribe(Alumite.INSTANCE);
        eventBus.subscribe(TURBO_PARTICLES);
        eventBus.subscribe(TURBO_ENTITIES);
        ClientCommandBootstrap.register(PerfCommand.class);
        PerfAutoRun.INSTANCE.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            eventBus.post(new ClientStartedEvent(client));
            try {
                ModUiTextures.loadTextures(client);
            } catch (Throwable throwable) {
                LOG.error("Failed to load mod UI textures", throwable);
            }
            try {
                ScreenshotManager.init();
            } catch (Throwable throwable) {
                LOG.error("Failed to initialise screenshot manager", throwable);
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            eventBus.post(new ClientStoppingEvent(client));
        });
        ClientTickEvents.END_CLIENT_TICK.register(Client::dispatchEndClientTick);
        registerTextWidthCacheReloadListener();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> {
            ClientCommandBootstrap.registerWithFabric(dispatcher);
        });

        ModuleRegistry.INSTANCE.initialize();
        RainbowColors.init();
        FascinatedWorldRenderTypes.registerWithIris();
        ModUiClientEntry.register();
        WaypointRepository.init();

        // Starts the periodic update check; staged updates are applied on shutdown.
        Updater.INSTANCE.start();
        PerfStartup.INSTANCE.markInitEnd();
    }
}
