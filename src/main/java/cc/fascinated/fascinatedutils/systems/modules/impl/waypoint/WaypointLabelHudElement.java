package cc.fascinated.fascinatedutils.systems.modules.impl.waypoint;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.render.ClientFovEvent;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public class WaypointLabelHudElement implements HudElement {
    public static final WaypointLabelHudElement INSTANCE = new WaypointLabelHudElement();

    private volatile float lastFovDegrees = 70f;

    private WaypointLabelHudElement() {
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    @EventHandler
    private void onClientFov(ClientFovEvent event) {
        lastFovDegrees = event.fovDegrees();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker tickCounter) {
        Optional<WaypointsModule> opt = ModuleRegistry.INSTANCE.getModule(WaypointsModule.class);
        if (opt.isEmpty() || !opt.get().isEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        String worldKey;
        if (minecraft.getSingleplayerServer() != null) {
            worldKey = "sp:" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            ServerData serverData = minecraft.getCurrentServer();
            worldKey = "mp:" + (serverData != null ? serverData.ip : "unknown");
        }

        String dimension = minecraft.level.dimension().identifier().toString();
        List<Waypoint> waypoints = ModConfig.waypoints().getForWorld(worldKey);
        if (waypoints.isEmpty()) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        double playerX = minecraft.player.getX();
        double playerY = minecraft.player.getY();
        double playerZ = minecraft.player.getZ();

        // Inverse camera rotation transforms world-relative vectors into camera space
        Quaternionf invCamRot = new Quaternionf(camera.rotation()).conjugate();
        float tanHalfFovY = (float) Math.tan(Math.toRadians(lastFovDegrees * 0.5));
        float aspectRatio = (float) minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight();
        float tanHalfFovX = tanHalfFovY * aspectRatio;

        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();

        WaypointsModule module = opt.get();
        boolean labelOnLook = module.getLabelOnLook().isEnabled();
        Vec3 lookVec = labelOnLook ? minecraft.player.getLookAngle() : null;
        boolean showBorder = module.getShowBorder().isEnabled();
        float labelPad = module.getLabelPadding().getValue().floatValue();

        GuiRenderer guiRenderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
        guiRenderer.begin(uiWidth, uiHeight);
        float fontHeight = guiRenderer.getFontHeight();

        for (Waypoint waypoint : waypoints) {
            if (!waypoint.isVisible() || !waypoint.getDimension().equals(dimension)) {
                continue;
            }

            if (labelOnLook) {
                double dirX = waypoint.getX() - playerX;
                double dirY = (waypoint.getY() + 0.9) - playerY;
                double dirZ = waypoint.getZ() - playerZ;
                double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                if (len > 0 && (lookVec.x * dirX + lookVec.y * dirY + lookVec.z * dirZ) / len < 0.866) {
                    continue;
                }
            }

            double relX = waypoint.getX() - cameraPos.x;
            double relY = (waypoint.getY() + 1.8) - cameraPos.y;
            double relZ = waypoint.getZ() - cameraPos.z;

            // Transform world-relative position into camera space
            Vector3f camVec = new Vector3f((float) relX, (float) relY, (float) relZ);
            invCamRot.transform(camVec);

            // Minecraft camera looks along -Z; forward depth is positive when waypoint is in front
            float depth = -camVec.z;
            if (depth <= 0.1f) {
                continue;
            }

            float ndcX = (camVec.x / depth) / tanHalfFovX;
            float ndcY = (camVec.y / depth) / tanHalfFovY;

            // Skip if too far off-screen (small margin so partially-visible labels still draw)
            if (Math.abs(ndcX) > 1.1f || Math.abs(ndcY) > 1.1f) {
                continue;
            }

            // NDC → UI scale-2 screen coordinates
            float screenX = (ndcX + 1.0f) * 0.5f * uiWidth;
            float screenY = (1.0f - ndcY) * 0.5f * uiHeight;

            double distX = playerX - waypoint.getX();
            double distY = playerY - waypoint.getY();
            double distZ = playerZ - waypoint.getZ();
            double dist = Math.sqrt(distX * distX + distY * distY + distZ * distZ);

            String label = waypoint.isShowDistance()
                    ? waypoint.getName() + " [" + (int) Math.round(dist) + "m]"
                    : waypoint.getName();

            int textWidth = guiRenderer.measureTextWidth(label, false);
            float bgW = textWidth + labelPad * 2f;
            float bgH = fontHeight + labelPad * 2f;
            float bgX = (float) Math.round(screenX - bgW * 0.5f);
            float bgY = (float) Math.round(screenY - bgH * 0.5f);

            guiRenderer.drawRect(bgX, bgY, bgW, bgH, 0xA0000000);
            if (showBorder) {
                guiRenderer.drawBorder(bgX, bgY, bgW, bgH, 1f, waypoint.getColor().getResolvedArgb());
            }
            guiRenderer.drawText(label, bgX + labelPad, bgY + labelPad, 0xFFFFFFFF, false, false);
        }

        guiRenderer.end();
    }
}
