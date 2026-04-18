package cc.fascinated.fascinatedutils.common;

import cc.fascinated.fascinatedutils.mixin.KeyBindingAccessorMixin;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.MovementModule;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

@UtilityClass
public class PlayerUtils {

    /**
     * Applies the client flight speed modifier when the local creative or
     * spectator player is flying and holding the sprint key.
     */
    public static float scaleFlyingSpeed(Player player, float vanillaSpeed) {
        Minecraft minecraftClient = Minecraft.getInstance();
        if (player != minecraftClient.player) {
            return vanillaSpeed;
        }
        if (!player.getAbilities().flying || player.isPassenger()) {
            return vanillaSpeed;
        }
        if ((!player.isCreative() && !player.isSpectator()) || !isSprintBindingPhysicallyHeld(minecraftClient)) {
            return vanillaSpeed;
        }
        Optional<MovementModule> movementOptional = ModuleRegistry.INSTANCE.getModule(MovementModule.class);
        if (movementOptional.isEmpty()) {
            return vanillaSpeed;
        }
        MovementModule module = movementOptional.get();
        if (!module.isEnabled()) {
            return vanillaSpeed;
        }
        if (module.getEnableFlightSpeedModifier().isDisabled()) {
            return vanillaSpeed;
        }
        return vanillaSpeed * module.getFlightSpeedModifier().getValue().floatValue();
    }

    /**
     * Return whether the sprint key binding is physically held, ignoring toggle-sprint sticky state.
     */
    private static boolean isSprintBindingPhysicallyHeld(Minecraft minecraftClient) {
        KeyMapping sprintBinding = minecraftClient.options.keySprint;
        Window window = minecraftClient.getWindow();
        InputConstants.Key boundKey = ((KeyBindingAccessorMixin) sprintBinding).getKey();
        return switch (boundKey.getType()) {
            case KEYSYM, SCANCODE -> InputConstants.isKeyDown(window, boundKey.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window.handle(), boundKey.getValue()) == GLFW.GLFW_PRESS;
        };
    }

    /**
     * The command to make the player run without "/"
     *
     * @param command the command to run
     */
    public static void runCommand(String command) {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        player.connection.sendCommand(command);
    }

    /**
     * The message to make the player send in chat
     *
     * @param message the command to send
     */
    public static void sendMessage(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        player.connection.sendChat(message);
    }
}
