package cc.fascinated.fascinatedutils.debug.perf.scene;

import cc.fascinated.fascinatedutils.mixin.KeyBindingAccessorMixin;
import lombok.Getter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Helpers a {@link PerfScene} uses to drive the client. Commands are queued and sent a few per tick so the server is
 * not flooded; keys held and screens opened here are released by the runner after the scene's teardown.
 */
@Getter
public class PerfSceneContext {
    private static final int COMMANDS_PER_TICK = 4;

    private final Minecraft minecraft;
    private final PerfScenario scenario;
    private final Deque<String> pendingCommands = new ArrayDeque<>();
    private final List<KeyMapping> heldKeys = new ArrayList<>();
    private Supplier<Screen> screenFactory;

    public PerfSceneContext(Minecraft minecraft, PerfScenario scenario) {
        this.minecraft = minecraft;
        this.scenario = scenario;
    }

    /**
     * Queues a server command (without the leading slash).
     *
     * @param command the command to run as the player
     */
    public void command(String command) {
        pendingCommands.add(command);
    }

    /**
     * Queues a command that runs at the world surface of a column relative to the world origin.
     *
     * @param offsetX the column x
     * @param offsetZ the column z
     * @param command the command to run there, using {@code ~} coordinates
     */
    public void surfaceCommand(int offsetX, int offsetZ, String command) {
        command(String.format(Locale.ROOT, "execute positioned %d 0 %d positioned over world_surface run %s", offsetX, offsetZ, command));
    }

    /**
     * Holds a key down until the scene ends.
     *
     * @param keyMapping the key to hold
     */
    public void holdKey(KeyMapping keyMapping) {
        keyMapping.setDown(true);
        if (!heldKeys.contains(keyMapping)) {
            heldKeys.add(keyMapping);
        }
    }

    /**
     * Registers one press of a key, as if the user tapped it; callback keybinds fire on the next tick.
     *
     * @param keyMapping the key to tap
     */
    public void clickKey(KeyMapping keyMapping) {
        KeyMapping.click(((KeyBindingAccessorMixin) keyMapping).getKey());
    }

    /**
     * Opens a screen for the whole scene. The runner reopens it if it closes itself and closes it after teardown.
     *
     * @param factory creates the screen
     */
    public void keepScreenOpen(Supplier<Screen> factory) {
        this.screenFactory = factory;
        minecraft.setScreen(factory.get());
    }

    /**
     * Reports whether the scene owns the current screen, so the runner must not close it.
     *
     * @return true when {@link #keepScreenOpen} was called
     */
    public boolean ownsScreen() {
        return screenFactory != null;
    }

    /**
     * Presses every held key again, since the game releases keys it thinks are up. Called by the runner each tick.
     */
    public void reassertHeldKeys() {
        for (KeyMapping keyMapping : heldKeys) {
            keyMapping.setDown(true);
        }
    }

    /**
     * Sends up to a few queued commands. Called by the runner each tick.
     */
    public void flushCommands() {
        for (int sent = 0; sent < COMMANDS_PER_TICK && !pendingCommands.isEmpty(); sent++) {
            minecraft.getConnection().sendCommand(pendingCommands.poll());
        }
    }

    /**
     * Reports whether queued commands are still waiting to be sent.
     *
     * @return true while commands are queued
     */
    public boolean hasPendingCommands() {
        return !pendingCommands.isEmpty();
    }

    /**
     * Reopens the scene's screen if something closed it. Called by the runner each tick.
     */
    public void reopenScreenIfClosed() {
        if (screenFactory != null && minecraft.screen == null) {
            minecraft.setScreen(screenFactory.get());
        }
    }

    /**
     * Releases held keys and closes the scene's screen. Called by the runner after the scene's teardown.
     */
    public void releaseAll() {
        for (KeyMapping keyMapping : heldKeys) {
            keyMapping.setDown(false);
        }
        heldKeys.clear();
        if (screenFactory != null) {
            screenFactory = null;
            minecraft.setScreen(null);
        }
    }
}
