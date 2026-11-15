package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlurModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.FogCustomizerModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.FreelookModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.FullbrightModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.HurtcamModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.TimeChangerModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.VibrancyModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.ZoomModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.clock.ClockWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.CoordinatesWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.days.DaysCounterWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.debug.DebugWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.fps.FpsWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.memory.MemoryWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.movement.MovementModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.MusicModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.ping.PingWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.speed.SpeedWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.systemcpu.SystemCpuUsageWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.tps.TpsWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.waypoint.WaypointLabelHudElement;

import java.util.List;

/**
 * The fixed camera over terrain with every module enabled and the HUD drawn. The baseline for every other scene.
 */
public class IdlePerfScene extends PerfScene {
    public static final String ID = "idle";

    public IdlePerfScene() {
        super(ID, "Fixed camera over terrain, every module enabled, HUD drawn");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("GameRendererMixin"), PerfCoverage.mixin("GuiRendererMixin"), PerfCoverage.mixin("GameRendererPostProcessorAccessorMixin"), PerfCoverage.mixin("ConnectionMixin"), PerfCoverage.mixin("blur.GameRendererMixin"), PerfCoverage.mixin("blur.GlobalSettingsUniformMixin"), PerfCoverage.mixin("fogcustomizer.AtmosphericFogModifierMixin"), PerfCoverage.mixin("fullbright.LightmapRenderStateExtractorMixin"), PerfCoverage.mixin("hurtcam.GameRendererMixin"), PerfCoverage.mixin("movement.ClientPlayerEntityMixin"), PerfCoverage.mixin("movement.PlayerEntityMixin"), PerfCoverage.mixin("music.MinecraftMusicMixin"), PerfCoverage.mixin("music.MusicManagerMixin"), PerfCoverage.mixin("timechanger.ClientClockManagerMixin"), PerfCoverage.mixin("zoom.CameraMixin"), PerfCoverage.mixin("zoom.HeldItemRendererMixin"), PerfCoverage.mixin("freelook.CameraFreelookMixin"), PerfCoverage.mixin("perf.GameRendererPerfMixin"), PerfCoverage.mixin("perf.MinecraftPerfMixin"),
                PerfCoverage.module(FpsWidget.class), PerfCoverage.module(ClockWidget.class), PerfCoverage.module(DaysCounterWidget.class), PerfCoverage.module(DebugWidget.class), PerfCoverage.module(CoordinatesWidget.class), PerfCoverage.module(PingWidget.class), PerfCoverage.module(MemoryWidget.class), PerfCoverage.module(SystemCpuUsageWidget.class), PerfCoverage.module(TpsWidget.class), PerfCoverage.module(SpeedWidget.class), PerfCoverage.module(MovementModule.class), PerfCoverage.module(FullbrightModule.class), PerfCoverage.module(HurtcamModule.class), PerfCoverage.module(FogCustomizerModule.class), PerfCoverage.module(TimeChangerModule.class), PerfCoverage.module(VibrancyModule.class), PerfCoverage.module(BlurModule.class), PerfCoverage.module(MusicModule.class),
                PerfCoverage.listener(RainbowColors.class), PerfCoverage.listener(VibrancyModule.class), PerfCoverage.listener(SpeedWidget.class), PerfCoverage.listener(TpsWidget.class), PerfCoverage.listener(FreelookModule.class), PerfCoverage.listener(ZoomModule.class), PerfCoverage.listener(WaypointLabelHudElement.class),
                PerfCoverage.system("hud"), PerfCoverage.system("modules"), PerfCoverage.system("TextureManager"), PerfCoverage.gui("render"), PerfCoverage.gui("core"), PerfCoverage.renderer("Renderer2D"), PerfCoverage.renderer("MeshBuilder"), PerfCoverage.renderer("MeshRenderer"), PerfCoverage.renderer("FascinatedUiPipelines"), PerfCoverage.renderer("RoundedRectCornerRadiiTexture"), PerfCoverage.renderer("RoundedRectTexRenderState"), PerfCoverage.renderer("AxisTexColorQuadRenderState"), PerfCoverage.renderer("text"),
                PerfCoverage.common("FrameCounter"), PerfCoverage.common("Colors"), PerfCoverage.common("TpsColors"), PerfCoverage.common("PingColors"), PerfCoverage.common("ByteUtils"), PerfCoverage.common("ValueSmoother"), PerfCoverage.common("IntegratedServerUtils"), PerfCoverage.common("NumberUtils"), PerfCoverage.common("StringUtils"), PerfCoverage.common("PlayerUtils"), PerfCoverage.common("LRUCache"), PerfCoverage.common("color"), PerfCoverage.common("culling"));
    }
}
