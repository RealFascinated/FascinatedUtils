package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.input.UiCursorController;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.waypoints.WaypointDeletePopupWidget;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public class WaypointsScreen extends WidgetScreen {
    private static final int FOCUS_SEARCH = 6220;
    private static final float CARD_PAD = 12f;
    private static final float ROW_H = 52f;
    private static final float COLOR_BAR_W = 6f;
    private static final float ACTION_BTN_W = 20f;
    private static final float ACTION_BTN_H = 20f;
    private static final float BTN_GAP = 3f;
    private static final float ADD_BTN_W = 64f;

    private final FWidgetHost host = new FWidgetHost();
    private final Ref<Float> scrollYRef = Ref.of(0f);
    private final String worldKey;
    private final FOutlinedTextInputWidget searchInput;
    private float scrollAccum;
    private @Nullable UUID pendingDeleteId;


    public WaypointsScreen() {
        super(Component.translatable("fascinatedutils.waypoints.title"));
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            this.worldKey = "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        else {
            ServerData server = mc.getCurrentServer();
            this.worldKey = "mp:" + (server != null ? server.ip : "unknown");
        }
        searchInput = new FOutlinedTextInputWidget(FOCUS_SEARCH, 64, 24f, () -> Component.translatable("fascinatedutils.waypoints.search").getString());
        searchInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);
        searchInput.setOnChange(value -> {
            scrollYRef.setValue(0f);
        });
        host.setRoot(buildRootWidget());
    }

    private FWidget buildRootWidget() {
        return new FWidget() {
            private FWidget inner;

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
                inner = buildContent(lw, lh);
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

        float cardW = Math.min(sw - 60f, 540f);
        float cardH = Math.min(sh - 60f, 520f);
        stack.addChild(buildCard(cardW, cardH));

        if (pendingDeleteId != null) {
            UUID deleteId = pendingDeleteId;
            ModConfig.waypoints().findById(deleteId).ifPresent(waypoint -> stack.addChild(new WaypointDeletePopupWidget(waypoint.getName(), () -> {
                pendingDeleteId = null;
            }, () -> {
                ModConfig.waypoints().delete(deleteId);
                pendingDeleteId = null;
            })));
        }

        return stack;
    }

    private FWidget buildCard(float cardW, float cardH) {
        FRectWidget cardBg = new FRectWidget();
        cardBg.setFillColorArgb(UITheme.COLOR_SURFACE);
        cardBg.setCornerRadius(8f);
        cardBg.setBorder(UITheme.COLOR_BORDER, 1f);

        FWidget header = buildHeader();
        FScrollColumnWidget list = buildList(cardW);

        FWidget cardContent = new FWidget() {
            {
                addChild(header);
                addChild(searchInput);
                addChild(list);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float headerH = 32f;
                float innerX = lx + CARD_PAD;
                float innerW = lw - 2f * CARD_PAD;
                float curY = ly + CARD_PAD;

                header.layout(measure, innerX, curY, innerW, headerH);
                curY += headerH + 6f;

                float searchH = searchInput.intrinsicHeightForColumn(measure, innerW);
                searchInput.layout(measure, innerX, curY, innerW, searchH);
                curY += searchH + 8f;

                float listH = Math.max(0f, lh - (curY - ly) - CARD_PAD);
                list.layout(measure, innerX, curY, innerW, listH);
            }
        };

        return new FWidget() {
            {
                addChild(cardBg);
                addChild(cardContent);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                float cx = lx + (lw - cardW) / 2f;
                float cy = ly + (lh - cardH) / 2f;
                setBounds(cx, cy, cardW, cardH);
                cardBg.layout(measure, cx, cy, cardW, cardH);
                cardContent.layout(measure, cx, cy, cardW, cardH);
            }
        };
    }

    private FWidget buildHeader() {
        FLabelWidget titleLabel = new FLabelWidget();
        titleLabel.setText(Component.translatable("fascinatedutils.waypoints.title").getString());
        titleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());
        titleLabel.setTextBold(true);
        titleLabel.setAlignY(Align.CENTER);

        FButtonWidget addBtn = new FButtonWidget(this::openCreateScreen, () -> Component.translatable("fascinatedutils.waypoints.add").getString(), ADD_BTN_W, 1, 1f, 4f, 1f, 8f) {
            @Override
            protected int resolveButtonFillColorArgb(boolean hovered) {
                return hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
            }

            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                return hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
            }
        };
        FButtonWidget closeBtn = new FButtonWidget(() -> Minecraft.getInstance().setScreen(null), () -> "✕", 22f, 1, 1f, 4f, 1f, 4f);

        return new FWidget() {
            {
                addChild(titleLabel);
                addChild(addBtn);
                addChild(closeBtn);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float closeBtnW = 22f;
                float rightW = ADD_BTN_W + BTN_GAP + closeBtnW;
                float labelsW = Math.max(0f, lw - rightW - BTN_GAP);
                titleLabel.layout(measure, lx, ly, labelsW, lh);
                float btnY = ly + (lh - ACTION_BTN_H) / 2f;
                addBtn.layout(measure, lx + lw - rightW, btnY, ADD_BTN_W, ACTION_BTN_H);
                closeBtn.layout(measure, lx + lw - closeBtnW, btnY, closeBtnW, ACTION_BTN_H);
            }
        };
    }

    private FScrollColumnWidget buildList(float cardW) {
        float innerW = cardW - 2f * CARD_PAD;
        FColumnWidget body = new FColumnWidget(5f, Align.START);

        String query = searchInput.value().trim().toLowerCase(java.util.Locale.ROOT);
        List<Waypoint> allWaypoints = ModConfig.waypoints().getForWorld(worldKey);
        List<Waypoint> waypoints = allWaypoints.stream().filter(w -> query.isEmpty() || w.getName().toLowerCase(java.util.Locale.ROOT).contains(query)).toList();

        if (waypoints.isEmpty()) {
            FLabelWidget emptyLabel = new FLabelWidget();
            emptyLabel.setText(query.isEmpty() ? Component.translatable("fascinatedutils.waypoints.empty").getString() : Component.translatable("fascinatedutils.waypoints.no_results").getString());
            emptyLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            emptyLabel.setAlignX(Align.CENTER);
            body.addChild(emptyLabel);
        }
        else {
            for (Waypoint waypoint : waypoints) {
                body.addChild(buildWaypointRow(waypoint, innerW));
            }
        }

        FScrollColumnWidget scroll = FTheme.components().createScrollColumn(body, 3f);
        scroll.setFillVerticalInColumn(true);
        Float savedY = scrollYRef.getValue();
        scroll.setScrollOffsetY(savedY == null ? 0f : savedY);
        scroll.setScrollOffsetChangeListener(scrollYRef::setValue);
        return scroll;
    }

    private FWidget buildWaypointRow(Waypoint waypoint, float rowW) {
        UUID id = waypoint.getId();
        boolean visible = waypoint.isVisible();
        int nameColor = visible ? FascinatedGuiTheme.INSTANCE.textPrimary() : UITheme.COLOR_TEXT_DISABLED;
        int subColor = visible ? FascinatedGuiTheme.INSTANCE.textMuted() : UITheme.COLOR_TEXT_DISABLED;

        float contentPad = 8f;
        float buttonsSection = ACTION_BTN_W * 3 + BTN_GAP * 2;
        float nameColW = Math.max(0f, rowW - COLOR_BAR_W - contentPad - buttonsSection - 8f);

        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(waypoint.getName().isBlank() ? "(unnamed)" : waypoint.getName());
        nameLabel.setColorArgb(nameColor);
        nameLabel.setTextBold(true);
        nameLabel.setOverflow(TextOverflow.ELLIPSIS);

        FLabelWidget coordsLabel = new FLabelWidget();
        coordsLabel.setText((int) waypoint.getX() + ", " + (int) waypoint.getY() + ", " + (int) waypoint.getZ());
        coordsLabel.setColorArgb(subColor);
        coordsLabel.setOverflow(TextOverflow.ELLIPSIS);

        FLabelWidget dimensionLabel = new FLabelWidget();
        dimensionLabel.setText(formatDimension(waypoint.getDimension()));
        dimensionLabel.setColorArgb(subColor);
        dimensionLabel.setOverflow(TextOverflow.ELLIPSIS);

        FColumnWidget nameCol = new FColumnWidget(2f, Align.START);
        nameCol.addChild(nameLabel);
        nameCol.addChild(coordsLabel);
        nameCol.addChild(dimensionLabel);

        FButtonWidget renameBtn = iconButton(() -> Minecraft.getInstance().setScreen(new WaypointEditScreen(waypoint)), ModUiTextures.EDIT, UITheme.COLOR_TEXT_DISABLED, UITheme.COLOR_TEXT_PRIMARY);
        FButtonWidget hideBtn = iconButton(() -> {
            waypoint.setVisible(!waypoint.isVisible());
            ModConfig.waypoints().save();
        }, waypoint.isVisible() ? ModUiTextures.VISIBILITY : ModUiTextures.VISIBILITY_OFF, UITheme.COLOR_TEXT_DISABLED, UITheme.COLOR_TEXT_PRIMARY);
        FButtonWidget deleteBtn = iconButton(() -> { pendingDeleteId = id; }, ModUiTextures.TRASH, UITheme.COLOR_TEXT_DISABLED, 0xFFFF5555);

        FRectWidget rowBg = new FRectWidget();
        rowBg.setFillColorArgb(UITheme.COLOR_BACKGROUND);
        rowBg.setCornerRadius(5f);

        FRectWidget colorBar = new FRectWidget();
        colorBar.setFillColorArgb(waypoint.getColor().getResolvedArgb());
        colorBar.setCornerRadius(3f);

        return new FWidget() {
            {
                addChild(rowBg);
                addChild(colorBar);
                addChild(nameCol);
                addChild(renameBtn);
                addChild(hideBtn);
                addChild(deleteBtn);
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return ROW_H;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                rowBg.layout(measure, lx, ly, lw, lh);
                colorBar.layout(measure, lx, ly + 2f, COLOR_BAR_W, lh - 4f);
                float contentX = lx + COLOR_BAR_W + contentPad;
                float vertPad = 6f;
                nameCol.layout(measure, contentX, ly + vertPad, nameColW, lh - 2f * vertPad);
                float btnY = ly + (lh - ACTION_BTN_H) / 2f;
                float btnX = lx + lw - buttonsSection;
                renameBtn.layout(measure, btnX, btnY, ACTION_BTN_W, ACTION_BTN_H);
                btnX += ACTION_BTN_W + BTN_GAP;
                hideBtn.layout(measure, btnX, btnY, ACTION_BTN_W, ACTION_BTN_H);
                btnX += ACTION_BTN_W + BTN_GAP;
                deleteBtn.layout(measure, btnX, btnY, ACTION_BTN_W, ACTION_BTN_H);
            }
        };
    }

    private String formatDimension(String dimension) {
        String result = dimension.replace("minecraft:", "").replace("_", " ");
        if (result.isEmpty()) {
            return dimension;
        }
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    private FButtonWidget iconButton(Runnable onClick, ModUiTextures icon, int normalColor, int hoverColor) {
        return new FButtonWidget(onClick, () -> "", ACTION_BTN_W, 1, 1f, 3f, 1f, 3f) {
            @Override
            protected int resolveButtonFillColorArgb(boolean hovered) {
                return 0x00000000;
            }

            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                return 0x00000000;
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
                hovered = containsPoint(mouseX, mouseY);
                int tint = hovered ? hoverColor : normalColor;
                float iconSize = Math.min(w(), h()) - 4f;
                float iconX = x() + (w() - iconSize) / 2f;
                float iconY = y() + (h() - iconSize) / 2f;
                graphics.drawTexture(icon.getId(), iconX, iconY, iconSize, iconSize, tint);
            }
        };
    }

    private void openCreateScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        String dimension = mc.level.dimension().identifier().toString();
        mc.setScreen(new WaypointCreateScreen(mc.player.getX(), mc.player.getY(), mc.player.getZ(), dimension, worldKey));
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    public void renderBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // intentionally empty; the dim overlay is drawn by our widget tree
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        float w = UIScale.uiWidth();
        float h = UIScale.uiHeight();
        float pX = UIScale.uiPointerX();
        float pY = UIScale.uiPointerY();
        float deltaSeconds = minecraftClient.getDeltaTracker().getGameTimeDeltaTicks() / 20f;
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

        if (scrollAccum != 0f) {
            host.dispatchInput(new InputEvent.MouseScroll(pX, pY, scrollAccum));
            scrollAccum = 0f;
        }
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
        scrollAccum += (float) verticalAmount;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean handled = host.dispatchInput(new InputEvent.KeyPress(event.key(), event.scancode(), event.modifiers()));
        if (handled) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(null);
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
