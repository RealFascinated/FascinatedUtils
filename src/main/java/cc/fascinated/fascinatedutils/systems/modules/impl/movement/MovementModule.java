package cc.fascinated.fascinatedutils.systems.modules.impl.movement;

import cc.fascinated.fascinatedutils.common.util.PlayerUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.modules.impl.movement.hud.MovementHudPanel;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Getter
public class MovementModule extends HudHostModule {

    private final BooleanSetting toggleSprint = toggleSetting("toggle_sprint", Options::toggleSprint);

    private final BooleanSetting toggleSneak = toggleSetting("toggle_sneak", Options::toggleCrouch);

    private final BooleanSetting enableFlightSpeedModifier = BooleanSetting.builder().id("enable_flight_speed_modifier").defaultValue(false).categoryDisplayKey("alumite.setting.category.flight_speed").build();

    private final SliderSetting flightSpeedModifier = SliderSetting.builder().id("flight_speed_modifier").defaultValue(2f).minValue(1f).maxValue(10f).step(1f).categoryDisplayKey("alumite.setting.category.flight_speed").build();


    public MovementModule() {
        super("movement", "Movement", HudDefaults.builder().defaultState(true).defaultAnchor(HUDWidgetAnchor.TOP_RIGHT).defaultXOffset(5).defaultYOffset(5).build());
        addAppearanceSettings(true);
        addSetting(toggleSprint);
        addSetting(toggleSneak);
        addSetting(enableFlightSpeedModifier);
        addSetting(flightSpeedModifier);
        enableFlightSpeedModifier.addSubSetting(flightSpeedModifier);
        registerHudPanel(new MovementHudPanel(this));
    }

    private static BooleanSetting toggleSetting(String id, Function<Options, OptionInstance<Boolean>> optionResolver) {
        return BooleanSetting.builder().id(id).defaultValue(false).bind(() -> optionResolver.apply(Minecraft.getInstance().options).get(), (enabled) -> {
            Options options = Minecraft.getInstance().options;
            optionResolver.apply(options).set(enabled);
            options.save();
        }).build();
    }

    @Override
    protected boolean appearanceDefaultShowBackground() {
        return false;
    }

    @Override
    protected float appearanceDefaultPadding() {
        return 0f;
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
        }
        else if (sprintToggledOn) {
            line = "Sprinting (Toggled)";
        }
        else if (player.isSprinting()) {
            line = "Sprinting";
        }
        else {
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
