package cc.fascinated.fascinatedutils.mixin.waypoints;

import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.waypoint.WaypointsModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.LevelRenderer;
import cc.fascinated.fascinatedutils.renderer.FascinatedWorldRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderBlockOutline", at = @At("RETURN"))
    private void fascinatedutils$renderWaypointBeams(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean onlyTranslucentBlocks, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (!onlyTranslucentBlocks) {
            return;
        }

        Optional<WaypointsModule> opt = ModuleRegistry.INSTANCE.getModule(WaypointsModule.class);
        if (opt.isEmpty() || !opt.get().isEnabled() || !opt.get().getShowBeam().isEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        String worldKey;
        if (minecraft.getSingleplayerServer() != null) {
            worldKey = "sp:" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        }
        else {
            ServerData serverData = minecraft.getCurrentServer();
            worldKey = "mp:" + (serverData != null ? serverData.ip : "unknown");
        }

        String dimension = minecraft.level.dimension().identifier().toString();
        List<Waypoint> waypoints = ModConfig.waypoints().getForWorld(worldKey);
        if (waypoints.isEmpty()) {
            return;
        }

        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double playerX = minecraft.player.getX();
        double playerY = minecraft.player.getY();
        double playerZ = minecraft.player.getZ();

        VertexConsumer beamConsumer = bufferSource.getBuffer(FascinatedWorldRenderTypes.WORLD_BEAM);
        PoseStack.Pose rootPose = poseStack.last();

        for (Waypoint waypoint : waypoints) {
            if (!waypoint.isVisible() || !waypoint.isShowBeam() || !waypoint.getDimension().equals(dimension)) {
                continue;
            }

            double beamDistX = playerX - waypoint.getX();
            double beamDistZ = playerZ - waypoint.getZ();
            double beamDist = Math.sqrt(beamDistX * beamDistX + beamDistZ * beamDistZ);
            if (beamDist <= 0 || beamDist > 1000) {
                continue;
            }
            int beamAlpha = (int) Math.min(160, 160 * (beamDist / 100.0));
            if (beamAlpha <= 0) {
                continue;
            }

            int argb = waypoint.getColor().getResolvedArgb();
            int red = (argb >> 16) & 0xFF;
            int green = (argb >> 8) & 0xFF;
            int blue = argb & 0xFF;

            float halfW = (float) Math.max(0.15, beamDist * 0.001);
            float wx = (float) (waypoint.getX() - cameraPos.x);
            float wz = (float) (waypoint.getZ() - cameraPos.z);
            float minX = wx - halfW;
            float maxX = wx + halfW;
            float minZ = wz - halfW;
            float maxZ = wz + halfW;
            float minY = (float) (waypoint.getY() - cameraPos.y);
            float maxY = (float) (320 - cameraPos.y);

            // -Z (north)
            beamConsumer.addVertex(rootPose, minX, minY, minZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, minX, maxY, minZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, maxX, maxY, minZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, maxX, minY, minZ).setColor(red, green, blue, beamAlpha);
            // +Z (south)
            beamConsumer.addVertex(rootPose, minX, minY, maxZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, maxX, minY, maxZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, maxX, maxY, maxZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, minX, maxY, maxZ).setColor(red, green, blue, beamAlpha);
            // -X (west)
            beamConsumer.addVertex(rootPose, minX, minY, minZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, minX, minY, maxZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, minX, maxY, maxZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, minX, maxY, minZ).setColor(red, green, blue, beamAlpha);
            // +X (east)
            beamConsumer.addVertex(rootPose, maxX, minY, minZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, maxX, maxY, minZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, maxX, maxY, maxZ).setColor(red, green, blue, beamAlpha);
            beamConsumer.addVertex(rootPose, maxX, minY, maxZ).setColor(red, green, blue, beamAlpha);
        }

        bufferSource.endBatch(FascinatedWorldRenderTypes.WORLD_BEAM);
    }
}