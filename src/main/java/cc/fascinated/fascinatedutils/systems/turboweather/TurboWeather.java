package cc.fascinated.fascinatedutils.systems.turboweather;

import cc.fascinated.fascinatedutils.settings.Settings;
import net.minecraft.world.level.Level;

import lombok.Getter;

import java.lang.ref.WeakReference;
import java.util.Arrays;

public class TurboWeather {
    private static final int TABLE_BITS = 6;
    private static final int TABLE_MASK = (1 << TABLE_BITS) - 1;
    private static final int TABLE_SIZE = 1 << (TABLE_BITS * 2);
    // A column's terrain data is reused for this many ticks; each column refreshes on its own offset so the work is spread out.
    private static final int REFRESH_TICKS = 10;

    private final ColumnTable heights = new ColumnTable();
    private final ColumnTable precipitations = new ColumnTable();
    private final ColumnTable lights = new ColumnTable();

    private WeakReference<Level> cachedLevel = new WeakReference<>(null);
    private long gameTime;
    private boolean enabled;

    /**
     * Prepares the cache for one weather extraction pass.
     *
     * @param level the level the weather is extracted for
     */
    public void beginFrame(Level level) {
        enabled = Settings.INSTANCE.getTurboWeather().isEnabled();
        if (!enabled) {
            return;
        }
        heights.snapshotAndReset();
        precipitations.snapshotAndReset();
        lights.snapshotAndReset();
        gameTime = level.getGameTime();
        if (cachedLevel.get() != level) {
            cachedLevel = new WeakReference<>(level);
            heights.clear();
            precipitations.clear();
            lights.clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ColumnTable heights() {
        return heights;
    }

    public ColumnTable precipitations() {
        return precipitations;
    }

    public ColumnTable lights() {
        return lights;
    }

    /**
     * Direct-mapped cache of one int per world column, keyed by the packed position and a refresh bucket.
     */
    public class ColumnTable {
        private final long[] keys = new long[TABLE_SIZE];
        private final int[] buckets = new int[TABLE_SIZE];
        private final int[] values = new int[TABLE_SIZE];
        private int slot;
        private int bucket;
        private long key;
        private int found;
        private int lookups;
        private int hits;
        @Getter
        private int lastLookups;
        @Getter
        private int lastHits;

        private ColumnTable() {
            clear();
        }

        private void snapshotAndReset() {
            lastLookups = lookups;
            lastHits = hits;
            lookups = 0;
            hits = 0;
        }

        private void clear() {
            Arrays.fill(buckets, -1);
        }

        /**
         * Looks up a cached value; on a miss the caller computes it and passes it to {@link #store(int)}.
         *
         * @param x       the block x
         * @param z       the block z
         * @param packed  the packed position the value depends on
         * @return whether a fresh value was found, readable with {@link #found()}
         */
        public boolean find(int x, int z, long packed) {
            slot = (x & TABLE_MASK) | ((z & TABLE_MASK) << TABLE_BITS);
            bucket = (int) ((gameTime + slot % REFRESH_TICKS) / REFRESH_TICKS);
            key = packed;
            lookups++;
            if (buckets[slot] == bucket && keys[slot] == key) {
                hits++;
                found = values[slot];
                return true;
            }
            return false;
        }

        public int found() {
            return found;
        }

        /**
         * Stores the value computed after a missed {@link #find(int, int, long)}.
         *
         * @param value the computed value
         * @return the same value
         */
        public int store(int value) {
            keys[slot] = key;
            buckets[slot] = bucket;
            values[slot] = value;
            return value;
        }
    }
}
