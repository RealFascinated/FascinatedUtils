package cc.fascinated.fascinatedutils.gui;

import com.mojang.blaze3d.platform.Window;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class UIScale {
    /**
     * Framebuffer pixels per one horizontal GUI pixel (same convention as vanilla mouse scaling).
     */
    public static float getUIScale() {
        return framebufferScaleX();
    }

    /**
     * Horizontal ratio of framebuffer pixels to one GUI-scaled pixel.
     */
    public static float framebufferScaleX() {
        Window window = Minecraft.getInstance().getWindow();
        return window.getWidth() / Math.max(1f, (float) window.getGuiScaledWidth());
    }

    /**
     * Vertical ratio of framebuffer pixels to one GUI-scaled pixel.
     */
    public static float framebufferScaleY() {
        Window window = Minecraft.getInstance().getWindow();
        return window.getHeight() / Math.max(1f, (float) window.getGuiScaledHeight());
    }

    /**
     * Pointer X in framebuffer-aligned UI units (logical pointer times horizontal framebuffer scale).
     */
    public static float hiResPointerX() {
        return logicalPointerX() * framebufferScaleX();
    }

    /**
     * Pointer Y in framebuffer-aligned UI units (logical pointer times vertical framebuffer scale).
     */
    public static float hiResPointerY() {
        return logicalPointerY() * framebufferScaleY();
    }

    /**
     * Width of the UI canvas in GUI-scaled pixels (matches {@link net.minecraft.client.gui.screens.Screen#width}).
     */
    public static float logicalWidth() {
        return (float) Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    /**
     * Height of the UI canvas in GUI-scaled pixels (matches {@link net.minecraft.client.gui.screens.Screen#height}).
     */
    public static float logicalHeight() {
        return (float) Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    /**
     * Physical framebuffer width ({@link Window#getWidth()}).
     */
    public static float physicalWidth() {
        return (float) Minecraft.getInstance().getWindow().getWidth();
    }

    /**
     * Physical framebuffer height ({@link Window#getHeight()}).
     */
    public static float physicalHeight() {
        return (float) Minecraft.getInstance().getWindow().getHeight();
    }

    /**
     * Converts a horizontal coordinate in the same space as {@link net.minecraft.client.MouseHandler#xpos()}
     * to GUI-scaled logical X.
     */
    public static float toLogicalX(float physicalX) {
        Window window = Minecraft.getInstance().getWindow();
        return physicalX * (float) window.getGuiScaledWidth() / Math.max(1f, (float) window.getScreenWidth());
    }

    /**
     * Converts a vertical coordinate in physical framebuffer space to GUI-scaled logical Y.
     */
    public static float toLogicalY(float physicalY) {
        Window window = Minecraft.getInstance().getWindow();
        return physicalY * (float) window.getGuiScaledHeight() / Math.max(1f, (float) window.getScreenHeight());
    }

    /**
     * Mouse X in GUI-scaled pixels (matches vanilla screen hit testing).
     */
    public static float logicalPointerX() {
        Minecraft minecraftClient = Minecraft.getInstance();
        return (float) minecraftClient.mouseHandler.getScaledXPos(minecraftClient.getWindow());
    }

    /**
     * Mouse Y in GUI-scaled pixels.
     */
    public static float logicalPointerY() {
        Minecraft minecraftClient = Minecraft.getInstance();
        return (float) minecraftClient.mouseHandler.getScaledYPos(minecraftClient.getWindow());
    }
}
