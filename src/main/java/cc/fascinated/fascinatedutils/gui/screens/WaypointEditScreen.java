package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.input.UiCursorController;
import cc.fascinated.fascinatedutils.gui.modsettings.FColorPickerPopupWidget;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.waypoints.components.WaypointEditCardComponent;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public class WaypointEditScreen extends WidgetScreen {

    private final FWidgetHost host = new FWidgetHost();
    private final FWidget root;
    private final Waypoint waypoint;

    private final FOutlinedTextInputWidget nameInput;
    private final FOutlinedTextInputWidget xInput;
    private final FOutlinedTextInputWidget yInput;
    private final FOutlinedTextInputWidget zInput;

    private final SettingColor color;
    private final FIconCheckboxWidget beamCheckbox;
    private final FIconCheckboxWidget distanceCheckbox;
    private FColorPickerPopupWidget colorPickerWidget;
    private boolean showBeam;
    private boolean showDistance;

    public WaypointEditScreen(Waypoint editedWaypoint) {
        super(Component.translatable("alumite.waypoints.edit.title"));
        this.waypoint = editedWaypoint;
        this.color = editedWaypoint.getColor().copy();
        this.showBeam = editedWaypoint.isShowBeam();
        this.showDistance = editedWaypoint.isShowDistance();

        nameInput = new FOutlinedTextInputWidget(64, 24f, () -> "");
        nameInput.setValue(editedWaypoint.getName());

        xInput = new FOutlinedTextInputWidget(16, 24f, () -> "");
        xInput.setValue(String.valueOf((int) Math.floor(editedWaypoint.getX())));

        yInput = new FOutlinedTextInputWidget(16, 24f, () -> "");
        yInput.setValue(String.valueOf((int) Math.floor(editedWaypoint.getY())));

        zInput = new FOutlinedTextInputWidget(16, 24f, () -> "");
        zInput.setValue(String.valueOf((int) Math.floor(editedWaypoint.getZ())));

        float checkboxBodyWidth = Math.max(0f, 460f - 2f * UITheme.PADDING_MD);
        beamCheckbox = new FIconCheckboxWidget(showBeam, value -> showBeam = value, () -> Component.translatable("alumite.waypoints.edit.show_beam").getString(), checkboxBodyWidth);
        distanceCheckbox = new FIconCheckboxWidget(showDistance, value -> showDistance = value, () -> Component.translatable("alumite.waypoints.edit.show_distance").getString(), checkboxBodyWidth);

        this.root = buildBody();
        host.setRoot(root);
    }

    private FWidget buildBody() {
        return new FWidget() {
            float lastWidth = Float.NaN;
            float lastHeight = Float.NaN;
            FColorPickerPopupWidget lastColorPicker = null;

            @Override
            public boolean fillsHorizontalInRow() { return true; }

            @Override
            public boolean fillsVerticalInColumn() { return true; }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                if (Math.abs(lw - lastWidth) > 0.5f || Math.abs(lh - lastHeight) > 0.5f
                        || lastColorPicker != colorPickerWidget || childrenView().isEmpty()) {
                    lastWidth = lw;
                    lastHeight = lh;
                    lastColorPicker = colorPickerWidget;
                    clearChildren();
                    float cardWidth = Math.min(lw - 80f, 460f);
                    float innerMaxHeight = Math.min(lh - 80f, 640f);
                    FRectWidget backdrop = new FRectWidget();
                    backdrop.setFillColorArgb(0xB0000000);
                    addChild(backdrop);
                    addChild(new FMaxCenterInsetsWidget(40f, 40f, cardWidth, innerMaxHeight,
                            WaypointEditCardComponent.build(color, nameInput, xInput, yInput, zInput,
                                    beamCheckbox, distanceCheckbox,
                                    WaypointEditScreen.this::openColorPicker,
                                    () -> Minecraft.getInstance().setScreen(new WaypointsScreen()),
                                    WaypointEditScreen.this::save)));
                    if (colorPickerWidget != null) {
                        addChild(colorPickerWidget);
                    }
                }
                for (FWidget child : childrenView()) {
                        child.layout(measure, lx, ly, lw, lh);
                    }
            }
        };
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
            trimmedName = I18n.get("alumite.waypoints.default_name");
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
        host.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
