package cc.fascinated.fascinatedutils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class UiSounds {
    /**
     * Play the standard Minecraft UI button click at master volume (matches vanilla screens).
     */
    public static void playButtonClick() {
        Minecraft client = Minecraft.getInstance();
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
