package cc.fascinated.fascinatedutils.gui.components;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;

/**
 * Ghost icon button that closes the surrounding screen.
 */
public class CloseButton extends ButtonNode {

    /**
     * Creates the button.
     *
     * @param onPress runs when the button is pressed
     */
    public CloseButton(Runnable onPress) {
        setIconCenter(ModUiTextures.CLOSE.getId());
        setVariant(ButtonVariant.GHOST);
        setOnPress(onPress);
        setTooltip(() -> UiTranslations.INSTANCE.get("alumite.common.close"));
    }
}
