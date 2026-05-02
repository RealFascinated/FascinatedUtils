package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.declare.DeclarativeMountHost;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiSlot;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.input.UiCursorController;
import cc.fascinated.fascinatedutils.gui.modsettings.FColorPickerPopupWidget;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.waypoints.components.WaypointEditCardComponent;
import cc.fascinated.fascinatedutils.gui.widgets.FIconCheckboxWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidgetHost;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class WaypointEditScreen extends WidgetScreen {
    private static final int FOCUS_NAME = 6210;
    private static final int FOCUS_X = 6211;
    private static final int FOCUS_Y = 6212;
    private static final int FOCUS_Z = 6213;

    private final FWidgetHost host = new FWidgetHost();
    private final DeclarativeMountHost declarativeMountHost;
    private final Waypoint waypoint;

    private final FOutlinedTextInputWidget nameInput;
    private final FOutlinedTextInputWidget xInput;
    private final FOutlinedTextInputWidget yInput;
    private final FOutlinedTextInputWidget zInput;

    private final SettingColor color;
    private FColorPickerPopupWidget colorPickerWidget;
    private boolean showBeam;
    private boolean showDistance;
    private final FIconCheckboxWidget beamCheckbox;
    private final FIconCheckboxWidget distanceCheckbox;

    public WaypointEditScreen(Waypoint editedWaypoint) {
        super(Component.translatable("fascinatedutils.waypoints.edit.title"));
        this.waypoint = editedWaypoint;
        this.color = editedWaypoint.getColor().copy();
        this.showBeam = editedWaypoint.isShowBeam();
        this.showDistance = editedWaypoint.isShowDistance();

        nameInput = new FOutlinedTextInputWidget(FOCUS_NAME, 64, 24f, () -> "");
        nameInput.setValue(editedWaypoint.getName());
        nameInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        xInput = new FOutlinedTextInputWidget(FOCUS_X, 16, 24f, () -> "");
        xInput.setValue(String.valueOf((int) Math.floor(editedWaypoint.getX())));
        xInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        yInput = new FOutlinedTextInputWidget(FOCUS_Y, 16, 24f, () -> "");
        yInput.setValue(String.valueOf((int) Math.floor(editedWaypoint.getY())));
        yInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        zInput = new FOutlinedTextInputWidget(FOCUS_Z, 16, 24f, () -> "");
        zInput.setValue(String.valueOf((int) Math.floor(editedWaypoint.getZ())));
        zInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        float checkboxBodyWidth = Math.max(0f, 460f - 2f * UITheme.PADDING_MD);
        beamCheckbox = new FIconCheckboxWidget(showBeam, value -> showBeam = value,
                () -> Component.translatable("fascinatedutils.waypoints.edit.show_beam").getString(), checkboxBodyWidth);
        distanceCheckbox = new FIconCheckboxWidget(showDistance, value -> showDistance = value,
                () -> Component.translatable("fascinatedutils.waypoints.edit.show_distance").getString(), checkboxBodyWidth);

        declarativeMountHost = new DeclarativeMountHost(this::buildViewport);
        host.setRoot(declarativeMountHost);
    }

    private UiView buildViewport(float viewportWidth, float viewportHeight) {
        float cardWidth = Math.min(viewportWidth - 80f, 460f);
        float innerMaxHeight = Math.min(viewportHeight - 80f, 640f);
        List<UiSlot> layers = new ArrayList<>();
        layers.add(UiSlot.of(Ui.rectPlain(0xB0000000)));
        layers.add(UiSlot.of(Ui.centerMax(40f, 40f, cardWidth, innerMaxHeight,
                WaypointEditCardComponent.view(new WaypointEditCardComponent.Props(
                        color,
                        nameInput,
                        xInput,
                        yInput,
                        zInput,
                        beamCheckbox,
                        distanceCheckbox,
                        this::openColorPicker,
                        () -> Minecraft.getInstance().setScreen(new WaypointsScreen()),
                        this::save)))));
        if (colorPickerWidget != null) {
            layers.add(UiSlot.keyed("waypoint-edit-color-picker", Ui.widgetSlot("picker", colorPickerWidget)));
        }
        return Ui.stackLayers(layers);
    }

    private void openColorPicker() {
        if (colorPickerWidget != null) {
            return;
        }
        colorPickerWidget = new FColorPickerPopupWidget(color.copy(), newColor -> {
            color.set(newColor);
            colorPickerWidget = null;
        }, () -> colorPickerWidget = null);
    }

    private void save() {
        String trimmedName = nameInput.value().trim();
        if (trimmedName.isEmpty()) {
            trimmedName = "Waypoint";
        }
        waypoint.setName(trimmedName);
        waypoint.setX(parseCoord(xInput.value(), waypoint.getX()));
        waypoint.setY(parseCoord(yInput.value(), waypoint.getY()));
        waypoint.setZ(parseCoord(zInput.value(), waypoint.getZ()));
        waypoint.setColor(color.copy());
        waypoint.setShowBeam(showBeam);
        waypoint.setShowDistance(showDistance);
        ModConfig.waypoints().save();
        Minecraft.getInstance().setScreen(new WaypointsScreen());
    }

    private double parseCoord(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    public void renderBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        float width = UIScale.uiWidth();
        float height = UIScale.uiHeight();
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        float deltaSeconds = minecraftClient.getDeltaTracker().getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = delta / 20f;
        }

        GuiRenderer renderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
        renderer.begin(width, height);
        host.tickAnimations(deltaSeconds);
        host.layoutAndRender(renderer, 0f, 0f, width, height, pointerX, pointerY, deltaSeconds);
        renderer.end();

        host.dispatchInput(new InputEvent.MouseMove(pointerX, pointerY));
        UiCursorController.apply(minecraftClient.getWindow().handle(), host.pointerCursorAt(pointerX, pointerY));
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        boolean handled = host.dispatchInput(new InputEvent.MousePress(pointerX, pointerY, event.button()));
        return handled || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        host.dispatchInput(new InputEvent.MouseRelease(pointerX, pointerY, event.button()));
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        host.dispatchInput(new InputEvent.MouseMove(pointerX, pointerY));
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean handled = host.dispatchInput(new InputEvent.KeyPress(event.key(), event.scancode(), event.modifiers()));
        if (handled) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (colorPickerWidget != null) {
                colorPickerWidget = null;
                return true;
            }
            Minecraft.getInstance().setScreen(new WaypointsScreen());
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (colorPickerWidget == null) {
                save();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int codepoint = event.codepoint();
        if (codepoint >= 0 && codepoint <= 0xFFFF) {
            return host.dispatchInput(new InputEvent.CharType((char) codepoint));
        }
        return super.charTyped(event);
    }

    @Override
    public void removed() {
        Minecraft minecraftClient = Minecraft.getInstance();
        UiCursorController.apply(minecraftClient.getWindow().handle(), UiPointerCursor.DEFAULT);
        declarativeMountHost.dispose();
        host.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
