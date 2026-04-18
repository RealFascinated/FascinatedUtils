package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import org.lwjgl.glfw.GLFW;

@UtilityClass
public class ClientUtils {
    private final String OS_NAME = System.getProperty("os.name").toLowerCase();

    public static boolean isMacOS() {
        return OS_NAME.contains("mac");
    }

    public static Identifier mobEffectSprite(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(ResourceKey::identifier).map(identifier -> identifier.withPrefix("mob_effect/")).orElseGet(MissingTextureAtlasSprite::getLocation);
    }

    /**
     * Copies text to the system clipboard using the game window.
     * Call from the Minecraft client thread (for example inside {@link Minecraft#execute(Runnable)}
     * when coming from a background thread).
     */
    public static void copyToClipboard(String text) {
        if (text == null) {
            return;
        }
        Minecraft minecraftClient = Minecraft.getInstance();
        GLFW.glfwSetClipboardString(minecraftClient.getWindow().handle(), text);
    }
}
