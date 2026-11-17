package cc.fascinated.fascinatedutils.systems.modules.impl.waypoint;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.render.ClientFovEvent;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.overlay.UiOverlay;
import cc.fascinated.fascinatedutils.gui.core.overlay.UiOverlays;
import cc.fascinated.fascinatedutils.gui.core.UiScale;
import cc.fascinated.fascinatedutils.gui.node.layout.PadNode;
import cc.fascinated.fascinatedutils.gui.node.display.RectNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WaypointLabelHudElement implements HudElement {
    public static final WaypointLabelHudElement INSTANCE = new WaypointLabelHudElement();

    private static final int LABEL_FILL_ARGB = 0xA0000000;
    private static final int LABEL_TEXT_ARGB = 0xFFFFFFFF;

    /**
     * A label projected onto the canvas.
     *
     * @param text            the label text
     * @param centerX         horizontal center in canvas pixels
     * @param centerY         vertical center in canvas pixels
     * @param borderArgb      border color, or zero for no border
     */
    private record ProjectedLabel(String text, float centerX, float centerY, int borderArgb) {}

    private final List<ProjectedLabel> labels = new ArrayList<>();
    private final UiOverlay overlay = UiOverlays.INSTANCE.register(new UiOverlay(stateStore -> composeLabels()).recomposeEveryFrame(true), UiOverlays.ORDER_WORLD);
    private volatile float lastFovDegrees = 70f;
    private float canvasWidth;
    private float canvasHeight;
    private int labelPadding;

    private WaypointLabelHudElement() {
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    @EventHandler
    private void onClientFov(ClientFovEvent event) {
        lastFovDegrees = event.fovDegrees();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker tickCounter) {
        Optional<WaypointsModule> enabledModule = ModuleRegistry.INSTANCE.getModule(WaypointsModule.class).filter(WaypointsModule::isEnabled);
        if (enabledModule.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        List<Waypoint> waypoints = WaypointRepository.getForCurrentWorldKey();
        if (waypoints.isEmpty()) {
            return;
        }

        canvasWidth = UiScale.uiWidth();
        canvasHeight = UiScale.uiHeight();
        WaypointsModule module = enabledModule.get();
        labelPadding = Math.round(module.getLabelPadding().getValue().floatValue());
        projectLabels(minecraft, module, waypoints);
        if (!labels.isEmpty()) {
            overlay.render(graphics, canvasWidth, canvasHeight, UiScale.uiPointerX(), UiScale.uiPointerY(), tickCounter.getRealtimeDeltaTicks() / 20f);
        }
    }

    /**
     * Projects the visible waypoints into canvas space, replacing the labels of the previous frame.
     */
    private void projectLabels(Minecraft minecraft, WaypointsModule module, List<Waypoint> waypoints) {
        labels.clear();
        String dimension = minecraft.level.dimension().identifier().toString();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.position();
        double playerX = minecraft.player.getX();
        double playerY = minecraft.player.getY();
        double playerZ = minecraft.player.getZ();

        // Inverse camera rotation transforms world-relative vectors into camera space
        Quaternionf inverseCameraRotation = new Quaternionf(camera.rotation()).conjugate();
        float tanHalfFovY = (float) Math.tan(Math.toRadians(lastFovDegrees * 0.5));
        float aspectRatio = (float) minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight();
        float tanHalfFovX = tanHalfFovY * aspectRatio;

        boolean labelOnLook = module.getLabelOnLook().isEnabled();
        Vec3 lookVector = labelOnLook ? minecraft.player.getLookAngle() : null;
        boolean showBorder = module.getShowBorder().isEnabled();

        for (Waypoint waypoint : waypoints) {
            if (!waypoint.isVisible() || !waypoint.getDimension().equals(dimension)) {
                continue;
            }

            if (labelOnLook) {
                double directionX = waypoint.getX() - playerX;
                double directionY = (waypoint.getY() + 0.9) - playerY;
                double directionZ = waypoint.getZ() - playerZ;
                double directionLength = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
                if (directionLength > 0 && (lookVector.x * directionX + lookVector.y * directionY + lookVector.z * directionZ) / directionLength < 0.866) {
                    continue;
                }
            }

            double relativeX = waypoint.getX() - cameraPosition.x;
            double relativeY = (waypoint.getY() + 1.8) - cameraPosition.y;
            double relativeZ = waypoint.getZ() - cameraPosition.z;

            // Transform world-relative position into camera space
            Vector3f cameraSpacePosition = new Vector3f((float) relativeX, (float) relativeY, (float) relativeZ);
            inverseCameraRotation.transform(cameraSpacePosition);

            // Minecraft camera looks along -Z; forward depth is positive when waypoint is in front
            float depth = -cameraSpacePosition.z;
            if (depth <= 0.1f) {
                continue;
            }

            float normalizedX = (cameraSpacePosition.x / depth) / tanHalfFovX;
            float normalizedY = (cameraSpacePosition.y / depth) / tanHalfFovY;

            // Skip if too far off-screen (small margin so partially-visible labels still draw)
            if (Math.abs(normalizedX) > 1.1f || Math.abs(normalizedY) > 1.1f) {
                continue;
            }

            // Normalized device coordinates to UI scale-2 screen coordinates
            float screenX = (normalizedX + 1.0f) * 0.5f * canvasWidth;
            float screenY = (1.0f - normalizedY) * 0.5f * canvasHeight;

            double distanceX = playerX - waypoint.getX();
            double distanceY = playerY - waypoint.getY();
            double distanceZ = playerZ - waypoint.getZ();
            double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);

            String label = waypoint.isShowDistance() ? waypoint.getName() + " [" + (int) Math.round(distance) + "m]" : waypoint.getName();
            labels.add(new ProjectedLabel(label, screenX, screenY, showBorder ? waypoint.getColor().getResolvedArgb() : 0));
        }
    }

    private UiNode composeLabels() {
        PositionedNode<?> layer = new PositionedNode<>().full();
        for (ProjectedLabel label : labels) {
            RectNode background = new RectNode().setFillArgb(LABEL_FILL_ARGB);
            if (label.borderArgb() != 0) {
                background.setBorderArgb(label.borderArgb());
            }
            background.columnGap(0).leftRel(label.centerX() / canvasWidth, 0, 0.5f).topRel(label.centerY() / canvasHeight, 0, 0.5f);
            background.addChild(new PadNode().padding(labelPadding).addChild(new TextNode(label::text).setColorArgb(LABEL_TEXT_ARGB)));
            layer.addChild(background);
        }
        return layer;
    }
}
