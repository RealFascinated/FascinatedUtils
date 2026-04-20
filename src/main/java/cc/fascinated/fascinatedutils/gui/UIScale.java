package cc.fascinated.fascinatedutils.gui;

import com.mojang.blaze3d.platform.Window;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class UIScale {

    /**
     * Fixed UI canvas width: half the framebuffer width, unconditionally.
     *
     * @return canvas width in UI pixels
     */
    public static float uiWidth() {
        return Math.max(1f, Minecraft.getInstance().getWindow().getWidth() / 2f);
    }

    /**
     * Fixed UI canvas height: half the framebuffer height, unconditionally.
     *
     * @return canvas height in UI pixels
     */
    public static float uiHeight() {
        return Math.max(1f, Minecraft.getInstance().getWindow().getHeight() / 2f);
    }

    /**
     * Pointer X in fixed scale-2 UI space.
     *
     * @return pointer X in UI pixels
     */
    public static float uiPointerX() {
        return hiResPointerX() / 2f;
    }

    /**
     * Pointer Y in fixed scale-2 UI space.
     *
     * @return pointer Y in UI pixels
     */
    public static float uiPointerY() {
        return hiResPointerY() / 2f;
    }

    /**
     * Pointer X in framebuffer-pixel-equivalent units (logical GUI pointer scaled by the framebuffer ratio).
     *
     * @return pointer X in framebuffer pixels
     */
    public static float hiResPointerX() {
        Window window = Minecraft.getInstance().getWindow();
        return (float) Minecraft.getInstance().mouseHandler.getScaledXPos(window) * (window.getWidth() / Math.max(1f, (float) window.getGuiScaledWidth()));
    }

    /**
     * Pointer Y in framebuffer-pixel-equivalent units (logical GUI pointer scaled by the framebuffer ratio).
     *
     * @return pointer Y in framebuffer pixels
     */
    public static float hiResPointerY() {
        Window window = Minecraft.getInstance().getWindow();
        return (float) Minecraft.getInstance().mouseHandler.getScaledYPos(window) * (window.getHeight() / Math.max(1f, (float) window.getGuiScaledHeight()));
    }

}
