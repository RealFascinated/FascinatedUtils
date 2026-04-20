package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.UIScale;

public class ModSettingsShellPointer {

    /**
     * Maps the current pointer into shell layout space.
     *
     * @return layout-space pointer coordinates
     */
    public static LayoutPoint layoutPointInShellSpace() {
        ModSettingsShellLayout.ShellPointerMapping shellMapping = ModSettingsShellLayout.pointMappingForCanvas();
        return new LayoutPoint(shellMapping.layoutX(UIScale.uiPointerX()), shellMapping.layoutY(UIScale.uiPointerY()));
    }

    /**
     * Pointer position in mod settings shell layout space.
     *
     * @param layoutPositionX layout X coordinate
     * @param layoutPositionY layout Y coordinate
     */
    public record LayoutPoint(float layoutPositionX, float layoutPositionY) {}
}
