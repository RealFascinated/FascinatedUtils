package cc.fascinated.fascinatedutils.gui.components;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;

/**
 * Ghost icon button that goes back to the previous screen or page.
 */
public class BackButton extends ButtonNode {

    /**
     * Creates the button.
     *
     * @param onPress runs when the button is pressed
     */
    public BackButton(Runnable onPress) {
        setIconCenter(ModUiTextures.BACK.getId());
        setVariant(ButtonVariant.GHOST);
        setOnPress(onPress);
        setTooltip(() -> UiTranslations.INSTANCE.get("alumite.common.back"));
    }
}
