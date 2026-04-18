package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.screens.ModSettingsScreen;

public class ModSettingsShellPointer {

    /**
     * Maps the current pointer into shell layout space.
     *
     * <p>Call only while {@link GuiDesignSpace} is active with the same
     * framebuffer scales as {@link ModSettingsScreen#renderCustom} (so shell bounds and widget hit geometry agree).
     *
     * @return layout-space pointer coordinates
     */
    public static LayoutPoint layoutPointInShellSpace() {
        ModSettingsShellLayout.ShellPointerMapping shellMapping = ModSettingsShellLayout.pointMappingForCanvas();
        return new LayoutPoint(shellMapping.layoutX(UIScale.hiResPointerX()), shellMapping.layoutY(UIScale.hiResPointerY()));
    }

    /**
     * Pointer position in mod settings shell layout space.
     *
     * @param layoutPositionX layout X coordinate
     * @param layoutPositionY layout Y coordinate
     */
    public record LayoutPoint(float layoutPositionX, float layoutPositionY) {}
}
