package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.client.command.ClientCommandBootstrap;
import cc.fascinated.fascinatedutils.client.keybind.Keybinds;
import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.gui.ModUiClientEntry;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.updater.UpdateChecker;
import cc.fascinated.fascinatedutils.updater.UpdateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger(FascinatedUtils.MOD_ID);


    public static final TurboEntities TURBO_ENTITIES = new TurboEntities();

    @Override
    public void onInitializeClient() {
        FascinatedEventBus eventBus = FascinatedEventBus.INSTANCE;
        eventBus.ensureSetup();
        eventBus.subscribe(ModUiTextures.class);
        eventBus.subscribe(TURBO_ENTITIES);

        ModUiTextures.registerResourceReloadListener();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> eventBus.post(new ClientStartedEvent(client)));
        ModuleRegistry.INSTANCE.initialize();
        RainbowColors.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> eventBus.post(new ClientStoppingEvent(client)));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ClientCommandBootstrap.registerWithFabric(dispatcher));
        new Keybinds();
        ModUiClientEntry.register();
        ClientTickEvents.END_CLIENT_TICK.register(_ -> KeybindsWrapper.dispatchRegisteredCallbacks());
        ClientTickEvents.END_CLIENT_TICK.register(client -> FascinatedEventBus.INSTANCE.post(new ClientTickEvent(client)));

        // Register updater shutdown hook and kick off an async update check
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            UpdateManager.registerShutdownHook();
            UpdateChecker.checkForUpdatesAsync();
        }

        // Subscribe UI notifier for update events so mod settings can show a badge
        FascinatedEventBus.INSTANCE.subscribe(cc.fascinated.fascinatedutils.gui.modsettings.ModSettingsUpdateNotifier.class);
    }
}
