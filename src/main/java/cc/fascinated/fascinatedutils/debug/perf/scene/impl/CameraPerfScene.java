package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.gui.input.MouseClickRateTracker;
import cc.fascinated.fascinatedutils.mixin.perf.MouseHandlerPerfInvoker;
import cc.fascinated.fascinatedutils.systems.modules.impl.FreelookModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.ZoomModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.cps.CpsWidget;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Zoom and freelook held while real mouse movement, scrolling and right clicks go through the vanilla mouse handler.
 */
public class CameraPerfScene extends PerfScene {
    public static final String ID = "camera";
    private static final String ZOOM_KEY = "key.alumite.zoom";
    private static final String FREELOOK_KEY = "key.alumite.freelook";
    private static final double MOUSE_DELTA = 4.0;
    private static final double MOVEMENT_TIME = 0.05;
    private static final int SCROLL_EVERY_TICKS = 10;
    private static final int CLICK_EVERY_TICKS = 5;

    public CameraPerfScene() {
        super(ID, "Zoom and freelook held, mouse moved every tick, scroll and right click through the mouse handler");
    }

    @Override
    public void setup(PerfSceneContext context) {
        context.holdKey(KeyMapping.get(ZOOM_KEY));
        context.holdKey(KeyMapping.get(FREELOOK_KEY));
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        Minecraft minecraft = context.getMinecraft();
        MouseHandlerPerfInvoker mouseHandler = (MouseHandlerPerfInvoker) minecraft.mouseHandler;
        long windowHandle = minecraft.getWindow().handle();
        double direction = tick % 2 == 0 ? 1.0 : -1.0;
        mouseHandler.alumite$setAccumulatedDX(MOUSE_DELTA * direction);
        mouseHandler.alumite$setAccumulatedDY(MOUSE_DELTA * direction);
        mouseHandler.alumite$turnPlayer(MOVEMENT_TIME);
        if (tick % SCROLL_EVERY_TICKS == 0) {
            mouseHandler.alumite$onScroll(windowHandle, 0.0, direction);
        }
        if (tick % CLICK_EVERY_TICKS == 0) {
            MouseButtonInfo rightButton = new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_RIGHT, 0);
            mouseHandler.alumite$onButton(windowHandle, rightButton, GLFW.GLFW_PRESS);
            mouseHandler.alumite$onButton(windowHandle, rightButton, GLFW.GLFW_RELEASE);
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("MouseHandlerMixin"), PerfCoverage.mixin("zoom.MouseHandlerMixin"), PerfCoverage.mixin("freelook.MouseHandlerFreelookMixin"), PerfCoverage.mixin("zoom.CameraMixin"), PerfCoverage.mixin("freelook.CameraFreelookMixin"), PerfCoverage.mixin("perf.MouseHandlerPerfInvoker"), PerfCoverage.mixin("KeyBindingAccessorMixin"), PerfCoverage.module(ZoomModule.class), PerfCoverage.module(FreelookModule.class), PerfCoverage.module(CpsWidget.class), PerfCoverage.listener(ZoomModule.class), PerfCoverage.listener(FreelookModule.class), PerfCoverage.listener(MouseClickRateTracker.class), PerfCoverage.gui("input"), PerfCoverage.keybind(ZOOM_KEY), PerfCoverage.keybind(FREELOOK_KEY));
    }
}
