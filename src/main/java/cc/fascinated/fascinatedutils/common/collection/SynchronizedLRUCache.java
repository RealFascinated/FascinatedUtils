package cc.fascinated.fascinatedutils.common.collection;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * {@link LRUCache} whose operations are synchronized, for caches shared between threads.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class SynchronizedLRUCache<K, V> extends LRUCache<K, V> {

    /**
     * Creates a cache.
     *
     * @param maxSize the entry count above which the least recently used entry is evicted
     */
    public SynchronizedLRUCache(int maxSize) {
        super(maxSize);
    }

    /**
     * Creates a cache that reports evictions.
     *
     * @param maxSize the entry count above which the least recently used entry is evicted
     * @param onEvict called with each evicted entry, or null
     */
    public SynchronizedLRUCache(int maxSize, BiConsumer<K, V> onEvict) {
        super(maxSize, onEvict);
    }

    @Override
    public synchronized V get(K key) {
        return super.get(key);
    }

    @Override
    public synchronized void put(K key, V value) {
        super.put(key, value);
    }

    @Override
    public synchronized V computeIfAbsent(K key, Function<K, V> mappingFunction) {
        return super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public synchronized void remove(K key) {
        super.remove(key);
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

    @Override
    public synchronized int size() {
        return super.size();
    }
}
