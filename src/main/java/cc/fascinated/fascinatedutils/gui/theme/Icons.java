package cc.fascinated.fascinatedutils.gui.theme;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Icons {

    public static void paintSettingResetCharacter(UIRenderer renderer, float positionX, float positionY, float width, float height, int tintArgb) {
        paintSquareChromeIcon(renderer, ModUiTextures.RESET, positionX, positionY, width, height, tintArgb);
    }

    public static void paintModSettingsCloseIcon(UIRenderer renderer, float positionX, float positionY, float width, float height, int tintArgb) {
        paintSquareChromeIcon(renderer, ModUiTextures.CLOSE, positionX, positionY, width, height, tintArgb);
    }

    public static void paintModSettingsBackIcon(UIRenderer renderer, float positionX, float positionY, float width, float height, int tintArgb) {
        paintSquareChromeIcon(renderer, ModUiTextures.BACK, positionX, positionY, width, height, tintArgb);
    }

    public static void paintTrashIcon(UIRenderer renderer, float positionX, float positionY, float width, float height, int tintArgb) {
        paintSquareChromeIcon(renderer, ModUiTextures.TRASH, positionX, positionY, width, height, tintArgb);
    }

    private static void paintSquareChromeIcon(UIRenderer renderer, ModUiTextures chrome, float positionX, float positionY, float width, float height, int tintArgb) {
        float box = Math.min(width, height);
        if (box < 2f) {
            return;
        }
        float drawX = positionX + (width - box) * 0.5f;
        float drawY = positionY + (height - box) * 0.5f;
        renderer.drawTexture(chrome.getId(), drawX, drawY, box, box, tintArgb);
    }
}
