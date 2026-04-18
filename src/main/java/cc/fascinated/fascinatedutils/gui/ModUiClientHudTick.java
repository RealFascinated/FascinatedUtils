package cc.fascinated.fascinatedutils.gui;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;

@Getter
public class ModUiClientHudTick {
    public static final ModUiClientHudTick INSTANCE = new ModUiClientHudTick(HUDManager.INSTANCE);

    private final HUDManager hudManager;
    private KeyMapping hudEditorKey;

    private ModUiClientHudTick(HUDManager hudManager) {
        this.hudManager = hudManager;
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    /**
     * Bind the editor key created during Fabric key registration.
     *
     * @param hudEditorKey vanilla key binding for toggling HUD edit mode
     */
    public void setHudEditorKey(KeyMapping hudEditorKey) {
        this.hudEditorKey = hudEditorKey;
    }

    @EventHandler
    private void fascinatedutils$onClientTick(ClientTickEvent event) {
        if (hudEditorKey != null && hudEditorKey.consumeClick()) {
            hudManager.setEditMode(!hudManager.isEditMode());
        }
    }
}
