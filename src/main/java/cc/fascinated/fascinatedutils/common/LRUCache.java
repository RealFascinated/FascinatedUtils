package cc.fascinated.fascinatedutils.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class LRUCache<K, V> {

    private final Map<K, V> map;

    public LRUCache(int maxSize) {
        this(maxSize, null);
    }

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

    public synchronized V get(K key) {
        return map.get(key);
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    public synchronized V computeIfAbsent(K key, java.util.function.Function<K, V> mappingFunction) {
        return map.computeIfAbsent(key, mappingFunction);
    }

    public synchronized void remove(K key) {
        map.remove(key);
    }

    public synchronized void clear() {
        map.clear();
    }

    public synchronized int size() {
        return map.size();
    }
}
