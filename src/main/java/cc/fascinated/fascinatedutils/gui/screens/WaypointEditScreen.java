package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.input.UiCursorController;
import cc.fascinated.fascinatedutils.gui.modsettings.FColorPickerPopupWidget;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
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

public class WaypointEditScreen extends WidgetScreen {
    private static final int FOCUS_NAME = 6210;
    private static final int FOCUS_X = 6211;
    private static final int FOCUS_Y = 6212;
    private static final int FOCUS_Z = 6213;

    private final FWidgetHost host = new FWidgetHost();
    private final Waypoint waypoint;

    private final FOutlinedTextInputWidget nameInput;
    private final FOutlinedTextInputWidget xInput;
    private final FOutlinedTextInputWidget yInput;
    private final FOutlinedTextInputWidget zInput;

    private final SettingColor color;
    private boolean showColorPicker = false;
    private FColorPickerPopupWidget colorPickerWidget;
    private boolean showBeam;
    private boolean showDistance;


    public WaypointEditScreen(Waypoint waypoint) {
        super(Component.translatable("fascinatedutils.waypoints.edit.title"));
        this.waypoint = waypoint;
        this.color = waypoint.getColor().copy();
        this.showBeam = waypoint.isShowBeam();
        this.showDistance = waypoint.isShowDistance();

        nameInput = new FOutlinedTextInputWidget(FOCUS_NAME, 64, 24f, () -> "");
        nameInput.setValue(waypoint.getName());
        nameInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        xInput = new FOutlinedTextInputWidget(FOCUS_X, 16, 24f, () -> "");
        xInput.setValue(String.valueOf((int) Math.floor(waypoint.getX())));
        xInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        yInput = new FOutlinedTextInputWidget(FOCUS_Y, 16, 24f, () -> "");
        yInput.setValue(String.valueOf((int) Math.floor(waypoint.getY())));
        yInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        zInput = new FOutlinedTextInputWidget(FOCUS_Z, 16, 24f, () -> "");
        zInput.setValue(String.valueOf((int) Math.floor(waypoint.getZ())));
        zInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);

        host.setRoot(buildRootWidget());
    }

    private FWidget buildRootWidget() {
        return new FWidget() {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                FWidget inner = buildContent(lw, lh);
                clearChildren();
                addChild(inner);
                inner.layout(measure, lx, ly, lw, lh);
            }
        };
    }

    private FWidget buildContent(float sw, float sh) {
        FAbsoluteStackWidget stack = new FAbsoluteStackWidget();

        FRectWidget backdrop = new FRectWidget();
        backdrop.setFillColorArgb(0xB0000000);
        stack.addChild(backdrop);

        float cardW = Math.min(sw - 80f, 460f);
        stack.addChild(buildCard(cardW));

        if (colorPickerWidget != null) {
            stack.addChild(colorPickerWidget);
        }

        return stack;
    }

    private FWidget buildCard(float cardW) {
        float pad = UITheme.PADDING_MD;
        float gap = UITheme.GAP_SM;
        float sectionGap = 8f;

        FLabelWidget titleLabel = new FLabelWidget();
        titleLabel.setText(Component.translatable("fascinatedutils.waypoints.edit.title").getString());
        titleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());
        titleLabel.setAlignX(Align.START);

        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(Component.translatable("fascinatedutils.waypoints.create.name").getString());
        nameLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
        nameLabel.setAlignX(Align.START);

        FLabelWidget xLabel = new FLabelWidget();
        xLabel.setText("X");
        xLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
        xLabel.setAlignX(Align.START);

        FLabelWidget yLabel = new FLabelWidget();
        yLabel.setText("Y");
        yLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
        yLabel.setAlignX(Align.START);

        FLabelWidget zLabel = new FLabelWidget();
        zLabel.setText("Z");
        zLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
        zLabel.setAlignX(Align.START);

        FLabelWidget colorLabel = new FLabelWidget();
        colorLabel.setText(Component.translatable("fascinatedutils.waypoints.create.color").getString());
        colorLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
        colorLabel.setAlignX(Align.START);

        // full-width color swatch button — background renders the chosen color
        FButtonWidget colorSwatchBtn = new FButtonWidget(this::openColorPicker, () -> Component.translatable("fascinatedutils.waypoints.create.change_color").getString(), 0f, 1, 2f, 6f, 1f, 8f) {
            @Override
            protected int resolveButtonFillColorArgb(boolean hovered) {
                int argb = color.getPackedArgb();
                if (!hovered) {
                    return argb;
                }
                int red = Math.min(255, ((argb >> 16) & 0xFF) + 20);
                int green = Math.min(255, ((argb >> 8) & 0xFF) + 20);
                int blue = Math.min(255, (argb & 0xFF) + 20);
                return (argb & 0xFF000000) | (red << 16) | (green << 8) | blue;
            }

            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                return UITheme.COLOR_BORDER;
            }
        };

        FButtonWidget cancelBtn = new FButtonWidget(() -> Minecraft.getInstance().setScreen(new WaypointsScreen()), () -> Component.translatable("fascinatedutils.waypoints.popup.cancel").getString(), 100f, 1, 2f, 8f, 1f, 8f);
        FButtonWidget saveBtn = new FButtonWidget(this::save, () -> Component.translatable("fascinatedutils.waypoints.edit.confirm").getString(), 100f, 1, 2f, 8f, 1f, 8f);

        float bodyW = Math.max(0f, cardW - 2f * pad);
        FIconCheckboxWidget beamCheckbox = new FIconCheckboxWidget(showBeam, val -> showBeam = val, () -> Component.translatable("fascinatedutils.waypoints.edit.show_beam").getString(), bodyW);
        FIconCheckboxWidget distanceCheckbox = new FIconCheckboxWidget(showDistance, val -> showDistance = val, () -> Component.translatable("fascinatedutils.waypoints.edit.show_distance").getString(), bodyW);

        return new FWidget() {
            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                clearChildren();

                float bodyW = Math.max(0f, cardW - 2f * pad);
                float coordW = Math.max(0f, (bodyW - 2f * gap) / 3f);

                float titleH = titleLabel.intrinsicHeightForColumn(measure, bodyW);
                float nameLblH = nameLabel.intrinsicHeightForColumn(measure, bodyW);
                float inputH = nameInput.intrinsicHeightForColumn(measure, bodyW);
                float axisLblH = xLabel.intrinsicHeightForColumn(measure, coordW);
                float btnH = cancelBtn.intrinsicHeightForColumn(measure, bodyW);
                float swatchH = btnH;
                float colorLblH = colorLabel.intrinsicHeightForColumn(measure, bodyW);
                float beamH = beamCheckbox.intrinsicHeightForColumn(measure, bodyW);
                float distH = distanceCheckbox.intrinsicHeightForColumn(measure, bodyW);

                float cardH = pad + titleH + sectionGap + nameLblH + gap + inputH + sectionGap + axisLblH + gap + inputH + sectionGap + colorLblH + gap + swatchH + sectionGap + beamH + sectionGap + distH + sectionGap + btnH + pad;

                float cx = lx + (lw - cardW) / 2f;
                float cy = ly + (lh - cardH) / 2f;

                FRectWidget bg = new FRectWidget();
                bg.setFillColorArgb(UITheme.COLOR_SURFACE);
                bg.setCornerRadius(6f);
                bg.setBorder(UITheme.COLOR_BORDER, 1f);
                addChild(bg);
                bg.layout(measure, cx, cy, cardW, cardH);

                float contentX = cx + pad;
                float curY = cy + pad;

                addChild(titleLabel);
                titleLabel.layout(measure, contentX, curY, bodyW, titleH);
                curY += titleH + sectionGap;

                addChild(nameLabel);
                nameLabel.layout(measure, contentX, curY, bodyW, nameLblH);
                curY += nameLblH + gap;
                addChild(nameInput);
                nameInput.layout(measure, contentX, curY, bodyW, inputH);
                curY += inputH + sectionGap;

                addChild(xLabel);
                xLabel.layout(measure, contentX, curY, coordW, axisLblH);
                addChild(yLabel);
                yLabel.layout(measure, contentX + coordW + gap, curY, coordW, axisLblH);
                addChild(zLabel);
                zLabel.layout(measure, contentX + 2f * (coordW + gap), curY, coordW, axisLblH);
                curY += axisLblH + gap;

                addChild(xInput);
                xInput.layout(measure, contentX, curY, coordW, inputH);
                addChild(yInput);
                yInput.layout(measure, contentX + coordW + gap, curY, coordW, inputH);
                addChild(zInput);
                zInput.layout(measure, contentX + 2f * (coordW + gap), curY, coordW, inputH);
                curY += inputH + sectionGap;

                addChild(colorLabel);
                colorLabel.layout(measure, contentX, curY, bodyW, colorLblH);
                curY += colorLblH + gap;

                addChild(colorSwatchBtn);
                colorSwatchBtn.layout(measure, contentX, curY, bodyW, swatchH);
                curY += swatchH + sectionGap;

                addChild(beamCheckbox);
                beamCheckbox.layout(measure, contentX, curY, bodyW, beamH);
                curY += beamH + sectionGap;

                addChild(distanceCheckbox);
                distanceCheckbox.layout(measure, contentX, curY, bodyW, distH);
                curY += distH + sectionGap;

                float actionGap = gap;
                float halfW = Math.max(0f, (bodyW - actionGap) / 2f);
                addChild(cancelBtn);
                cancelBtn.layout(measure, contentX, curY, halfW, btnH);
                addChild(saveBtn);
                saveBtn.layout(measure, contentX + halfW + actionGap, curY, halfW, btnH);
            }
        };
    }

    private void openColorPicker() {
        if (colorPickerWidget != null) {
            return;
        }
        showColorPicker = true;
        colorPickerWidget = new FColorPickerPopupWidget(color.copy(), newColor -> {
            color.set(newColor);
            showColorPicker = false;
            colorPickerWidget = null;
        }, () -> {
            showColorPicker = false;
            colorPickerWidget = null;
        });
    }

    private void save() {
        String trimmed = nameInput.value().trim();
        if (trimmed.isEmpty()) {
            trimmed = "Waypoint";
        }
        waypoint.setName(trimmed);
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
        Minecraft minecraft = Minecraft.getInstance();
        float w = UIScale.uiWidth();
        float h = UIScale.uiHeight();
        float pX = UIScale.uiPointerX();
        float pY = UIScale.uiPointerY();
        float deltaSeconds = minecraft.getDeltaTracker().getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = delta / 20f;
        }

        GuiRenderer renderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
        renderer.begin(w, h);
        host.tickAnimations(deltaSeconds);
        host.layoutAndRender(renderer, 0f, 0f, w, h, pX, pY, deltaSeconds);
        renderer.end();

        host.dispatchInput(new InputEvent.MouseMove(pX, pY));
        UiCursorController.apply(minecraft.getWindow().handle(), host.pointerCursorAt(pX, pY));
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        float pX = UIScale.uiPointerX();
        float pY = UIScale.uiPointerY();
        boolean handled = host.dispatchInput(new InputEvent.MousePress(pX, pY, event.button()));
        return handled || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        float pX = UIScale.uiPointerX();
        float pY = UIScale.uiPointerY();
        host.dispatchInput(new InputEvent.MouseRelease(pX, pY, event.button()));
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        float pX = UIScale.uiPointerX();
        float pY = UIScale.uiPointerY();
        host.dispatchInput(new InputEvent.MouseMove(pX, pY));
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
            if (showColorPicker) {
                showColorPicker = false;
                colorPickerWidget = null;
                return true;
            }
            Minecraft.getInstance().setScreen(new WaypointsScreen());
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (!showColorPicker) {
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
        Minecraft minecraft = Minecraft.getInstance();
        UiCursorController.apply(minecraft.getWindow().handle(), UiPointerCursor.DEFAULT);
        host.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
