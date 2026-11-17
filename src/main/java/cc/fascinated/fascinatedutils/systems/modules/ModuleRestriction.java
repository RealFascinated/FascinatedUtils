package cc.fascinated.fascinatedutils.systems.modules;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * A condition that keeps a module switched off while it holds, together with the reason shown to the player.
 * Register one with {@link Module#addRestriction(ModuleRestriction)}; the module resumes once it stops holding.
 */
@Getter
public class ModuleRestriction {

    private final BooleanSupplier violated;
    private final Component reason;
    private final boolean hidesModule;

    private ModuleRestriction(BooleanSupplier violated, Component reason, boolean hidesModule) {
        this.violated = violated;
        this.reason = reason;
        this.hidesModule = hidesModule;
    }

    /**
     * Creates a restriction that keeps the module off while the condition holds.
     *
     * @param violated supplies true while the module must stay off
     * @param reason   explains to the player why the module cannot run
     * @return the restriction
     */
    public static ModuleRestriction when(BooleanSupplier violated, Component reason) {
        return new ModuleRestriction(violated, reason, false);
    }

    /**
     * Restricts a module to single player. Outside of a world the restriction does not apply.
     *
     * @return the restriction
     */
    public static ModuleRestriction singleplayerOnly() {
        return when(() -> isInWorld() && !Minecraft.getInstance().hasSingleplayerServer(),
                Component.translatable("alumite.module.restriction.singleplayer_only"));
    }

    /**
     * Restricts a module to multiplayer. Outside of a world the restriction does not apply.
     *
     * @return the restriction
     */
    public static ModuleRestriction multiplayerOnly() {
        return when(() -> isInWorld() && Minecraft.getInstance().hasSingleplayerServer(),
                Component.translatable("alumite.module.restriction.multiplayer_only"));
    }

    /**
     * Restricts a module to one server. The module is hidden from the settings screen while elsewhere.
     *
     * @param serverName  display name of the server
     * @param onServer    supplies true while connected to that server
     * @return the restriction
     */
    public static ModuleRestriction serverOnly(String serverName, BooleanSupplier onServer) {
        return new ModuleRestriction(() -> !onServer.getAsBoolean(),
                Component.translatable("alumite.module.restriction.server_only", serverName), true);
    }

    /**
     * Whether the module must currently stay off because of this restriction.
     *
     * @return true while the restriction holds
     */
    public boolean isViolated() {
        return violated.getAsBoolean();
    }

    private static boolean isInWorld() {
        return Minecraft.getInstance().level != null;
    }
}
