package cc.fascinated.fascinatedutils.debug.perf.core;

import cc.fascinated.fascinatedutils.systems.texture.TextureManager;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.Map;

/**
 * JIT compilation time at one moment, so a scene can report how much compilation happened while it was measured,
 * plus the direct and mapped buffer memory and the mod's loaded texture count at the end of that measurement.
 *
 * @param compilationMs total milliseconds spent JIT compiling so far, or -1 when the JVM does not report it
 */
public record PerfJvmSnapshot(long compilationMs) {
    private static final double BYTES_PER_MEGABYTE = 1024.0 * 1024.0;
    private static final String DIRECT_POOL = "direct";
    private static final String MAPPED_POOL = "mapped";

    /**
     * Reads the current JIT compilation total.
     *
     * @return the snapshot
     */
    public static PerfJvmSnapshot now() {
        CompilationMXBean compilation = ManagementFactory.getCompilationMXBean();
        boolean supported = compilation != null && compilation.isCompilationTimeMonitoringSupported();
        return new PerfJvmSnapshot(supported ? compilation.getTotalCompilationTime() : -1);
    }

    /**
     * Records the JIT time since this snapshot, plus the buffer memory and texture count right now, into report meta as
     * {@code jvm.<id>.jitMs}, {@code jvm.<id>.directMb}, {@code jvm.<id>.mappedMb} and {@code jvm.<id>.textures}.
     *
     * @param resultId the scene id, for example {@code scene.entities}
     * @param meta     the meta map to write into
     * @return a printable {@code jvm <id> jitMs=… directMb=… mappedMb=… textures=…} line
     */
    public String recordSince(String resultId, Map<String, String> meta) {
        PerfJvmSnapshot current = now();
        long jitMs = compilationMs < 0 || current.compilationMs() < 0 ? -1 : current.compilationMs() - compilationMs;
        double directMb = bufferPoolMegabytes(DIRECT_POOL);
        double mappedMb = bufferPoolMegabytes(MAPPED_POOL);
        long textures = TextureManager.INSTANCE.loadedTextures();
        meta.put("jvm." + resultId + ".jitMs", String.valueOf(jitMs));
        meta.put("jvm." + resultId + ".directMb", String.format(Locale.ROOT, "%.1f", directMb));
        meta.put("jvm." + resultId + ".mappedMb", String.format(Locale.ROOT, "%.1f", mappedMb));
        meta.put("jvm." + resultId + ".textures", String.valueOf(textures));
        return String.format(Locale.ROOT, "jvm %s jitMs=%d directMb=%.1f mappedMb=%.1f textures=%d", resultId, jitMs, directMb, mappedMb, textures);
    }

    private static double bufferPoolMegabytes(String poolName) {
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            if (pool.getName().equals(poolName)) {
                return pool.getMemoryUsed() / BYTES_PER_MEGABYTE;
            }
        }
        return -1;
    }
}
