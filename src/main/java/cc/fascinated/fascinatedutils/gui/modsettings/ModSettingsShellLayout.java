package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;

@UtilityClass
public class ModSettingsShellLayout {

    /**
     * Size and position the mod settings shell from canvas dimensions, snapping outer bounds to integer pixels.
     */
    public static ShellBounds computeShell(float canvasWidth, float canvasHeight) {
        float maxW = Math.min(GuiDesignSpace.pxX(ModSettingsTheme.PANEL_MAX_W), canvasWidth * ModSettingsTheme.SHELL_MAX_WIDTH_FRAC);
        float maxH = Math.min(GuiDesignSpace.pxY(ModSettingsTheme.PANEL_MAX_H), canvasHeight * ModSettingsTheme.SHELL_MAX_HEIGHT_FRAC);
        float aspectWidth = ModSettingsTheme.PANEL_ASPECT_W;
        float aspectHeight = ModSettingsTheme.PANEL_ASPECT_H;
        float shellW = maxW;
        float shellH = shellW * aspectHeight / aspectWidth;
        if (shellH > maxH) {
            shellH = maxH;
            shellW = shellH * aspectWidth / aspectHeight;
        }
        shellW = Math.min(shellW, maxW);
        float shellX = (canvasWidth - shellW) * 0.5f;
        float shellY = (canvasHeight - shellH) * 0.5f;
        float pixelShellX = Mth.floor(shellX);
        float pixelShellY = Mth.floor(shellY);
        float pixelShellWidth = Mth.ceil(shellX + shellW) - pixelShellX;
        float pixelShellHeight = Mth.ceil(shellY + shellH) - pixelShellY;
        return new ShellBounds(pixelShellX, pixelShellY, pixelShellWidth, pixelShellHeight);
    }

    /**
     * Build pointer mapping for shell transform state.
     */
    public static ShellPointerMapping computePointMapping(ShellBounds shell) {
        float shellCenterX = shell.positionX() + shell.width() * 0.5f;
        float shellCenterY = shell.positionY() + shell.height() * 0.5f;
        return new ShellPointerMapping(shellCenterX, shellCenterY, 1f);
    }

    /**
     * Shell layout mapping for the current window using {@link UIScale} canvas size.
     */
    public static ShellPointerMapping pointMappingForCanvas() {
        ShellBounds shell = computeShell(UIScale.physicalWidth(), UIScale.physicalHeight());
        return computePointMapping(shell);
    }

    /**
     * Axis-aligned bounds for shell chrome hit-testing and layout, in shell layout units.
     */
    public record ShellBounds(float positionX, float positionY, float width, float height) {
        /**
         * Whether the point lies inside the rectangle (half-open on the right and bottom edges).
         */
        public boolean contains(float pointX, float pointY) {
            return pointX >= positionX && pointY >= positionY && pointX < positionX + width && pointY < positionY + height;
        }
    }

    /**
     * Maps high-resolution pointer coordinates into shell layout space for the current shell transform.
     */
    public record ShellPointerMapping(float centerX, float centerY, float scale) {
        private static float shellPointerToLayoutX(float screenX, float shellCenterX, float shellScale) {
            float safeScale = Math.max(shellScale, 0.001f);
            return (screenX - (1f - shellScale) * shellCenterX) / safeScale;
        }

        private static float shellPointerToLayoutY(float screenY, float shellCenterY, float shellScale) {
            float safeScale = Math.max(shellScale, 0.001f);
            return (screenY - (1f - shellScale) * shellCenterY) / safeScale;
        }

        /**
         * Map a screen X coordinate into layout X inside the scaled shell.
         */
        public float layoutX(float screenX) {
            return shellPointerToLayoutX(screenX, centerX, scale);
        }

        /**
         * Map a screen Y coordinate into layout Y inside the scaled shell.
         */
        public float layoutY(float screenY) {
            return shellPointerToLayoutY(screenY, centerY, scale);
        }
    }
}
