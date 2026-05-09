package cc.fascinated.fascinatedutils.oldgui.screens;

import cc.fascinated.fascinatedutils.gui2.core.UIScale;
import cc.fascinated.fascinatedutils.oldgui.core.InputEvent;
import cc.fascinated.fascinatedutils.oldgui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.oldgui.input.UiCursorController;
import cc.fascinated.fascinatedutils.oldgui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.oldgui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.oldgui.waypoints.components.WaypointsRootComponent;
import cc.fascinated.fascinatedutils.oldgui.widgets.FWidgetHost;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class WaypointsScreen extends WidgetScreen {
    private final FWidgetHost host = new FWidgetHost();
    private final WaypointsRootComponent rootWidget;
    private final String worldKey;
    private float scrollAccum;

    public WaypointsScreen() {
        super(Component.translatable("alumite.waypoints.title"));
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient.getSingleplayerServer() != null) {
            this.worldKey = "sp:" + minecraftClient.getSingleplayerServer().getWorldData().getLevelName();
        }
        else {
            ServerData serverData = minecraftClient.getCurrentServer();
            this.worldKey = "mp:" + (serverData != null ? serverData.ip : "unknown");
        }
        this.rootWidget = new WaypointsRootComponent(
                worldKey,
                this::openCreateScreen,
                () -> Minecraft.getInstance().setScreen(null),
                waypoint -> Minecraft.getInstance().setScreen(new WaypointEditScreen(waypoint)),
                waypoint -> { waypoint.setVisible(!waypoint.isVisible()); ModConfig.waypoints().save(); },
                this::formatDimension);
        host.setRoot(rootWidget);
    }

    private String formatDimension(String dimensionId) {
        String formatted = dimensionId.replace("minecraft:", "").replace("_", " ");
        if (formatted.isEmpty()) {
            return dimensionId;
        }
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }

    private void openCreateScreen() {
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient.player == null || minecraftClient.level == null) {
            return;
        }
        String dimensionId = minecraftClient.level.dimension().identifier().toString();
        minecraftClient.setScreen(new WaypointCreateScreen(minecraftClient.player.getX(), minecraftClient.player.getY(), minecraftClient.player.getZ(), dimensionId, worldKey));
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

        if (scrollAccum != 0f) {
            host.dispatchInput(new InputEvent.MouseScroll(pointerX, pointerY, scrollAccum));
            scrollAccum = 0f;
        }
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
        scrollAccum += (float) verticalAmount;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean handled = host.dispatchInput(new InputEvent.KeyPress(event.key(), event.scancode(), event.modifiers()));
        if (handled) {
            return true;
        }
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
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
        rootWidget.dispose();
        host.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
