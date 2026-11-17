package cc.fascinated.fascinatedutils.gui.screens.impl.settings;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;

/**
 * One page of the settings shell: its header controls, its content and any popups it owns.
 */
public interface SettingsTab {

    /**
     * Adds the page's controls to the shell header, before the shared close button.
     *
     * @param header the header row
     */
    void buildHeader(PositionedNode<?> header);

    /**
     * Adds the page's content.
     *
     * @param contents the content column below the header
     */
    void buildContents(PositionedNode<?> contents);

    /**
     * Adds the popups the page currently needs on top of the window.
     *
     * @param root the screen root
     */
    default void addPopups(PositionedNode<?> root) {
    }
}
