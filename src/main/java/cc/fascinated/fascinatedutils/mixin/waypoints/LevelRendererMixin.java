package cc.fascinated.fascinatedutils.mixin.waypoints;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.renderer.FascinatedWorldRenderTypes;
import cc.fascinated.fascinatedutils.renderer.WorldBoxFaces;
import cc.fascinated.fascinatedutils.systems.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.waypoint.WaypointsModule;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Unique
    private static final PerfProbe.Section ALUMITE$BEAMS = PerfProbe.INSTANCE.section("waypoints.beams");
    @Unique
    private static final int BEAM_TOP_Y = 320;
    @Unique
    private static final int BEAM_MAX_DISTANCE = 1000;
    @Unique
    private static final int BEAM_MAX_ALPHA = 160;
    @Unique
    private static final double BEAM_ALPHA_RAMP_DISTANCE = 100.0;

    @Inject(method = "renderBlockOutline", at = @At("RETURN"))
    private void alumite$renderWaypointBeams(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean onlyTranslucentBlocks, LevelRenderState levelRenderState, CallbackInfo ci) {
        long start = ALUMITE$BEAMS.begin();
        try {
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

            String dimension = minecraft.level.dimension().identifier().toString();
            List<Waypoint> waypoints = WaypointRepository.getForCurrentWorldKey();
            if (waypoints.isEmpty()) {
                return;
            }

            Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
            double playerX = minecraft.player.getX();
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
                if (beamDist <= 0 || beamDist > BEAM_MAX_DISTANCE) {
                    continue;
                }
                int beamAlpha = (int) Math.min(BEAM_MAX_ALPHA, BEAM_MAX_ALPHA * (beamDist / BEAM_ALPHA_RAMP_DISTANCE));
                if (beamAlpha <= 0) {
                    continue;
                }

                int argb = waypoint.getColor().getResolvedArgb();
                int beamArgb = (beamAlpha << 24) | (argb & 0xFFFFFF);

                float halfW = (float) Math.max(0.15, beamDist * 0.001);
                float wx = (float) (waypoint.getX() - cameraPos.x);
                float wz = (float) (waypoint.getZ() - cameraPos.z);
                float minX = wx - halfW;
                float maxX = wx + halfW;
                float minZ = wz - halfW;
                float maxZ = wz + halfW;
                float minY = (float) (waypoint.getY() - cameraPos.y);
                float maxY = (float) (BEAM_TOP_Y - cameraPos.y);

                WorldBoxFaces.emitBoxFaces(beamConsumer, rootPose, minX, minY, minZ, maxX, maxY, maxZ, beamArgb, false);
            }

            bufferSource.endBatch(FascinatedWorldRenderTypes.WORLD_BEAM);
        } finally {
            ALUMITE$BEAMS.end(start);
        }
    }
}
