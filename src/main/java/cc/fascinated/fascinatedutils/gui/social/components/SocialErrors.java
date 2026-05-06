package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.Errors;
import net.minecraft.network.chat.Component;

class SocialErrors {

    static String message(Errors error) {
        if (error == null) {
            return Component.translatable("alumite.social.error.generic").getString();
        }
        String translationKey = "alumite.social.error." + error.getCode();
        String translated = Component.translatable(translationKey).getString();
        if (translated.equals(translationKey)) {
            return error.getDisplayText();
        }
        return translated;
    }

    static String message(AlumiteApiException exception) {
        Errors error = exception.getError();
        if (error != null) {
            return message(error);
        }
        String displayText = exception.getDisplayText();
        if (displayText != null && !displayText.isBlank()) {
            return displayText;
        }
        return Component.translatable("alumite.social.error.generic").getString();
    }
}
