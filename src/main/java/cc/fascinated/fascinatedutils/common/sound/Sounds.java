package cc.fascinated.fascinatedutils.common.sound;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

@Getter
public enum Sounds {
	NOTIFICATION(SoundEvents.AMETHYST_BLOCK_HIT),
	UI_CLICK(SoundEvents.UI_BUTTON_CLICK.value()),
	;

	private final SoundEvent id;

	Sounds(SoundEvent id) {
		this.id = id;
	}

	/**
	 * Plays the sound to the player
	 */
	public void play() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(this.id, 1.0f));
	}
}
