package cc.fascinated.fascinatedutils.debug;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.util.ByteUtils;
import cc.fascinated.fascinatedutils.common.timing.ValueSmoother;
import cc.fascinated.fascinatedutils.mixin.accessor.DebugScreenEntriesInvokerMixin;
import cc.fascinated.fascinatedutils.systems.texture.TextureManager;
import cc.fascinated.fascinatedutils.systems.turboentities.EntitiesCullTask;
import cc.fascinated.fascinatedutils.systems.turboentities.TurboEntities;
import cc.fascinated.fascinatedutils.systems.turboparticles.ParticleCullTask;
import cc.fascinated.fascinatedutils.systems.turboparticles.TurboParticles;
import cc.fascinated.fascinatedutils.systems.turboweather.TurboWeather;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleFunction;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AlumiteDebugScreenEntry implements DebugScreenEntry {
    private static final DebugEntryCategory CATEGORY = new DebugEntryCategory(Component.literal("Alumite"), 3.0F);
    private static final Identifier GROUP = Identifier.fromNamespaceAndPath("alumite", "debug");
    private static final String PREFIX = "§7[Alumite]§r ";
    private static final String DISABLED = "§7Disabled";
    private static final String STOPPED = "§cStopped";

    private static final ValueSmoother HUD_CPU_MS = new ValueSmoother(500);
    private static final ValueSmoother HUD_GPU_MS = new ValueSmoother(500);
    private static final ValueSmoother ENTITY_PASS_MS = new ValueSmoother(500);
    private static final ValueSmoother ENTITY_SNAPSHOT_MS = new ValueSmoother(500);
    private static final ValueSmoother PARTICLE_PASS_MS = new ValueSmoother(500);

    private final DoubleFunction<List<String>> lineBuilder;
    private long lastDisplayNanos = System.nanoTime();

    /**
     * Registers every Alumite section with vanilla so each shows as its own toggle in the F3 overlay and the debug options screen.
     */
    public static void register() {
        registerEntry("alumite_hud", AlumiteDebugScreenEntry::hudLines);
        registerEntry("alumite_turbo_entities", AlumiteDebugScreenEntry::turboEntitiesLines);
        registerEntry("alumite_turbo_particles", AlumiteDebugScreenEntry::turboParticlesLines);
        registerEntry("alumite_turbo_weather", AlumiteDebugScreenEntry::turboWeatherLines);
        registerEntry("alumite_textures", AlumiteDebugScreenEntry::textureLines);
    }

    private static void registerEntry(String path, DoubleFunction<List<String>> lineBuilder) {
        Identifier entryId = Identifier.fromNamespaceAndPath("alumite", path);
        DebugScreenEntriesInvokerMixin.invokeRegister(entryId, new AlumiteDebugScreenEntry(lineBuilder));
    }

    private static String formatStat(int numerator, int denominator) {
        return numerator + "/" + denominator;
    }

    private static List<String> hudLines(double deltaSeconds) {
        HudFrameProfiler profiler = HudFrameProfiler.INSTANCE;
        double cpuMs = HUD_CPU_MS.smooth((profiler.getLastBuildNanos() + profiler.getLastGuiPassNanos()) / 1_000_000.0, deltaSeconds);
        String gpu = "n/a";
        if (profiler.isGpuSupported() && profiler.getLastGpuNanos() >= 0L) {
            gpu = String.format(Locale.ENGLISH, "%.2f ms", HUD_GPU_MS.smooth(profiler.getLastGpuNanos() / 1_000_000.0, deltaSeconds));
        }
        // The HUD is hidden while the overlay is open, so its figures are the last ones measured before it opened.
        String stale = Minecraft.getInstance().debugEntries.isOverlayVisible() ? " (last)" : "";
        return List.of(String.format(Locale.ENGLISH, PREFIX + "HUD %.2f ms cpu · %s gpu%s", cpuMs, gpu, stale));
    }

    private static List<String> turboEntitiesLines(double deltaSeconds) {
        TurboEntities turboEntities = Client.TURBO_ENTITIES;
        EntitiesCullTask cullTask = turboEntities.getCullTask();
        if (cullTask == null) {
            ENTITY_PASS_MS.reset();
            ENTITY_SNAPSHOT_MS.reset();
            return List.of(PREFIX + "Turbo Entities " + DISABLED);
        }
        int consideredBlockEntities = cullTask.getConsideredBlockEntityCount();
        int visibleBlockEntities = consideredBlockEntities - cullTask.getCulledBlockEntityCount();
        int consideredEntities = cullTask.getConsideredEntityCount();
        int visibleEntities = consideredEntities - cullTask.getCulledEntityCount();
        int consideredFrames = turboEntities.itemFrameCounters.getLastConsidered();
        int consideredSigns = turboEntities.signCounters.getLastConsidered();
        int skipped = turboEntities.getLastSkippedEntityTicks();
        int total = turboEntities.getLastTickedEntities() + skipped;
        double passMs = ENTITY_PASS_MS.smooth(cullTask.getLastPassNanos() / 1_000_000.0, deltaSeconds);
        double snapshotMs = ENTITY_SNAPSHOT_MS.smooth(turboEntities.getLastSnapshotNanos() / 1_000_000.0, deltaSeconds);
        return List.of(PREFIX + "Turbo Entities E " + formatStat(visibleEntities, consideredEntities) + " BE " + formatStat(visibleBlockEntities, consideredBlockEntities) + " Frames " + formatStat(consideredFrames - turboEntities.itemFrameCounters.getLastCulled(), consideredFrames) + " Signs " + formatStat(consideredSigns - turboEntities.signCounters.getLastCulled(), consideredSigns),
                String.format(Locale.ENGLISH, PREFIX + "Turbo Entities Tick %s Models %d Pass %.2f ms Snapshot %.2f ms %s", formatStat(total - skipped, total), turboEntities.getBlockModelCache().size(), passMs, snapshotMs, turboEntities.isCullThreadAlive() ? "§aOK" : STOPPED));
    }

    private static List<String> turboParticlesLines(double deltaSeconds) {
        TurboParticles particles = Client.TURBO_PARTICLES;
        ParticleCullTask particleCullTask = particles.getCullTask();
        if (particleCullTask == null) {
            PARTICLE_PASS_MS.reset();
            return List.of(PREFIX + "Turbo Particles " + DISABLED);
        }
        int consideredParticles = particleCullTask.getConsideredParticleCount();
        int visibleParticles = consideredParticles - particleCullTask.getCulledParticleCount();
        double passMs = PARTICLE_PASS_MS.smooth(particleCullTask.getLastPassNanos() / 1_000_000.0, deltaSeconds);
        return List.of(String.format(Locale.ENGLISH, PREFIX + "Turbo Particles %s Pass %.2f ms %s", formatStat(visibleParticles, consideredParticles), passMs, particles.isCullThreadAlive() ? "§aOK" : STOPPED));
    }

    private static List<String> turboWeatherLines(double deltaSeconds) {
        TurboWeather weather = Client.TURBO_WEATHER;
        if (!weather.isEnabled()) {
            return List.of(PREFIX + "Turbo Weather " + DISABLED);
        }
        TurboWeather.ColumnTable heights = weather.heights();
        TurboWeather.ColumnTable precipitations = weather.precipitations();
        TurboWeather.ColumnTable lights = weather.lights();
        return List.of(PREFIX + "Turbo Weather Height " + formatStat(heights.getLastHits(), heights.getLastLookups()) + " Precip " + formatStat(precipitations.getLastHits(), precipitations.getLastLookups()) + " Light " + formatStat(lights.getLastHits(), lights.getLastLookups()));
    }

    private static List<String> textureLines(double deltaSeconds) {
        return List.of(PREFIX + "Cached Textures " + TextureManager.INSTANCE.loadedTextures() + " (" + ByteUtils.formatBytes(TextureManager.INSTANCE.loadedTextureTotalSize(), 2) + ")");
    }

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        long nowNanos = System.nanoTime();
        double deltaSeconds = (nowNanos - lastDisplayNanos) / 1_000_000_000.0;
        lastDisplayNanos = nowNanos;
        displayer.addToGroup(GROUP,lineBuilder.apply(deltaSeconds));
    }

    @Override
    public DebugEntryCategory category() {
        return CATEGORY;
    }
}
