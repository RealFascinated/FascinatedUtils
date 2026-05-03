package cc.fascinated.fascinatedutils.systems.modules.impl.movement;

import cc.fascinated.fascinatedutils.common.PlayerUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.modules.impl.movement.hud.MovementHudPanel;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;
import java.util.Locale;

@Getter
public class MovementModule extends HudHostModule {

    private final BooleanSetting enableFlightSpeedModifier = BooleanSetting.builder().id("enable_flight_speed_modifier")
            .defaultValue(false)
            .categoryDisplayKey("Flight Speed")
            .build();

    private final SliderSetting flightSpeedModifier = SliderSetting.builder().id("flight_speed_modifier")
            .defaultValue(2f)
            .minValue(2f)
            .maxValue(10f)
            .step(1f)
            .categoryDisplayKey("Flight Speed")
            .build();

    public MovementModule() {
        super("movement", "Movement", HudDefaults.builder()
                .defaultState(true)
                .defaultAnchor(HUDWidgetAnchor.TOP_RIGHT)
                .defaultXOffset(5)
                .defaultYOffset(5)
                .build()
        );
        MiniMessageHudChrome.register(this);
        addSetting(enableFlightSpeedModifier);
        addSetting(flightSpeedModifier);
        registerHudPanel(new MovementHudPanel(this));
    }

    private List<String> resolveMovementLines(boolean editorMode) {
        Minecraft minecraftClient = Minecraft.getInstance();
        LocalPlayer player = minecraftClient.player;
        if (minecraftClient.level == null || player == null) {
            return editorMode ? List.of("<grey>Movement</grey>") : null;
        }

        boolean sprintPhysicallyHeld = PlayerUtils.isSprintBindingPhysicallyHeld(minecraftClient);
        boolean sprintToggledOn = PlayerUtils.isSprintBindingToggledOn(minecraftClient);
        boolean flying = player.getAbilities().flying && !player.isPassenger();

        String line;
        if (flying) {
            line = "Flying";
        } else if (sprintToggledOn) {
            line = "Sprinting (Toggled)";
        } else if (player.isSprinting()) {
            line = "Sprinting";
        } else {
            return editorMode ? List.of("<grey>Movement</grey>") : null;
        }

        if (isFlyBoostActive(player, sprintPhysicallyHeld)) {
            line += " (%sx Fly Boost)".formatted(formatMultiplier(flightSpeedModifier.getValue().floatValue()));
        }
        return List.of(line);
    }

    boolean isFlyBoostActive(LocalPlayer player, boolean sprintPhysicallyHeld) {
        if (!enableFlightSpeedModifier.isEnabled()) {
            return false;
        }
        if (!player.getAbilities().flying || player.isPassenger()) {
            return false;
        }
        if ((!player.isCreative() && !player.isSpectator()) || !sprintPhysicallyHeld) {
            return false;
        }
        return flightSpeedModifier.getValue().floatValue() > 1f;
    }

    private String formatMultiplier(float multiplier) {
        if (Math.rint(multiplier) == multiplier) {
            return Integer.toString((int) multiplier);
        }
        return String.format(Locale.ENGLISH, "%.1f", multiplier);
    }

    /**
     * HUD text lines for the movement widget, or {@code null} when nothing should show in-game.
     *
     * @param editorMode layout editor preview
     * @return formatted lines, or {@code null}
     */
    public List<String> resolveMovementHudLines(boolean editorMode) {
        return resolveMovementLines(editorMode);
    }
}
