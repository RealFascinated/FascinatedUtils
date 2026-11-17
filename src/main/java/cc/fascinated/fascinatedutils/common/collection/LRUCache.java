package cc.fascinated.fascinatedutils.common.collection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Bounded least-recently-used cache for a single thread. Use {@link SynchronizedLRUCache} when it is shared between
 * threads.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class LRUCache<K, V> {

    private final Map<K, V> map;

    /**
     * Creates a cache.
     *
     * @param maxSize the entry count above which the least recently used entry is evicted
     */
    public LRUCache(int maxSize) {
        this(maxSize, null);
    }

    /**
     * Creates a cache that reports evictions.
     *
     * @param maxSize the entry count above which the least recently used entry is evicted
     * @param onEvict called with each evicted entry, or null
     */
    public LRUCache(int maxSize, BiConsumer<K, V> onEvict) {
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                if (size() > maxSize) {
                    if (onEvict != null) {
                        onEvict.accept(eldest.getKey(), eldest.getValue());
                    }
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * @param key the key
     * @return the value, or null when absent; marks the entry as recently used
     */
    public V get(K key) {
        return map.get(key);
    }

    /**
     * Stores a value.
     *
     * @param key   the key
     * @param value the value
     */
    public void put(K key, V value) {
        map.put(key, value);
    }

    /**
     * Returns the cached value, computing and storing it when absent.
     *
     * @param key             the key
     * @param mappingFunction computes the value for an absent key
     * @return the cached or computed value
     */
    public V computeIfAbsent(K key, Function<K, V> mappingFunction) {
        return map.computeIfAbsent(key, mappingFunction);
    }

    /**
     * Removes an entry.
     *
     * @param key the key
     */
    public void remove(K key) {
        map.remove(key);
    }

    /**
     * Removes every entry.
     */
    public void clear() {
        map.clear();
    }

    /**
     * @return the number of entries
     */
    public int size() {
        return map.size();
    }
}
