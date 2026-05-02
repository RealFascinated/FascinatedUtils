package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.declare.DeclarativeMountHost;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.input.UiCursorController;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.waypoints.components.WaypointsListComponent;
import cc.fascinated.fascinatedutils.gui.waypoints.components.WaypointsRootComponent;
import cc.fascinated.fascinatedutils.gui.widgets.FWidgetHost;
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
    private final DeclarativeMountHost declarativeMountHost;
    private final String worldKey;
    private float scrollAccum;

    public WaypointsScreen() {
        super(Component.translatable("fascinatedutils.waypoints.title"));
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient.getSingleplayerServer() != null) {
            this.worldKey = "sp:" + minecraftClient.getSingleplayerServer().getWorldData().getLevelName();
        }
        else {
            ServerData serverData = minecraftClient.getCurrentServer();
            this.worldKey = "mp:" + (serverData != null ? serverData.ip : "unknown");
        }
        WaypointsListComponent.DimensionLabelFormatter dimensionLabelFormatter = this::formatDimension;
        this.declarativeMountHost = new DeclarativeMountHost((viewportWidth, viewportHeight) -> viewportView(viewportWidth, viewportHeight, dimensionLabelFormatter));
        host.setRoot(declarativeMountHost);
    }

    private UiView viewportView(float viewportWidth, float viewportHeight, WaypointsListComponent.DimensionLabelFormatter dimensionLabelFormatter) {
        return Ui.component(WaypointsRootComponent.class, WaypointsRootComponent::new,
                new WaypointsRootComponent.Props(
                        viewportWidth,
                        viewportHeight,
                        ModConfig.waypoints().getForWorld(worldKey),
                        this::openCreateScreen,
                        () -> Minecraft.getInstance().setScreen(null),
                        waypoint -> Minecraft.getInstance().setScreen(new WaypointEditScreen(waypoint)),
                        waypoint -> {
                            waypoint.setVisible(!waypoint.isVisible());
                            ModConfig.waypoints().save();
                        },
                        dimensionLabelFormatter));
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
        declarativeMountHost.dispose();
        host.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
